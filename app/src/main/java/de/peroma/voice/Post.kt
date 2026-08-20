package de.peroma.voice

import org.json.JSONObject

/**
 * A timeline entry reduced to what can be spoken aloud.
 *
 * The text is built in whichever language the session is running in, so a post
 * parsed as German cannot be re-read in English — the service re-parses the
 * timeline when the language changes.
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
    fun toSpeech(index: Int, total: Int, strings: Strings): String {
        val parts = ArrayList<String>()
        parts.add(strings.postCounter(index, total))

        if (boostedBy != null) {
            parts.add(strings.boostedBy(boostedBy, author))
        } else {
            parts.add(strings.byAuthor(author))
        }

        if (spoiler.isNotBlank()) {
            parts.add(strings.contentWarning(spoiler))
        }

        if (body.isNotBlank()) {
            parts.add(body)
        }

        when (mediaDescriptions.size) {
            0 -> Unit
            1 -> parts.add(strings.oneAttachment(mediaDescriptions[0]))
            else -> {
                parts.add(strings.manyAttachments(mediaDescriptions.size))
                mediaDescriptions.forEach { parts.add(it) }
            }
        }

        if (body.isBlank() && mediaDescriptions.isEmpty() && spoiler.isBlank()) {
            parts.add(strings.noReadableText)
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

        fun fromJson(obj: JSONObject, strings: Strings): Post {
            // A boost ("reblog") carries the original post as a nested object.
            val reblog = obj.optJSONObject("reblog")
            val source = reblog ?: obj
            val boostedBy = if (reblog != null) {
                displayName(obj.optJSONObject("account"), strings)
            } else {
                null
            }

            val media = ArrayList<String>()
            val attachments = source.optJSONArray("media_attachments")
            if (attachments != null) {
                for (i in 0 until attachments.length()) {
                    val item = attachments.optJSONObject(i) ?: continue
                    val type = when (item.optString("type")) {
                        "image" -> strings.mediaImage
                        "video" -> strings.mediaVideo
                        "audio" -> strings.mediaAudio
                        "gifv" -> strings.mediaAnimation
                        else -> strings.mediaOther
                    }
                    val description = item.optString("description").takeIf {
                        it.isNotBlank() && it != "null"
                    }
                    media.add(
                        if (description != null) {
                            strings.attachmentWith(type, description)
                        } else {
                            strings.attachmentWithout(type)
                        }
                    )
                }
            }

            return Post(
                id = source.optString("id"),
                author = displayName(source.optJSONObject("account"), strings),
                acct = source.optJSONObject("account")?.optString("acct").orEmpty(),
                body = SpeechText.fromHtml(source.optString("content"), strings),
                spoiler = SpeechText.fromHtml(source.optString("spoiler_text"), strings),
                mediaDescriptions = media,
                boostedBy = boostedBy
            )
        }

        private fun displayName(account: JSONObject?, strings: Strings): String {
            if (account == null) return strings.unknownAuthor
            val display = account.optString("display_name")
            val username = account.optString("username")
            val name = if (display.isNotBlank()) display else username
            return SpeechText.cleanupName(name, strings).ifBlank { strings.unknownAuthor }
        }
    }
}
