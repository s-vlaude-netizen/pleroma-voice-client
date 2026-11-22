package de.peroma.voice

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Which post a like, a boost or a comment lands on.
 *
 * What the listener hears is the post inside a boost, so that is the one the
 * reaction has to go to — not the boost entry that merely carries it.
 */
class PostFromJsonTest {

    private val en = EnglishStrings

    private fun account(acct: String, name: String) =
        """{"acct":"$acct","username":"${acct.substringBefore('@')}","display_name":"$name"}"""

    @Test
    fun anOrdinaryPostKeepsItsOwnIdAndVisibility() {
        val json = JSONObject(
            """{"id":"100","visibility":"unlisted","content":"<p>Hi</p>","spoiler_text":"",
                "account":${account("alice", "Alice")},"media_attachments":[]}"""
        )
        val post = Post.fromJson(json, en)
        assertEquals("100", post.id)
        assertEquals("alice", post.acct)
        assertEquals("unlisted", post.visibility)
        assertNull(post.boostedBy)
    }

    @Test
    fun aBoostIsActedOnThroughTheOriginalPost() {
        val json = JSONObject(
            """{"id":"boost-1","visibility":"public","content":"","account":${account("eve", "Eve")},
                "reblog":{"id":"orig-9","visibility":"private","content":"<p>Hello</p>",
                  "spoiler_text":"","account":${account("bob@remote.example", "Bob")},
                  "media_attachments":[]}}"""
        )
        val post = Post.fromJson(json, en)
        assertEquals("orig-9", post.id)
        assertEquals("bob@remote.example", post.acct)
        assertEquals("Bob", post.author)
        // The original's visibility, not the boost's: a comment must not go out
        // wider than the post it answers.
        assertEquals("private", post.visibility)
        assertEquals("Eve", post.boostedBy)
    }

    @Test
    fun aMissingVisibilityStaysEmptyRatherThanBeingMadeUp() {
        val json = JSONObject(
            """{"id":"5","content":"x","account":${account("alice", "Alice")},"media_attachments":[]}"""
        )
        assertEquals("", Post.fromJson(json, en).visibility)
    }
}
