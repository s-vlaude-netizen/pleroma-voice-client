package de.peroma.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Voice-first control surface: large buttons for playback and one button that
 * records a post by microphone, reads the transcript back and publishes it.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView

    private var recognizer: SpeechRecognizer? = null
    private var confirmTts: TextToSpeech? = null
    private var pendingDictation = false

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginDictation()
        } else {
            setStatus("Ohne Mikrofon-Freigabe kann nicht diktiert werden.")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Playback works either way; the notification is just nicer to have. */ }

    private val fallbackRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isBlank()) {
                setStatus("Nichts verstanden.")
            } else {
                confirmAndPost(spoken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        if (!prefs.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.btnRead).setOnClickListener {
            PlaybackService.send(this, PlaybackService.ACTION_START)
        }
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            PlaybackService.send(this, PlaybackService.ACTION_TOGGLE_PAUSE)
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            PlaybackService.send(this, PlaybackService.ACTION_NEXT)
        }
        findViewById<Button>(R.id.btnPrev).setOnClickListener {
            PlaybackService.send(this, PlaybackService.ACTION_PREVIOUS)
        }
        findViewById<Button>(R.id.btnRepeat).setOnClickListener {
            PlaybackService.send(this, PlaybackService.ACTION_REPEAT)
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            PlaybackService.send(this, PlaybackService.ACTION_STOP)
        }
        findViewById<Button>(R.id.btnDictate).setOnClickListener { onDictateClicked() }
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }

        confirmTts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                confirmTts?.setLanguage(Locale.GERMAN)
            }
        }

        requestNotificationPermissionIfNeeded()
        setStatus("Angemeldet als ${prefs.accountName} auf ${prefs.instance}.")
    }

    override fun onStart() {
        super.onStart()
        // onCreate bails out to the login screen before the views exist.
        if (!prefs.isLoggedIn) return
        PlaybackService.State.listener = { state ->
            Background.onMain { statusText.text = state.statusText }
        }
        statusText.text = PlaybackService.State.current.statusText
    }

    override fun onStop() {
        super.onStop()
        PlaybackService.State.listener = null
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---- dictation ---------------------------------------------------------

    private fun onDictateClicked() {
        // The microphone and the speech output must not fight over the audio.
        if (PlaybackService.State.current.playing) {
            PlaybackService.send(this, PlaybackService.ACTION_TOGGLE_PAUSE)
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            beginDictation()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun recognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMAN.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Beitrag diktieren")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    private fun beginDictation() {
        if (pendingDictation) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            // Some devices only offer the system dialog; use it as a fallback.
            try {
                fallbackRecognizerLauncher.launch(recognizerIntent())
            } catch (e: Exception) {
                setStatus("Auf diesem Gerät ist keine Spracherkennung verfügbar.")
            }
            return
        }

        pendingDictation = true
        setStatus("Sprich jetzt …")

        recognizer?.destroy()
        val speech = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                setStatus("Mikrofon ist bereit. Sprich jetzt …")
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                setStatus("Wird ausgewertet …")
            }

            override fun onError(error: Int) {
                pendingDictation = false
                setStatus(describeRecognizerError(error))
            }

            override fun onResults(results: Bundle?) {
                pendingDictation = false
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (spoken.isBlank()) {
                    setStatus("Nichts verstanden.")
                } else {
                    confirmAndPost(spoken)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        try {
            speech.startListening(recognizerIntent())
        } catch (e: Exception) {
            pendingDictation = false
            setStatus("Spracherkennung konnte nicht gestartet werden.")
        }
    }

    private fun describeRecognizerError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Fehler bei der Audioaufnahme."
        SpeechRecognizer.ERROR_CLIENT -> "Spracherkennung abgebrochen."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon-Freigabe fehlt."
        SpeechRecognizer.ERROR_NETWORK -> "Netzwerkfehler bei der Spracherkennung."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Zeitüberschreitung im Netzwerk."
        SpeechRecognizer.ERROR_NO_MATCH -> "Nichts verstanden. Bitte nochmal versuchen."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Spracherkennung ist beschäftigt."
        SpeechRecognizer.ERROR_SERVER -> "Server der Spracherkennung meldet einen Fehler."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Keine Sprache erkannt."
        else -> "Spracherkennung fehlgeschlagen."
    }

    /** Reads the transcript back and asks before anything is published. */
    private fun confirmAndPost(text: String) {
        setStatus("Erkannt: $text")
        confirmTts?.speak(
            "Du hast diktiert: $text. Zum Veröffentlichen auf Senden tippen.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "confirm"
        )

        AlertDialog.Builder(this)
            .setTitle("Beitrag senden?")
            .setMessage(text)
            .setPositiveButton(R.string.send) { _, _ -> publish(text) }
            .setNeutralButton(R.string.retry) { _, _ -> beginDictation() }
            .setNegativeButton(R.string.cancel) { _, _ ->
                setStatus("Beitrag verworfen.")
            }
            .show()
    }

    private fun publish(text: String) {
        setStatus("Beitrag wird gesendet …")
        val instance = prefs.instance
        val token = prefs.accessToken

        Background.run(
            work = { PleromaApi.postStatus(instance, token, text) },
            onSuccess = {
                setStatus("Beitrag veröffentlicht.")
                confirmTts?.speak(
                    "Beitrag veröffentlicht.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "posted"
                )
            },
            onError = { error ->
                val message = "Senden fehlgeschlagen: ${error.userMessage()}"
                setStatus(message)
                confirmTts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "post-failed")
            }
        )
    }

    private fun logout() {
        PlaybackService.send(this, PlaybackService.ACTION_STOP)
        prefs.clearSession()
        Toast.makeText(this, "Abgemeldet", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setStatus(message: String) {
        statusText.text = message
        statusText.announceForAccessibility(message)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        confirmTts?.stop()
        confirmTts?.shutdown()
        confirmTts = null
        super.onDestroy()
    }
}
