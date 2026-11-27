package de.peroma.voice

import org.json.JSONObject

/** What a notification is about. Only [hasStatus] kinds carry a post to read. */
enum class NotificationKind(val apiType: String, val hasStatus: Boolean) {
    MENTION("mention", true),
    BOOST("reblog", true),
    FAVORITE("favourite", true),
    REACTION("pleroma:emoji_reaction", true),
    FOLLOW("follow", false),
    FOLLOW_REQUEST("follow_request", false),
    POLL("poll", true),
    NEW_POST("status", true),
    EDIT("update", true);

    companion object {
        fun fromApi(type: String): NotificationKind? = values().firstOrNull { it.apiType == type }
    }
}

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
    val boostedBy: String?,
    /** Set when this entry is a notification; [author] is then who caused it. */
    val notification: NotificationKind? = null,
    /** The emoji of an emoji reaction. */
    val emoji: String? = null
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
        if (notification != null) {
            parts.add(strings.notificationCounter(index, total))
            parts.add(strings.notificationLead(notification, author, emoji))
        } else {
            parts.add(strings.postCounter(index, total))
            if (boostedBy != null) {
                parts.add(strings.boostedBy(boostedBy, author))
            } else {
                parts.add(strings.byAuthor(author))
            }
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

        if (body.isBlank() && media.isEmpty() && !isSensitive &&
            (notification == null || notification.hasStatus)
        ) {
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
        val shortened = shorten(if (body.isNotBlank()) body else spoiler)
        return if (shortened.isBlank()) author else "$author: $shortened"
    }

    /**
     * What "where am I" says: the position, then what is there.
     *
     * A notification is introduced the way it is read out, because the plain
     * summary would credit the post to whoever reacted to it — "Sam: Nice
     * weather" — when it is the listener's own.
     */
    fun statusLine(index: Int, total: Int, strings: Strings): String {
        if (notification == null) return strings.statusAt(index, total, summary())
        val lead = strings.notificationCounter(index, total) + " " +
            strings.notificationLead(notification, author, emoji)
        // Behind a warning only the warning is shown, as when reading.
        val text = shorten(if (isSensitive) spoiler else body)
        return if (text.isBlank()) lead else "$lead $text"
    }

    private fun shorten(text: String): String =
        if (text.length > 80) text.take(80) + "…" else text

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

        /**
         * A notification from `/api/v1/notifications`, or null for a type this
         * app does not know how to say.
         *
         * The actor becomes the author and the post it concerns supplies the
         * text, so favourites and boosts read back the post that was liked.
         */
        fun fromNotification(obj: JSONObject, strings: Strings): Post? {
            val kind = NotificationKind.fromApi(obj.optString("type")) ?: return null
            val account = obj.optJSONObject("account")
            val status = obj.optJSONObject("status")?.let { fromJson(it, strings) }
            return Post(
                id = obj.optString("id"),
                author = displayName(account, strings),
                acct = account?.optString("acct").orEmpty(),
                body = status?.body.orEmpty(),
                spoiler = status?.spoiler.orEmpty(),
                media = status?.media.orEmpty(),
                boostedBy = null,
                notification = kind,
                emoji = obj.optString("emoji").takeIf { it.isNotBlank() && it != "null" }
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
