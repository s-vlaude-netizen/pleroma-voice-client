package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandsTest {

    private val de = Language.GERMAN
    private val en = Language.ENGLISH

    private fun parseDe(spoken: String) = VoiceCommands.parse(spoken, de)
    private fun parseEn(spoken: String) = VoiceCommands.parse(spoken, en)

    private val ru = Language.RUSSIAN
    private fun parseRu(spoken: String) = VoiceCommands.parse(spoken, ru)

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

    @Test
    fun bothLanguagesCanBeSelectedByVoice() {
        assertEquals(VoiceCommand.LANGUAGE_ENGLISH, parseDe("englisch"))
        assertEquals(VoiceCommand.LANGUAGE_GERMAN, parseDe("sprich deutsch"))
        assertEquals(VoiceCommand.LANGUAGE_GERMAN, parseEn("speak german"))
        assertEquals(VoiceCommand.LANGUAGE_ENGLISH, parseEn("english"))
    }

    // ---- Russian ------------------------------------------------------------

    @Test
    fun russianRecognisesTimelineRequests() {
        assertEquals(VoiceCommand.READ_TIMELINE, parseRu("читай ленту"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseRu("что нового"))
        assertEquals(VoiceCommand.READ_TIMELINE, parseRu("новости"))
    }

    /** The same command arrives in different grammatical forms. */
    @Test
    fun russianAcceptsInflectedForms() {
        assertEquals(VoiceCommand.NEXT, parseRu("следующий"))
        assertEquals(VoiceCommand.NEXT, parseRu("следующая запись"))
        assertEquals(VoiceCommand.PREVIOUS, parseRu("предыдущий пост"))
        assertEquals(VoiceCommand.REPEAT, parseRu("повтори"))
        assertEquals(VoiceCommand.REPEAT, parseRu("ещё раз"))
        assertEquals(VoiceCommand.REPEAT, parseRu("еще раз"))
    }

    /** "следующий пост" must not be mistaken for "write a post". */
    @Test
    fun russianNavigationWinsOverPosting() {
        assertEquals(VoiceCommand.NEXT, parseRu("следующий пост"))
        assertEquals(VoiceCommand.NEW_POST, parseRu("новый пост"))
        assertEquals(VoiceCommand.NEW_POST, parseRu("написать пост"))
    }

    @Test
    fun russianRecognisesPlaybackControl() {
        assertEquals(VoiceCommand.PAUSE, parseRu("пауза"))
        assertEquals(VoiceCommand.RESUME, parseRu("продолжай"))
        assertEquals(VoiceCommand.STOP_READING, parseRu("стоп"))
        assertEquals(VoiceCommand.FASTER, parseRu("быстрее"))
        assertEquals(VoiceCommand.SLOWER, parseRu("медленнее"))
    }

    /** "без пауз" switches the mode; it must not read as "пауза". */
    @Test
    fun russianReadingModeDoesNotCollideWithPause() {
        assertEquals(VoiceCommand.PAUSES_OFF, parseRu("без пауз"))
        assertEquals(VoiceCommand.PAUSES_OFF, parseRu("подряд"))
        assertEquals(VoiceCommand.PAUSES_ON, parseRu("с паузами"))
        assertEquals(VoiceCommand.PAUSE, parseRu("пауза"))
    }

    /** "не спрашивай" contains "спрашивай", so the negation must win. */
    @Test
    fun russianNegatedReadingModeWins() {
        assertEquals(VoiceCommand.PAUSES_OFF, parseRu("не спрашивай"))
        assertEquals(VoiceCommand.PAUSES_ON, parseRu("спрашивай"))
    }

    @Test
    fun russianRecognisesSessionCommands() {
        assertEquals(VoiceCommand.HELP, parseRu("помощь"))
        assertEquals(VoiceCommand.END_SESSION, parseRu("завершить"))
        assertEquals(VoiceCommand.LOGOUT, parseRu("выйти из аккаунта"))
    }

    @Test
    fun russianConfirmationIsPlainYesNo() {
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("да", ru))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("отправь", ru))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("нет", ru))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("нет, не отправляй", ru))
    }

    // ---- language selection --------------------------------------------------

    @Test
    fun everyLanguageCanBeSelectedFromEveryLanguage() {
        assertEquals(VoiceCommand.LANGUAGE_RUSSIAN, parseEn("speak russian"))
        assertEquals(VoiceCommand.LANGUAGE_RUSSIAN, parseDe("sprich russisch"))
        assertEquals(VoiceCommand.LANGUAGE_ENGLISH, parseRu("английский"))
        assertEquals(VoiceCommand.LANGUAGE_GERMAN, parseRu("немецкий"))
        assertEquals(VoiceCommand.LANGUAGE_RUSSIAN, parseRu("русский"))
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
