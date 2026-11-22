package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyTest {

    private val remote = ReplyTarget("42", "bob@remote.example", "Bob", "public")

    @Test
    fun theAuthorIsMentionedInFrontOfTheComment() {
        assertEquals("@bob@remote.example hello", Replies.statusText("hello", remote, "alice"))
    }

    @Test
    fun aLocalAuthorIsMentionedByTheShortName() {
        val local = remote.copy(acct = "carol")
        assertEquals("@carol hello", Replies.statusText("hello", local, "alice"))
    }

    @Test
    fun answeringOneselfNeedsNoMention() {
        val mine = remote.copy(acct = "alice")
        assertEquals("hello", Replies.statusText("hello", mine, "alice"))
        assertEquals("hello", Replies.statusText("hello", mine, "@Alice"))
    }

    /** Better a mention too many than an author who is never told. */
    @Test
    fun theMentionStaysWhenOwnAccountIsUnknown() {
        assertEquals("@bob@remote.example hello", Replies.statusText("hello", remote, null))
    }

    @Test
    fun anAtSignAlreadyThereIsNotDoubled() {
        val withAt = remote.copy(acct = "@bob@remote.example")
        assertEquals("@bob@remote.example hello", Replies.statusText("hello", withAt, "alice"))
    }

    @Test
    fun withoutAnAccountThereIsNothingToMention() {
        assertEquals("hello", Replies.statusText("hello", remote.copy(acct = ""), "alice"))
    }

    @Test
    fun theTargetTakesWhatTheReplyNeedsFromThePost() {
        val post = Post(
            id = "7",
            author = "Bob",
            acct = "bob@remote.example",
            body = "Hi",
            spoiler = "",
            media = emptyList(),
            boostedBy = "Eve",
            visibility = "private"
        )
        assertEquals(ReplyTarget("7", "bob@remote.example", "Bob", "private"), ReplyTarget.of(post))
    }
}
