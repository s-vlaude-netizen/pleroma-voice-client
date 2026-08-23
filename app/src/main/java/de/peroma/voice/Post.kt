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
    val media: List<Media>,
    val boostedBy: String?
) {

    /** True when the author put this post behind a content warning. */
    val isSensitive: Boolean get() = spoiler.isNotBlank()

    /**
     * The full text the speech engine reads for this post.
     *
     * A post behind a content warning stops after the warning unless
     * [revealSensitive] says otherwise: the point of the warning is to let its
     * reader decide, and reading on regardless would take that decision away.
     */
    fun toSpeech(
        index: Int,
        total: Int,
        strings: Strings,
        revealSensitive: Boolean = false
    ): String {
        val parts = ArrayList<String>()
        parts.add(strings.postCounter(index, total))

        if (boostedBy != null) {
            parts.add(strings.boostedBy(boostedBy, author))
        } else {
            parts.add(strings.byAuthor(author))
        }

        if (isSensitive) {
            parts.add(strings.contentWarning(spoiler))
            if (!revealSensitive) {
                parts.add(strings.contentHidden)
                return parts.joinToString(" ")
            }
        }

        if (body.isNotBlank()) {
            parts.add(body)
        }

        parts.addAll(describeMedia(strings))

        if (body.isBlank() && media.isEmpty() && !isSensitive) {
            parts.add(strings.noReadableText)
        }

        return parts.joinToString(" ")
    }

    /**
     * Attachments as spoken sentences.
     *
     * Whoever wrote a description meant it to be heard, so those are read one
     * by one. The rest carry nothing to read, and posts routinely hold several
     * of them — they are counted per kind instead, in the order the kinds first
     * appear, which keeps a gallery of twelve undescribed photos to a single
     * sentence.
     */
    private fun describeMedia(strings: Strings): List<String> {
        val spoken = ArrayList<String>()
        val undescribed = LinkedHashMap<MediaKind, Int>()
        for (item in media) {
            if (item.description != null) {
                spoken.add(strings.attachmentWith(item.kind, item.description))
            } else {
                undescribed[item.kind] = (undescribed[item.kind] ?: 0) + 1
            }
        }
        undescribed.forEach { (kind, count) ->
            spoken.add(strings.attachmentsWithout(kind, count))
        }
        return spoken
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

            val media = ArrayList<Media>()
            val attachments = source.optJSONArray("media_attachments")
            if (attachments != null) {
                for (i in 0 until attachments.length()) {
                    val item = attachments.optJSONObject(i) ?: continue
                    val kind = when (item.optString("type")) {
                        "image" -> MediaKind.IMAGE
                        "video" -> MediaKind.VIDEO
                        "audio" -> MediaKind.AUDIO
                        "gifv" -> MediaKind.ANIMATION
                        else -> MediaKind.OTHER
                    }
                    // A missing description arrives as an empty string or as
                    // the four letters "null", depending on the instance.
                    val description = item.optString("description").takeIf {
                        it.isNotBlank() && it != "null"
                    }
                    media.add(Media(kind, description))
                }
            }

            return Post(
                id = source.optString("id"),
                author = displayName(source.optJSONObject("account"), strings),
                acct = source.optJSONObject("account")?.optString("acct").orEmpty(),
                body = SpeechText.fromHtml(source.optString("content"), strings),
                spoiler = SpeechText.fromHtml(source.optString("spoiler_text"), strings),
                media = media,
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
