package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class EndpointerTest {

    private val rate = Endpointer.SAMPLE_RATE

    private fun silence(ms: Int) = FloatArray(rate * ms / 1000)

    private fun tone(ms: Int, amplitude: Float = 0.3f) =
        FloatArray(rate * ms / 1000) { amplitude * sin(2 * PI * 220 * it / rate).toFloat() }

    private fun Endpointer.run(audio: FloatArray): List<Endpointer.Event> {
        val events = mutableListOf<Endpointer.Event>()
        var i = 0
        while (i < audio.size) {
            val chunk = audio.copyOfRange(i, minOf(i + 1600, audio.size))
            events += feed(chunk)
            i += chunk.size
        }
        return events.filter { it != Endpointer.Event.NONE }
    }

    @Test
    fun silenceAloneNeverStartsAnUtterance() {
        val e = Endpointer()
        assertTrue(e.run(silence(5000)).isEmpty())
        assertFalse(e.isSpeaking)
    }

    @Test
    fun speechThenPauseIsOneUtterance() {
        val e = Endpointer()
        val events = e.run(silence(500) + tone(1000) + silence(1500))
        assertEquals(
            listOf(Endpointer.Event.SPEECH_STARTED, Endpointer.Event.SPEECH_ENDED),
            events
        )
    }

    @Test
    fun theUtteranceKeepsTheLeadInSoTheFirstWordSurvives() {
        val e = Endpointer()
        e.run(silence(500) + tone(1000) + silence(1500))
        val audio = e.utteranceAudio()
        // At least the speech itself plus some of the quiet before it.
        assertTrue(audio.size > rate * 1000 / 1000)
        assertTrue(audio.size < rate * 3)
    }

    @Test
    fun aShortPauseInsideSpeechDoesNotEndIt() {
        val e = Endpointer()
        val events = e.run(tone(600) + silence(400) + tone(600) + silence(1500))
        assertEquals(
            listOf(Endpointer.Event.SPEECH_STARTED, Endpointer.Event.SPEECH_ENDED),
            events
        )
    }

    @Test
    fun aBriefClickIsNotSpeech() {
        val e = Endpointer()
        assertTrue(e.run(silence(500) + tone(40) + silence(2000)).isEmpty())
    }

    @Test
    fun aNoisyRoomDoesNotCountAsSpeech() {
        val e = Endpointer()
        // Steady hiss, loud enough to beat the fixed floor but constant.
        val hiss = FloatArray(rate * 4) { 0.012f * sin(it * 0.9f) }
        assertTrue(e.run(hiss).isEmpty())
    }

    @Test
    fun endlessSpeechIsCutAtTheCap() {
        val e = Endpointer(maxUtteranceMs = 3000)
        val events = e.run(tone(6000))
        assertEquals(Endpointer.Event.SPEECH_STARTED, events.first())
        assertTrue(events.contains(Endpointer.Event.SPEECH_ENDED))
    }
}
