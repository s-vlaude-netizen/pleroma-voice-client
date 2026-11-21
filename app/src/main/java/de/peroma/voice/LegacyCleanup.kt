package de.peroma.voice

import android.content.Context
import java.io.File

/**
 * Removes what versions 2.10.0 and 2.10.1 left behind.
 *
 * Those versions recognized speech on the device and downloaded a 270 MB model
 * into the app's private storage. The feature is gone again, so nothing will
 * ever read that file — but it would stay and keep taking the space until the
 * app's data is cleared, which would also sign the user out.
 */
object LegacyCleanup {

    /** The folder the on-device speech model lived in, below the app's files. */
    private const val MODEL_DIR = "models"

    /** Deletes the leftover model folder, if any. Returns whether it is gone now. */
    fun removeSpeechModel(filesDir: File): Boolean {
        val dir = File(filesDir, MODEL_DIR)
        if (!dir.exists()) return false
        return dir.deleteRecursively()
    }

    /** Same, for the app's own storage. Blocking: call it off the main thread. */
    fun removeSpeechModel(context: Context): Boolean = removeSpeechModel(context.filesDir)
}
