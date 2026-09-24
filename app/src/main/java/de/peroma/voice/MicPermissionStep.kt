package de.peroma.voice

/**
 * What to do when the app needs the microphone and does not have it.
 *
 * Android gives no single answer to "is this permission refused for good?".
 * [android.app.Activity.shouldShowRequestPermissionRationale] returns false in
 * two opposite situations: before the first request, and after the user has
 * refused for good. Telling them apart needs one more fact — whether we have
 * ever asked — which the app has to remember itself.
 */
enum class MicPermissionStep {
    /** The microphone is available; go ahead. */
    READY,

    /** Ask Android for it — either the first time, or with a reason shown. */
    ASK,

    /**
     * Asking is pointless: Android refuses the dialog and denies at once. Only
     * the system settings can turn the microphone back on, so the app has to
     * say so and offer to open them.
     */
    SEND_TO_SETTINGS;

    companion object {

        fun decide(
            granted: Boolean,
            everAsked: Boolean,
            canShowRationale: Boolean
        ): MicPermissionStep = when {
            granted -> READY
            !everAsked -> ASK
            canShowRationale -> ASK
            else -> SEND_TO_SETTINGS
        }
    }
}
