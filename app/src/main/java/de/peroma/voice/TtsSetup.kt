package de.peroma.voice

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Picks the best German voice the device actually has installed.
 *
 * Android ships a fairly plain default voice. Most devices carry several
 * German variants (and users can install a better engine); this prefers the
 * highest quality non-network one so playback also works offline.
 */
object TtsSetup {

    const val MIN_RATE = 0.5f
    const val MAX_RATE = 2.0f
    const val RATE_STEP = 0.15f

    fun applyGermanVoice(engine: TextToSpeech): Boolean {
        val languageResult = engine.setLanguage(Locale.GERMANY)
        val germanAvailable = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        if (!germanAvailable) {
            engine.setLanguage(Locale.getDefault())
            return false
        }

        bestGermanVoice(engine)?.let { engine.voice = it }
        return true
    }

    private fun bestGermanVoice(engine: TextToSpeech): Voice? {
        val voices = try {
            engine.voices
        } catch (e: Exception) {
            null
        } ?: return null

        return voices
            .filter { it.locale.language == Locale.GERMAN.language }
            .filterNot { it.isNetworkConnectionRequired }
            .filterNot { it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true }
            .maxByOrNull { it.quality }
    }

    fun clampRate(rate: Float): Float = rate.coerceIn(MIN_RATE, MAX_RATE)
}
