package de.peroma.voice

import java.io.File

/** Turns one utterance of 16 kHz mono audio into text. */
interface Transcriber {
    /** [language] is a short code such as "de"; blank lets the model decide. */
    fun transcribe(pcm: FloatArray, language: String): String
    fun close()
}

/**
 * On-device speech recognition through transcribe.cpp, the engine behind Handy
 * (https://handy.computer). Audio never leaves the phone.
 *
 * Not thread-safe: a session runs one utterance at a time.
 */
class LocalStt private constructor(private var handle: Long) : Transcriber {

    @Synchronized
    override fun transcribe(pcm: FloatArray, language: String): String {
        check(handle != 0L) { "model closed" }
        return nativeTranscribe(handle, pcm, language).trim()
    }

    @Synchronized
    override fun close() {
        if (handle != 0L) {
            nativeClose(handle)
            handle = 0L
        }
    }

    private external fun nativeTranscribe(handle: Long, pcm: FloatArray, language: String): String
    private external fun nativeClose(handle: Long)

    companion object {
        init {
            System.loadLibrary("peroma_stt")
        }

        /** Loads the model; slow (about a second), so call it off the main thread. */
        fun open(model: File, threads: Int = defaultThreads()): LocalStt =
            LocalStt(nativeOpen(model.absolutePath, threads))

        /** Big cores only help; the little ones just slow the whole decode down. */
        fun defaultThreads(): Int =
            (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)

        /** Throws IllegalStateException if the file is not a model the engine can run. */
        @JvmStatic
        private external fun nativeOpen(path: String, threads: Int): Long
    }
}
