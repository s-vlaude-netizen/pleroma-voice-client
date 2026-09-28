package de.peroma.voice

/**
 * Reads what a moment of microphone input says about whether anything is
 * getting through.
 *
 * Kept free of Android types so the judgement can be tested on its own; the
 * recording itself is in [DeviceMicProbe].
 */
object MicSignal {

    enum class Reading {
        /** Sound is coming through: the microphone is open. */
        OPEN,

        /** Nothing yet — blocked, or not settled. Keep listening. */
        NOTHING_YET
    }

    /**
     * [silencedBySystem] is Android's own verdict on this recording, where it
     * gives one, and it is trusted over the samples in both directions. A
     * blocked microphone always delivers perfect silence, but so can a quiet
     * room behind a noise gate; only without that verdict do the samples
     * decide, and then any sound at all proves the microphone open.
     */
    fun judge(silencedBySystem: Boolean?, samples: ShortArray, count: Int): Reading =
        when (silencedBySystem) {
            true -> Reading.NOTHING_YET
            false -> Reading.OPEN
            null ->
                if ((0 until count).any { samples[it] != 0.toShort() }) {
                    Reading.OPEN
                } else {
                    Reading.NOTHING_YET
                }
        }
}
