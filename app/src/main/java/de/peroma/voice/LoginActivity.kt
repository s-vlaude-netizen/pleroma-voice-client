package de.peroma.voice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Registers the app on the chosen instance, sends the user to the instance's
 * OAuth page in the browser and receives the redirect back via the
 * peromavoice://oauth deep link.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var instanceInput: EditText
    private lateinit var loginButton: Button
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = Prefs(this)
        instanceInput = findViewById(R.id.instanceInput)
        loginButton = findViewById(R.id.btnLogin)
        statusView = findViewById(R.id.loginStatus)

        if (prefs.instance.isNotBlank()) {
            instanceInput.setText(prefs.instance)
        }

        loginButton.setOnClickListener { startLogin() }

        // The activity is singleTask, so a cold start from the redirect lands here.
        handleRedirect(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRedirect(intent)
    }

    private fun startLogin() {
        val instance = PleromaApi.normalizeInstance(instanceInput.text.toString())
        if (instance.isBlank() || !instance.contains(".")) {
            setStatus("Bitte eine gültige Instanz-Adresse eingeben.")
            return
        }

        setBusy(true)
        setStatus("Registriere App auf $instance …")

        Background.run(
            work = { PleromaApi.registerApp(instance) },
            onSuccess = { credentials ->
                prefs.instance = instance
                prefs.clientId = credentials.clientId
                prefs.clientSecret = credentials.clientSecret
                setBusy(false)
                setStatus("Browser wird geöffnet. Bitte anmelden und Zugriff erlauben.")
                openAuthorizePage(instance, credentials.clientId)
            },
            onError = { error ->
                setBusy(false)
                setStatus("Anmeldung fehlgeschlagen: ${error.userMessage()}")
            }
        )
    }

    private fun openAuthorizePage(instance: String, clientId: String) {
        val url = PleromaApi.authorizeUrl(instance, clientId)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            setStatus("Kein Browser gefunden, um die Anmeldung zu öffnen.")
        }
    }

    private fun handleRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "peromavoice") return

        val error = data.getQueryParameter("error")
        if (error != null) {
            setStatus("Zugriff wurde nicht erteilt ($error).")
            return
        }

        val code = data.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            setStatus("Die Antwort der Instanz enthielt keinen Anmeldecode.")
            return
        }

        // Do not process the same redirect twice on rotation.
        intent.data = null

        val instance = prefs.instance
        val clientId = prefs.clientId
        val clientSecret = prefs.clientSecret
        if (instance.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            setStatus("Anmeldedaten fehlen. Bitte erneut mit der Instanz beginnen.")
            return
        }

        setBusy(true)
        setStatus("Melde an …")

        Background.run(
            work = {
                val token = PleromaApi.exchangeCodeForToken(instance, clientId, clientSecret, code)
                val name = PleromaApi.verifyCredentials(instance, token)
                token to name
            },
            onSuccess = { (token, name) ->
                prefs.accessToken = token
                prefs.accountName = name
                setBusy(false)
                Toast.makeText(this, "Angemeldet als $name", Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                finish()
            },
            onError = { e ->
                setBusy(false)
                setStatus("Anmeldung fehlgeschlagen: ${e.userMessage()}")
            }
        )
    }

    private fun setBusy(busy: Boolean) {
        loginButton.isEnabled = !busy
    }

    private fun setStatus(message: String) {
        statusView.text = message
        statusView.announceForAccessibility(message)
    }
}
