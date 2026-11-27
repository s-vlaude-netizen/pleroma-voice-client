package de.peroma.voice

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * The calls behind like, boost and comment, run against a local stand-in for
 * the server: which path and method, with which token, and — for a comment —
 * exactly which fields.
 */
class PleromaApiReactionsTest {

    private class Seen(val method: String, val path: String, val auth: String?, val body: String) {
        /** The form fields of the request body. */
        val form: Map<String, String>
            get() = if (body.isBlank()) emptyMap() else body.split("&").associate {
                val (key, value) = it.split("=", limit = 2)
                URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }
    }

    /**
     * A just-enough HTTP server on a local port. The JVM's own server classes
     * are not on the Android unit-test class path, a plain socket is.
     */
    private class FakeServer {
        private val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        val port: Int get() = socket.localPort
        val seen = CopyOnWriteArrayList<Seen>()
        val answers = ConcurrentHashMap<String, Pair<Int, String>>()

        init {
            thread(isDaemon = true) {
                while (!socket.isClosed) {
                    val client = try {
                        socket.accept()
                    } catch (e: Exception) {
                        break
                    }
                    client.use { handle(it) }
                }
            }
        }

        private fun handle(client: Socket) {
            val input = BufferedInputStream(client.getInputStream())

            fun readLine(): String {
                val line = StringBuilder()
                while (true) {
                    val c = input.read()
                    if (c == -1 || c == '\n'.code) break
                    if (c != '\r'.code) line.append(c.toChar())
                }
                return line.toString()
            }

            val requestLine = readLine()
            val headers = HashMap<String, String>()
            while (true) {
                val line = readLine()
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
            }
            val length = headers["content-length"]?.toIntOrNull() ?: 0
            val body = ByteArray(length)
            var read = 0
            while (read < length) {
                val n = input.read(body, read, length - read)
                if (n < 0) break
                read += n
            }

            val (method, target) = requestLine.split(" ")
            val path = target.substringBefore('?')
            seen.add(Seen(method, path, headers["authorization"], String(body, Charsets.UTF_8)))

            val (status, text) = answers["$method $path"] ?: (200 to "{}")
            val bytes = text.toByteArray(Charsets.UTF_8)
            val out = client.getOutputStream()
            out.write(
                ("HTTP/1.1 $status Answer\r\nContent-Type: application/json\r\n" +
                    "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n")
                    .toByteArray(Charsets.UTF_8)
            )
            out.write(bytes)
            out.flush()
        }

        fun close() = socket.close()
    }

    private lateinit var server: FakeServer
    private val seen get() = server.seen

    private val instance get() = "127.0.0.1:${server.port}"
    private val token = "secret-token"

    @Before
    fun startServer() {
        server = FakeServer()
        PleromaApi.scheme = "http"
    }

    @After
    fun stopServer() {
        PleromaApi.scheme = "https"
        server.close()
    }

    private fun answer(method: String, path: String, status: Int, body: String) {
        server.answers["$method $path"] = status to body
    }

    private fun only(method: String, path: String): Seen {
        val matching = seen.filter { it.method == method && it.path == path }
        assertEquals("requests to $method $path", 1, matching.size)
        return matching.single()
    }

    private val bob = ReplyTarget("42", "bob@remote.example", "Bob", "private")

    // ---- like and boost -------------------------------------------------------

    @Test
    fun likeFavouritesThePostWithTheToken() {
        PleromaApi.favourite(instance, token, "abc123")
        val request = only("POST", "/api/v1/statuses/abc123/favourite")
        assertEquals("Bearer $token", request.auth)
    }

    @Test
    fun boostReblogsThePostWithTheToken() {
        PleromaApi.reblog(instance, token, "abc123")
        val request = only("POST", "/api/v1/statuses/abc123/reblog")
        assertEquals("Bearer $token", request.auth)
    }

    @Test
    fun aMissingPostIsReportedAsNotFound() {
        answer("POST", "/api/v1/statuses/gone/favourite", 404, """{"error":"Record not found"}""")
        try {
            PleromaApi.favourite(instance, token, "gone")
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(ApiErrorKind.NOT_FOUND, e.kind)
            assertEquals("Record not found", e.detail)
        }
    }

    @Test
    fun aRefusedBoostIsReportedAsRejected() {
        answer("POST", "/api/v1/statuses/p/reblog", 422, """{"error":"Validation failed"}""")
        try {
            PleromaApi.reblog(instance, token, "p")
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(ApiErrorKind.REJECTED, e.kind)
        }
    }

    // ---- comments -------------------------------------------------------------

    @Test
    fun aCommentIsThreadedMentionsTheAuthorAndKeepsTheVisibility() {
        answer("GET", "/api/v1/accounts/verify_credentials", 200, """{"acct":"me"}""")
        answer("POST", "/api/v1/statuses", 200, """{"id":"99"}""")

        val id = PleromaApi.postReply(instance, token, "good point", bob)

        assertEquals("99", id)
        val sent = only("POST", "/api/v1/statuses")
        assertEquals("Bearer $token", sent.auth)
        assertEquals("@bob@remote.example good point", sent.form["status"])
        assertEquals("42", sent.form["in_reply_to_id"])
        // Never wider than the post it answers.
        assertEquals("private", sent.form["visibility"])
    }

    @Test
    fun answeringOneselfSendsNoMention() {
        answer("GET", "/api/v1/accounts/verify_credentials", 200, """{"acct":"bob@remote.example"}""")
        PleromaApi.postReply(instance, token, "afterthought", bob)
        assertEquals("afterthought", only("POST", "/api/v1/statuses").form["status"])
    }

    @Test
    fun aCommentStillGoesOutWhenTheOwnAccountCannotBeLookedUp() {
        answer("GET", "/api/v1/accounts/verify_credentials", 500, """{"error":"oops"}""")
        PleromaApi.postReply(instance, token, "hi", bob)
        assertEquals("@bob@remote.example hi", only("POST", "/api/v1/statuses").form["status"])
    }

    @Test
    fun withoutAKnownVisibilityNoneIsSent() {
        PleromaApi.postReply(instance, token, "hi", bob.copy(visibility = ""))
        val form = only("POST", "/api/v1/statuses").form
        assertFalse(form.containsKey("visibility"))
        assertEquals("42", form["in_reply_to_id"])
    }

    @Test
    fun anOrdinaryPostIsStillPublicAndNotAReply() {
        PleromaApi.postStatus(instance, token, "hello world")
        val form = only("POST", "/api/v1/statuses").form
        assertEquals("hello world", form["status"])
        assertEquals("public", form["visibility"])
        assertFalse(form.containsKey("in_reply_to_id"))
    }

    @Test
    fun statusParamsLeaveOutWhatIsNotGiven() {
        val bare = PleromaApi.statusParams("x", visibility = null, inReplyToId = null)
        assertEquals(mapOf("status" to "x"), bare)
        assertNull(bare["visibility"])
        assertTrue(
            PleromaApi.statusParams("x", "unlisted", "7").keys ==
                setOf("status", "visibility", "in_reply_to_id")
        )
    }
}
