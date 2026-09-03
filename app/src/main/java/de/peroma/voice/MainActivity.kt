package de.peroma.voice

import android.Manifest
import android.content.Context
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

    /**
     * Language this screen's resources were resolved for.
     *
     * A voice command can switch language while the screen sits in the
     * background. The labels on it are then stale, because resources were
     * picked when the context was attached — so [onStart] compares the two and
     * recreates the screen if they have drifted apart.
     */
    private var attachedLanguage: Language? = null
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var pauseToggle: Button
    private lateinit var warningsToggle: Button
    private lateinit var autoStartToggle: Button
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

    override fun attachBaseContext(newBase: Context) {
        val language = Prefs(newBase).language
        attachedLanguage = language
        super.attachBaseContext(Locales.wrap(newBase, language))
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

        autoStartToggle = findViewById(R.id.btnToggleAutoStart)
        autoStartToggle.setOnClickListener {
            prefs.startVoiceOnLaunch = !prefs.startVoiceOnLaunch
            updateAutoStartToggle()
            setStatus(
                getString(
                    if (prefs.startVoiceOnLaunch) {
                        R.string.auto_start_on_status
                    } else {
                        R.string.auto_start_off_status
                    }
                )
            )
        }
        updateAutoStartToggle()

        warningsToggle = findViewById(R.id.btnToggleWarnings)
        warningsToggle.setOnClickListener {
            prefs.readSensitiveContent = !prefs.readSensitiveContent
            updateWarningsToggle()
            setStatus(
                if (prefs.readSensitiveContent) {
                    prefs.language.strings.warningsRead
                } else {
                    prefs.language.strings.warningsSkipped
                }
            )
        }
        updateWarningsToggle()

        languageToggle = findViewById(R.id.btnToggleLanguage)
        languageToggle.setOnClickListener {
            val next = prefs.language.next()
            prefs.language = next
            // The running session caches the language, so let it restart cleanly.
            VoiceService.send(this, VoiceService.ACTION_STOP_SESSION)
            // The labels are resources, and those were resolved when this
            // screen was created. Rebuilding it is what puts them into the new
            // language; the announcement is carried across the restart.
            pendingStatus = next.strings.nowSpeakingThisLanguage
            recreate()
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
        setStatus(
            pendingStatus
                ?: getString(R.string.main_intro, prefs.accountName, prefs.instance)
        )
        pendingStatus = null
        startVoiceOnLaunchIfWanted(savedInstanceState)
    }

    /**
     * Starts a session as soon as the app opens, which is the whole point of an
     * audio client — having to find a button first is the step it exists to save.
     *
     * Three things hold it back. A non-null [savedInstanceState] means the screen
     * was rebuilt rather than opened, after a language switch say, and a rebuild
     * is not an arrival. A session that is already live must not be greeted a
     * second time, which would cut off whatever it is reading. And without the
     * microphone permission this would open a permission dialog on top of the one
     * for notifications; the listener grants it once through the button, and every
     * launch after that starts on its own.
     */
    private fun startVoiceOnLaunchIfWanted(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return
        if (!prefs.startVoiceOnLaunch) return
        if (VoiceService.State.running) return
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) return
        startVoiceSession()
    }

    override fun onStart() {
        super.onStart()
        if (!prefs.isLoggedIn) return
        if (attachedLanguage != prefs.language) {
            recreate()
            return
        }
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
        languageToggle.text = getString(R.string.language_state, prefs.language.displayName)
    }

    private fun updateAutoStartToggle() {
        autoStartToggle.setText(
            if (prefs.startVoiceOnLaunch) {
                R.string.auto_start_state_on
            } else {
                R.string.auto_start_state_off
            }
        )
    }

    private fun updateWarningsToggle() {
        warningsToggle.setText(
            if (prefs.readSensitiveContent) {
                R.string.warnings_state_on
            } else {
                R.string.warnings_state_off
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

    private companion object {
        /**
         * Message to show once the screen has been rebuilt.
         *
         * Recreating the activity throws its instance away, so the sentence
         * announcing the new language is parked here rather than in a field.
         */
        var pendingStatus: String? = null
    }
}
