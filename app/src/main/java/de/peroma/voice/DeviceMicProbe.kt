package de.peroma.voice

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.SensorPrivacyManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock

/**
 * Finds out whether the device's microphone switch lets anything through, and
 * brings up Android's own offer to unblock it when it does not.
 *
 * Since Android 12 the microphone can be blocked for every app at once from
 * the quick settings. No app may read that switch. What Android does instead
 * is watch for an app that opens the microphone while it is blocked and is on
 * screen at the time: that app gets the system's "Unblock device microphone?"
 * dialog — the one other voice apps bring up.
 *
 * This app never opened the microphone itself. Speech recognition records in
 * the recognition service's own process, which is never on screen, so the
 * system stayed quiet and a session simply heard nothing. Opening the
 * microphone here, for a moment and while the screen is up, is what puts the
 * system's dialog in front of the user; listening on while it is up tells us
 * the moment the block is lifted.
 */
object DeviceMicProbe {

    enum class Outcome {
        /** Sound came through: go ahead. */
        OPEN,

        /** Still nothing when the wait ran out. */
        BLOCKED,

        /** Could not tell, because the probe itself failed. Go ahead regardless. */
        UNKNOWN
    }

    /** The first moments of a recording can be silent on any device. */
    private const val WARM_UP_PASSES = 2
    private const val SAMPLE_RATE = 16_000

    /** Whether this device has the switch at all; without it there is nothing to find. */
    fun applies(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val manager = context.getSystemService(SensorPrivacyManager::class.java) ?: return false
        return manager.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE)
    }

    /**
     * Listens until sound comes through or the time [deadline] returns has
     * passed, in [SystemClock.elapsedRealtime] milliseconds. The deadline is
     * asked again on every pass, so the caller can bring it forward.
     * [onNothingYet] is called once, the first time a pass comes back silent.
     *
     * Blocks the calling thread; run it off the main thread. Needs the
     * RECORD_AUDIO permission, which the caller has checked.
     */
    @SuppressLint("MissingPermission")
    fun listen(context: Context, deadline: () -> Long, onNothingYet: () -> Unit): Outcome {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return Outcome.UNKNOWN
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, SAMPLE_RATE / 2)
            )
        } catch (e: Exception) {
            return Outcome.UNKNOWN
        }
        try {
            if (record.state != AudioRecord.STATE_INITIALIZED) return Outcome.UNKNOWN
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return Outcome.UNKNOWN
            }
            val audio = context.getSystemService(AudioManager::class.java)
            val chunk = ShortArray(SAMPLE_RATE / 10)
            var passes = 0
            var reported = false
            while (SystemClock.elapsedRealtime() < deadline()) {
                val read = record.read(chunk, 0, chunk.size)
                if (read < 0) return Outcome.UNKNOWN
                if (++passes <= WARM_UP_PASSES) continue
                val reading = MicSignal.judge(silencedBySystem(audio, record), chunk, read)
                if (reading == MicSignal.Reading.OPEN) return Outcome.OPEN
                if (!reported) {
                    reported = true
                    onNothingYet()
                }
            }
            return Outcome.BLOCKED
        } catch (e: Exception) {
            return Outcome.UNKNOWN
        } finally {
            try {
                record.stop()
            } catch (e: Exception) {
                // Never started, or already gone; nothing to stop.
            }
            record.release()
        }
    }

    /**
     * Android's own verdict on whether this recording is being fed silence,
     * which is what a blocked microphone does. Null where it gives none.
     */
    private fun silencedBySystem(audio: AudioManager?, record: AudioRecord): Boolean? {
        if (audio == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val mine = audio.activeRecordingConfigurations
            .firstOrNull { it.clientAudioSessionId == record.audioSessionId }
            ?: return null
        return mine.isClientSilenced
    }
}
