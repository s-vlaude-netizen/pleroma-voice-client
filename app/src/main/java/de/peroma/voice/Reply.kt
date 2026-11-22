package de.peroma.voice

/**
 * The post a dictated comment answers, kept for as long as the comment lives.
 *
 * It is stored next to the draft (see [Prefs.pendingReply]) for the same reason
 * the draft is stored: a comment that lost its target on the way — a session
 * ended, Android stopped the service — would come back as a post of its own.
 * For an answer to a private post that would mean a public one.
 */
data class ReplyTarget(
    /** The id of the post being answered. For a boost this is the original post's. */
    val id: String,

    /** The author's account as the server writes it: "alice" or "bob@example.social". */
    val acct: String,

    /** The author's display name, as it is spoken. */
    val author: String,

    /** Visibility of that post; the comment never goes out wider than this. */
    val visibility: String
) {
    companion object {
        fun of(post: Post) = ReplyTarget(post.id, post.acct, post.author, post.visibility)
    }
}

/** How a comment turns into the status that is sent. */
object Replies {

    /**
     * The text to send: what was dictated, with the author mentioned in front.
     *
     * The API threads a reply through `in_reply_to_id`, but the mention has to
     * be in the text itself — it is what notifies the author. Answering one's
     * own post needs none, and a self-mention at the start of it would only look
     * odd. [ownAcct] is null when it could not be found out, in which case the
     * mention stays: better a mention too many than an author left unnotified.
     */
    fun statusText(text: String, target: ReplyTarget, ownAcct: String?): String {
        val acct = target.acct.trim().removePrefix("@")
        if (acct.isEmpty() || acct.equals(ownAcct?.trim()?.removePrefix("@"), ignoreCase = true)) {
            return text
        }
        return "@$acct $text"
    }
}
