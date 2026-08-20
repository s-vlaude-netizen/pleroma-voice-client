package de.peroma.voice

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the chosen language to Android's resource layer.
 *
 * Everything the app *says* is built in plain Kotlin and follows
 * [Prefs.language] directly. Everything the app *shows* comes from the
 * resources in `values/`, `values-de/` and `values-ja/`, and those are picked
 * by the system locale — which is why the buttons kept following the phone's
 * setting no matter what was chosen in the app.
 *
 * Every component that reads a string therefore attaches a context configured
 * for the chosen language, in [android.app.Activity.attachBaseContext] and its
 * equivalent on the service. Resources are resolved once, when that context is
 * attached, so a screen already on display has to be recreated for a change to
 * reach it.
 */
object Locales {

    fun wrap(base: Context): Context = wrap(base, Prefs(base).language)

    fun wrap(base: Context, language: Language): Context {
        val locale = language.locale
        // Also covers what is formatted outside the resource system — number
        // and date formatting, and the default locale a TextToSpeech engine
        // falls back to.
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}
