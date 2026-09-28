package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How a moment of recording is read: open, or nothing yet.
 *
 * The case that matters is the blocked device microphone, which delivers
 * perfect silence. Android's own verdict on the recording, where it gives one,
 * outranks the samples, because a quiet room behind a noise gate can look the
 * same.
 */
class MicSignalTest {

    private val silence = ShortArray(1600)
    private val sound = ShortArray(1600) { if (it == 800) 42 else 0 }

    @Test
    fun androidSayingSilencedMeansKeepWaitingEvenWithSound() {
        assertEquals(MicSignal.Reading.NOTHING_YET, MicSignal.judge(true, sound, sound.size))
    }

    /** A quiet room is not a blocked microphone. */
    @Test
    fun androidSayingNotSilencedMeansOpenEvenInPerfectQuiet() {
        assertEquals(MicSignal.Reading.OPEN, MicSignal.judge(false, silence, silence.size))
    }

    @Test
    fun withoutAndroidsVerdictAnySoundProvesTheMicrophoneOpen() {
        assertEquals(MicSignal.Reading.OPEN, MicSignal.judge(null, sound, sound.size))
    }

    @Test
    fun withoutAndroidsVerdictPerfectSilenceMeansNothingYet() {
        assertEquals(MicSignal.Reading.NOTHING_YET, MicSignal.judge(null, silence, silence.size))
    }

    /** Only the samples actually read count, not stale ones left in the buffer. */
    @Test
    fun samplesBeyondWhatWasReadAreIgnored() {
        assertEquals(MicSignal.Reading.NOTHING_YET, MicSignal.judge(null, sound, 400))
    }
}
