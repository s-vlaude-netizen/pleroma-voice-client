package de.peroma.voice

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Minimal client for the Mastodon-compatible API that Pleroma exposes.
 *
 * Only the endpoints an audio client actually needs are implemented:
 * app registration, the OAuth code flow, the home timeline, posting and
 * answering, and the two reactions (favourite, boost) it offers on a post.
 * Everything runs on plain HttpURLConnection so the app carries no
 * networking dependency.
 */
object PleromaApi {

    const val REDIRECT_URI = "peromavoice://oauth"
    const val SCOPES = "read write"

    /**
     * What the API is reached over. Never anything else outside the tests, which
     * point it at a local server; every request goes through [api].
     */
    internal var scheme = "https"

    private fun api(instance: String, path: String) = "$scheme://$instance$path"

    /** Accepts "example.social", "https://example.social/" or "@user@example.social". */
    fun normalizeInstance(raw: String): String {
        var s = raw.trim()
        if (s.contains("@")) s = s.substringAfterLast("@")
        s = s.removePrefix("https://").removePrefix("http://")
        s = s.substringBefore("/")
        return s.trim().trimEnd('.').lowercase()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun formBody(params: Map<String, String>): String =
        params.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }

    private fun request(
        method: String,
        url: String,
        token: String? = null,
        body: String? = null
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "PeromaVoice/1.0 (Android)")
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
                )
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw apiException(status, text)
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    /** Classifies a failed response so the wording can be chosen per language. */
    private fun apiException(status: Int, body: String): ApiException {
        val detail = try {
            if (body.isNotBlank()) JSONObject(body).optString("error", "") else ""
        } catch (e: Exception) {
            ""
        }
        val kind = when (status) {
            401 -> ApiErrorKind.UNAUTHORIZED
            403 -> ApiErrorKind.FORBIDDEN
            404 -> ApiErrorKind.NOT_FOUND
            422 -> ApiErrorKind.REJECTED
            429 -> ApiErrorKind.RATE_LIMITED
            in 500..599 -> ApiErrorKind.SERVER
            else -> ApiErrorKind.OTHER
        }
        return ApiException(kind, status, detail)
    }

    data class AppCredentials(val clientId: String, val clientSecret: String)

    /** Registers this installation as an OAuth app on the given instance. */
    fun registerApp(instance: String): AppCredentials {
        val body = formBody(
            mapOf(
                "client_name" to "Peroma Voice",
                "redirect_uris" to REDIRECT_URI,
                "scopes" to SCOPES,
                "website" to "https://github.com/s-vlaude-netizen/pleroma-voice-client"
            )
        )
        val json = JSONObject(request("POST", api(instance, "/api/v1/apps"), body = body))
        val id = json.optString("client_id")
        val secret = json.optString("client_secret")
        if (id.isBlank() || secret.isBlank()) {
            throw ApiException(ApiErrorKind.NO_CREDENTIALS)
        }
        return AppCredentials(id, secret)
    }

    fun authorizeUrl(instance: String, clientId: String): String =
        "https://$instance/oauth/authorize" +
            "?response_type=code" +
            "&client_id=${encode(clientId)}" +
            "&redirect_uri=${encode(REDIRECT_URI)}" +
            "&scope=${encode(SCOPES)}"

    /** Exchanges the authorization code from the redirect for an access token. */
    fun exchangeCodeForToken(
        instance: String,
        clientId: String,
        clientSecret: String,
        code: String
    ): String {
        val body = formBody(
            mapOf(
                "grant_type" to "authorization_code",
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "redirect_uri" to REDIRECT_URI,
                "scope" to SCOPES,
                "code" to code
            )
        )
        val json = JSONObject(request("POST", api(instance, "/oauth/token"), body = body))
        val token = json.optString("access_token")
        if (token.isBlank()) throw ApiException(ApiErrorKind.NO_TOKEN)
        return token
    }

    /** Returns the display name of the logged in account; also validates the token. */
    fun verifyCredentials(instance: String, token: String): String {
        val json = JSONObject(
            request("GET", api(instance, "/api/v1/accounts/verify_credentials"), token = token)
        )
        val name = json.optString("display_name")
        return if (name.isNotBlank()) name else json.optString("username")
    }

    fun fetchHomeTimeline(
        instance: String,
        token: String,
        strings: Strings,
        limit: Int = 20
    ): List<Post> {
        val text = request(
            "GET",
            api(instance, "/api/v1/timelines/home?limit=$limit"),
            token = token
        )
        val array = JSONArray(text)
        val posts = ArrayList<Post>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            posts.add(Post.fromJson(obj, strings))
        }
        return posts
    }

    /** Recent notifications — favourites, boosts, mentions, follows — newest first. */
    fun fetchNotifications(
        instance: String,
        token: String,
        strings: Strings,
        limit: Int = 20
    ): List<Post> = parseNotifications(
        request(
            "GET",
            api(instance, "/api/v1/notifications?limit=$limit"),
            token = token
        ),
        strings
    )

    /**
     * The notifications the app can say, in the order the server sent them.
     *
     * Types it has no wording for — chat messages, admin reports — are left
     * out rather than read as something they are not.
     */
    internal fun parseNotifications(text: String, strings: Strings): List<Post> {
        val array = JSONArray(text)
        val posts = ArrayList<Post>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            Post.fromNotification(obj, strings)?.let { posts.add(it) }
        }
        return posts
    }

    /**
     * The form fields of a new status.
     *
     * [visibility] and [inReplyToId] are left out when null, so the server
     * applies its own defaults rather than being told something false.
     */
    internal fun statusParams(
        text: String,
        visibility: String?,
        inReplyToId: String?
    ): Map<String, String> {
        val params = LinkedHashMap<String, String>()
        params["status"] = text
        if (visibility != null) params["visibility"] = visibility
        if (inReplyToId != null) params["in_reply_to_id"] = inReplyToId
        return params
    }

    /** Publishes a status. Returns the id of the created post. */
    fun postStatus(
        instance: String,
        token: String,
        text: String,
        visibility: String? = "public",
        inReplyToId: String? = null
    ): String {
        val body = formBody(statusParams(text, visibility, inReplyToId))
        val json = JSONObject(
            request("POST", api(instance, "/api/v1/statuses"), token = token, body = body)
        )
        return json.optString("id")
    }

    /**
     * The logged in account's own name as the server writes it ("alice").
     * Needed to tell when a post being answered is one's own.
     */
    fun ownAcct(instance: String, token: String): String {
        val json = JSONObject(
            request("GET", api(instance, "/api/v1/accounts/verify_credentials"), token = token)
        )
        return json.optString("acct")
    }

    /**
     * Answers a post. Returns the id of the comment.
     *
     * It goes out with the visibility of the post it answers and never a wider
     * one, so a reply to a private post cannot end up public. When the server
     * did not report a visibility, none is sent and its own rule applies.
     */
    fun postReply(
        instance: String,
        token: String,
        text: String,
        target: ReplyTarget
    ): String {
        val own = try {
            ownAcct(instance, token)
        } catch (e: Exception) {
            null // The post below fails the same way if the problem is real.
        }
        return postStatus(
            instance,
            token,
            Replies.statusText(text, target, own),
            visibility = target.visibility.ifBlank { null },
            inReplyToId = target.id
        )
    }

    /** Favourites ("likes") a post. The server treats a repeat as no change. */
    fun favourite(instance: String, token: String, postId: String) {
        request(
            "POST",
            api(instance, "/api/v1/statuses/${encode(postId)}/favourite"),
            token = token,
            body = ""
        )
    }

    /** Boosts (reblogs) a post to the account's followers. */
    fun reblog(instance: String, token: String, postId: String) {
        request(
            "POST",
            api(instance, "/api/v1/statuses/${encode(postId)}/reblog"),
            token = token,
            body = ""
        )
    }
}
