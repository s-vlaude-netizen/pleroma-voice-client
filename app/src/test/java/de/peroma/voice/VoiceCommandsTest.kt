package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandsTest {

    private val de = Language.GERMAN
    private val en = Language.ENGLISH

    private fun parseDe(spoken: String) = VoiceCommands.parse(spoken, de)
    private fun parseEn(spoken: String) = VoiceCommands.parse(spoken, en)

    private val ja = Language.JAPANESE
    private fun parseJa(spoken: String) = VoiceCommands.parse(spoken, ja)

    // ---- German -------------------------------------------------------------

    @Test
    fun germanRecognisesTimelineRequests() {
        assertEquals(VoiceCommand.READ_TIMELINE, parseDe("Timeline vorlesen"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseDe("was gibt es neues"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseDe("lies vor bitte"))
    }

    @Test
    fun germanRecognisesNavigation() {
        assertEquals(VoiceCommand.NEXT, parseDe("nächster Beitrag"))
        assertEquals(VoiceCommand.NEXT, parseDe("überspringen"))
        assertEquals(VoiceCommand.PREVIOUS, parseDe("vorheriger Beitrag"))
        assertEquals(VoiceCommand.PREVIOUS, parseDe("zurück"))
        assertEquals(VoiceCommand.REPEAT, parseDe("wiederholen"))
    }

    /** "nächster Beitrag" must not be mistaken for "write a post". */
    @Test
    fun germanNavigationWinsOverPosting() {
        assertEquals(VoiceCommand.NEXT, parseDe("nächster Beitrag"))
        assertEquals(VoiceCommand.REPEAT, parseDe("Beitrag wiederholen"))
        assertEquals(VoiceCommand.NEW_POST, parseDe("neuer Beitrag"))
        assertEquals(VoiceCommand.NEW_POST, parseDe("ich möchte etwas posten"))
    }

    @Test
    fun germanRecognisesPlaybackControl() {
        assertEquals(VoiceCommand.PAUSE, parseDe("Pause"))
        assertEquals(VoiceCommand.RESUME, parseDe("weiter"))
        assertEquals(VoiceCommand.STOP_READING, parseDe("stopp"))
        assertEquals(VoiceCommand.FASTER, parseDe("schneller"))
        assertEquals(VoiceCommand.SLOWER, parseDe("langsamer"))
    }

    @Test
    fun germanRecognisesReadingModeCommands() {
        assertEquals(VoiceCommand.PAUSES_OFF, parseDe("am Stück"))
        assertEquals(VoiceCommand.PAUSES_OFF, parseDe("ohne Pausen"))
        assertEquals(VoiceCommand.PAUSES_OFF, parseDe("Pausen aus"))
        assertEquals(VoiceCommand.PAUSES_ON, parseDe("mit Pausen"))
        assertEquals(VoiceCommand.PAUSES_ON, parseDe("nachfragen"))
    }

    /** "nicht nachfragen" contains "nachfragen", so the negation must win. */
    @Test
    fun germanNegatedReadingModeWins() {
        assertEquals(VoiceCommand.PAUSES_OFF, parseDe("nicht nachfragen"))
        assertEquals(VoiceCommand.PAUSES_OFF, parseDe("nicht unterbrechen"))
    }

    @Test
    fun germanRecognisesSessionCommands() {
        assertEquals(VoiceCommand.HELP, parseDe("Hilfe"))
        assertEquals(VoiceCommand.END_SESSION, parseDe("beenden"))
        assertEquals(VoiceCommand.LOGOUT, parseDe("abmelden"))
    }

    @Test
    fun germanRepeatAcceptsBothSpellingsOfNochmal() {
        assertEquals(VoiceCommand.REPEAT, parseDe("nochmal"))
        assertEquals(VoiceCommand.REPEAT, parseDe("noch mal"))
        assertEquals(VoiceCommand.REPEAT, parseDe("noch einmal"))
    }

    // ---- English ------------------------------------------------------------

    @Test
    fun englishRecognisesTimelineRequests() {
        assertEquals(VoiceCommand.READ_TIMELINE, parseEn("read timeline"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseEn("read my timeline please"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseEn("what's new"))
    }

    @Test
    fun englishRecognisesNavigation() {
        assertEquals(VoiceCommand.NEXT, parseEn("next"))
        assertEquals(VoiceCommand.NEXT, parseEn("skip this one"))
        assertEquals(VoiceCommand.PREVIOUS, parseEn("previous post"))
        assertEquals(VoiceCommand.PREVIOUS, parseEn("go back"))
        assertEquals(VoiceCommand.REPEAT, parseEn("repeat"))
    }

    /** "next post" must not be mistaken for "write a post". */
    @Test
    fun englishNavigationWinsOverPosting() {
        assertEquals(VoiceCommand.NEXT, parseEn("next post"))
        assertEquals(VoiceCommand.REPEAT, parseEn("repeat that post"))
        assertEquals(VoiceCommand.NEW_POST, parseEn("new post"))
        assertEquals(VoiceCommand.NEW_POST, parseEn("I want to post something"))
    }

    @Test
    fun englishRecognisesPlaybackControl() {
        assertEquals(VoiceCommand.PAUSE, parseEn("pause"))
        assertEquals(VoiceCommand.RESUME, parseEn("continue"))
        assertEquals(VoiceCommand.STOP_READING, parseEn("stop"))
        assertEquals(VoiceCommand.FASTER, parseEn("faster"))
        assertEquals(VoiceCommand.SLOWER, parseEn("slow down"))
    }

    @Test
    fun englishRecognisesReadingModeCommands() {
        assertEquals(VoiceCommand.PAUSES_OFF, parseEn("straight through"))
        assertEquals(VoiceCommand.PAUSES_OFF, parseEn("no pauses"))
        assertEquals(VoiceCommand.PAUSES_OFF, parseEn("pauses off"))
        assertEquals(VoiceCommand.PAUSES_ON, parseEn("with pauses"))
        assertEquals(VoiceCommand.PAUSES_ON, parseEn("ask me between posts"))
    }

    /** Contractions lose their apostrophe when punctuation is stripped. */
    @Test
    fun englishHandlesContractions() {
        assertEquals(VoiceCommand.PAUSES_OFF, parseEn("don't ask"))
        assertEquals(VoiceCommand.PAUSES_OFF, parseEn("do not ask"))
    }

    @Test
    fun englishRecognisesSessionCommands() {
        assertEquals(VoiceCommand.HELP, parseEn("help"))
        assertEquals(VoiceCommand.HELP, parseEn("what can I say"))
        assertEquals(VoiceCommand.END_SESSION, parseEn("goodbye"))
        assertEquals(VoiceCommand.LOGOUT, parseEn("sign out"))
    }

    /**
     * "end" is a single short word and recognizers mangle it, so the session
     * has to be closable by several sturdier words as well.
     */
    @Test
    fun englishEndSessionHasReliableAlternatives() {
        assertEquals(VoiceCommand.END_SESSION, parseEn("end"))
        assertEquals(VoiceCommand.END_SESSION, parseEn("quit"))
        assertEquals(VoiceCommand.END_SESSION, parseEn("exit"))
        assertEquals(VoiceCommand.END_SESSION, parseEn("finished"))
        assertEquals(VoiceCommand.END_SESSION, parseEn("I am done"))
        assertEquals(VoiceCommand.END_SESSION, parseEn("that is all"))
        assertEquals(VoiceCommand.END_SESSION, parseEn("stop voice control"))
    }

    /** Ending the session must not swallow the ordinary stop and slow commands. */
    @Test
    fun englishEndSessionDoesNotSwallowNeighbours() {
        assertEquals(VoiceCommand.STOP_READING, parseEn("stop"))
        assertEquals(VoiceCommand.SLOWER, parseEn("slow down"))
        assertEquals(VoiceCommand.PAUSE, parseEn("pause"))
    }

    @Test
    fun bothLanguagesCanBeSelectedByVoice() {
        assertEquals(VoiceCommand.LANGUAGE_ENGLISH, parseDe("englisch"))
        assertEquals(VoiceCommand.LANGUAGE_GERMAN, parseDe("sprich deutsch"))
        assertEquals(VoiceCommand.LANGUAGE_GERMAN, parseEn("speak german"))
        assertEquals(VoiceCommand.LANGUAGE_ENGLISH, parseEn("english"))
    }

    // ---- Japanese -----------------------------------------------------------
    //
    // Japanese has no word boundaries, so these also cover the substring
    // matcher: every phrase below is looked for inside the utterance rather
    // than compared word by word.

    @Test
    fun japaneseRecognisesTimelineRequests() {
        assertEquals(VoiceCommand.READ_TIMELINE, parseJa("タイムライン"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseJa("タイムラインを読んでください"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseJa("新着は"))
    }

    /** A phrase must be found inside a longer sentence, not only on its own. */
    @Test
    fun japaneseMatchesInsideASentence() {
        assertEquals(VoiceCommand.NEXT, parseJa("次の投稿をお願いします"))
        assertEquals(VoiceCommand.PREVIOUS, parseJa("前の投稿に戻ってください"))
        assertEquals(VoiceCommand.REPEAT, parseJa("もう一度お願いします"))
    }

    /** Recognizers punctuate; the marks must not stop a phrase from matching. */
    @Test
    fun japaneseIgnoresPunctuationAndSpaces() {
        assertEquals(VoiceCommand.READ_TIMELINE, parseJa("タイムライン。"))
        assertEquals(VoiceCommand.NEW_POST, parseJa("新しい 投稿、"))
    }

    /** "次の投稿" must not be mistaken for "write a post". */
    @Test
    fun japaneseNavigationWinsOverPosting() {
        assertEquals(VoiceCommand.NEXT, parseJa("次の投稿"))
        assertEquals(VoiceCommand.NEW_POST, parseJa("新しい投稿"))
        assertEquals(VoiceCommand.NEW_POST, parseJa("投稿したい"))
    }

    @Test
    fun japaneseRecognisesPlaybackControl() {
        assertEquals(VoiceCommand.PAUSE, parseJa("一時停止"))
        assertEquals(VoiceCommand.RESUME, parseJa("続けて"))
        assertEquals(VoiceCommand.STOP_READING, parseJa("停止"))
        assertEquals(VoiceCommand.FASTER, parseJa("もっと速く"))
        assertEquals(VoiceCommand.SLOWER, parseJa("ゆっくり"))
    }

    /** 一時停止 contains 停止, so the longer phrase has to be tested first. */
    @Test
    fun japanesePauseIsNotSwallowedByStop() {
        assertEquals(VoiceCommand.PAUSE, parseJa("一時停止して"))
        assertEquals(VoiceCommand.STOP_READING, parseJa("停止して"))
    }

    /** 続けて読む contains 続けて, and 間で止まって contains 止まって. */
    @Test
    fun japaneseReadingModeWinsOverPlayback() {
        assertEquals(VoiceCommand.PAUSES_OFF, parseJa("続けて読んで"))
        assertEquals(VoiceCommand.PAUSES_ON, parseJa("間で止まって"))
        assertEquals(VoiceCommand.RESUME, parseJa("続けて"))
        assertEquals(VoiceCommand.STOP_READING, parseJa("止まって"))
    }

    @Test
    fun japaneseRecognisesSessionCommands() {
        assertEquals(VoiceCommand.HELP, parseJa("ヘルプ"))
        assertEquals(VoiceCommand.END_SESSION, parseJa("終了"))
        assertEquals(VoiceCommand.END_SESSION, parseJa("さようなら"))
        assertEquals(VoiceCommand.LOGOUT, parseJa("ログアウト"))
    }

    /** やめ is inside both やめる and やめて, so it is matched last. */
    @Test
    fun japaneseShortestQuitPhraseIsMatchedLast() {
        assertEquals(VoiceCommand.END_SESSION, parseJa("やめる"))
        assertEquals(VoiceCommand.STOP_READING, parseJa("やめて"))
        assertEquals(VoiceCommand.DECLINE, parseJa("やめ"))
    }

    @Test
    fun japaneseConfirmationIsPlainYesNo() {
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("はい", ja))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("送信して", ja))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("投稿してください", ja))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("いいえ", ja))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("送らないで", ja))
    }

    @Test
    fun japaneseUnknownStaysUnknown() {
        assertEquals(VoiceCommand.UNKNOWN, parseJa("明日の天気はどうですか"))
        assertEquals(VoiceCommand.UNKNOWN, parseJa(""))
    }

    // ---- language selection --------------------------------------------------

    @Test
    fun everyLanguageCanBeSelectedFromEveryLanguage() {
        assertEquals(VoiceCommand.LANGUAGE_JAPANESE, parseEn("speak japanese"))
        assertEquals(VoiceCommand.LANGUAGE_JAPANESE, parseDe("sprich japanisch"))
        assertEquals(VoiceCommand.LANGUAGE_ENGLISH, parseJa("英語"))
        assertEquals(VoiceCommand.LANGUAGE_GERMAN, parseJa("ドイツ語"))
        assertEquals(VoiceCommand.LANGUAGE_JAPANESE, parseJa("日本語にして"))
    }

    /** The button cycles through every language and comes back round. */
    @Test
    fun languageCycleVisitsAllAndWrapsAround() {
        val seen = mutableListOf(Language.ENGLISH)
        var current = Language.ENGLISH
        repeat(Language.values().size - 1) {
            current = current.next()
            seen.add(current)
        }
        assertEquals(Language.values().toList(), seen)
        assertEquals(Language.ENGLISH, current.next())
    }

    // ---- confirmations ------------------------------------------------------

    @Test
    fun confirmationReadsSendAsYesNotAsNewPost() {
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("senden", de))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("ja", de))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("send", en))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("yes please", en))
    }

    @Test
    fun confirmationRecognisesRejection() {
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("nein", de))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("verwerfen", de))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("no", en))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("discard", en))
    }

    /** Confirming a post is a plain yes/no — re-dictating is not an option. */
    @Test
    fun confirmationHasNoRedictateOption() {
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parseConfirmation("nochmal", de))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parseConfirmation("noch mal", de))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parseConfirmation("again", en))
    }

    /** A rejection is checked before acceptance so "no, don't send" is a no. */
    @Test
    fun rejectionWinsWhenBothAppear() {
        assertEquals(
            VoiceCommand.DECLINE,
            VoiceCommands.parseConfirmation("nein, nicht senden", de)
        )
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("no, don't send", en))
    }

    /** Whole-word matching: "Jahr" contains "ja" but is not a yes. */
    @Test
    fun doesNotMatchOnSubstrings() {
        assertEquals(
            VoiceCommand.UNKNOWN,
            VoiceCommands.parseConfirmation("dieses Jahr war schön", de)
        )
        assertEquals(VoiceCommand.UNKNOWN, parseDe("völliger Unsinn hier"))
    }

    @Test
    fun ignoresPunctuationAndCase() {
        assertEquals(VoiceCommand.NEXT, parseDe("NÄCHSTER!"))
        assertEquals(VoiceCommand.HELP, parseEn("Help?"))
    }

    @Test
    fun emptyInputIsUnknown() {
        assertEquals(VoiceCommand.UNKNOWN, parseDe(""))
        assertEquals(VoiceCommand.UNKNOWN, parseEn("   "))
    }
}
