package de.peroma.voice

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * The speech model Peroma Voice listens with, and how to get it.
 *
 * It is Whisper Small from Handy's model catalogue, quantised to 8 bits:
 * 270 MB, and it understands English, German and Japanese alike. It is
 * downloaded once, from Hugging Face; after that, listening needs no network.
 * The file is pinned to one revision and checked against its SHA-256, so what
 * runs is exactly what was reviewed.
 */
object SpeechModel {

    const val FILE_NAME = "whisper-small-Q8_0.gguf"
    const val SIZE_BYTES = 269_751_136L
    const val SHA256 = "9b9c8811bbcc82a7766f0fb0925614bdacb0923b2cc630daeac17108b655b860"

    private const val URL_TEMPLATE =
        "https://huggingface.co/handy-computer/whisper-small-gguf/resolve/" +
            "c0214bd34be9296695486f838e0142f900803159/$FILE_NAME"

    fun file(context: Context): File =
        File(File(context.filesDir, "models").apply { mkdirs() }, FILE_NAME)

    /** Only a fully downloaded and verified model is ever stored under this name. */
    fun isInstalled(context: Context): Boolean = file(context).length() == SIZE_BYTES

    class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /**
     * Downloads the model, resuming a partial file. Blocking: run it off the
     * main thread. [onProgress] gets bytes so far and the total; return false
     * from [isCancelled] to stop (the partial file is kept for next time).
     */
    fun download(
        context: Context,
        isCancelled: () -> Boolean,
        onProgress: (Long, Long) -> Unit
    ) {
        val target = file(context)
        val part = File(target.parentFile, "$FILE_NAME.part")
        var have = if (part.exists()) part.length() else 0L
        if (have > SIZE_BYTES) {
            part.delete()
            have = 0L
        }

        if (have < SIZE_BYTES) {
            val connection = (URL(URL_TEMPLATE).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                if (have > 0) setRequestProperty("Range", "bytes=$have-")
            }
            try {
                val code = connection.responseCode
                val resumed = code == HttpURLConnection.HTTP_PARTIAL
                if (code != HttpURLConnection.HTTP_OK && !resumed) {
                    throw DownloadException("HTTP $code")
                }
                if (!resumed) have = 0L // the server ignored Range: start over

                FileOutputStream(part, resumed).use { out ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (isCancelled()) return
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            have += n
                            onProgress(have, SIZE_BYTES)
                        }
                    }
                }
            } catch (e: DownloadException) {
                throw e
            } catch (e: Exception) {
                throw DownloadException(e.message ?: "download failed", e)
            } finally {
                connection.disconnect()
            }
        }

        if (part.length() != SIZE_BYTES) {
            throw DownloadException("incomplete download (${part.length()} of $SIZE_BYTES bytes)")
        }
        if (!sha256Matches(part)) {
            part.delete()
            throw DownloadException("checksum mismatch")
        }
        // Rename rather than copy: a model that is memory-mapped must never be
        // rewritten in place.
        if (!part.renameTo(target)) throw DownloadException("could not store the model")
    }

    private fun sha256Matches(file: File): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == SHA256
    }
}
