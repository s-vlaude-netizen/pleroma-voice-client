package de.peroma.voice

import org.json.JSONObject

/**
 * A timeline entry reduced to what can be spoken aloud.
 */
data class Post(
    val id: String,
    val author: String,
    val acct: String,
    val body: String,
    val spoiler: String,
    val mediaDescriptions: List<String>,
    val boostedBy: String?
) {

    /** The full text the speech engine reads for this post. */
    fun toSpeech(index: Int, total: Int): String {
        val parts = ArrayList<String>()
        parts.add("Beitrag $index von $total.")

        if (boostedBy != null) {
            parts.add("$boostedBy teilt einen Beitrag von $author.")
        } else {
            parts.add("Von $author.")
        }

        if (spoiler.isNotBlank()) {
            parts.add("Inhaltswarnung: $spoiler.")
        }

        if (body.isNotBlank()) {
            parts.add(body)
        }

        when (mediaDescriptions.size) {
            0 -> Unit
            1 -> parts.add("Ein Anhang. ${mediaDescriptions[0]}")
            else -> {
                parts.add("${mediaDescriptions.size} Anhänge.")
                mediaDescriptions.forEach { parts.add(it) }
            }
        }

        if (body.isBlank() && mediaDescriptions.isEmpty() && spoiler.isBlank()) {
            parts.add("Dieser Beitrag enthält keinen lesbaren Text.")
        }

        return parts.joinToString(" ")
    }

    /** Short label for the notification and the screen. */
    fun summary(): String {
        val text = if (body.isNotBlank()) body else spoiler
        val shortened = if (text.length > 80) text.take(80) + "…" else text
        return if (shortened.isBlank()) author else "$author: $shortened"
    }

    companion object {

        fun fromJson(obj: JSONObject): Post {
            // A boost ("reblog") carries the original post as a nested object.
            val reblog = obj.optJSONObject("reblog")
            val source = reblog ?: obj
            val boostedBy = if (reblog != null) displayName(obj.optJSONObject("account")) else null

            val media = ArrayList<String>()
            val attachments = source.optJSONArray("media_attachments")
            if (attachments != null) {
                for (i in 0 until attachments.length()) {
                    val item = attachments.optJSONObject(i) ?: continue
                    val type = when (item.optString("type")) {
                        "image" -> "Bild"
                        "video" -> "Video"
                        "audio" -> "Audio"
                        "gifv" -> "Animation"
                        else -> "Anhang"
                    }
                    val description = item.optString("description").takeIf {
                        it.isNotBlank() && it != "null"
                    }
                    media.add(
                        if (description != null) "$type: $description." else "$type ohne Beschreibung."
                    )
                }
            }

            return Post(
                id = source.optString("id"),
                author = displayName(source.optJSONObject("account")),
                acct = source.optJSONObject("account")?.optString("acct").orEmpty(),
                body = SpeechText.fromHtml(source.optString("content")),
                spoiler = SpeechText.fromHtml(source.optString("spoiler_text")),
                mediaDescriptions = media,
                boostedBy = boostedBy
            )
        }

        private fun displayName(account: JSONObject?): String {
            if (account == null) return "Unbekannt"
            val display = account.optString("display_name")
            val username = account.optString("username")
            val name = if (display.isNotBlank()) display else username
            return SpeechText.cleanupName(name).ifBlank { "Unbekannt" }
        }
    }
}
