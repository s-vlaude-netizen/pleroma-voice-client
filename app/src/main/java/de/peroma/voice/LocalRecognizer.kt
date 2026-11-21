package de.peroma.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper

/**
 * Listens for one utterance and turns it into text, entirely on the device.
 *
 * Replaces android.speech.SpeechRecognizer, which on nearly every phone hands
 * the audio to Google. All callbacks arrive on the main thread.
 */
class LocalRecognizer(private val transcriber: () -> Transcriber?) {

    interface Listener {
        /** The user began speaking. */
        fun onSpeechStarted()

        /** Recognised text; blank means nothing intelligible was said. */
        fun onResult(text: String)

        fun onError(error: Error)
    }

    enum class Error {
        /** The microphone could not be opened, or stopped delivering audio. */
        MICROPHONE,

        /** The speech model is not installed or failed to load. */
        NO_MODEL,

        /** Transcription itself failed. */
        ENGINE
    }

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var session: Session? = null

    /** Starts listening. Any listening in progress is cancelled first. */
    fun start(language: String, listener: Listener) {
        cancel()
        val s = Session(language, listener)
        session = s
        s.thread.start()
    }

    /** Stops listening and drops whatever was heard; the listener hears nothing more. */
    fun cancel() {
        session?.cancelled = true
        session = null
    }

    private inner class Session(val language: String, val listener: Listener) {
        @Volatile
        var cancelled = false

        val thread = Thread({ run() }, "peroma-stt").apply { isDaemon = true }

        private fun deliver(block: () -> Unit) {
            main.post { if (!cancelled) block() }
        }

        @SuppressLint("MissingPermission") // checked by the service before it listens
        private fun run() {
            val engine = transcriber()
            if (engine == null) {
                deliver { listener.onError(Error.NO_MODEL) }
                return
            }

            val minBuffer = AudioRecord.getMinBufferSize(
                Endpointer.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            if (minBuffer <= 0) {
                deliver { listener.onError(Error.MICROPHONE) }
                return
            }

            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    Endpointer.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    maxOf(minBuffer, Endpointer.SAMPLE_RATE * 4)
                )
            } catch (e: Exception) {
                deliver { listener.onError(Error.MICROPHONE) }
                return
            }

            val audio: FloatArray
            try {
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    deliver { listener.onError(Error.MICROPHONE) }
                    return
                }
                record.startRecording()
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    deliver { listener.onError(Error.MICROPHONE) }
                    return
                }

                val endpointer = Endpointer()
                val buffer = FloatArray(Endpointer.SAMPLE_RATE / 10)
                var announced = false
                var ended = false
                while (!cancelled && !ended) {
                    val n = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (n <= 0) {
                        deliver { listener.onError(Error.MICROPHONE) }
                        return
                    }
                    when (endpointer.feed(buffer, n)) {
                        Endpointer.Event.SPEECH_STARTED -> if (!announced) {
                            announced = true
                            deliver { listener.onSpeechStarted() }
                        }
                        Endpointer.Event.SPEECH_ENDED -> ended = true
                        Endpointer.Event.NONE -> Unit
                    }
                }
                if (cancelled) return
                audio = endpointer.utteranceAudio()
            } finally {
                try {
                    record.stop()
                } catch (e: IllegalStateException) {
                    // Never started; nothing to stop.
                }
                record.release()
            }

            val text = try {
                engine.transcribe(audio, language)
            } catch (e: Exception) {
                deliver { listener.onError(Error.ENGINE) }
                return
            }
            deliver { listener.onResult(text) }
        }
    }
}
