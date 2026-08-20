package de.peroma.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * In version 2 the screen is only a launcher and a fallback.
 *
 * The intended flow is: open the app, start the voice session, pocket the
 * phone. The buttons underneath do the same things for anyone who would
 * rather tap, or when speech recognition is unavailable.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var pauseToggle: Button
    private lateinit var languageToggle: Button

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceSession()
        } else {
            setStatus(getString(R.string.mic_denied))
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The session runs either way; the notification is just nicer to have. */ }

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
        startButton = findViewById(R.id.btnStartVoice)

        startButton.setOnClickListener { onStartVoiceClicked() }
        findViewById<Button>(R.id.btnStopVoice).setOnClickListener {
            VoiceService.send(this, VoiceService.ACTION_STOP_SESSION)
        }

        findViewById<Button>(R.id.btnRead).setOnClickListener {
            VoiceService.send(this, VoiceService.ACTION_READ_TIMELINE)
        }

        pauseToggle = findViewById(R.id.btnTogglePauses)
        pauseToggle.setOnClickListener {
            prefs.pauseBetweenPosts = !prefs.pauseBetweenPosts
            updatePauseToggle()
            setStatus(
                getString(
                    if (prefs.pauseBetweenPosts) {
                        R.string.pauses_on_status
                    } else {
                        R.string.pauses_off_status
                    }
                )
            )
        }
        updatePauseToggle()

        languageToggle = findViewById(R.id.btnToggleLanguage)
        languageToggle.setOnClickListener {
            val next = if (prefs.language == Language.GERMAN) {
                Language.ENGLISH
            } else {
                Language.GERMAN
            }
            prefs.language = next
            updateLanguageToggle()
            // The running session caches the language, so let it restart cleanly.
            VoiceService.send(this, VoiceService.ACTION_STOP_SESSION)
            setStatus(next.strings.languageSwitched(next))
        }
        updateLanguageToggle()
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            VoiceService.send(this, VoiceService.ACTION_NEXT)
        }
        findViewById<Button>(R.id.btnPrev).setOnClickListener {
            VoiceService.send(this, VoiceService.ACTION_PREVIOUS)
        }
        findViewById<Button>(R.id.btnRepeat).setOnClickListener {
            VoiceService.send(this, VoiceService.ACTION_REPEAT)
        }
        findViewById<Button>(R.id.btnDictate).setOnClickListener {
            onDictateClicked()
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }

        findViewById<View>(R.id.manualControls).visibility = View.VISIBLE

        requestNotificationPermissionIfNeeded()
        setStatus(getString(R.string.main_intro, prefs.accountName, prefs.instance))
    }

    override fun onStart() {
        super.onStart()
        if (!prefs.isLoggedIn) return
        VoiceService.State.listener = { state ->
            Background.onMain { statusText.text = state.statusText }
        }
        // Keep the intro on screen until the service actually reports something.
        val current = VoiceService.State.current.statusText
        if (current.isNotBlank()) statusText.text = current
    }

    override fun onStop() {
        super.onStop()
        VoiceService.State.listener = null
    }

    private fun updateLanguageToggle() {
        languageToggle.setText(
            if (prefs.language == Language.GERMAN) {
                R.string.language_state_de
            } else {
                R.string.language_state_en
            }
        )
    }

    private fun updatePauseToggle() {
        pauseToggle.setText(
            if (prefs.pauseBetweenPosts) R.string.pauses_state_on else R.string.pauses_state_off
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun onStartVoiceClicked() {
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            startVoiceSession()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceSession() {
        VoiceService.send(this, VoiceService.ACTION_START_SESSION)
        setStatus(getString(R.string.starting_voice))
    }

    private fun onDictateClicked() {
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            VoiceService.send(this, VoiceService.ACTION_NEW_POST)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun logout() {
        VoiceService.send(this, VoiceService.ACTION_STOP_SESSION)
        prefs.clearSession()
        Toast.makeText(this, R.string.logged_out_toast, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setStatus(message: String) {
        statusText.text = message
        statusText.announceForAccessibility(message)
    }
}
