package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {

    @Test
    fun stripsParagraphsAndLineBreaks() {
        val html = "<p>Hallo Welt</p><p>Zweiter Absatz<br/>Dritte Zeile</p>"
        assertEquals("Hallo Welt Zweiter Absatz Dritte Zeile", SpeechText.fromHtml(html))
    }

    @Test
    fun announcesLinksByHostInsteadOfSpellingThem() {
        val result = SpeechText.fromHtml("<p>Siehe https://www.example.org/a/very/long/path</p>")
        assertEquals("Siehe Link zu example.org", result)
    }

    @Test
    fun readsMentionsWithoutTheAtSign() {
        val html = "<p><a href=\"https://x.social/@ada\">@ada@x.social</a> hallo</p>"
        assertEquals("ada hallo", SpeechText.fromHtml(html))
    }

    @Test
    fun expandsHashtags() {
        assertEquals("Hashtag Fediverse", SpeechText.fromHtml("<p>#Fediverse</p>"))
    }

    @Test
    fun decodesEntities() {
        assertEquals(
            "Tee und Kekse \"gut\"",
            SpeechText.fromHtml("<p>Tee &amp; Kekse &quot;gut&quot;</p>")
        )
    }

    @Test
    fun decodesNumericEntities() {
        assertEquals("Das ist’s", SpeechText.fromHtml("Das ist&#8217;s"))
    }

    @Test
    fun dropsCustomEmojiShortcodes() {
        assertEquals("Moin", SpeechText.fromHtml("<p>Moin :blobcat:</p>"))
    }

    @Test
    fun handlesEmptyInput() {
        assertEquals("", SpeechText.fromHtml(null))
        assertEquals("", SpeechText.fromHtml(""))
    }

    @Test
    fun cleansDisplayNames() {
        assertEquals("Ada", SpeechText.cleanupName("Ada :verified:"))
    }

    @Test
    fun keepsPlainSentencesIntact() {
        val text = SpeechText.fromHtml("<p>Guten Morgen, wie geht es dir?</p>")
        assertTrue(text.endsWith("?"))
        assertEquals("Guten Morgen, wie geht es dir?", text)
    }
}
