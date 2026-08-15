package de.peroma.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * Reads the home timeline aloud. Runs as a foreground service so playback
 * continues with the screen off, which is the whole point of an audio client.
 */
class PlaybackService : Service() {

    private lateinit var prefs: Prefs
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var posts: List<Post> = emptyList()
    private var index = 0
    private var paused = false
    private var loading = false

    /** Bumped on every navigation so stale utterance callbacks are ignored. */
    private var generation = 0
    private var advanceUtteranceId: String? = null

    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        createNotificationChannel()
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val result = engine.setLanguage(Locale.GERMAN)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    engine.setLanguage(Locale.getDefault())
                }
                engine.setOnUtteranceProgressListener(progressListener)
                ttsReady = true
                mainHandler.post { onTtsReady() }
            } else {
                publish("Sprachausgabe konnte nicht gestartet werden.")
            }
        }
    }

    private var startWhenReady = false

    private fun onTtsReady() {
        if (startWhenReady) {
            startWhenReady = false
            loadTimeline()
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            mainHandler.post {
                // Only the final chunk of the current post advances the queue.
                if (utteranceId != null && utteranceId == advanceUtteranceId && !paused) {
                    advanceUtteranceId = null
                    goToNext(automatic = true)
                }
            }
        }

        override fun onError(utteranceId: String?) {
            mainHandler.post { publish("Fehler bei der Sprachausgabe.") }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always go foreground promptly — the system kills us otherwise.
        startForegroundCompat(buildNotification(State.current.statusText))

        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_TOGGLE_PAUSE -> togglePause()
            ACTION_NEXT -> goToNext(automatic = false)
            ACTION_PREVIOUS -> goToPrevious()
            ACTION_REPEAT -> speakCurrent()
            ACTION_STOP -> stopPlayback()
            else -> Unit
        }
        return START_NOT_STICKY
    }

    private fun handleStart() {
        if (loading) return
        if (!ttsReady) {
            startWhenReady = true
            publish("Sprachausgabe wird vorbereitet …")
            return
        }
        loadTimeline()
    }

    private fun loadTimeline() {
        if (!prefs.isLoggedIn) {
            publish("Nicht angemeldet.")
            return
        }
        loading = true
        publish("Timeline wird geladen …")

        val instance = prefs.instance
        val token = prefs.accessToken

        Background.run(
            work = { PleromaApi.fetchHomeTimeline(instance, token, limit = 20) },
            onSuccess = { result ->
                loading = false
                posts = result
                index = 0
                paused = false
                if (result.isEmpty()) {
                    speakOneOff("Deine Timeline enthält keine Beiträge.")
                    publish("Timeline ist leer.")
                } else {
                    speakCurrent()
                }
            },
            onError = { error ->
                loading = false
                val message = "Timeline konnte nicht geladen werden. ${error.userMessage()}"
                speakOneOff(message)
                publish(message)
            }
        )
    }

    private fun speakCurrent() {
        val engine = tts ?: return
        if (posts.isEmpty()) return
        if (index !in posts.indices) return

        paused = false
        requestAudioFocus()
        generation++

        val post = posts[index]
        val text = post.toSpeech(index + 1, posts.size)
        val chunks = chunk(text)

        chunks.forEachIndexed { position, part ->
            val utteranceId = "post-$generation-$position"
            val queueMode = if (position == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(part, queueMode, null, utteranceId)
            if (position == chunks.lastIndex) {
                advanceUtteranceId = utteranceId
            }
        }

        publish(post.summary(), playing = true)
    }

    /** Speaks a notice without touching the timeline position. */
    private fun speakOneOff(text: String) {
        val engine = tts ?: return
        advanceUtteranceId = null
        requestAudioFocus()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notice-${System.nanoTime()}")
    }

    private fun goToNext(automatic: Boolean) {
        if (posts.isEmpty()) return
        if (index >= posts.lastIndex) {
            advanceUtteranceId = null
            if (automatic) {
                speakOneOff("Ende der Timeline.")
                publish("Ende der Timeline.", playing = false)
                abandonAudioFocus()
            } else {
                speakOneOff("Das war der letzte Beitrag.")
            }
            return
        }
        index++
        speakCurrent()
    }

    private fun goToPrevious() {
        if (posts.isEmpty()) return
        if (index <= 0) {
            speakOneOff("Das ist der erste Beitrag.")
            return
        }
        index--
        speakCurrent()
    }

    private fun togglePause() {
        if (posts.isEmpty()) return
        if (paused) {
            speakCurrent()
        } else {
            paused = true
            advanceUtteranceId = null
            tts?.stop()
            abandonAudioFocus()
            publish("Pausiert. " + posts.getOrNull(index)?.summary().orEmpty(), playing = false)
        }
    }

    private fun stopPlayback() {
        advanceUtteranceId = null
        paused = false
        tts?.stop()
        abandonAudioFocus()
        publish("Bereit.", playing = false)
        stopForegroundCompat()
        stopSelf()
    }

    /** TTS rejects very long strings; split on sentence boundaries. */
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

    // ---- audio focus -------------------------------------------------------

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
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
                AudioManager.AUDIOFOCUS_GAIN
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

    // ---- notification ------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun action(actionName: String, label: String): NotificationCompat.Action {
        val intent = Intent(this, PlaybackService::class.java).setAction(actionName)
        val pending = PendingIntent.getService(
            this,
            actionName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, label, pending).build()
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(action(ACTION_TOGGLE_PAUSE, getString(R.string.pause)))
            .addAction(action(ACTION_NEXT, getString(R.string.next_post)))
            .addAction(action(ACTION_STOP, getString(R.string.stop)))
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /** Updates the notification and anyone listening on screen. */
    private fun publish(text: String, playing: Boolean = false) {
        State.current = State(text, playing)
        State.listener?.invoke(State.current)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        State.listener?.invoke(State("Bereit.", false))
        abandonAudioFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    data class State(val statusText: String, val playing: Boolean) {
        companion object {
            var current = State("Bereit.", false)

            /** Set by MainActivity while it is visible. */
            var listener: ((State) -> Unit)? = null
        }
    }

    companion object {
        const val ACTION_START = "de.peroma.voice.action.START"
        const val ACTION_TOGGLE_PAUSE = "de.peroma.voice.action.TOGGLE_PAUSE"
        const val ACTION_NEXT = "de.peroma.voice.action.NEXT"
        const val ACTION_PREVIOUS = "de.peroma.voice.action.PREVIOUS"
        const val ACTION_REPEAT = "de.peroma.voice.action.REPEAT"
        const val ACTION_STOP = "de.peroma.voice.action.STOP"

        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 42

        fun send(context: Context, action: String) {
            val intent = Intent(context, PlaybackService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
