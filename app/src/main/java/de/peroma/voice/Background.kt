package de.peroma.voice

import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.Executors

/**
 * A request that failed, described by kind rather than by a ready-made
 * sentence, so the message can be worded in the session's language.
 */
class ApiException(
    val kind: ApiErrorKind,
    val status: Int = 0,
    val detail: String = ""
) : IOException("$kind $status $detail")

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
fun Exception.userMessage(strings: Strings): String = when (this) {
    is ApiException -> strings.apiError(kind, status, detail)
    is UnknownHostException -> strings.apiError(ApiErrorKind.UNREACHABLE, 0, "")
    else -> localizedMessage?.takeIf { it.isNotBlank() } ?: strings.unknownError
}
