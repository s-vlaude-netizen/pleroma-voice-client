package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
