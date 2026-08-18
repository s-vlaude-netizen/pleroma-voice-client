package de.peroma.voice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * The whole app, driven by voice.
 *
 * Speech output and speech input strictly take turns: the engine speaks, then
 * the microphone opens, then it speaks again. Running both at once would make
 * the recognizer hear the app's own voice, so there is exactly one owner of
 * the audio at any moment.
 *
 * Everything lives in a foreground service holding a partial wake lock, so the
 * session keeps working after the screen is switched off — which is the point:
 * start the app, put the phone in your pocket, talk to it.
 */
class VoiceService : Service() {

    /** What the session is currently doing — decides how speech is interpreted. */
    enum class Stage { IDLE, MENU, READING, DICTATING, CONFIRMING }

    /** What happens once the current utterance has finished playing. */
    private enum class After {
        NOTHING,
        LISTEN_COMMAND,
        LISTEN_BETWEEN_POSTS,
        NEXT_POST,
        LISTEN_DICTATION,
        LISTEN_CONFIRM,
        STOP_SESSION
    }

    private lateinit var prefs: Prefs
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingAction: String? = null
    private var speechRate = 1.0f

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var listenTimeout: Runnable? = null
    private var listenStart: Runnable? = null

    private var toneGenerator: ToneGenerator? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var stage = Stage.IDLE
    private var posts: List<Post> = emptyList()
    private var index = 0
    private var loading = false
    private var silenceStreak = 0
    private var dictationRetries = 0
    private var confirmMisses = 0
    private var draft: String = ""

    private var utteranceCounter = 0
    private var awaitedUtterance: String? = null
    private var afterSpeech: After = After.NOTHING

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        speechRate = prefs.speechRate
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            null
        }
        createNotificationChannel()
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                publish("Sprachausgabe nicht verfügbar.")
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            TtsSetup.applyGermanVoice(engine)
            engine.setSpeechRate(speechRate)
            engine.setOnUtteranceProgressListener(utteranceListener)
            ttsReady = true
            mainHandler.post {
                pendingAction?.let { queued ->
                    pendingAction = null
                    dispatch(queued)
                }
            }
        }
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            mainHandler.post {
                if (utteranceId != null && utteranceId == awaitedUtterance) {
                    awaitedUtterance = null
                    val next = afterSpeech
                    afterSpeech = After.NOTHING
                    runAfterSpeech(next)
                }
            }
        }

        override fun onError(utteranceId: String?) {
            mainHandler.post {
                if (utteranceId != null && utteranceId == awaitedUtterance) {
                    awaitedUtterance = null
                    afterSpeech = After.NOTHING
                    // Do not strand the session in silence.
                    listenForCommand()
                }
            }
        }
    }

    // ---- service entry points ---------------------------------------------

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(buildNotification(lastStatus))

        val action = intent?.action ?: return START_NOT_STICKY
        if (action == ACTION_STOP_SESSION) {
            endSession(spokenFarewell = false)
            return START_NOT_STICKY
        }
        if (!prefs.isLoggedIn) {
            publish("Nicht angemeldet.")
            return START_NOT_STICKY
        }

        acquireWakeLock()
        if (!ttsReady) {
            // The engine is still starting up; replay the request once it is.
            pendingAction = action
            publish("Sprachausgabe wird vorbereitet …")
            return START_NOT_STICKY
        }
        dispatch(action)
        return START_NOT_STICKY
    }

    private fun dispatch(action: String) {
        when (action) {
            ACTION_START_SESSION -> startSession()
            ACTION_READ_TIMELINE -> loadAndRead()
            ACTION_NEXT -> nextPost(auto = false)
            ACTION_PREVIOUS -> previousPost()
            ACTION_REPEAT -> speakCurrentPost()
            ACTION_NEW_POST -> promptForDictation()
            else -> Unit
        }
    }

    private fun startSession() {
        acquireWakeLock()
        silenceStreak = 0
        stage = Stage.MENU
        speak(
            "Sprachsteuerung aktiv. Du kannst den Bildschirm jetzt ausschalten. " +
                "Sag Timeline vorlesen, Neuer Beitrag, oder Hilfe.",
            After.LISTEN_COMMAND
        )
    }

    // ---- speaking ----------------------------------------------------------

    private fun speak(text: String, after: After) {
        val engine = tts ?: return
        cancelListening()
        requestAudioFocus()

        afterSpeech = after
        val chunks = chunk(text)
        utteranceCounter++
        chunks.forEachIndexed { position, part ->
            val id = "u$utteranceCounter-$position"
            val mode = if (position == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(part, mode, null, id)
            if (position == chunks.lastIndex) awaitedUtterance = id
        }
        publish(text.take(120))
    }

    private fun runAfterSpeech(after: After) {
        when (after) {
            After.NOTHING -> Unit
            After.LISTEN_COMMAND -> listenForCommand()
            After.LISTEN_BETWEEN_POSTS -> listenBetweenPosts()
            After.NEXT_POST -> nextPost(auto = true)
            After.LISTEN_DICTATION -> listenForDictation()
            After.LISTEN_CONFIRM -> listenForConfirmation()
            After.STOP_SESSION -> endSession(spokenFarewell = false)
        }
    }

    /** TTS engines reject very long strings; split on sentence boundaries. */
    private fun chunk(text: String): List<String> {
        val limit = 1000
        if (text.length <= limit) return listOf(text)
        val chunks = ArrayList<String>()
        var remaining = text
        while (remaining.length > limit) {
            val window = remaining.substring(0, limit)
            var cut = window.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'))
            if (cut < limit / 2) cut = window.lastIndexOf(' ')
            if (cut <= 0) cut = limit - 1
            chunks.add(remaining.substring(0, cut + 1).trim())
            remaining = remaining.substring(cut + 1).trim()
        }
        if (remaining.isNotEmpty()) chunks.add(remaining)
        return chunks
    }

    // ---- listening ---------------------------------------------------------

    private fun listenForCommand() {
        stage = Stage.MENU
        listen(COMMAND_WINDOW_MS)
    }

    private fun listenBetweenPosts() {
        stage = Stage.READING
        listen(BETWEEN_POSTS_WINDOW_MS)
    }

    private fun listenForDictation() {
        stage = Stage.DICTATING
        listen(DICTATION_WINDOW_MS)
    }

    private fun listenForConfirmation() {
        stage = Stage.CONFIRMING
        listen(COMMAND_WINDOW_MS)
    }

    private fun listen(windowMs: Long) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak(
                "Auf diesem Gerät ist keine Spracherkennung eingerichtet. " +
                    "Die Sprachsteuerung wird beendet.",
                After.STOP_SESSION
            )
            return
        }

        cancelListening()
        abandonAudioFocus()

        val speech = recognizer ?: SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(recognitionListener)
            recognizer = it
        }

        beep(ToneGenerator.TONE_PROP_BEEP)
        listening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMANY.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        // Open the microphone only once the earcon has died away. Starting it
        // immediately means the recognizer hears our own beep and reports that
        // it understood nothing.
        val start = Runnable {
            if (!listening) return@Runnable
            try {
                speech.startListening(intent)
            } catch (e: Exception) {
                listening = false
                retryAfterRecognizerTrouble()
                return@Runnable
            }

            // The engine's own silence timeout is only a hint, so close the
            // window ourselves — otherwise the pause between posts drags on.
            val timeout = Runnable {
                if (listening) {
                    listening = false
                    try {
                        recognizer?.cancel()
                    } catch (e: Exception) {
                        // Nothing useful to do; treat it as silence below.
                    }
                    handleSilence()
                }
            }
            listenTimeout = timeout
            mainHandler.postDelayed(timeout, windowMs)
        }
        listenStart = start
        mainHandler.postDelayed(start, BEEP_SETTLE_MS)
    }

    /**
     * A recognizer that is busy or broken fails almost instantly. Without a
     * pause the session would retry in a tight loop, burn through the silence
     * budget in a fraction of a second and shut itself down in a burst of beeps.
     */
    private fun retryAfterRecognizerTrouble() {
        mainHandler.postDelayed({ handleSilence() }, RECOGNIZER_RETRY_DELAY_MS)
    }

    private fun cancelListening() {
        listenStart?.let { mainHandler.removeCallbacks(it) }
        listenStart = null
        listenTimeout?.let { mainHandler.removeCallbacks(it) }
        listenTimeout = null
        if (listening) {
            listening = false
            try {
                recognizer?.cancel()
            } catch (e: Exception) {
                // Ignored: we are tearing the turn down anyway.
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() {
            // Speech started: let the user finish rather than cutting them off.
            listenTimeout?.let { mainHandler.removeCallbacks(it) }
            listenTimeout = null
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (!listening) return
            listening = false
            listenTimeout?.let { mainHandler.removeCallbacks(it) }
            listenTimeout = null
            mainHandler.post {
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        speak(
                            "Mir fehlt die Freigabe für das Mikrofon. Bitte in der App erteilen.",
                            After.STOP_SESSION
                        )

                    // The user simply said nothing — carry on straight away.
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> handleSilence()

                    // Busy, client or audio errors come back instantly, so
                    // back off before trying again.
                    else -> retryAfterRecognizerTrouble()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            if (!listening) return
            listening = false
            listenTimeout?.let { mainHandler.removeCallbacks(it) }
            listenTimeout = null
            val spoken = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            mainHandler.post {
                if (spoken.isBlank()) handleSilence() else handleSpoken(spoken)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    /** Nothing was said inside the window. What that means depends on the stage. */
    private fun handleSilence() {
        when (stage) {
            Stage.READING -> {
                // Silence between posts simply means "keep going".
                silenceStreak = 0
                nextPost(auto = true)
            }

            Stage.DICTATING -> {
                dictationRetries++
                if (dictationRetries >= 2) {
                    dictationRetries = 0
                    speak("Ich habe nichts gehört. Zurück zum Hauptmenü.", After.LISTEN_COMMAND)
                } else {
                    speak("Ich habe nichts gehört. Sprich deinen Beitrag nach dem Ton.", After.LISTEN_DICTATION)
                }
            }

            Stage.CONFIRMING -> {
                silenceStreak++
                if (silenceStreak >= 2) {
                    silenceStreak = 0
                    draft = ""
                    speak("Ich verwerfe den Beitrag. Zurück zum Hauptmenü.", After.LISTEN_COMMAND)
                } else {
                    speak("Soll ich den Beitrag senden? Sag Ja oder Nein.", After.LISTEN_CONFIRM)
                }
            }

            else -> {
                silenceStreak++
                when {
                    silenceStreak >= 6 -> {
                        speak(
                            "Ich höre nichts mehr und beende die Sprachsteuerung.",
                            After.STOP_SESSION
                        )
                    }

                    silenceStreak == 3 -> {
                        speak("Ich höre zu. Sag Hilfe, wenn du die Befehle brauchst.", After.LISTEN_COMMAND)
                    }

                    else -> listenForCommand()
                }
            }
        }
    }

    private fun handleSpoken(spoken: String) {
        silenceStreak = 0
        when (stage) {
            Stage.DICTATING -> handleDictationResult(spoken)
            Stage.CONFIRMING -> handleConfirmation(spoken)
            Stage.READING -> handleCommand(VoiceCommands.parse(spoken), duringReading = true)
            else -> handleCommand(VoiceCommands.parse(spoken), duringReading = false)
        }
    }

    private fun handleCommand(command: VoiceCommand, duringReading: Boolean) {
        when (command) {
            VoiceCommand.READ_TIMELINE -> loadAndRead()
            VoiceCommand.NEXT -> nextPost(auto = false)
            VoiceCommand.PREVIOUS -> previousPost()
            VoiceCommand.REPEAT -> speakCurrentPost()
            VoiceCommand.RESUME -> speakCurrentPost()

            VoiceCommand.PAUSE, VoiceCommand.STOP_READING -> {
                stage = Stage.MENU
                speak("Pausiert. Sag Weiter, um fortzufahren.", After.LISTEN_COMMAND)
            }

            VoiceCommand.NEW_POST -> promptForDictation()
            VoiceCommand.HELP -> speak(VoiceCommands.HELP_TEXT, After.LISTEN_COMMAND)

            VoiceCommand.FASTER -> changeRate(TtsSetup.RATE_STEP, duringReading)
            VoiceCommand.SLOWER -> changeRate(-TtsSetup.RATE_STEP, duringReading)

            VoiceCommand.STATUS -> speak(describeStatus(), After.LISTEN_COMMAND)

            VoiceCommand.LOGOUT -> {
                prefs.clearSession()
                speak("Du bist abgemeldet.", After.STOP_SESSION)
            }

            VoiceCommand.END_SESSION -> endSession(spokenFarewell = true)

            VoiceCommand.CONFIRM, VoiceCommand.DECLINE, VoiceCommand.UNKNOWN -> {
                if (duringReading) {
                    // Don't nag in the middle of the timeline — just carry on.
                    nextPost(auto = true)
                } else {
                    speak(
                        "Das habe ich nicht verstanden. Sag Hilfe für die möglichen Befehle.",
                        After.LISTEN_COMMAND
                    )
                }
            }
        }
    }

    private fun describeStatus(): String = when {
        posts.isEmpty() -> "Es ist keine Timeline geladen. Sag Timeline vorlesen."
        else -> "Beitrag ${index + 1} von ${posts.size}. ${posts[index].summary()}"
    }

    private fun changeRate(delta: Float, duringReading: Boolean) {
        speechRate = TtsSetup.clampRate(speechRate + delta)
        prefs.speechRate = speechRate
        tts?.setSpeechRate(speechRate)
        val percent = (speechRate * 100).toInt()
        if (duringReading) {
            speak("Tempo $percent Prozent.", After.NEXT_POST)
        } else {
            speak("Tempo $percent Prozent.", After.LISTEN_COMMAND)
        }
    }

    // ---- timeline ----------------------------------------------------------

    private fun loadAndRead() {
        if (loading) return
        if (!prefs.isLoggedIn) {
            speak("Du bist nicht angemeldet.", After.STOP_SESSION)
            return
        }
        loading = true
        speak("Timeline wird geladen.", After.NOTHING)

        val instance = prefs.instance
        val token = prefs.accessToken

        Background.run(
            work = { PleromaApi.fetchHomeTimeline(instance, token, limit = 20) },
            onSuccess = { result ->
                loading = false
                posts = result
                index = 0
                if (result.isEmpty()) {
                    speak("Deine Timeline enthält keine Beiträge.", After.LISTEN_COMMAND)
                } else {
                    speakCurrentPost()
                }
            },
            onError = { error ->
                loading = false
                speak(
                    "Die Timeline konnte nicht geladen werden. ${error.userMessage()}",
                    After.LISTEN_COMMAND
                )
            }
        )
    }

    private fun speakCurrentPost() {
        if (posts.isEmpty()) {
            speak("Es ist keine Timeline geladen. Sag Timeline vorlesen.", After.LISTEN_COMMAND)
            return
        }
        if (index !in posts.indices) index = 0
        stage = Stage.READING
        val post = posts[index]
        speak(post.toSpeech(index + 1, posts.size), After.LISTEN_BETWEEN_POSTS)
    }

    private fun nextPost(auto: Boolean) {
        if (posts.isEmpty()) {
            speak("Es ist keine Timeline geladen. Sag Timeline vorlesen.", After.LISTEN_COMMAND)
            return
        }
        if (index >= posts.lastIndex) {
            speak(
                if (auto) "Das war der letzte Beitrag. Was möchtest du tun?"
                else "Es gibt keinen weiteren Beitrag. Was möchtest du tun?",
                After.LISTEN_COMMAND
            )
            return
        }
        index++
        speakCurrentPost()
    }

    private fun previousPost() {
        if (posts.isEmpty()) {
            speak("Es ist keine Timeline geladen. Sag Timeline vorlesen.", After.LISTEN_COMMAND)
            return
        }
        if (index <= 0) {
            speak("Das ist bereits der erste Beitrag.", After.LISTEN_BETWEEN_POSTS)
            return
        }
        index--
        speakCurrentPost()
    }

    // ---- dictation ---------------------------------------------------------

    private fun promptForDictation() {
        dictationRetries = 0
        draft = ""
        speak("Sprich deinen Beitrag nach dem Ton.", After.LISTEN_DICTATION)
    }

    private fun handleDictationResult(spoken: String) {
        // A bare "abbrechen" cancels; longer text is taken as the post itself,
        // so the word can still appear inside a real message.
        val wordCount = spoken.trim().split(Regex("\\s+")).size
        if (wordCount <= 2 && VoiceCommands.parseConfirmation(spoken) == VoiceCommand.DECLINE) {
            speak("Abgebrochen. Zurück zum Hauptmenü.", After.LISTEN_COMMAND)
            return
        }

        draft = spoken.trim()
        dictationRetries = 0
        confirmMisses = 0
        speak(
            "Dein Beitrag lautet: $draft. Soll ich das senden? Sag Ja oder Nein.",
            After.LISTEN_CONFIRM
        )
    }

    private fun handleConfirmation(spoken: String) {
        when (VoiceCommands.parseConfirmation(spoken)) {
            VoiceCommand.CONFIRM -> {
                confirmMisses = 0
                publishDraft()
            }

            VoiceCommand.DECLINE -> {
                confirmMisses = 0
                draft = ""
                speak("Beitrag verworfen. Zurück zum Hauptmenü.", After.LISTEN_COMMAND)
            }

            // Anything else is neither yes nor no. Give the user a couple of
            // tries, but never loop forever: without a cap, a recognizer that
            // keeps mishearing would keep asking the same question and the
            // session could not be left by voice at all.
            else -> {
                confirmMisses++
                if (confirmMisses >= MAX_CONFIRM_MISSES) {
                    confirmMisses = 0
                    draft = ""
                    speak(
                        "Ich habe dich nicht verstanden. Der Beitrag wird verworfen. " +
                            "Zurück zum Hauptmenü.",
                        After.LISTEN_COMMAND
                    )
                } else {
                    speak("Bitte sag Ja oder Nein.", After.LISTEN_CONFIRM)
                }
            }
        }
    }

    private fun publishDraft() {
        val text = draft
        if (text.isBlank()) {
            speak("Es liegt kein Beitrag vor.", After.LISTEN_COMMAND)
            return
        }
        speak("Wird gesendet.", After.NOTHING)

        val instance = prefs.instance
        val token = prefs.accessToken

        Background.run(
            work = { PleromaApi.postStatus(instance, token, text) },
            onSuccess = {
                draft = ""
                beep(ToneGenerator.TONE_PROP_ACK)
                speak("Beitrag veröffentlicht. Was möchtest du tun?", After.LISTEN_COMMAND)
            },
            onError = { error ->
                beep(ToneGenerator.TONE_PROP_NACK)
                confirmMisses = 0
                speak(
                    "Senden fehlgeschlagen. ${error.userMessage()}. " +
                        "Soll ich es erneut versuchen? Sag Ja oder Nein.",
                    After.LISTEN_CONFIRM
                )
            }
        )
    }

    // ---- session end -------------------------------------------------------

    private fun endSession(spokenFarewell: Boolean) {
        if (spokenFarewell) {
            speak("Sprachsteuerung beendet. Bis bald.", After.STOP_SESSION)
            return
        }
        stage = Stage.IDLE
        cancelListening()
        tts?.stop()
        abandonAudioFocus()
        releaseWakeLock()
        publish("Bereit.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---- audio plumbing ----------------------------------------------------

    private fun beep(tone: Int) {
        try {
            toneGenerator?.startTone(tone, 150)
        } catch (e: Exception) {
            // A missing earcon is not worth interrupting the session for.
        }
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest != null) return
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        val lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PeromaVoice::session")
        lock.setReferenceCounted(false)
        lock.acquire(WAKE_LOCK_TIMEOUT_MS)
        wakeLock = lock
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ---- notification ------------------------------------------------------

    private var lastStatus: String = "Bereit."

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceService::class.java).setAction(ACTION_STOP_SESSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.stop), stopIntent)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification)
            return
        }
        // Android 14 rejects a microphone-typed foreground service unless
        // RECORD_AUDIO is already granted, so only claim the type when we have it.
        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val types = if (micGranted) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        }
        startForeground(NOTIFICATION_ID, notification, types)
    }

    private fun publish(text: String) {
        lastStatus = text
        State.current = State(text, stage)
        State.listener?.invoke(State.current)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        cancelListening()
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        toneGenerator?.release()
        toneGenerator = null
        abandonAudioFocus()
        releaseWakeLock()
        State.current = State("Bereit.", Stage.IDLE)
        State.listener?.invoke(State.current)
        super.onDestroy()
    }

    data class State(val statusText: String, val stage: Stage) {
        companion object {
            var current = State("Bereit.", Stage.IDLE)

            /** Set by MainActivity while it is on screen. */
            var listener: ((State) -> Unit)? = null
        }
    }

    companion object {
        const val ACTION_START_SESSION = "de.peroma.voice.action.START_SESSION"
        const val ACTION_STOP_SESSION = "de.peroma.voice.action.STOP_SESSION"
        const val ACTION_READ_TIMELINE = "de.peroma.voice.action.READ_TIMELINE"
        const val ACTION_NEXT = "de.peroma.voice.action.NEXT"
        const val ACTION_PREVIOUS = "de.peroma.voice.action.PREVIOUS"
        const val ACTION_REPEAT = "de.peroma.voice.action.REPEAT"
        const val ACTION_NEW_POST = "de.peroma.voice.action.NEW_POST"

        private const val CHANNEL_ID = "voice_session"
        private const val NOTIFICATION_ID = 42

        /** Let the earcon fade before the microphone opens. */
        private const val BEEP_SETTLE_MS = 220L

        /** Backoff after a recognizer failure, so retries cannot spin. */
        private const val RECOGNIZER_RETRY_DELAY_MS = 800L

        /** Unrecognised yes/no answers tolerated before the draft is dropped. */
        private const val MAX_CONFIRM_MISSES = 3

        private const val COMMAND_WINDOW_MS = 6_000L
        private const val BETWEEN_POSTS_WINDOW_MS = 2_200L
        private const val DICTATION_WINDOW_MS = 12_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 2 * 60 * 60 * 1000L

        fun send(context: Context, action: String) {
            val intent = Intent(context, VoiceService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
