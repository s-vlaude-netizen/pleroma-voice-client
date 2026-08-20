package de.peroma.voice

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Picks the best voice the device actually has installed for a language.
 *
 * Android ships a fairly plain default voice. Most devices carry several
 * variants per language (and users can install a better engine); this prefers
 * the highest quality non-network one so playback also works offline.
 */
object TtsSetup {

    const val MIN_RATE = 0.5f
    const val MAX_RATE = 2.0f
    const val RATE_STEP = 0.15f

    fun applyVoice(engine: TextToSpeech, locale: Locale): Boolean {
        val languageResult = engine.setLanguage(locale)
        val available = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        if (!available) {
            engine.setLanguage(Locale.getDefault())
            return false
        }

        bestVoiceFor(engine, locale)?.let { engine.voice = it }
        return true
    }

    private fun bestVoiceFor(engine: TextToSpeech, locale: Locale): Voice? {
        val voices = try {
            engine.voices
        } catch (e: Exception) {
            null
        } ?: return null

        return voices
            .filter { it.locale.language == locale.language }
            .filterNot { it.isNetworkConnectionRequired }
            .filterNot { it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true }
            .maxByOrNull { it.quality }
    }

    fun clampRate(rate: Float): Float = rate.coerceIn(MIN_RATE, MAX_RATE)
}
