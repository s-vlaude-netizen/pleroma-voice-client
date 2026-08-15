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
    fun confirmationRecognisesRejectionAndRetry() {
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("nein"))
        assertEquals(VoiceCommand.DECLINE, VoiceCommands.parseConfirmation("verwerfen"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parseConfirmation("nochmal"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parseConfirmation("neu diktieren"))
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
