package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Telling a first run apart from a permanent refusal.
 *
 * Android reports both as "no rationale to show", which is why this decision
 * is worth having in one place with its own tests.
 */
class MicPermissionStepTest {

    @Test
    fun aGrantedMicrophoneNeedsNothing() {
        assertEquals(
            MicPermissionStep.READY,
            MicPermissionStep.decide(granted = true, everAsked = true, canShowRationale = false)
        )
        assertEquals(
            MicPermissionStep.READY,
            MicPermissionStep.decide(granted = true, everAsked = false, canShowRationale = false)
        )
    }

    /** Nobody has been asked yet, so ask. */
    @Test
    fun theFirstRunAsks() {
        assertEquals(
            MicPermissionStep.ASK,
            MicPermissionStep.decide(granted = false, everAsked = false, canShowRationale = false)
        )
    }

    /** Refused once: Android will still show its dialog, so ask again. */
    @Test
    fun aSingleRefusalIsWorthAnotherAsk() {
        assertEquals(
            MicPermissionStep.ASK,
            MicPermissionStep.decide(granted = false, everAsked = true, canShowRationale = true)
        )
    }

    /**
     * Asked before, no dialog left to show, still no microphone: only the
     * system settings can undo this, and the app has to say so.
     */
    @Test
    fun aRefusalThatHasBecomePermanentLeadsToTheSettings() {
        assertEquals(
            MicPermissionStep.SEND_TO_SETTINGS,
            MicPermissionStep.decide(granted = false, everAsked = true, canShowRationale = false)
        )
    }
}
