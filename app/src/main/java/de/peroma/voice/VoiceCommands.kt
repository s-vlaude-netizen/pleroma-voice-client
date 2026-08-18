package de.peroma.voice

/** Everything the app can be told to do by voice. */
enum class VoiceCommand {
    HELP,
    READ_TIMELINE,
    NEXT,
    PREVIOUS,
    REPEAT,
    PAUSE,
    RESUME,
    STOP_READING,
    NEW_POST,
    CONFIRM,
    DECLINE,
    FASTER,
    SLOWER,
    STATUS,
    LOGOUT,
    END_SESSION,
    UNKNOWN
}

/**
 * Maps spoken German to commands.
 *
 * Matching is done on whole words rather than substrings, so "Jahr" does not
 * count as "ja" and "Beitrag" inside "nächster Beitrag" does not start a new
 * post. Where two commands share a word, the more specific one is listed
 * first — the table is evaluated in order.
 */
object VoiceCommands {

    private val TABLE: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.END_SESSION to listOf(
            "sprachsteuerung beenden", "sitzung beenden", "beenden", "schlafen",
            "auf wiedersehen", "tschüss", "tschüss dann", "ende"
        ),
        VoiceCommand.HELP to listOf(
            "hilfe", "was kann ich sagen", "welche befehle", "befehle", "kommandos"
        ),
        VoiceCommand.NEXT to listOf(
            "nächster", "nächste", "nächstes", "weiter zum nächsten",
            "überspringen", "überspring", "skip"
        ),
        VoiceCommand.PREVIOUS to listOf(
            "vorheriger", "vorherige", "vorheriges", "zurück", "davor", "nochmal zurück"
        ),
        VoiceCommand.REPEAT to listOf(
            // Recognizers write this as one word or two, so accept both.
            "wiederholen", "wiederhole", "nochmal", "noch mal", "noch einmal",
            "was war das"
        ),
        VoiceCommand.PAUSE to listOf(
            "pause", "pausieren", "warte", "moment", "still"
        ),
        VoiceCommand.RESUME to listOf(
            "weiterlesen", "weiter", "fortsetzen", "mach weiter"
        ),
        VoiceCommand.STOP_READING to listOf(
            "stopp", "stop", "anhalten", "aufhören", "hör auf", "abbrechen lesen"
        ),
        VoiceCommand.FASTER to listOf(
            "schneller", "schneller sprechen", "zu langsam"
        ),
        VoiceCommand.SLOWER to listOf(
            "langsamer", "langsamer sprechen", "zu schnell"
        ),
        VoiceCommand.READ_TIMELINE to listOf(
            "timeline vorlesen", "timeline lesen", "timeline", "vorlesen", "lies vor",
            "neuigkeiten", "was gibt es neues", "startseite", "zeitleiste"
        ),
        VoiceCommand.NEW_POST to listOf(
            "neuer beitrag", "neuen beitrag", "beitrag schreiben", "beitrag senden",
            "etwas posten", "posten", "diktieren", "schreiben", "beitrag"
        ),
        VoiceCommand.STATUS to listOf(
            "wo bin ich", "was läuft", "aktueller stand"
        ),
        VoiceCommand.LOGOUT to listOf(
            "abmelden", "ausloggen", "konto wechseln"
        ),
        VoiceCommand.CONFIRM to listOf(
            "ja", "senden", "abschicken", "veröffentlichen", "bestätigen", "okay", "ok"
        ),
        VoiceCommand.DECLINE to listOf(
            "nein", "verwerfen", "abbrechen", "löschen", "doch nicht"
        )
    )

    private val PREPARED: List<Pair<VoiceCommand, List<List<String>>>> =
        TABLE.map { (command, phrases) -> command to phrases.map { words(it) } }

    /**
     * The confirmation step is a plain yes/no.
     *
     * "senden" has to read as yes here rather than as "write a post". There is
     * deliberately no re-dictate option: it did the same thing as no from the
     * user's point of view, and it was unreliable anyway, since recognizers
     * write "nochmal" as two words about as often as one.
     */
    private val CONFIRMATION: List<Pair<VoiceCommand, List<List<String>>>> = listOf(
        VoiceCommand.DECLINE to listOf(
            "nein", "verwerfen", "abbrechen", "löschen", "doch nicht", "stopp"
        ).map { words(it) },
        VoiceCommand.CONFIRM to listOf(
            "ja", "senden", "abschicken", "veröffentlichen", "bestätigen", "okay", "ok",
            "passt", "richtig"
        ).map { words(it) }
    )

    fun parse(spoken: String): VoiceCommand = match(spoken, PREPARED)

    fun parseConfirmation(spoken: String): VoiceCommand = match(spoken, CONFIRMATION)

    private fun match(
        spoken: String,
        table: List<Pair<VoiceCommand, List<List<String>>>>
    ): VoiceCommand {
        val said = words(spoken)
        if (said.isEmpty()) return VoiceCommand.UNKNOWN
        for ((command, phrases) in table) {
            if (phrases.any { contains(said, it) }) return command
        }
        return VoiceCommand.UNKNOWN
    }

    private fun words(input: String): List<String> =
        input.lowercase()
            .replace(Regex("[^\\p{L}0-9]+"), " ")
            .split(' ')
            .filter { it.isNotBlank() }

    /** True when [phrase] occurs as a consecutive run of words inside [said]. */
    private fun contains(said: List<String>, phrase: List<String>): Boolean {
        if (phrase.isEmpty() || phrase.size > said.size) return false
        for (start in 0..said.size - phrase.size) {
            if (phrase.indices.all { said[start + it] == phrase[it] }) return true
        }
        return false
    }

    /** Spoken when the user asks for help. */
    const val HELP_TEXT: String =
        "Du kannst sagen: Timeline vorlesen. Nächster Beitrag. Vorheriger Beitrag. " +
            "Wiederholen. Pause. Weiter. Stopp. Neuer Beitrag, um etwas zu diktieren. " +
            "Schneller oder langsamer für das Sprechtempo. Abmelden. " +
            "Oder: Beenden, um die Sprachsteuerung zu schließen."
}
