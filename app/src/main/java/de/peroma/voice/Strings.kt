package de.peroma.voice

import java.util.Locale

/** The languages the app can speak and listen in. */
enum class Language(val tag: String) {
    ENGLISH("en"),
    GERMAN("de"),
    JAPANESE("ja");

    /**
     * Used to pick a speech voice. Deliberately without a region: any installed
     * voice for the language qualifies, and demanding en-GB over en-US would
     * only push the engine onto its fallback.
     */
    val locale: Locale
        get() = Locale(tag)

    /**
     * Japanese is written without spaces, so commands cannot be matched word by
     * word the way English and German are. Such languages are matched on
     * substrings instead.
     */
    val hasWordBoundaries: Boolean
        get() = this != JAPANESE

    /**
     * Used to ask the recognizer for a language. The region is included,
     * because several recognition services honour a full BCP-47 tag and quietly
     * fall back to the system language when given a bare one.
     */
    val recognizerTag: String
        get() = when (this) {
            ENGLISH -> "en-US"
            GERMAN -> "de-DE"
            JAPANESE -> "ja-JP"
        }

    /** The language's own name, so the button reads the same in every locale. */
    val displayName: String
        get() = when (this) {
            ENGLISH -> "English"
            GERMAN -> "Deutsch"
            JAPANESE -> "日本語"
        }

    val strings: Strings
        get() = when (this) {
            ENGLISH -> EnglishStrings
            GERMAN -> GermanStrings
            JAPANESE -> JapaneseStrings
        }

    /** The next language in the cycle, for the button on screen. */
    fun next(): Language = values()[(ordinal + 1) % values().size]

    companion object {
        /** Device language, falling back to English for anything unsupported. */
        fun fromDevice(): Language =
            values().firstOrNull { it.tag == Locale.getDefault().language } ?: ENGLISH

        fun fromTag(tag: String): Language? = values().firstOrNull { it.tag == tag }

        fun tagOf(language: Language): String = language.tag
    }
}

/** Why a request failed, so the wording can be chosen per language. */
enum class ApiErrorKind {
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    REJECTED,
    RATE_LIMITED,
    SERVER,
    UNREACHABLE,
    NO_CREDENTIALS,
    NO_TOKEN,
    OTHER
}

/**
 * Everything the app says out loud, in one place per language.
 *
 * Spoken text lives here rather than in `strings.xml` so that the pieces that
 * build it — [Post], [SpeechText], [VoiceCommands] — stay plain Kotlin and can
 * be unit tested on the JVM in both languages. Screen labels do use Android
 * resources, where the framework's own language switching applies.
 */
interface Strings {

    // ---- speech-text building blocks ---------------------------------------
    fun linkTo(host: String): String
    val bareLink: String
    fun hashtag(tag: String): String
    val ampersand: String
    val unknownAuthor: String

    // ---- a single post -----------------------------------------------------
    fun postCounter(index: Int, total: Int): String
    fun boostedBy(booster: String, author: String): String
    fun byAuthor(author: String): String
    fun contentWarning(warning: String): String
    val noReadableText: String
    fun oneAttachment(description: String): String
    fun manyAttachments(count: Int): String
    fun attachmentWith(type: String, description: String): String
    fun attachmentWithout(type: String): String
    val mediaImage: String
    val mediaVideo: String
    val mediaAudio: String
    val mediaAnimation: String
    val mediaOther: String

    // ---- session -----------------------------------------------------------
    val ready: String
    val ttsUnavailable: String
    val preparingSpeech: String
    val notLoggedIn: String
    val sessionStarted: String
    val sessionEnded: String
    val loggedOut: String
    val noRecognizer: String
    val missingMicPermission: String
    val recognizerRefusedMic: String
    fun recognizerDiagnostic(packages: List<String>): String
    val nothingHeardEnding: String
    val stillListening: String
    val notUnderstood: String
    val paused: String
    val helpText: String
    val pausesOn: String
    val pausesOff: String
    /** Spoken after switching, in the language just switched to. */
    val nowSpeakingThisLanguage: String
    fun speechRate(percent: Int): String

    // ---- timeline ----------------------------------------------------------
    val loadingTimeline: String
    val emptyTimeline: String
    fun timelineFailed(reason: String): String
    val noTimelineLoaded: String
    fun statusAt(index: Int, total: Int, summary: String): String
    fun allPostsRead(total: Int): String
    val noFurtherPost: String
    val whatNow: String
    val alreadyFirstPost: String

    // ---- dictation ---------------------------------------------------------
    val dictatePrompt: String
    val nothingHeardRetry: String
    val nothingHeardToMenu: String
    val dictationCancelled: String
    fun confirmDraft(draft: String): String
    val confirmAgain: String
    val sayYesOrNo: String
    val draftDiscarded: String
    val draftDiscardedNotUnderstood: String
    val draftDiscardedSilence: String
    val noDraft: String
    val sending: String
    val published: String
    fun sendFailed(reason: String): String

    // ---- errors ------------------------------------------------------------
    fun apiError(kind: ApiErrorKind, status: Int, detail: String): String
    val unknownError: String
}

object EnglishStrings : Strings {

    override fun linkTo(host: String) = "link to $host"
    override val bareLink = "a link"
    override fun hashtag(tag: String) = "hashtag $tag"
    override val ampersand = " and "
    override val unknownAuthor = "Unknown"

    override fun postCounter(index: Int, total: Int) = "Post $index of $total."
    override fun boostedBy(booster: String, author: String) =
        "$booster shared a post by $author."

    override fun byAuthor(author: String) = "By $author."
    override fun contentWarning(warning: String) = "Content warning: $warning."
    override val noReadableText = "This post has no readable text."
    override fun oneAttachment(description: String) = "One attachment. $description"
    override fun manyAttachments(count: Int) = "$count attachments."
    override fun attachmentWith(type: String, description: String) = "$type: $description."
    override fun attachmentWithout(type: String) = "$type without a description."
    override val mediaImage = "Image"
    override val mediaVideo = "Video"
    override val mediaAudio = "Audio"
    override val mediaAnimation = "Animation"
    override val mediaOther = "Attachment"

    override val ready = "Ready."
    override val ttsUnavailable = "Speech output is not available."
    override val preparingSpeech = "Preparing speech output …"
    override val notLoggedIn = "You are not logged in."
    override val sessionStarted =
        "Voice control is active. You can switch the screen off now. " +
            "Say read timeline, new post, or help."
    override val sessionEnded = "Voice control ended. See you."
    override val loggedOut = "You are logged out."
    override val noRecognizer =
        "This device has no speech recognition set up. Voice control will stop."
    override val missingMicPermission =
        "I do not have microphone permission. Please grant it in the app."
    override val recognizerRefusedMic =
        "The speech recognition service is refusing the microphone, even though this " +
            "app has permission. Check that your voice input app — usually Google — " +
            "has microphone permission, or pick a different voice input service in " +
            "the system settings."

    override fun recognizerDiagnostic(packages: List<String>): String {
        val listed = if (packages.isEmpty()) "none found" else packages.joinToString(", ")
        return "Speech recognition refused the microphone although this app holds " +
            "RECORD_AUDIO. Recognition services installed: $listed."
    }
    override val nothingHeardEnding = "I cannot hear anything, so I am ending voice control."
    override val stillListening = "I am listening. Say help if you need the commands."
    override val notUnderstood = "I did not understand that. Say help for the available commands."
    override val paused = "Paused. Say continue to carry on."
    override val helpText =
        "You can say: read timeline. Next post. Previous post. Repeat. Pause. Continue. " +
            "Stop. New post, to dictate something. Faster or slower for the speaking rate. " +
            "With pauses, if I should ask between posts, or straight through, if I should " +
            "read without stopping. Say German or Japanese to switch language. " +
            "Say log out to sign out of your account. " +
            "Say quit, or goodbye, to close voice control."
    override val pausesOn = "I will ask between posts from now on."
    override val pausesOff = "I will read the timeline straight through from now on."
    override val nowSpeakingThisLanguage = "I will speak English from now on."

    override fun speechRate(percent: Int) = "Rate $percent percent."

    override val loadingTimeline = "Loading the timeline."
    override val emptyTimeline = "Your timeline has no posts."
    override fun timelineFailed(reason: String) = "The timeline could not be loaded. $reason"
    override val noTimelineLoaded = "No timeline is loaded. Say read timeline."
    override fun statusAt(index: Int, total: Int, summary: String) =
        "Post $index of $total. $summary"

    override fun allPostsRead(total: Int) = "That was all $total posts."
    override val noFurtherPost = "There is no further post."
    override val whatNow = "Would you like to write a post? Say new post, read timeline, or end."
    override val alreadyFirstPost = "This is already the first post."

    override val dictatePrompt = "Speak your post after the tone."
    override val nothingHeardRetry = "I did not hear anything. Speak your post after the tone."
    override val nothingHeardToMenu = "I did not hear anything. Back to the main menu."
    override val dictationCancelled = "Cancelled. Back to the main menu."
    override fun confirmDraft(draft: String) =
        "Your post reads: $draft. Should I send it? Say yes or no."

    override val confirmAgain = "Should I send the post? Say yes or no."
    override val sayYesOrNo = "Please say yes or no."
    override val draftDiscarded = "Post discarded. Back to the main menu."
    override val draftDiscardedNotUnderstood =
        "I did not understand you. The post is discarded. Back to the main menu."
    override val draftDiscardedSilence = "I am discarding the post. Back to the main menu."
    override val noDraft = "There is no post to send."
    override val sending = "Sending."
    override val published = "Post published. What would you like to do?"
    override fun sendFailed(reason: String) =
        "Sending failed. $reason. Should I try again? Say yes or no."

    override fun apiError(kind: ApiErrorKind, status: Int, detail: String): String {
        val suffix = if (detail.isNotBlank()) ": $detail" else ""
        return when (kind) {
            ApiErrorKind.UNAUTHORIZED -> "Not logged in, or the access has expired$suffix"
            ApiErrorKind.FORBIDDEN -> "Access denied$suffix"
            ApiErrorKind.NOT_FOUND ->
                "Endpoint not found — is that really a Pleroma instance?$suffix"
            ApiErrorKind.REJECTED -> "The post was rejected$suffix"
            ApiErrorKind.RATE_LIMITED -> "Too many requests, please wait a moment$suffix"
            ApiErrorKind.SERVER -> "Server error ($status)$suffix"
            ApiErrorKind.UNREACHABLE -> "Server unreachable"
            ApiErrorKind.NO_CREDENTIALS -> "The instance returned no OAuth credentials"
            ApiErrorKind.NO_TOKEN -> "No access token was returned"
            ApiErrorKind.OTHER -> "HTTP $status$suffix"
        }
    }

    override val unknownError = "Unknown error"
}

object GermanStrings : Strings {

    override fun linkTo(host: String) = "Link zu $host"
    override val bareLink = "ein Link"
    override fun hashtag(tag: String) = "Hashtag $tag"
    override val ampersand = " und "
    override val unknownAuthor = "Unbekannt"

    override fun postCounter(index: Int, total: Int) = "Beitrag $index von $total."
    override fun boostedBy(booster: String, author: String) =
        "$booster teilt einen Beitrag von $author."

    override fun byAuthor(author: String) = "Von $author."
    override fun contentWarning(warning: String) = "Inhaltswarnung: $warning."
    override val noReadableText = "Dieser Beitrag enthält keinen lesbaren Text."
    override fun oneAttachment(description: String) = "Ein Anhang. $description"
    override fun manyAttachments(count: Int) = "$count Anhänge."
    override fun attachmentWith(type: String, description: String) = "$type: $description."
    override fun attachmentWithout(type: String) = "$type ohne Beschreibung."
    override val mediaImage = "Bild"
    override val mediaVideo = "Video"
    override val mediaAudio = "Audio"
    override val mediaAnimation = "Animation"
    override val mediaOther = "Anhang"

    override val ready = "Bereit."
    override val ttsUnavailable = "Sprachausgabe nicht verfügbar."
    override val preparingSpeech = "Sprachausgabe wird vorbereitet …"
    override val notLoggedIn = "Du bist nicht angemeldet."
    override val sessionStarted =
        "Sprachsteuerung aktiv. Du kannst den Bildschirm jetzt ausschalten. " +
            "Sag Timeline vorlesen, Neuer Beitrag, oder Hilfe."
    override val sessionEnded = "Sprachsteuerung beendet. Bis bald."
    override val loggedOut = "Du bist abgemeldet."
    override val noRecognizer =
        "Auf diesem Gerät ist keine Spracherkennung eingerichtet. " +
            "Die Sprachsteuerung wird beendet."
    override val missingMicPermission =
        "Mir fehlt die Freigabe für das Mikrofon. Bitte in der App erteilen."
    override val recognizerRefusedMic =
        "Der Spracherkennungsdienst verweigert das Mikrofon, obwohl diese App die " +
            "Freigabe hat. Prüfe, ob deine Spracheingabe-App — meist Google — " +
            "Mikrofonzugriff hat, oder wähle in den Systemeinstellungen einen " +
            "anderen Dienst für die Spracheingabe."

    override fun recognizerDiagnostic(packages: List<String>): String {
        val listed = if (packages.isEmpty()) "keine gefunden" else packages.joinToString(", ")
        return "Spracherkennung verweigert das Mikrofon, obwohl die App RECORD_AUDIO " +
            "besitzt. Installierte Erkennungsdienste: $listed."
    }
    override val nothingHeardEnding = "Ich höre nichts mehr und beende die Sprachsteuerung."
    override val stillListening = "Ich höre zu. Sag Hilfe, wenn du die Befehle brauchst."
    override val notUnderstood =
        "Das habe ich nicht verstanden. Sag Hilfe für die möglichen Befehle."
    override val paused = "Pausiert. Sag Weiter, um fortzufahren."
    override val helpText =
        "Du kannst sagen: Timeline vorlesen. Nächster Beitrag. Vorheriger Beitrag. " +
            "Wiederholen. Pause. Weiter. Stopp. Neuer Beitrag, um etwas zu diktieren. " +
            "Schneller oder langsamer für das Sprechtempo. " +
            "Mit Pausen, wenn ich zwischen den Beiträgen nachfragen soll, " +
            "oder Am Stück, wenn ich durchlesen soll. " +
            "Sag Englisch oder Japanisch, um die Sprache zu wechseln. " +
            "Sag Abmelden, um dich vom Konto abzumelden. " +
            "Sag Beenden, um die Sprachsteuerung zu schließen."
    override val pausesOn = "Ich frage jetzt zwischen den Beiträgen nach."
    override val pausesOff = "Ich lese die Timeline jetzt am Stück vor."
    override val nowSpeakingThisLanguage = "Ich spreche ab jetzt Deutsch."

    override fun speechRate(percent: Int) = "Tempo $percent Prozent."

    override val loadingTimeline = "Timeline wird geladen."
    override val emptyTimeline = "Deine Timeline enthält keine Beiträge."
    override fun timelineFailed(reason: String) =
        "Die Timeline konnte nicht geladen werden. $reason"

    override val noTimelineLoaded = "Es ist keine Timeline geladen. Sag Timeline vorlesen."
    override fun statusAt(index: Int, total: Int, summary: String) =
        "Beitrag $index von $total. $summary"

    override fun allPostsRead(total: Int) = "Das waren alle $total Beiträge."
    override val noFurtherPost = "Es gibt keinen weiteren Beitrag."
    override val whatNow =
        "Möchtest du einen Beitrag schreiben? Sag Neuer Beitrag, Timeline vorlesen, oder Beenden."
    override val alreadyFirstPost = "Das ist bereits der erste Beitrag."

    override val dictatePrompt = "Sprich deinen Beitrag nach dem Ton."
    override val nothingHeardRetry =
        "Ich habe nichts gehört. Sprich deinen Beitrag nach dem Ton."
    override val nothingHeardToMenu = "Ich habe nichts gehört. Zurück zum Hauptmenü."
    override val dictationCancelled = "Abgebrochen. Zurück zum Hauptmenü."
    override fun confirmDraft(draft: String) =
        "Dein Beitrag lautet: $draft. Soll ich das senden? Sag Ja oder Nein."

    override val confirmAgain = "Soll ich den Beitrag senden? Sag Ja oder Nein."
    override val sayYesOrNo = "Bitte sag Ja oder Nein."
    override val draftDiscarded = "Beitrag verworfen. Zurück zum Hauptmenü."
    override val draftDiscardedNotUnderstood =
        "Ich habe dich nicht verstanden. Der Beitrag wird verworfen. Zurück zum Hauptmenü."
    override val draftDiscardedSilence = "Ich verwerfe den Beitrag. Zurück zum Hauptmenü."
    override val noDraft = "Es liegt kein Beitrag vor."
    override val sending = "Wird gesendet."
    override val published = "Beitrag veröffentlicht. Was möchtest du tun?"
    override fun sendFailed(reason: String) =
        "Senden fehlgeschlagen. $reason. Soll ich es erneut versuchen? Sag Ja oder Nein."

    override fun apiError(kind: ApiErrorKind, status: Int, detail: String): String {
        val suffix = if (detail.isNotBlank()) ": $detail" else ""
        return when (kind) {
            ApiErrorKind.UNAUTHORIZED -> "Nicht angemeldet oder Zugang abgelaufen$suffix"
            ApiErrorKind.FORBIDDEN -> "Zugriff verweigert$suffix"
            ApiErrorKind.NOT_FOUND ->
                "Endpunkt nicht gefunden — ist das wirklich eine Pleroma-Instanz?$suffix"
            ApiErrorKind.REJECTED -> "Beitrag abgelehnt$suffix"
            ApiErrorKind.RATE_LIMITED -> "Zu viele Anfragen, bitte kurz warten$suffix"
            ApiErrorKind.SERVER -> "Server-Fehler ($status)$suffix"
            ApiErrorKind.UNREACHABLE -> "Server nicht erreichbar"
            ApiErrorKind.NO_CREDENTIALS -> "Die Instanz lieferte keine OAuth-Zugangsdaten"
            ApiErrorKind.NO_TOKEN -> "Kein Zugriffstoken erhalten"
            ApiErrorKind.OTHER -> "HTTP $status$suffix"
        }
    }

    override val unknownError = "Unbekannter Fehler"
}


object JapaneseStrings : Strings {

    override fun linkTo(host: String) = "$host へのリンク"
    override val bareLink = "リンク"
    override fun hashtag(tag: String) = "ハッシュタグ $tag"
    override val ampersand = "と"
    override val unknownAuthor = "不明"

    override fun postCounter(index: Int, total: Int) = "$total 件中 $index 件目。"
    override fun boostedBy(booster: String, author: String) =
        "$booster さんが $author さんの投稿をシェアしました。"

    override fun byAuthor(author: String) = "$author さんの投稿。"
    override fun contentWarning(warning: String) = "内容の警告: $warning。"
    override val noReadableText = "この投稿には読み上げられる本文がありません。"
    override fun oneAttachment(description: String) = "添付が一件。$description"
    override fun manyAttachments(count: Int) = "添付が $count 件。"
    override fun attachmentWith(type: String, description: String) = "$type: $description。"
    override fun attachmentWithout(type: String) = "説明のない$type。"
    override val mediaImage = "画像"
    override val mediaVideo = "動画"
    override val mediaAudio = "音声"
    override val mediaAnimation = "アニメーション"
    override val mediaOther = "添付"

    override val ready = "準備完了。"
    override val ttsUnavailable = "音声合成が利用できません。"
    override val preparingSpeech = "音声合成を準備しています …"
    override val notLoggedIn = "ログインしていません。"
    override val sessionStarted =
        "音声操作を開始しました。画面を消してかまいません。" +
            "タイムライン、新しい投稿、またはヘルプと言ってください。"
    override val sessionEnded = "音声操作を終了します。またどうぞ。"
    override val loggedOut = "ログアウトしました。"
    override val noRecognizer =
        "この端末では音声認識が設定されていません。音声操作を終了します。"
    override val missingMicPermission =
        "マイクの許可がありません。アプリで許可してください。"
    override val recognizerRefusedMic =
        "アプリには許可があるのに、音声認識サービスがマイクを拒否しています。" +
            "音声入力アプリ、通常は Google にマイクの許可があるか確認するか、" +
            "システム設定で別の音声入力サービスを選んでください。"

    override fun recognizerDiagnostic(packages: List<String>): String {
        val listed = if (packages.isEmpty()) "見つかりません" else packages.joinToString(", ")
        return "アプリは RECORD_AUDIO を持っていますが、音声認識がマイクを拒否しました。" +
            "インストール済みの音声認識サービス: $listed。"
    }

    override val nothingHeardEnding = "何も聞こえないので音声操作を終了します。"
    override val stillListening = "聞いています。コマンドが必要ならヘルプと言ってください。"
    override val notUnderstood = "聞き取れませんでした。ヘルプと言うとコマンドを読み上げます。"
    override val paused = "一時停止しました。続けると言うと再開します。"
    override val helpText =
        "次のように言えます。タイムライン。次の投稿。前の投稿。もう一度。一時停止。続けて。" +
            "停止。新しい投稿、と言うと口述できます。速く、または遅く、で読み上げの速さを変えます。" +
            "続けて読む、と言うと止まらずに読みます。間で聞く、と言うと投稿ごとに尋ねます。" +
            "英語、またはドイツ語、と言うと言語を切り替えます。" +
            "ログアウト、と言うとアカウントから出ます。" +
            "終了、と言うと音声操作を閉じます。"
    override val pausesOn = "これからは投稿ごとに尋ねます。"
    override val pausesOff = "これからはタイムラインを続けて読みます。"
    override val nowSpeakingThisLanguage = "これからは日本語で話します。"

    override fun speechRate(percent: Int) = "速さは $percent パーセントです。"

    override val loadingTimeline = "タイムラインを読み込んでいます。"
    override val emptyTimeline = "タイムラインに投稿がありません。"
    override fun timelineFailed(reason: String) = "タイムラインを読み込めませんでした。$reason"
    override val noTimelineLoaded = "タイムラインが読み込まれていません。タイムラインと言ってください。"
    override fun statusAt(index: Int, total: Int, summary: String) =
        "$total 件中 $index 件目。$summary"

    override fun allPostsRead(total: Int) = "以上、$total 件すべてです。"
    override val noFurtherPost = "次の投稿はありません。"
    override val whatNow =
        "投稿を書きますか。新しい投稿、タイムライン、または終了、と言ってください。"
    override val alreadyFirstPost = "これが最初の投稿です。"

    override val dictatePrompt = "音のあとに投稿を話してください。"
    override val nothingHeardRetry = "何も聞こえませんでした。音のあとに投稿を話してください。"
    override val nothingHeardToMenu = "何も聞こえませんでした。メインメニューに戻ります。"
    override val dictationCancelled = "取り消しました。メインメニューに戻ります。"
    override fun confirmDraft(draft: String) =
        "投稿の内容は、$draft。送信しますか。はい、またはいいえ、と言ってください。"

    override val confirmAgain = "投稿を送信しますか。はい、またはいいえ、と言ってください。"
    override val sayYesOrNo = "はい、またはいいえ、と言ってください。"
    override val draftDiscarded = "投稿を破棄しました。メインメニューに戻ります。"
    override val draftDiscardedNotUnderstood =
        "聞き取れませんでした。投稿を破棄してメインメニューに戻ります。"
    override val draftDiscardedSilence = "投稿を破棄します。メインメニューに戻ります。"
    override val noDraft = "送信する投稿がありません。"
    override val sending = "送信しています。"
    override val published = "投稿しました。次はどうしますか。"
    override fun sendFailed(reason: String) =
        "送信できませんでした。$reason。もう一度試しますか。はい、またはいいえ、と言ってください。"

    override fun apiError(kind: ApiErrorKind, status: Int, detail: String): String {
        val suffix = if (detail.isNotBlank()) ": $detail" else ""
        return when (kind) {
            ApiErrorKind.UNAUTHORIZED -> "ログインしていないか、アクセス権限が失効しています$suffix"
            ApiErrorKind.FORBIDDEN -> "アクセスが拒否されました$suffix"
            ApiErrorKind.NOT_FOUND -> "宛先が見つかりません。本当に Pleroma のサーバーですか$suffix"
            ApiErrorKind.REJECTED -> "投稿が拒否されました$suffix"
            ApiErrorKind.RATE_LIMITED -> "リクエストが多すぎます。少し待ってください$suffix"
            ApiErrorKind.SERVER -> "サーバーエラー ($status)$suffix"
            ApiErrorKind.UNREACHABLE -> "サーバーに接続できません"
            ApiErrorKind.NO_CREDENTIALS -> "サーバーが OAuth の情報を返しませんでした"
            ApiErrorKind.NO_TOKEN -> "アクセストークンを取得できませんでした"
            ApiErrorKind.OTHER -> "HTTP $status$suffix"
        }
    }

    override val unknownError = "不明なエラー"
}
