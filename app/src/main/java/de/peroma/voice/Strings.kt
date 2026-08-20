package de.peroma.voice

import java.util.Locale

/** The languages the app can speak and listen in. */
enum class Language(val tag: String) {
    ENGLISH("en"),
    GERMAN("de"),
    RUSSIAN("ru");

    /**
     * Used to pick a speech voice. Deliberately without a region: any installed
     * voice for the language qualifies, and demanding en-GB over en-US would
     * only push the engine onto its fallback.
     */
    val locale: Locale
        get() = Locale(tag)

    /**
     * Used to ask the recognizer for a language. Here the region is included,
     * because several recognition services honour a full BCP-47 tag and quietly
     * fall back to the system language when given a bare one.
     */
    val recognizerTag: String
        get() = when (this) {
            ENGLISH -> "en-US"
            GERMAN -> "de-DE"
            RUSSIAN -> "ru-RU"
        }

    /** The language's own name, so the button reads the same in every locale. */
    val displayName: String
        get() = when (this) {
            ENGLISH -> "English"
            GERMAN -> "Deutsch"
            RUSSIAN -> "Русский"
        }

    val strings: Strings
        get() = when (this) {
            ENGLISH -> EnglishStrings
            GERMAN -> GermanStrings
            RUSSIAN -> RussianStrings
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
            "read without stopping. German, to switch language. Log out. " +
            "Or: end, to close voice control."
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
            "oder Am Stück, wenn ich durchlesen soll. Englisch, um die Sprache zu wechseln. " +
            "Abmelden. Oder: Beenden, um die Sprachsteuerung zu schließen."
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

object RussianStrings : Strings {

    override fun linkTo(host: String) = "ссылка на $host"
    override val bareLink = "ссылка"
    override fun hashtag(tag: String) = "хэштег $tag"
    override val ampersand = " и "
    override val unknownAuthor = "Неизвестный"

    override fun postCounter(index: Int, total: Int) = "Пост $index из $total."
    override fun boostedBy(booster: String, author: String) =
        "$booster поделился постом автора $author."

    override fun byAuthor(author: String) = "Автор: $author."
    override fun contentWarning(warning: String) = "Предупреждение о содержании: $warning."
    override val noReadableText = "В этом посте нет читаемого текста."
    override fun oneAttachment(description: String) = "Одно вложение. $description"
    override fun manyAttachments(count: Int) = "Вложений: $count."
    override fun attachmentWith(type: String, description: String) = "$type: $description."
    override fun attachmentWithout(type: String) = "$type без описания."
    override val mediaImage = "Изображение"
    override val mediaVideo = "Видео"
    override val mediaAudio = "Аудио"
    override val mediaAnimation = "Анимация"
    override val mediaOther = "Вложение"

    override val ready = "Готово."
    override val ttsUnavailable = "Синтез речи недоступен."
    override val preparingSpeech = "Подготовка синтеза речи …"
    override val notLoggedIn = "Вы не вошли в аккаунт."
    override val sessionStarted =
        "Голосовое управление включено. Экран можно выключить. " +
            "Скажите: читай ленту, новый пост или помощь."
    override val sessionEnded = "Голосовое управление завершено. До встречи."
    override val loggedOut = "Вы вышли из аккаунта."
    override val noRecognizer =
        "На этом устройстве не настроено распознавание речи. Голосовое управление остановлено."
    override val missingMicPermission =
        "У меня нет доступа к микрофону. Пожалуйста, разрешите его в приложении."
    override val recognizerRefusedMic =
        "Служба распознавания речи не даёт доступ к микрофону, хотя у приложения " +
            "разрешение есть. Проверьте, есть ли доступ к микрофону у приложения " +
            "голосового ввода — обычно это Google — или выберите другую службу " +
            "голосового ввода в настройках системы."

    override fun recognizerDiagnostic(packages: List<String>): String {
        val listed = if (packages.isEmpty()) "не найдено" else packages.joinToString(", ")
        return "Распознавание речи не даёт микрофон, хотя у приложения есть RECORD_AUDIO. " +
            "Установленные службы распознавания: $listed."
    }

    override val nothingHeardEnding = "Я ничего не слышу и завершаю голосовое управление."
    override val stillListening = "Я слушаю. Скажите «помощь», если нужны команды."
    override val notUnderstood = "Я не понял. Скажите «помощь», чтобы услышать команды."
    override val paused = "Пауза. Скажите «продолжай», чтобы читать дальше."
    override val helpText =
        "Вы можете сказать: читай ленту. Следующий пост. Предыдущий пост. Повтори. " +
            "Пауза. Продолжай. Стоп. Новый пост, чтобы продиктовать. " +
            "Быстрее или медленнее для скорости речи. " +
            "Подряд, чтобы читать без остановок, или с паузами, чтобы я спрашивал " +
            "между постами. Английский или немецкий, чтобы сменить язык. Выйти. " +
            "Или: завершить, чтобы закрыть голосовое управление."
    override val pausesOn = "Теперь я буду спрашивать между постами."
    override val pausesOff = "Теперь я буду читать ленту подряд."
    override val nowSpeakingThisLanguage = "Теперь я говорю по-русски."

    override fun speechRate(percent: Int) = "Скорость $percent процентов."

    override val loadingTimeline = "Загружаю ленту."
    override val emptyTimeline = "В вашей ленте нет постов."
    override fun timelineFailed(reason: String) = "Не удалось загрузить ленту. $reason"
    override val noTimelineLoaded = "Лента не загружена. Скажите «читай ленту»."
    override fun statusAt(index: Int, total: Int, summary: String) =
        "Пост $index из $total. $summary"

    override fun allPostsRead(total: Int) = "Это были все посты, всего $total."
    override val noFurtherPost = "Следующего поста нет."
    override val whatNow =
        "Хотите написать пост? Скажите: новый пост, читай ленту или завершить."
    override val alreadyFirstPost = "Это уже первый пост."

    override val dictatePrompt = "Продиктуйте свой пост после сигнала."
    override val nothingHeardRetry = "Я ничего не услышал. Продиктуйте пост после сигнала."
    override val nothingHeardToMenu = "Я ничего не услышал. Возвращаюсь в главное меню."
    override val dictationCancelled = "Отменено. Возвращаюсь в главное меню."
    override fun confirmDraft(draft: String) =
        "Ваш пост: $draft. Отправить? Скажите да или нет."

    override val confirmAgain = "Отправить пост? Скажите да или нет."
    override val sayYesOrNo = "Пожалуйста, скажите да или нет."
    override val draftDiscarded = "Пост удалён. Возвращаюсь в главное меню."
    override val draftDiscardedNotUnderstood =
        "Я вас не понял. Пост удалён. Возвращаюсь в главное меню."
    override val draftDiscardedSilence = "Удаляю пост. Возвращаюсь в главное меню."
    override val noDraft = "Нет поста для отправки."
    override val sending = "Отправляю."
    override val published = "Пост опубликован. Что дальше?"
    override fun sendFailed(reason: String) =
        "Не удалось отправить. $reason. Попробовать ещё раз? Скажите да или нет."

    override fun apiError(kind: ApiErrorKind, status: Int, detail: String): String {
        val suffix = if (detail.isNotBlank()) ": $detail" else ""
        return when (kind) {
            ApiErrorKind.UNAUTHORIZED -> "Вы не вошли или доступ истёк$suffix"
            ApiErrorKind.FORBIDDEN -> "Доступ запрещён$suffix"
            ApiErrorKind.NOT_FOUND ->
                "Адрес не найден — это действительно сервер Pleroma?$suffix"
            ApiErrorKind.REJECTED -> "Пост отклонён$suffix"
            ApiErrorKind.RATE_LIMITED -> "Слишком много запросов, подождите немного$suffix"
            ApiErrorKind.SERVER -> "Ошибка сервера ($status)$suffix"
            ApiErrorKind.UNREACHABLE -> "Сервер недоступен"
            ApiErrorKind.NO_CREDENTIALS -> "Сервер не выдал данные OAuth"
            ApiErrorKind.NO_TOKEN -> "Токен доступа не получен"
            ApiErrorKind.OTHER -> "HTTP $status$suffix"
        }
    }

    override val unknownError = "Неизвестная ошибка"
}
