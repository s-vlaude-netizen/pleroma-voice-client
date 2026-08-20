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

    /** Speaking speed, adjustable by voice ("schneller" / "langsamer"). */
    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    /**
     * Whether the microphone opens briefly between two posts.
     *
     * Off by default: the timeline is read straight through and the app only
     * asks what to do once it reaches the end.
     */
    var pauseBetweenPosts: Boolean
        get() = prefs.getBoolean(KEY_PAUSE_BETWEEN_POSTS, false)
        set(value) = prefs.edit().putBoolean(KEY_PAUSE_BETWEEN_POSTS, value).apply()

    /**
     * Language the app speaks and listens in.
     *
     * Defaults to the device language on first run, then sticks to whatever the
     * user last chose, so switching does not get undone by the system locale.
     */
    var language: Language
        get() {
            val stored = prefs.getString(KEY_LANGUAGE, null)
            return stored?.let { Language.fromTag(it) } ?: Language.fromDevice()
        }
        set(value) = prefs.edit().putString(KEY_LANGUAGE, Language.tagOf(value)).apply()

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
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_PAUSE_BETWEEN_POSTS = "pause_between_posts"
        private const val KEY_LANGUAGE = "language"
    }
}
