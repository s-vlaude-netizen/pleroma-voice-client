package de.peroma.voice

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a post turns into when it is read aloud. */
class PostSpeechTest {

    private val en = EnglishStrings
    private val de = GermanStrings
    private val ja = JapaneseStrings

    private fun post(
        body: String = "Hello there.",
        spoiler: String = "",
        media: List<Media> = emptyList()
    ) = Post(
        id = "1",
        author = "Alex",
        acct = "alex@example.social",
        body = body,
        spoiler = spoiler,
        media = media,
        boostedBy = null
    )

    private fun images(count: Int, description: String? = null) =
        List(count) { Media(MediaKind.IMAGE, description) }

    // ---- content warnings ---------------------------------------------------

    @Test
    fun aContentWarningIsReadButTheContentIsNot() {
        val spoken = post(body = "Spoilers for the finale.", spoiler = "TV")
            .toSpeech(1, 1, en)
        assertTrue(spoken.contains("Content warning: TV."))
        assertTrue(spoken.contains("Content skipped."))
        assertFalse(spoken.contains("Spoilers for the finale."))
    }

    /** Attachments are part of what the warning covers. */
    @Test
    fun aContentWarningAlsoHidesTheAttachments() {
        val spoken = post(spoiler = "Food", media = images(1, "A birthday cake"))
            .toSpeech(1, 1, en)
        assertFalse(spoken.contains("A birthday cake"))
    }

    @Test
    fun contentIsReadWhenTheListenerAsksForIt() {
        val spoken = post(body = "Spoilers for the finale.", spoiler = "TV")
            .toSpeech(1, 1, en, revealSensitive = true)
        assertTrue(spoken.contains("Content warning: TV."))
        assertTrue(spoken.contains("Spoilers for the finale."))
        assertFalse(spoken.contains("Content skipped."))
    }

    @Test
    fun aPostWithoutAWarningIsUnaffected() {
        val spoken = post().toSpeech(1, 1, en)
        assertTrue(spoken.contains("Hello there."))
        assertFalse(spoken.contains("Content skipped."))
    }

    /** An empty post is only "nothing to read" when nothing was hidden either. */
    @Test
    fun aWarningAloneIsNotAnEmptyPost() {
        val spoken = post(body = "", spoiler = "Politics").toSpeech(1, 1, en)
        assertFalse(spoken.contains(en.noReadableText))
        assertTrue(post(body = "").toSpeech(1, 1, en).contains(en.noReadableText))
    }

    // ---- attachments --------------------------------------------------------

    /** Twelve undescribed photos are one sentence, not twelve. */
    @Test
    fun undescribedAttachmentsAreCountedRatherThanRepeated() {
        val spoken = post(media = images(12)).toSpeech(1, 1, en)
        assertTrue(spoken.contains("12 images without a description."))
        assertEquals(1, Regex("without a description").findAll(spoken).count())
    }

    @Test
    fun aSingleUndescribedAttachmentIsSaidInTheSingular() {
        assertEquals("One image without a description.", en.attachmentsWithout(MediaKind.IMAGE, 1))
        assertEquals("Ein Bild ohne Beschreibung.", de.attachmentsWithout(MediaKind.IMAGE, 1))
        assertEquals("3 Videos ohne Beschreibung.", de.attachmentsWithout(MediaKind.VIDEO, 3))
        assertEquals("説明のない画像が 4 件。", ja.attachmentsWithout(MediaKind.IMAGE, 4))
    }

    /** Whoever wrote a description meant it to be heard. */
    @Test
    fun describedAttachmentsAreStillReadOneByOne() {
        val spoken = post(
            media = listOf(
                Media(MediaKind.IMAGE, "A dog in the snow"),
                Media(MediaKind.IMAGE, "The same dog, closer")
            )
        ).toSpeech(1, 1, en)
        assertTrue(spoken.contains("Image: A dog in the snow."))
        assertTrue(spoken.contains("Image: The same dog, closer."))
    }

    @Test
    fun describedAndUndescribedAttachmentsAreBothAccountedFor() {
        val spoken = post(
            media = listOf(
                Media(MediaKind.IMAGE, "A dog in the snow"),
                Media(MediaKind.IMAGE, null),
                Media(MediaKind.IMAGE, null),
                Media(MediaKind.VIDEO, null)
            )
        ).toSpeech(1, 1, en)
        assertTrue(spoken.contains("Image: A dog in the snow."))
        assertTrue(spoken.contains("2 images without a description."))
        assertTrue(spoken.contains("One video without a description."))
    }

    /** Counting happens per kind, in the order the kinds first turn up. */
    @Test
    fun kindsAreCountedSeparatelyAndKeepTheirOrder() {
        val spoken = post(
            media = listOf(
                Media(MediaKind.VIDEO, null),
                Media(MediaKind.IMAGE, null),
                Media(MediaKind.VIDEO, null)
            )
        ).toSpeech(1, 1, en)
        assertTrue(
            spoken.indexOf("2 videos without a description.") <
                spoken.indexOf("One image without a description.")
        )
    }

    @Test
    fun attachmentsAloneAreNotAnEmptyPost() {
        val spoken = post(body = "", media = images(2)).toSpeech(1, 1, en)
        assertFalse(spoken.contains(en.noReadableText))
    }

    // ---- notifications ------------------------------------------------------

    private fun notification(
        kind: NotificationKind,
        body: String = "Nice weather.",
        spoiler: String = "",
        emoji: String? = null
    ) = Post(
        id = "n1",
        author = "Sam",
        acct = "sam@example.social",
        body = body,
        spoiler = spoiler,
        media = emptyList(),
        boostedBy = null,
        notification = kind,
        emoji = emoji
    )

    @Test
    fun favoriteNotificationNamesWhoAndReadsThePost() {
        val spoken = notification(NotificationKind.FAVORITE).toSpeech(1, 3, en)
        assertEquals("Notification 1 of 3. Sam favorited your post: Nice weather.", spoken)
    }

    @Test
    fun followNotificationHasNoPostAndNoComplaintAboutIt() {
        val spoken = notification(NotificationKind.FOLLOW, body = "").toSpeech(2, 3, de)
        assertEquals("Benachrichtigung 2 von 3. Sam folgt dir jetzt.", spoken)
    }

    @Test
    fun notificationIsIntroducedInJapaneseToo() {
        val spoken = notification(NotificationKind.FAVORITE).toSpeech(1, 3, ja)
        assertEquals(
            "3 件中 1 件目の通知。 Sam さんがあなたの投稿をお気に入りにしました。 Nice weather.",
            spoken
        )
    }

    @Test
    fun everyKindIsSpokenInEveryLanguage() {
        for (strings in listOf(en, de, ja)) {
            for (kind in NotificationKind.values()) {
                val spoken = notification(kind, emoji = "🎉").toSpeech(1, 1, strings)
                assertTrue("$kind: $spoken", spoken.isNotBlank())
                // The poll announcement is about the poll, not about a person.
                if (kind != NotificationKind.POLL) {
                    assertTrue("$kind: $spoken", spoken.contains("Sam"))
                }
            }
        }
    }

    @Test
    fun reactionSaysWhichEmojiWhenItIsKnown() {
        assertEquals(
            "Notification 1 of 1. Sam reacted with 🎉 to your post: Nice weather.",
            notification(NotificationKind.REACTION, emoji = "🎉").toSpeech(1, 1, en)
        )
        assertEquals(
            "Notification 1 of 1. Sam reacted to your post: Nice weather.",
            notification(NotificationKind.REACTION).toSpeech(1, 1, en)
        )
    }

    @Test
    fun aWarningOnTheFavoritedPostStillHoldsItsContentBack() {
        val post = notification(NotificationKind.FAVORITE, body = "Secret.", spoiler = "Food")
        assertEquals(
            "Notification 1 of 1. Sam favorited your post: " +
                "Content warning: Food. Content skipped.",
            post.toSpeech(1, 1, en)
        )
        assertTrue(post.toSpeech(1, 1, en, revealSensitive = true).endsWith("Secret."))
    }

    // ---- "where am I" ---------------------------------------------------------

    @Test
    fun whereAmIIntroducesANotificationTheWayItIsRead() {
        assertEquals(
            "Notification 2 of 3. Sam favorited your post: Nice weather.",
            notification(NotificationKind.FAVORITE).statusLine(2, 3, en)
        )
        assertEquals(
            "Notification 2 of 3. Sam followed you.",
            notification(NotificationKind.FOLLOW, body = "").statusLine(2, 3, en)
        )
    }

    @Test
    fun whereAmIKeepsTheTimelineWording() {
        assertEquals("Post 2 of 3. Alex: Hello there.", post().statusLine(2, 3, en))
    }

    @Test
    fun whereAmIDoesNotOpenAWarningOnANotification() {
        val line = notification(NotificationKind.FAVORITE, body = "Secret.", spoiler = "Food")
            .statusLine(1, 1, en)
        assertTrue(line, line.endsWith("Food"))
        assertFalse(line, line.contains("Secret"))
    }

    // ---- notifications as the server sends them -------------------------------

    private val favouriteJson = """
        {"id":"9","type":"favourite","created_at":"2026-11-27T10:00:00.000Z",
         "account":{"id":"2","display_name":"Sam","username":"sam","acct":"sam@example.social"},
         "status":{"id":"5","content":"<p>Nice <b>weather</b></p>","spoiler_text":"",
                   "media_attachments":[],
                   "account":{"id":"1","display_name":"Me","username":"me","acct":"me"}}}
    """

    @Test
    fun aFavouriteParsesWithItsActorAndThePost() {
        val post = Post.fromNotification(JSONObject(favouriteJson), en)!!
        assertEquals("Sam", post.author)
        assertEquals("sam@example.social", post.acct)
        assertEquals(NotificationKind.FAVORITE, post.notification)
        assertTrue(post.body, post.body.contains("Nice weather"))
        assertNull(post.emoji)
    }

    @Test
    fun aFollowParsesWithoutAPost() {
        val post = Post.fromNotification(
            JSONObject(
                """{"id":"3","type":"follow",
                    "account":{"display_name":"Sam","username":"sam","acct":"sam"}}"""
            ),
            en
        )!!
        assertEquals(NotificationKind.FOLLOW, post.notification)
        assertEquals("", post.body)
    }

    @Test
    fun anEmojiReactionKeepsItsEmoji() {
        val reaction = JSONObject(favouriteJson)
            .put("type", "pleroma:emoji_reaction")
            .put("emoji", "🎉")
        assertEquals("🎉", Post.fromNotification(reaction, en)!!.emoji)
    }

    @Test
    fun aNullEmojiIsNotTheWordNull() {
        // org.json turns a JSON null into the string "null" when asked for text.
        val reaction = JSONObject(favouriteJson)
            .put("type", "pleroma:emoji_reaction")
            .put("emoji", JSONObject.NULL)
        assertNull(Post.fromNotification(reaction, en)!!.emoji)
    }

    @Test
    fun aTypeTheAppCannotSayIsLeftOut() {
        val chat = JSONObject("""{"id":"4","type":"pleroma:chat_mention"}""")
        assertNull(Post.fromNotification(chat, en))
    }

    @Test
    fun theListKeepsTheServersOrderAndDropsWhatItCannotSay() {
        val list = "[$favouriteJson," +
            """{"id":"4","type":"pleroma:chat_mention"},""" +
            """{"id":"3","type":"follow","account":{"display_name":"Alex","username":"alex"}}]"""
        val posts = PleromaApi.parseNotifications(list, en)
        assertEquals(
            listOf(NotificationKind.FAVORITE, NotificationKind.FOLLOW),
            posts.map { it.notification }
        )
        assertEquals(listOf("Sam", "Alex"), posts.map { it.author })
    }

    // ---- comment, like and boost on a notification ----------------------------

    private fun mentionJson(visibility: String = "private") = """
        {"id":"9","type":"mention",
         "account":{"id":"2","display_name":"Sam","username":"sam","acct":"sam@example.social"},
         "status":{"id":"5","content":"<p>Are you there?</p>","spoiler_text":"",
                   "visibility":"$visibility","media_attachments":[],
                   "account":{"id":"2","display_name":"Sam","username":"sam",
                              "acct":"sam@example.social"}}}
    """

    @Test
    fun aNotificationActsOnThePostNotOnTheNotification() {
        // Liking or boosting what was just read goes by the post's id. Handing
        // over the notification's ("9") would favourite nothing, or the wrong thing.
        val post = Post.fromNotification(JSONObject(mentionJson()), en)!!
        assertEquals("5", post.id)
        assertEquals("private", post.visibility)
        assertEquals("5", Post.fromNotification(JSONObject(favouriteJson), en)!!.id)
    }

    @Test
    fun aCommentOnAMentionAnswersThePostAndStaysAsNarrowAsIt() {
        val target = ReplyTarget.of(Post.fromNotification(JSONObject(mentionJson()), en)!!)
        assertEquals(ReplyTarget("5", "sam@example.social", "Sam", "private"), target)
    }

    @Test
    fun onlyNotificationsAboutTheOtherPersonsPostCanBeAnswered() {
        val answerable = setOf(
            NotificationKind.MENTION, NotificationKind.POLL,
            NotificationKind.NEW_POST, NotificationKind.EDIT
        )
        for (kind in NotificationKind.values()) {
            assertEquals(kind.name, kind in answerable, notification(kind).canBeAnswered)
        }
    }

    @Test
    fun aFavouriteIsAboutTheListenersOwnPostSoThereIsNothingToAnswer() {
        // "Sam favorited your post" — a boost of it would otherwise be offered as
        // "boost Sam's post".
        assertFalse(Post.fromNotification(JSONObject(favouriteJson), en)!!.canBeAnswered)
    }

    @Test
    fun aMentionWhosePostIsGoneCannotBeAnswered() {
        val json = JSONObject(mentionJson()).apply { remove("status") }
        assertFalse(Post.fromNotification(json, en)!!.canBeAnswered)
    }

    @Test
    fun timelinePostsCanAlwaysBeAnswered() {
        assertTrue(post().canBeAnswered)
    }

    @Test
    fun theNothingToDoMessageIsSaidInEveryLanguage() {
        for (strings in listOf(en, de, ja)) assertTrue(strings.nothingToReactTo.isNotBlank())
    }
}
