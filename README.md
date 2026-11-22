# Peroma Voice — Audio client for Pleroma

*English, German and Japanese. The app speaks whichever language you pick — see
[Language](#language).*

A purely **audio client** for the fediverse network [Pleroma](https://pleroma.social)
(often pronounced "Peroma"). Since version 2 the app is **operated by voice, like a
phone menu**: start the app, switch the screen off, pocket the phone — have the
timeline read out and dictate posts without looking once.

## Features

- **Complete voice control** — the app speaks, listens, acts and asks again.
  Speaking and listening strictly alternate, so the microphone never hears the app's
  own speech output.
- **Runs with the screen off** — a foreground service with a wake lock; the session
  keeps going in your pocket.
- **Audio cues** — a short tone shows when the app is listening; separate tones for
  success and error. So you can tell without looking whose turn it is.
- **Timeline read straight through** — author, content warning, text and attachment
  descriptions. By default all posts run without interruption; only at the end does
  the app ask how to continue.
- **Optional questions between posts** — if you want to navigate hands-free, "with
  pauses" (or the button) turns on a mode in which the microphone opens briefly after
  each post. "Straight through" switches back. The setting is remembered.
- **Dictate posts** — dictate, the draft is **read back for checking**, and only
  published after a spoken "yes".
- **React to the post you just heard** — say *comment* to answer it, *like* to
  favourite it, or *boost* to share it with your followers. A comment is dictated,
  read back and only sent after a spoken "yes", just like a post; a boost asks
  first. See [Reacting to the post you just heard](#reacting-to-the-post-you-just-heard).
- **Speaking rate by voice** — "faster" / "slower", remembered permanently.
- **Buttons stay optional** — the same functions can still be tapped, in case speech
  recognition is unavailable.
- **Three languages** — English, German and Japanese, each complete: interface,
  spoken prompts and voice commands. Switch by voice ("English", "Deutsch", "日本語")
  or with the button, which cycles through them. Only the label "Language" stays
  English in every language, so the button can still be found when the app is
  speaking Japanese.
- **Sign-in via OAuth** — no password in the app; sign-in goes through your
  instance's web interface.

## Voice commands

Say the commands in the language the app is set to. Commands of the other languages
work too — see [Language](#language).

### English

| Say | Effect |
| --- | --- |
| "read timeline", "what's new" | load and read the home timeline |
| "next", "skip" / "previous", "back" | move between posts |
| "repeat", "again" | read the current post again |
| "pause" / "continue" | stop and resume |
| "straight through" / "with pauses" | read without or with a prompt between posts |
| "new post", "dictate", "post something" | dictate a post |
| "comment", "reply", "comment on this" | comment on the post that was just read: dictate, check, send |
| "like", "like this", "add like" | favourite the post that was just read |
| "boost", "repost", "retweet", "share" | boost it to your followers (asks first) |
| "yes", "send" / "no", "discard" / "again" | send, discard, or dictate the draft anew |
| "faster" / "slower" | change the speaking rate |
| "where am I" | say the current position |
| "help" | read out all commands |
| "read anyway" | hear the post behind the content warning |
| "always read content" / "only the warning" | open content warnings for good, or keep them closed |
| "German" / "English" / "Japanese" | switch language |
| "sign out" | log out of the account |
| "quit", "goodbye", "finished" | end the voice session |

In English, **"quit" or "goodbye"** is more reliable than "end": recognizers often
hear a single short word like "end" as "and" or "in". "end" still works when it
arrives correctly.

### German

| Say | Effect |
| --- | --- |
| "Timeline vorlesen", "Was gibt es Neues" | load and read the home timeline |
| "Nächster", "Überspringen" | next post |
| "Vorheriger", "Zurück" | previous post |
| "Wiederholen", "Nochmal" | read the current post again |
| "Pause" / "Weiter" | pause / continue |
| "Am Stück" / "Mit Pausen" | read without or with a prompt between posts |
| "Neuer Beitrag", "Diktieren", "Posten" | dictate a post |
| "Kommentar", "Kommentieren", "Antworten" | comment on the post that was just read |
| "Like", "Gefällt mir", "Liken" | favourite the post that was just read |
| "Boost", "Teilen", "Reposten" | boost it to your followers (asks first) |
| "Ja", "Senden" / "Nein", "Verwerfen" / "Wiederholen" | send, discard, or dictate the draft anew |
| "Schneller" / "Langsamer" | change the speaking rate |
| "Wo bin ich" | say the current position |
| "Hilfe" | read out all commands |
| "Abmelden" | log out of the account |
| "Trotzdem vorlesen" | hear the post behind the content warning |
| "Inhalte immer vorlesen" / "Nur die Warnung" | open content warnings for good, or keep them closed |
| "English" / "Deutsch" / "Japanisch" | switch language |
| "Beenden", "Tschüss" | end the voice session |

### Japanese (日本語)

| Say | Effect |
| --- | --- |
| タイムライン、新着 | read the home timeline aloud |
| 次 / 前、戻って | move between posts |
| もう一度 | read the current post again |
| 一時停止 / 続けて | stop / resume |
| 続けて読んで / 間で聞いて | read without stopping / ask after each post |
| 新しい投稿、口述 | dictate a post |
| コメント、返信 | comment on the post that was just read |
| いいね、お気に入り | favourite the post that was just read |
| ブースト、リポスト、シェア | boost it to your followers (asks first) |
| はい、送信 / いいえ、キャンセル / もう一度 | send, discard, or dictate the draft anew |
| 速く / ゆっくり | change the speaking rate |
| 今どこ | say the current position |
| ヘルプ | read out the commands |
| それでも読んで | hear the content behind the warning for this post only |
| 内容も読んで / 警告だけ | always read / never read the content behind warnings |
| 英語 / ドイツ語 / 日本語 | switch language |
| ログアウト | log out of the account |
| 終了、さようなら | end the voice session |

Japanese is written without spaces, so commands are matched by **substring** rather
than word by word. So that long phrases are not swallowed by short ones, the order of
the lookup table in `VoiceCommands.kt` matters (for example 「一時停止」 is checked
before 「停止」).

## When the microphone is blocked

If microphone permission is taken away — refused permanently, or switched off later
in Android's settings — Android refuses every further request silently. An app that
then simply asks again looks broken: nothing happens.

In that case Peroma Voice shows a notice with an **Open settings** button that leads
straight to the app's permission page. When you come back, the permission is checked
again and whatever you had started continues on its own. The notice also appears when
you open the app and voice control should be starting — otherwise the app would stay
silent without explanation.

On opening, the app settles the microphone question **before** it starts voice
control: if the permission is missing, it asks normally; if it is blocked, the notice
appears. The notification permission request comes only afterwards, so that two
system dialogs don't end up on top of each other.

### The device-wide microphone switch

Since Android 12 the microphone can be blocked for **all** apps at once in the quick
settings ("Microphone access"). That is different from the per-app permission — the
app keeps its permission, it just receives silence.

No app may read this switch. But Android itself offers to lift it — with the dialog
"Unblock device microphone?" — to any app that opens the microphone while it is in the
foreground. Speech recognition, however, records in the process of the recognition
service, which is never in the foreground; the dialog therefore never came, and a
session simply heard nothing.

The app therefore briefly opens the microphone **itself** at start-up, while the
screen is in front. If it is blocked, Android's dialog appears; the app keeps listening
and starts voice control as soon as sound arrives. If you decline, the screen says
where the switch is. On devices without this switch the check is skipped.

## Start

The app **starts voice control by itself when opened** — that is what it is for, and
reaching for a button is exactly the step it is meant to save. Open the app, pocket
the phone, listen.

Two caveats: the very first time it waits for the button, because the microphone
permission has to be granted first; from then on it starts on its own. And if a
session is already running, it is not greeted a second time but carries on reading.
You can switch this off with "Start voice control on launch" in the app.

## A dictated post is never lost

Speech recognition regularly gets things wrong — that is the normal case, not the
exception. The app therefore **never** throws a dictated post away because it did not
understand the follow-up question. It is only discarded when you say so explicitly.

The question "Should I send this?" has three answers: **yes**, **no** and **again**
(German "Wiederholen", Japanese 「もう一度」) for dictating anew. The word is
deliberately short and stored in every common spelling — in German "wiederholen",
"nochmal", "noch mal", "noch einmal", "neu diktieren".

If the app fails to understand you several times, or hears nothing, it goes back to
the menu and **keeps the draft**; there "send", "discard" and "new post" still apply.
And because the end of a session would otherwise take the draft with it, it lives
outside the session: at the next start the app reads it out again and asks again.

## Reacting to the post you just heard

The post the app has just read out is the one the next three commands refer to. They
work whenever the microphone opens after a post — between posts in "with pauses"
mode, and in the menu after you paused or the timeline ended — and each has a button
in the app as well.

- **Comment** (also "reply") — you dictate the comment, the app reads it back, and
  only a spoken "yes" sends it, exactly as with a new post. It goes out as a reply to
  that post, with its author mentioned at the start. If the post was a boost, the
  comment goes to the original post. "New post" still starts an unrelated post, so
  both are always available.
- **Like** (also "favourite") — favourites the post at once and says "Liked."
- **Boost** (also "repost", "retweet", "share") — boosts the post to your followers.
  A boost is public, and "boost" sounds a lot like "post", so the app asks first:
  "Boost the post by …? Say yes or no." Silence counts as no. The button boosts
  straight away, because tapping it is deliberate.

After a like, a boost or a sent comment from the pause between posts, the microphone
opens again for a moment: say another reaction, or stay silent and the timeline
carries on with the next post.

A comment never goes out wider than the post it answers: a reply to a followers-only
post stays followers-only, a reply to a direct message stays direct. And like the draft
of a post, a draft comment survives the end of a session: at the next start the app
reads it out again and asks again — as a comment on the same post.

No new permission is needed. The app already asks for read and write access, which
covers favouriting, boosting and replying, so signing in again is not necessary.

## Content warnings and attachments

A post with a **content warning** is read out with its warning only — the text behind
it and its attachments stay closed. That is exactly what the warning is for: it lets
its reader decide. There are two ways past it:

- "Read anyway" opens **the one** post the app is currently on.
- "Always read content" switches it on permanently; "Only the warning" switches back.
  The same switch exists as a button in the app.

**Attachments without a description are counted rather than listed.** Twelve photos
nobody described make one sentence — "Twelve images without a description." — instead
of the same announcement twelve times. Attachments **with** a description are still
read one by one: whoever wrote one wanted it to be heard.

## Language

On first start the app follows the device language: German on a German device,
Japanese on a Japanese one, English otherwise. That way international users get their
language without doing anything. After that, whatever you chose last applies — by
voice command ("English", "Deutsch", "日本語") or with the button in the app.

**Commands of the other languages still apply.** The choice determines what the app
*speaks* — not which words you must have forgotten. If it is set to German and you
answer "yes", that counts as yes; if it is set to English and you say "Hilfe", you get
help. The selected language is always checked first and completely, so nothing can be
taken away from it. Only when it can make nothing of the utterance do the others get
their turn, and there the whole utterance has to be the command — a "bye" in the
middle of a sentence therefore ends nothing.

**Dictation follows this choice automatically.** With every recording the app tells
the recognition service the desired language (`en-US`, `de-DE`, `ja-JP`) — there is
nothing to set in the system settings. Two caveats: the service has to know the
language (offline this needs the respective language pack, downloadable under *Voice
input → Google → Offline speech recognition*), and some services ignore the request
and use the system language — Samsung's own is known for this. If that happens,
choose Google as the voice input.

The choice applies to **everything**: interface, announcements, recognized commands
and the voice that reads the timeline. Switching in the middle of a session discards
the loaded timeline, because its spoken texts were built in the old language — just
say "read timeline" / "Timeline vorlesen" again.

In the default mode ("straight through") the microphone does **not** listen while the
timeline is being read — use the notification or the button in the app to interrupt.
If you want to steer by voice in the middle of reading, switch on "with pauses".

A draft is only discarded after an explicit "no" — see
[A dictated post is never lost](#a-dictated-post-is-never-lost).

Commands are matched on **whole words**, not substrings — so "Jahr" does not count as
"ja", and "nächster Beitrag" does not start a new post.

The app talks to Pleroma's Mastodon-compatible API (`/api/v1/timelines/home`,
`/api/v1/statuses`, `/oauth/*`) and therefore also works with Akkoma, Mastodon and
other compatible servers.

## Download the APK

Finished release APKs are under
**[Releases](https://github.com/s-vlaude-netizen/pleroma-voice-client/releases)**.

1. Download `peroma-voice-<version>.apk` to the Android device.
2. When opening it, allow installation from unknown sources for the browser or file
   manager.
3. Install and start.

Requirement: **Android 7.0 (API 24)** or newer.

Every build also stores the APK as a workflow artifact under *Actions*.

### Versions

| Version | What it does |
| --- | --- |
| **2.3.x** | additionally Russian; updates without uninstalling |
| **2.2.x** | English and German, switchable |
| **2.1.x** | timeline runs straight through, questions in between optional |
| **2.0.x** | voice control as the main way of operating, prompt after every post |
| **1.0.0** | button operation only, dictation through a confirmation dialog |

All remain available under *Releases*. Version 2 changes the operation fundamentally,
but uses the same account and sign-in data — an update keeps you signed in.

## Usage

1. **Enter the instance** — e.g. `pleroma.example` (with or without `https://`).
2. The app registers itself on the instance with `POST /api/v1/apps` and opens the
   sign-in page in the browser.
3. After "Allow access" the browser returns to the app via `peromavoice://oauth`.
4. Tap **Start voice control** and grant the microphone permission.
5. Switch the screen off. From here on everything is by voice.

### Updating

From **2.2.2** on, a new version can simply be installed over the old one. Before
that, every build was signed with a random debug key, which is why Android rejected
updates with "App not installed".

**Needed once:** moving from 2.2.1 or older to 2.2.2 still requires uninstalling,
because the installed version carries a different key. From 2.2.2 onwards it works
directly. Your sign-in is lost when uninstalling — enter the instance once more.

### Improving the speech output

The voice quality comes from the **device's text-to-speech service**, not from the
app — Android does not allow shipping your own speech model for that. The app does get
the best out of what is installed, though: it automatically picks the highest-quality
voice for the chosen language that works **offline**.

If it sounds tinny, a better service helps:

1. Install or update *Speech Services by Google*.
2. Android settings → *Accessibility → Text-to-speech output* → choose the engine.
3. There, under *Install voice data*, download the high-quality voices for **German or
   English** (often marked "High quality" / "Neural").

Alternatively there are free engines such as RHVoice. Restart the app after switching.

Speech recognition also uses Android's built-in facilities (`SpeechRecognizer`). If
none is set up, the app says so and ends the session — the buttons keep working.

### When the session is killed in the background

Some manufacturers (Xiaomi, Samsung, Huawei …) kill background services aggressively.
If voice control stops while the screen is off, exempt the app from optimization in
Android's settings under *Battery → Battery optimization*.

## Building it yourself

```bash
git clone https://github.com/s-vlaude-netizen/pleroma-voice-client
cd pleroma-voice-client
./gradlew assembleRelease
```

The APK is then in `app/build/outputs/apk/release/`.
Requires JDK 17 and the Android SDK (compile SDK 34).

### Signing

By default every build is signed with `app/peroma-release.jks` — a key that is
**deliberately in the repository** (alias `peroma`, password `peromavoice`). It makes
sure that consecutive releases have the same identity and can replace one another.
Without a fixed key, the build tools on a CI runner generate a new one on every run,
and Android refuses every update.

> **What this means:** The key is public. It does not prove who built an APK, and
> **any** APK signed with it is accepted by Android as an update of an existing
> installation — including access to the Pleroma token stored there. For a privately
> distributed personal app this is a deliberate compromise; for publication to third
> parties or in a store it is not.

To use a private key instead, set these repository secrets — they take precedence:

| Secret | Meaning |
| --- | --- |
| `KEYSTORE_BASE64` | keystore file, base64-encoded |
| `KEYSTORE_PASSWORD` | password of the keystore |
| `KEY_ALIAS` | alias of the key |
| `KEY_PASSWORD` | password of the key |

Locally the same values work as environment variables (`KEYSTORE_FILE` as a path).
Changing the key requires uninstalling again on every device.

## Structure

| File | Purpose |
| --- | --- |
| `PleromaApi.kt` | OAuth registration, token exchange, timeline, posting |
| `Post.kt` | timeline entry, prepared as speakable text |
| `Reply.kt` | the post a comment answers, and how the reply text is built |
| `SpeechText.kt` | HTML → speakable text (links, hashtags, mentions, entities) |
| `Strings.kt` | all spoken texts per language, plus language selection |
| `VoiceCommands.kt` | voice commands per language → commands, matched word by word |
| `VoiceService.kt` | dialog state machine: speaking ↔ listening, wake lock, tones |
| `TtsSetup.kt` | picks the best installed offline voice for the chosen language |
| `MainActivity.kt` | starting the voice session, optional buttons |
| `LegacyCleanup.kt` | deletes the speech model that 2.10.x downloaded |
| `LoginActivity.kt` | instance selection and OAuth redirect |

The dialog flow in `VoiceService` is a state machine with the stages `MENU`,
`READING`, `DICTATING` and `CONFIRMING`. Every utterance is interpreted depending on
the stage: in dictation "send" is part of the text, in confirmation it is a yes. After
each speech output an `After` value decides whether to listen next, keep reading, or
stop.

The app needs no networking library (`HttpURLConnection` + `org.json`).

## Speech recognition

The app uses Android's `SpeechRecognizer`, i.e. whatever voice-input service the
device has set up. On most phones that is Google's, which may send the audio to
Google for recognition — see [Privacy](#privacy).

An on-device alternative was tried in 2.10.0 and 2.10.1: Whisper Small, run through
[transcribe.cpp](https://github.com/handy-computer/transcribe.cpp), the engine behind
[Handy](https://handy.computer). Its recognition turned out worse than the system's,
so 2.11.0 goes back. The code is in the git history (commits `e8ba0c6` and
`ec8961d`), and 2.11.0 deletes the 270 MB model those versions had downloaded.

## Privacy

The access token and the instance address are stored exclusively locally, in the
app-private `SharedPreferences`. There is no server of the app, no analytics and no
third-party SDKs. Speech recognition runs through the Android service set up on the
device; depending on the device, that service may send the recording to its provider
for recognition.

## License

MIT — see [LICENSE](LICENSE).
