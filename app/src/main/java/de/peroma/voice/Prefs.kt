package de.peroma.voice

import android.content.Context

/**
 * Stores the instance and OAuth credentials in the app-private preferences.
 */
class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("peroma_voice", Context.MODE_PRIVATE)

    var instance: String
        get() = prefs.getString(KEY_INSTANCE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_INSTANCE, value).apply()

    var clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CLIENT_ID, value).apply()

    var clientSecret: String
        get() = prefs.getString(KEY_CLIENT_SECRET, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CLIENT_SECRET, value).apply()

    var accessToken: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var accountName: String
        get() = prefs.getString(KEY_ACCOUNT, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_ACCOUNT, value).apply()

    val isLoggedIn: Boolean
        get() = instance.isNotBlank() && accessToken.isNotBlank()

    /** Clears the session but keeps the registered OAuth app for this instance. */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ACCOUNT)
            .apply()
    }

    companion object {
        private const val KEY_INSTANCE = "instance"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_ACCOUNT = "account_name"
    }
}
