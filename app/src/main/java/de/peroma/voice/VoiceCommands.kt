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
    PAUSES_OFF,
    PAUSES_ON,
    STOP_READING,
    NEW_POST,
    CONFIRM,
    DECLINE,
    FASTER,
    SLOWER,
    STATUS,
    LOGOUT,
    LANGUAGE_ENGLISH,
    LANGUAGE_GERMAN,
    LANGUAGE_RUSSIAN,
    END_SESSION,
    UNKNOWN
}

/**
 * Maps spoken English, German or Russian to commands.
 *
 * Matching is done on whole words rather than substrings, so "Jahr" does not
 * count as "ja" and "Beitrag" inside "nächster Beitrag" does not start a new
 * post. Where two commands share a word, the more specific one is listed
 * first — each table is evaluated in order.
 *
 * Punctuation is stripped before matching, which turns "don't" into two words,
 * so contractions are spelled out that way in the tables below.
 */
object VoiceCommands {

    private val ENGLISH_TABLE: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.END_SESSION to listOf(
            "end voice control", "stop voice control", "end session", "end",
            "goodbye", "good bye", "bye", "quit", "exit"
        ),
        VoiceCommand.HELP to listOf(
            "help", "what can i say", "which commands", "commands"
        ),
        VoiceCommand.LANGUAGE_GERMAN to listOf(
            "german", "speak german", "in german", "deutsch"
        ),
        VoiceCommand.LANGUAGE_ENGLISH to listOf(
            "english", "speak english", "in english"
        ),
        VoiceCommand.LANGUAGE_RUSSIAN to listOf(
            "russian", "speak russian", "in russian"
        ),
        VoiceCommand.NEXT to listOf(
            "next", "skip", "forward"
        ),
        VoiceCommand.PREVIOUS to listOf(
            "previous", "back", "go back", "before"
        ),
        VoiceCommand.REPEAT to listOf(
            "repeat", "again", "once more", "say that again", "what was that"
        ),
        // Before PAUSE, so "no pauses" is never heard as "pause".
        // Negations first: "do not ask" contains "ask".
        VoiceCommand.PAUSES_OFF to listOf(
            "straight through", "without pauses", "without stopping", "no pauses",
            "pauses off", "do not ask", "don t ask", "do not interrupt", "continuous"
        ),
        VoiceCommand.PAUSES_ON to listOf(
            "with pauses", "pauses on", "ask between", "ask me", "stop between"
        ),
        VoiceCommand.PAUSE to listOf(
            "pause", "wait", "hold on", "one moment"
        ),
        VoiceCommand.RESUME to listOf(
            "continue", "resume", "carry on", "keep reading", "go on"
        ),
        VoiceCommand.STOP_READING to listOf(
            "stop", "halt", "be quiet", "silence"
        ),
        VoiceCommand.FASTER to listOf(
            "faster", "speed up", "too slow"
        ),
        VoiceCommand.SLOWER to listOf(
            "slower", "slow down", "too fast"
        ),
        VoiceCommand.READ_TIMELINE to listOf(
            "read timeline", "read the timeline", "read my timeline", "timeline",
            "what is new", "what s new", "news", "home"
        ),
        VoiceCommand.NEW_POST to listOf(
            "new post", "write a post", "compose", "post something", "dictate",
            "write", "post"
        ),
        VoiceCommand.STATUS to listOf(
            "where am i", "what is playing", "what s playing", "status"
        ),
        VoiceCommand.LOGOUT to listOf(
            "log out", "logout", "sign out", "switch account"
        ),
        VoiceCommand.CONFIRM to listOf(
            "yes", "send", "publish", "confirm", "okay", "ok"
        ),
        VoiceCommand.DECLINE to listOf(
            "no", "discard", "cancel", "delete"
        )
    )

    private val GERMAN_TABLE: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.END_SESSION to listOf(
            "sprachsteuerung beenden", "sitzung beenden", "beenden", "schlafen",
            "auf wiedersehen", "tschüss", "tschüss dann", "ende"
        ),
        VoiceCommand.HELP to listOf(
            "hilfe", "was kann ich sagen", "welche befehle", "befehle", "kommandos"
        ),
        VoiceCommand.LANGUAGE_ENGLISH to listOf(
            "englisch", "sprich englisch", "auf englisch", "english"
        ),
        VoiceCommand.LANGUAGE_GERMAN to listOf(
            "deutsch", "sprich deutsch", "auf deutsch"
        ),
        VoiceCommand.LANGUAGE_RUSSIAN to listOf(
            "russisch", "sprich russisch", "auf russisch"
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
        // Listed before PAUSE: "Pausen aus" must never be heard as "Pause".
        // OFF before ON, because "nicht nachfragen" contains "nachfragen".
        VoiceCommand.PAUSES_OFF to listOf(
            "am stück", "durchlesen", "ohne pausen", "ohne unterbrechung",
            "pausen aus", "nicht nachfragen", "nicht unterbrechen"
        ),
        VoiceCommand.PAUSES_ON to listOf(
            "mit pausen", "pausen an", "pausen ein", "zwischendurch fragen",
            "nachfragen", "zwischenfragen"
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


    /**
     * Russian inflects heavily, so a command can arrive in several forms:
     * "следующий пост" but "следующая запись". Rather than stem the input,
     * the common endings are simply listed — the tables stay readable and the
     * matcher stays the same one the other languages use.
     */
    private val RUSSIAN_TABLE: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.END_SESSION to listOf(
            "завершить", "заверши", "закончить", "закончи", "выключи голосовое управление",
            "до свидания", "пока"
        ),
        VoiceCommand.HELP to listOf(
            "помощь", "помоги", "что я могу сказать", "команды"
        ),
        VoiceCommand.LANGUAGE_ENGLISH to listOf(
            "английский", "по английски", "английском"
        ),
        VoiceCommand.LANGUAGE_GERMAN to listOf(
            "немецкий", "по немецки", "немецком"
        ),
        VoiceCommand.LANGUAGE_RUSSIAN to listOf(
            "русский", "по русски", "русском"
        ),
        VoiceCommand.NEXT to listOf(
            "следующий", "следующая", "следующее", "дальше", "пропусти", "пропустить"
        ),
        VoiceCommand.PREVIOUS to listOf(
            "предыдущий", "предыдущая", "предыдущее", "назад", "вернись"
        ),
        VoiceCommand.REPEAT to listOf(
            "повтори", "повторить", "ещё раз", "еще раз", "что это было"
        ),
        // Before PAUSE, so "без пауз" is not heard as "пауза".
        // Negations first: "не спрашивай" contains "спрашивай".
        VoiceCommand.PAUSES_OFF to listOf(
            "подряд", "без пауз", "без остановок", "не спрашивай", "не перебивай"
        ),
        VoiceCommand.PAUSES_ON to listOf(
            "с паузами", "спрашивай", "спрашивай между"
        ),
        VoiceCommand.PAUSE to listOf(
            "пауза", "подожди", "погоди", "минуту"
        ),
        VoiceCommand.RESUME to listOf(
            "продолжай", "продолжи", "продолжить", "читай дальше"
        ),
        VoiceCommand.STOP_READING to listOf(
            "стоп", "хватит", "прекрати", "останови"
        ),
        VoiceCommand.FASTER to listOf(
            "быстрее", "слишком медленно"
        ),
        VoiceCommand.SLOWER to listOf(
            "медленнее", "слишком быстро"
        ),
        VoiceCommand.READ_TIMELINE to listOf(
            "читай ленту", "прочитай ленту", "лента", "ленту", "новости",
            "что нового", "главная"
        ),
        VoiceCommand.NEW_POST to listOf(
            "новый пост", "написать пост", "написать", "продиктовать", "опубликовать пост",
            "пост", "запись"
        ),
        VoiceCommand.STATUS to listOf(
            "где я", "что сейчас", "текущий статус"
        ),
        VoiceCommand.LOGOUT to listOf(
            "выйти из аккаунта", "выйти", "выход", "сменить аккаунт"
        ),
        VoiceCommand.CONFIRM to listOf(
            "да", "отправь", "отправить", "опубликуй", "подтверди", "хорошо", "ок"
        ),
        VoiceCommand.DECLINE to listOf(
            "нет", "отмена", "отмени", "удали", "не надо"
        )
    )

    /**
     * The confirmation step is a plain yes/no.
     *
     * "send" has to read as yes here rather than as "write a post". There is
     * deliberately no re-dictate option: it did the same thing as no from the
     * user's point of view, and it was unreliable anyway, since recognizers
     * write "nochmal" as two words about as often as one.
     */
    private val ENGLISH_CONFIRMATION: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.DECLINE to listOf(
            "no", "discard", "cancel", "delete", "stop", "do not send", "don t send"
        ),
        VoiceCommand.CONFIRM to listOf(
            "yes", "send", "publish", "confirm", "okay", "ok", "correct", "right"
        )
    )

    private val GERMAN_CONFIRMATION: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.DECLINE to listOf(
            "nein", "verwerfen", "abbrechen", "löschen", "doch nicht", "stopp"
        ),
        VoiceCommand.CONFIRM to listOf(
            "ja", "senden", "abschicken", "veröffentlichen", "bestätigen", "okay", "ok",
            "passt", "richtig"
        )
    )

    private val RUSSIAN_CONFIRMATION: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.DECLINE to listOf(
            "нет", "отмена", "отмени", "удали", "не надо", "не отправляй", "стоп"
        ),
        VoiceCommand.CONFIRM to listOf(
            "да", "отправь", "отправить", "опубликуй", "подтверди", "хорошо", "ок",
            "верно", "правильно"
        )
    )

    private val TABLES = mapOf(
        Language.ENGLISH to prepare(ENGLISH_TABLE),
        Language.GERMAN to prepare(GERMAN_TABLE),
        Language.RUSSIAN to prepare(RUSSIAN_TABLE)
    )

    private val CONFIRMATIONS = mapOf(
        Language.ENGLISH to prepare(ENGLISH_CONFIRMATION),
        Language.GERMAN to prepare(GERMAN_CONFIRMATION),
        Language.RUSSIAN to prepare(RUSSIAN_CONFIRMATION)
    )

    private fun prepare(
        table: List<Pair<VoiceCommand, List<String>>>
    ): List<Pair<VoiceCommand, List<List<String>>>> =
        table.map { (command, phrases) -> command to phrases.map { words(it) } }

    fun parse(spoken: String, language: Language): VoiceCommand =
        match(spoken, TABLES.getValue(language))

    fun parseConfirmation(spoken: String, language: Language): VoiceCommand =
        match(spoken, CONFIRMATIONS.getValue(language))

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
}
