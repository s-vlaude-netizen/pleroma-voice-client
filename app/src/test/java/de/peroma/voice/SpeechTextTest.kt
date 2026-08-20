package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {

    private val en = EnglishStrings
    private val de = GermanStrings

    @Test
    fun stripsParagraphsAndLineBreaks() {
        val html = "<p>Hallo Welt</p><p>Zweiter Absatz<br/>Dritte Zeile</p>"
        assertEquals("Hallo Welt Zweiter Absatz Dritte Zeile", SpeechText.fromHtml(html, de))
    }

    @Test
    fun announcesLinksByHostInsteadOfSpellingThem() {
        assertEquals(
            "Siehe Link zu example.org",
            SpeechText.fromHtml("<p>Siehe https://www.example.org/a/very/long/path</p>", de)
        )
        assertEquals(
            "See link to example.org",
            SpeechText.fromHtml("<p>See https://www.example.org/a/very/long/path</p>", en)
        )
    }

    @Test
    fun readsMentionsWithoutTheAtSign() {
        val html = "<p><a href=\"https://x.social/@ada\">@ada@x.social</a> hallo</p>"
        assertEquals("ada hallo", SpeechText.fromHtml(html, de))
    }

    @Test
    fun expandsHashtagsInBothLanguages() {
        assertEquals("Hashtag Fediverse", SpeechText.fromHtml("<p>#Fediverse</p>", de))
        assertEquals("hashtag Fediverse", SpeechText.fromHtml("<p>#Fediverse</p>", en))
    }

    @Test
    fun spellsOutAmpersandPerLanguage() {
        assertEquals("Tee und Kekse", SpeechText.fromHtml("<p>Tee &amp; Kekse</p>", de))
        assertEquals("tea and biscuits", SpeechText.fromHtml("<p>tea &amp; biscuits</p>", en))
    }

    @Test
    fun decodesEntities() {
        assertEquals(
            "Tee und Kekse \"gut\"",
            SpeechText.fromHtml("<p>Tee &amp; Kekse &quot;gut&quot;</p>", de)
        )
    }

    @Test
    fun decodesNumericEntities() {
        assertEquals("Das ist’s", SpeechText.fromHtml("Das ist&#8217;s", de))
    }

    @Test
    fun dropsCustomEmojiShortcodes() {
        assertEquals("Moin", SpeechText.fromHtml("<p>Moin :blobcat:</p>", de))
    }

    @Test
    fun handlesEmptyInput() {
        assertEquals("", SpeechText.fromHtml(null, en))
        assertEquals("", SpeechText.fromHtml("", en))
    }

    @Test
    fun cleansDisplayNames() {
        assertEquals("Ada", SpeechText.cleanupName("Ada :verified:", en))
    }

    @Test
    fun keepsPlainSentencesIntact() {
        val text = SpeechText.fromHtml("<p>Guten Morgen, wie geht es dir?</p>", de)
        assertTrue(text.endsWith("?"))
        assertEquals("Guten Morgen, wie geht es dir?", text)
    }
}
