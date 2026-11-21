package de.peroma.voice

import kotlin.math.sqrt

/**
 * Decides where an utterance starts and ends in a stream of 16 kHz audio.
 *
 * The system recognizer used to do this for us. A local model only transcribes
 * what it is given, so something has to notice that the user began speaking
 * and, later, stopped. A plain energy gate is enough for that and has no
 * dependencies: speech is a stretch of frames clearly louder than the room.
 *
 * Pure logic with no Android types, so it is covered by unit tests.
 */
class Endpointer(
    private val sampleRate: Int = SAMPLE_RATE,
    /** Silence after speech that ends the utterance. */
    private val endSilenceMs: Int = 900,
    /** Hard cap, so a noisy room cannot record forever. */
    private val maxUtteranceMs: Int = 30_000
) {
    enum class Event { NONE, SPEECH_STARTED, SPEECH_ENDED }

    private val frameSize = sampleRate * FRAME_MS / 1000
    private val frame = FloatArray(frameSize)
    private var filled = 0

    private var noiseFloor = INITIAL_NOISE
    private var loudFrames = 0
    private var quietFrames = 0
    private var speaking = false
    private var utteranceFrames = 0

    /** Audio kept from just before speech was detected, so the first word survives. */
    private val preRoll = ArrayDeque<FloatArray>()
    private val utterance = ArrayList<FloatArray>()

    val isSpeaking: Boolean get() = speaking

    /** Feeds samples; returns the most significant event they caused. */
    fun feed(samples: FloatArray, count: Int = samples.size): Event {
        var result = Event.NONE
        var i = 0
        while (i < count) {
            val take = minOf(frameSize - filled, count - i)
            System.arraycopy(samples, i, frame, filled, take)
            filled += take
            i += take
            if (filled == frameSize) {
                filled = 0
                val event = onFrame(frame.copyOf())
                if (event != Event.NONE) result = event
                if (event == Event.SPEECH_ENDED) return result
            }
        }
        return result
    }

    /** The audio of the utterance so far (including the lead-in), or empty. */
    fun utteranceAudio(): FloatArray {
        val total = utterance.sumOf { it.size }
        val out = FloatArray(total)
        var pos = 0
        for (f in utterance) {
            System.arraycopy(f, 0, out, pos, f.size)
            pos += f.size
        }
        return out
    }

    private fun onFrame(f: FloatArray): Event {
        val level = rms(f)
        val threshold = maxOf(MIN_SPEECH_RMS, noiseFloor * SPEECH_OVER_NOISE)
        val loud = level > threshold

        if (!speaking) {
            // Track the room only while nobody speaks, and let it follow a
            // louder room quickly but a quieter one slowly.
            noiseFloor = if (level > noiseFloor) {
                noiseFloor * 0.95f + level * 0.05f
            } else {
                noiseFloor * 0.8f + level * 0.2f
            }
            preRoll.addLast(f)
            while (preRoll.size > PRE_ROLL_FRAMES) preRoll.removeFirst()

            loudFrames = if (loud) loudFrames + 1 else 0
            if (loudFrames >= START_FRAMES) {
                speaking = true
                quietFrames = 0
                utteranceFrames = 0
                utterance.addAll(preRoll)
                utteranceFrames = utterance.size
                preRoll.clear()
                return Event.SPEECH_STARTED
            }
            return Event.NONE
        }

        utterance.add(f)
        utteranceFrames++
        quietFrames = if (loud) 0 else quietFrames + 1
        val silentLongEnough = quietFrames * FRAME_MS >= endSilenceMs
        val tooLong = utteranceFrames * FRAME_MS >= maxUtteranceMs
        if (silentLongEnough || tooLong) {
            speaking = false
            return Event.SPEECH_ENDED
        }
        return Event.NONE
    }

    private fun rms(f: FloatArray): Float {
        var sum = 0.0
        for (s in f) sum += s * s
        return sqrt(sum / f.size).toFloat()
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val FRAME_MS = 30
        private const val INITIAL_NOISE = 0.005f
        private const val MIN_SPEECH_RMS = 0.015f
        private const val SPEECH_OVER_NOISE = 3.0f
        private const val START_FRAMES = 3
        private const val PRE_ROLL_FRAMES = 10
    }
}
