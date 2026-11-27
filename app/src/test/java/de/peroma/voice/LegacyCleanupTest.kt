package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LegacyCleanupTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun removesTheModelFolderWithEverythingInIt() {
        val filesDir = tmp.newFolder("files")
        val models = File(filesDir, "models").apply { mkdirs() }
        File(models, "whisper-small-Q8_0.gguf").writeText("model")
        File(models, "whisper-small-Q8_0.gguf.part").writeText("half a download")

        assertTrue(LegacyCleanup.removeSpeechModel(filesDir))
        assertFalse(models.exists())
    }

    @Test
    fun doesNothingWhenThereIsNoModel() {
        val filesDir = tmp.newFolder("files")
        assertFalse(LegacyCleanup.removeSpeechModel(filesDir))
    }

    @Test
    fun leavesEverythingElseAlone() {
        val filesDir = tmp.newFolder("files")
        File(filesDir, "models").mkdirs()
        val other = File(filesDir, "keep.txt").apply { writeText("mine") }

        LegacyCleanup.removeSpeechModel(filesDir)

        assertEquals("mine", other.readText())
    }
}
