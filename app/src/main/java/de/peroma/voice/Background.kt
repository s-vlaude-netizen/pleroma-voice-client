package de.peroma.voice

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Tiny helper for "do this off the main thread, then continue on it".
 * Keeps the app free of a coroutines dependency.
 */
object Background {

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onError: (Exception) -> Unit) {
        executor.execute {
            try {
                val result = work()
                mainHandler.post { onSuccess(result) }
            } catch (e: Exception) {
                mainHandler.post { onError(e) }
            }
        }
    }

    fun onMain(action: () -> Unit) {
        mainHandler.post(action)
    }
}

/** Human readable message for anything thrown by the API layer. */
fun Exception.userMessage(): String {
    val message = localizedMessage
    return when {
        !message.isNullOrBlank() -> message
        this is java.net.UnknownHostException -> "Server nicht erreichbar"
        else -> "Unbekannter Fehler"
    }
}
