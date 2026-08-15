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
 * app registration, the OAuth code flow, the home timeline and posting.
 * Everything runs on plain HttpURLConnection so the app carries no
 * networking dependency.
 */
object PleromaApi {

    const val REDIRECT_URI = "peromavoice://oauth"
    const val SCOPES = "read write"

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
                throw IOException(describeError(status, text))
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun describeError(status: Int, body: String): String {
        val detail = try {
            if (body.isNotBlank()) JSONObject(body).optString("error", "") else ""
        } catch (e: Exception) {
            ""
        }
        val suffix = if (detail.isNotBlank()) ": $detail" else ""
        return when (status) {
            401 -> "Nicht angemeldet oder Zugang abgelaufen$suffix"
            403 -> "Zugriff verweigert$suffix"
            404 -> "Endpunkt nicht gefunden — ist das wirklich eine Pleroma-Instanz?$suffix"
            422 -> "Beitrag abgelehnt$suffix"
            429 -> "Zu viele Anfragen, bitte kurz warten$suffix"
            in 500..599 -> "Server-Fehler ($status)$suffix"
            else -> "HTTP $status$suffix"
        }
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
        val json = JSONObject(request("POST", "https://$instance/api/v1/apps", body = body))
        val id = json.optString("client_id")
        val secret = json.optString("client_secret")
        if (id.isBlank() || secret.isBlank()) {
            throw IOException("Instanz lieferte keine OAuth-Zugangsdaten")
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
        val json = JSONObject(request("POST", "https://$instance/oauth/token", body = body))
        val token = json.optString("access_token")
        if (token.isBlank()) throw IOException("Kein Zugriffstoken erhalten")
        return token
    }

    /** Returns the display name of the logged in account; also validates the token. */
    fun verifyCredentials(instance: String, token: String): String {
        val json = JSONObject(
            request("GET", "https://$instance/api/v1/accounts/verify_credentials", token = token)
        )
        val name = json.optString("display_name")
        return if (name.isNotBlank()) name else json.optString("username")
    }

    fun fetchHomeTimeline(instance: String, token: String, limit: Int = 20): List<Post> {
        val text = request(
            "GET",
            "https://$instance/api/v1/timelines/home?limit=$limit",
            token = token
        )
        val array = JSONArray(text)
        val posts = ArrayList<Post>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            posts.add(Post.fromJson(obj))
        }
        return posts
    }

    /** Publishes a status. Returns the id of the created post. */
    fun postStatus(
        instance: String,
        token: String,
        text: String,
        visibility: String = "public"
    ): String {
        val body = formBody(
            mapOf(
                "status" to text,
                "visibility" to visibility
            )
        )
        val json = JSONObject(
            request("POST", "https://$instance/api/v1/statuses", token = token, body = body)
        )
        return json.optString("id")
    }
}
