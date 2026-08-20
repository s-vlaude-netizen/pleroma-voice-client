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
    LANGUAGE_JAPANESE,
    END_SESSION,
    UNKNOWN
}

/**
 * Maps spoken English, German or Japanese to commands.
 *
 * In English and German, matching is done on whole words rather than
 * substrings, so "Jahr" does not count as "ja" and "Beitrag" inside "nächster
 * Beitrag" does not start a new post. Japanese is written without spaces, so
 * there are no word boundaries to match on and its phrases are looked for as
 * substrings instead.
 *
 * Either way, where two commands share a phrase the more specific one is
 * listed first — each table is evaluated in order. Substring matching makes
 * that ordering matter far more, so the Japanese table is commented where a
 * phrase contains another.
 *
 * Punctuation is stripped before matching, which turns "don't" into two words,
 * so contractions are spelled out that way in the tables below.
 */
object VoiceCommands {

    private val ENGLISH_TABLE: List<Pair<VoiceCommand, List<String>>> = listOf(
        // "end" on its own is a poor voice command: recognizers hear a single
        // short function word as "and" or "in" about as often as "end". It stays
        // for anyone who tries it, but the reliable words come first and the
        // help text offers those instead.
        VoiceCommand.END_SESSION to listOf(
            "quit", "goodbye", "good bye", "bye", "exit", "finish", "finished",
            "i am done", "im done", "done", "that is all", "shut down",
            "end voice control", "stop voice control", "end session",
            "close voice control", "end"
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
        VoiceCommand.LANGUAGE_JAPANESE to listOf(
            "japanese", "speak japanese", "in japanese", "nihongo"
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
        VoiceCommand.LANGUAGE_JAPANESE to listOf(
            "japanisch", "sprich japanisch", "auf japanisch", "japanese"
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
     * Japanese is matched on substrings, so a short phrase swallows every
     * longer one that contains it. The order below is therefore load-bearing,
     * and each phrase is kept to the invariant part of the expression: 続けて
     * covers 続けて and 続けてください alike, so politeness endings need no
     * entries of their own.
     */
    private val JAPANESE_TABLE: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.END_SESSION to listOf(
            "終了", "終わり", "おわり", "さようなら", "さよなら", "バイバイ",
            "おしまい", "やめる"
        ),
        VoiceCommand.HELP to listOf(
            "ヘルプ", "助けて", "たすけて", "使い方", "何が言える", "コマンド"
        ),
        VoiceCommand.LANGUAGE_ENGLISH to listOf(
            "英語", "えいご", "イングリッシュ"
        ),
        VoiceCommand.LANGUAGE_GERMAN to listOf(
            "ドイツ語", "どいつご"
        ),
        VoiceCommand.LANGUAGE_JAPANESE to listOf(
            "日本語", "にほんご"
        ),
        VoiceCommand.NEXT to listOf(
            "次", "つぎ", "スキップ", "飛ばして"
        ),
        VoiceCommand.PREVIOUS to listOf(
            "前", "まえ", "戻", "もどって"
        ),
        VoiceCommand.REPEAT to listOf(
            "もう一度", "もういちど", "繰り返", "くりかえ", "何だって"
        ),
        // Before RESUME (続けて読 contains 続けて) and before STOP_READING and
        // PAUSES_ON (間で止まって contains 止まって).
        VoiceCommand.PAUSES_OFF to listOf(
            "続けて読", "通しで", "止めないで", "聞かないで", "中断しないで",
            "ポーズなし", "最後まで"
        ),
        VoiceCommand.PAUSES_ON to listOf(
            "ポーズあり", "途中で聞", "間で聞", "間で止まって", "一つずつ", "毎回聞"
        ),
        // Before STOP_READING: 一時停止 contains 停止. The bare kana まって is
        // deliberately absent — 止まって, which stops the reading altogether,
        // ends in it.
        VoiceCommand.PAUSE to listOf(
            "一時停止", "ちょっと待って", "ちょっとまって", "待って"
        ),
        VoiceCommand.RESUME to listOf(
            "続けて", "続き", "再開", "つづけて"
        ),
        VoiceCommand.STOP_READING to listOf(
            "停止", "ストップ", "止まって", "やめて", "静かに"
        ),
        VoiceCommand.FASTER to listOf(
            "速く", "はやく", "早く"
        ),
        VoiceCommand.SLOWER to listOf(
            "ゆっくり", "遅く", "おそく"
        ),
        VoiceCommand.READ_TIMELINE to listOf(
            "タイムライン", "読んで", "よんで", "ホーム", "新着", "ニュース"
        ),
        VoiceCommand.NEW_POST to listOf(
            "新しい投稿", "投稿", "ポスト", "書く", "書きたい", "口述"
        ),
        VoiceCommand.STATUS to listOf(
            "今どこ", "いまどこ", "状態", "ステータス"
        ),
        VoiceCommand.LOGOUT to listOf(
            "ログアウト", "サインアウト", "アカウント切り替え"
        ),
        // 投稿して is deliberately absent here: it would be caught by NEW_POST
        // above. It is offered in the confirmation table, where no such
        // command competes for it.
        VoiceCommand.CONFIRM to listOf(
            "はい", "送信", "送って", "オーケー", "オッケー", "いいよ"
        ),
        // Last: やめ is contained in END_SESSION's やめる and STOP_READING's やめて.
        VoiceCommand.DECLINE to listOf(
            "いいえ", "キャンセル", "取り消", "削除", "だめ", "やめ"
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

    // 送らない has to be tested before 送 in any form, so DECLINE stays first
    // here just as it does in the other languages.
    private val JAPANESE_CONFIRMATION: List<Pair<VoiceCommand, List<String>>> = listOf(
        VoiceCommand.DECLINE to listOf(
            "いいえ", "送らない", "送りません", "キャンセル", "取り消", "削除",
            "だめ", "やめ", "違う", "ちがう"
        ),
        VoiceCommand.CONFIRM to listOf(
            "はい", "送信", "送って", "投稿して", "オーケー", "オッケー", "いいよ",
            "そのまま", "合ってる", "正しい"
        )
    )

    private val TABLES = mapOf(
        Language.ENGLISH to Prepared(Language.ENGLISH, ENGLISH_TABLE),
        Language.GERMAN to Prepared(Language.GERMAN, GERMAN_TABLE),
        Language.JAPANESE to Prepared(Language.JAPANESE, JAPANESE_TABLE)
    )

    private val CONFIRMATIONS = mapOf(
        Language.ENGLISH to Prepared(Language.ENGLISH, ENGLISH_CONFIRMATION),
        Language.GERMAN to Prepared(Language.GERMAN, GERMAN_CONFIRMATION),
        Language.JAPANESE to Prepared(Language.JAPANESE, JAPANESE_CONFIRMATION)
    )

    fun parse(spoken: String, language: Language): VoiceCommand =
        TABLES.getValue(language).match(spoken)

    fun parseConfirmation(spoken: String, language: Language): VoiceCommand =
        CONFIRMATIONS.getValue(language).match(spoken)

    /**
     * A table with its phrases normalised once, ahead of any recognition.
     *
     * Both matching modes start from the same word split, which is what strips
     * the punctuation a recognizer adds. Languages written with spaces then
     * compare word by word; the others compare the words joined back together,
     * which for Japanese simply means the utterance with its punctuation and
     * any stray spaces removed.
     */
    private class Prepared(
        language: Language,
        table: List<Pair<VoiceCommand, List<String>>>
    ) {
        private val byWord = language.hasWordBoundaries

        private val entries: List<Pair<VoiceCommand, List<List<String>>>> =
            table.map { (command, phrases) -> command to phrases.map { words(it) } }

        private val joined: List<Pair<VoiceCommand, List<String>>> =
            if (byWord) emptyList()
            else entries.map { (command, phrases) ->
                command to phrases.map { it.joinToString("") }
            }

        fun match(spoken: String): VoiceCommand {
            val said = words(spoken)
            if (said.isEmpty()) return VoiceCommand.UNKNOWN
            if (byWord) {
                for ((command, phrases) in entries) {
                    if (phrases.any { contains(said, it) }) return command
                }
            } else {
                val text = said.joinToString("")
                for ((command, phrases) in joined) {
                    if (phrases.any { it.isNotEmpty() && text.contains(it) }) return command
                }
            }
            return VoiceCommand.UNKNOWN
        }
    }
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
