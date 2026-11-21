package de.peroma.voice

import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs the real model through the real JNI library on the desktop JVM.
 *
 * Skipped unless the environment provides what it needs, since the model is
 * 270 MB and the library has to be built for the desktop:
 *
 *   PEROMA_STT_MODEL    path to whisper-small-Q8_0.gguf
 *   PEROMA_STT_LIB_DIR  directory holding a desktop build of libperoma_stt.so
 *   PEROMA_STT_SAMPLES  directory with 16 kHz mono 16-bit WAVs: jfk16, en, de, ja
 */
class LocalSttModelTest {

    private val model = System.getenv("PEROMA_STT_MODEL")
    private val samples = System.getenv("PEROMA_STT_SAMPLES")

    private fun wav(name: String): FloatArray {
        val bytes = File(samples, "$name.wav").readBytes()
        // Canonical 44-byte header; the samples are produced that way.
        val data = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray((bytes.size - 44) / 2) { data.short / 32768f }
    }

    private fun normalise(s: String) = s.lowercase().filter { it.isLetterOrDigit() || it == ' ' }

    @Test
    fun transcribesEnglishGermanAndJapanese() {
        assumeTrue(model != null && samples != null)
        val stt = LocalStt.open(File(model!!))
        try {
            val jfk = normalise(stt.transcribe(wav("jfk16"), "en"))
            println("jfk: $jfk")
            assertTrue(jfk, "ask not what your country can do for you" in jfk)

            val en = normalise(stt.transcribe(wav("en"), "en"))
            println("en: $en")
            assertTrue(en, "timeline" in en)

            val de = normalise(stt.transcribe(wav("de"), "de"))
            println("de: $de")
            assertTrue(de, "neuer" in de && "vor" in de)

            val ja = stt.transcribe(wav("ja"), "ja")
            println("ja: $ja")
            assertTrue(ja, ja.isNotBlank())

            // The engine must also survive silence without throwing.
            stt.transcribe(FloatArray(16000), "en")
        } finally {
            stt.close()
        }
    }
}
