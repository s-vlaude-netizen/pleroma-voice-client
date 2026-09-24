package de.peroma.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

    /** What the microphone was wanted for, so the answer can continue it. */
    private var micWantedFor: (() -> Unit)? = null

    /** Keeps the notification request to one per visit to this screen. */
    private var notificationPermissionHandled = false

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            continueWithMicrophone()
            requestNotificationPermissionIfNeeded()
        } else {
            setStatus(getString(R.string.mic_denied))
            // Refusing a second time means Android will not ask again. Say so
            // now rather than letting the next attempt fail in silence.
            if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                showMicrophoneBlockedDialog()
            } else {
                requestNotificationPermissionIfNeeded()
            }
        }
    }

    /** Returning from the system settings: pick up where the block interrupted. */
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) continueWithMicrophone()
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

        setStatus(
            pendingStatus
                ?: getString(R.string.main_intro, prefs.accountName, prefs.instance)
        )
        pendingStatus = null
        // The microphone goes first: it is what the app is for, and asking for
        // two permissions at once leaves one of the dialogs unanswered. The
        // notification request follows once the microphone is settled.
        if (!startVoiceOnLaunchIfWanted(savedInstanceState)) {
            requestNotificationPermissionIfNeeded()
        }
    }

    /**
     * Starts a session as soon as the app opens, which is the whole point of an
     * audio client — having to find a button first is the step it exists to save.
     *
     * Two things hold it back, both meaning this is not an arrival: a non-null
     * [savedInstanceState], so the screen was rebuilt rather than opened, after
     * a language switch say; and a session that is already live, which must not
     * be greeted a second time in the middle of reading.
     *
     * Otherwise the microphone is settled first, whatever state it is in.
     * Returns whether it took that question on, since the notification request
     * has to wait for the answer.
     */
    private fun startVoiceOnLaunchIfWanted(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState != null) return false
        if (!prefs.startVoiceOnLaunch) return false
        if (VoiceService.State.running) return false
        // Settle the microphone here and now: ask for it if it is missing, and
        // show the way to the settings if Android will no longer ask. Doing
        // nothing was the bug — the app opened, stayed silent, and gave no hint
        // that a permission was in the way.
        withMicrophone { startVoiceSession() }
        return true
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
        if (notificationPermissionHandled) return
        notificationPermissionHandled = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun onStartVoiceClicked() {
        withMicrophone { startVoiceSession() }
    }

    /**
     * Runs [action] once the microphone is available, or explains why it is not.
     *
     * The case worth handling is the third one: the permission was refused for
     * good, or switched off later in the system settings. Android then denies
     * every further request outright, without showing anything, so an app that
     * just asks again looks broken. The way back leads through the settings,
     * and only the app can point at them.
     */
    private fun withMicrophone(action: () -> Unit) {
        micWantedFor = action
        val step = MicPermissionStep.decide(
            granted = hasPermission(Manifest.permission.RECORD_AUDIO),
            everAsked = prefs.micPermissionAsked,
            canShowRationale =
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        )
        when (step) {
            MicPermissionStep.READY -> {
                continueWithMicrophone()
                requestNotificationPermissionIfNeeded()
            }

            MicPermissionStep.ASK -> {
                prefs.micPermissionAsked = true
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            MicPermissionStep.SEND_TO_SETTINGS -> showMicrophoneBlockedDialog()
        }
    }

    private fun continueWithMicrophone() {
        val action = micWantedFor ?: return
        micWantedFor = null
        action()
    }

    private fun showMicrophoneBlockedDialog() {
        val message = getString(R.string.mic_blocked_message)
        // Also on screen, so a screen reader reads it even if the dialog is
        // dismissed, and so the reason stays visible afterwards.
        setStatus(message)
        AlertDialog.Builder(this)
            .setTitle(R.string.mic_blocked_title)
            .setMessage(message)
            .setPositiveButton(R.string.mic_blocked_open_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { requestNotificationPermissionIfNeeded() }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        try {
            settingsLauncher.launch(intent)
        } catch (e: Exception) {
            // No settings screen to open: nothing left but to say so.
            setStatus(getString(R.string.mic_blocked_no_settings))
        }
    }

    private fun startVoiceSession() {
        VoiceService.send(this, VoiceService.ACTION_START_SESSION)
        setStatus(getString(R.string.starting_voice))
    }

    private fun onDictateClicked() {
        withMicrophone { VoiceService.send(this, VoiceService.ACTION_NEW_POST) }
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
