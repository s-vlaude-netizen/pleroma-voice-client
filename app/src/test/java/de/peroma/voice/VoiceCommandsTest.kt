package de.peroma.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandsTest {

    @Test
    fun recognisesTimelineRequests() {
        assertEquals(VoiceCommand.READ_TIMELINE, VoiceCommands.parse("Timeline vorlesen"))
        assertEquals(VoiceCommand.READ_TIMELINE, VoiceCommands.parse("was gibt es neues"))
        assertEquals(VoiceCommand.READ_TIMELINE, VoiceCommands.parse("lies vor bitte"))
    }

    @Test
    fun recognisesNavigation() {
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("nächster Beitrag"))
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("überspringen"))
        assertEquals(VoiceCommand.PREVIOUS, VoiceCommands.parse("vorheriger Beitrag"))
        assertEquals(VoiceCommand.PREVIOUS, VoiceCommands.parse("zurück"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parse("wiederholen"))
    }

    /** "nächster Beitrag" must not be mistaken for "write a post". */
    @Test
    fun navigationWinsOverPostingWhenBothWordsAppear() {
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("nächster Beitrag"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parse("Beitrag wiederholen"))
        assertEquals(VoiceCommand.NEW_POST, VoiceCommands.parse("neuer Beitrag"))
        assertEquals(VoiceCommand.NEW_POST, VoiceCommands.parse("ich möchte etwas posten"))
    }

    @Test
    fun recognisesPlaybackControl() {
        assertEquals(VoiceCommand.PAUSE, VoiceCommands.parse("Pause"))
        assertEquals(VoiceCommand.RESUME, VoiceCommands.parse("weiter"))
        assertEquals(VoiceCommand.STOP_READING, VoiceCommands.parse("stopp"))
        assertEquals(VoiceCommand.FASTER, VoiceCommands.parse("schneller"))
        assertEquals(VoiceCommand.SLOWER, VoiceCommands.parse("langsamer"))
    }

    @Test
    fun recognisesReadingModeCommands() {
        assertEquals(VoiceCommand.PAUSES_OFF, VoiceCommands.parse("am Stück"))
        assertEquals(VoiceCommand.PAUSES_OFF, VoiceCommands.parse("durchlesen"))
        assertEquals(VoiceCommand.PAUSES_OFF, VoiceCommands.parse("ohne Pausen"))
        assertEquals(VoiceCommand.PAUSES_ON, VoiceCommands.parse("mit Pausen"))
        assertEquals(VoiceCommand.PAUSES_ON, VoiceCommands.parse("Pausen an"))
        assertEquals(VoiceCommand.PAUSES_ON, VoiceCommands.parse("nachfragen"))
    }

    /** "Pausen aus" must switch the mode, not pause playback. */
    @Test
    fun readingModeDoesNotCollideWithPause() {
        assertEquals(VoiceCommand.PAUSES_OFF, VoiceCommands.parse("Pausen aus"))
        assertEquals(VoiceCommand.PAUSE, VoiceCommands.parse("Pause"))
        assertEquals(VoiceCommand.PAUSE, VoiceCommands.parse("pausieren"))
    }

    /** "nicht nachfragen" contains "nachfragen", so the negation must win. */
    @Test
    fun negatedReadingModeWins() {
        assertEquals(VoiceCommand.PAUSES_OFF, VoiceCommands.parse("nicht nachfragen"))
        assertEquals(VoiceCommand.PAUSES_OFF, VoiceCommands.parse("nicht unterbrechen"))
    }

    @Test
    fun recognisesSessionCommands() {
        assertEquals(VoiceCommand.HELP, VoiceCommands.parse("Hilfe"))
        assertEquals(VoiceCommand.HELP, VoiceCommands.parse("was kann ich sagen"))
        assertEquals(VoiceCommand.END_SESSION, VoiceCommands.parse("beenden"))
        assertEquals(VoiceCommand.LOGOUT, VoiceCommands.parse("abmelden"))
    }

    /** Whole-word matching: "Jahr" contains "ja" but is not a yes. */
    @Test
    fun doesNotMatchOnSubstrings() {
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parseConfirmation("dieses Jahr war schön"))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parse("völliger Unsinn hier"))
    }

    @Test
    fun ignoresPunctuationAndCase() {
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("NÄCHSTER!"))
        assertEquals(VoiceCommand.HELP, VoiceCommands.parse("hilfe?"))
    }

    @Test
    fun confirmationReadsSendAsYesNotAsNewPost() {
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("senden"))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("ja"))
        assertEquals(VoiceCommand.CONFIRM, VoiceCommands.parseConfirmation("ja bitte abschicken"))
    }

    @Test
    fun confirmationRecognisesRejection() {
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("nein"))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("verwerfen"))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("abbrechen"))
    }

    /** Confirming a post is a plain yes/no — re-dictating is not an option. */
    @Test
    fun confirmationHasNoRedictateOption() {
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parseConfirmation("nochmal"))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parseConfirmation("noch mal"))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parseConfirmation("neu diktieren"))
    }

    /** Recognizers split "nochmal" about as often as they keep it together. */
    @Test
    fun repeatAcceptsBothSpellingsOfNochmal() {
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parse("nochmal"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parse("noch mal"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parse("noch einmal"))
    }

    /** A rejection is checked before acceptance so "nein, nicht senden" is a no. */
    @Test
    fun rejectionWinsWhenBothAppear() {
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("nein, nicht senden"))
    }

    @Test
    fun emptyInputIsUnknown() {
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parse(""))
        assertEquals(VoiceCommand.UNKNOWN, VoiceCommands.parse("   "))
    }
}
