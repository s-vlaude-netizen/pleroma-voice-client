# Peroma Voice — Audio-Client für Pleroma

*English, German and Japanese. The app speaks whichever language you pick — see
[Sprache / Language](#sprache--language).*

Ein reiner **Audio-Client** für das Fediverse-Netzwerk [Pleroma](https://pleroma.social)
(oft „Peroma" ausgesprochen). Ab Version 2 wird die App **wie ein Telefonmenü per
Stimme bedient**: App starten, Bildschirm ausschalten, Handy einstecken — Timeline
vorlesen lassen und Beiträge diktieren, ohne einmal hinzusehen.

## Funktionen

- **Vollständige Sprachsteuerung** — die App spricht, hört zu, führt aus und fragt
  wieder nach. Sprechen und Zuhören wechseln sich strikt ab, damit das Mikrofon nicht
  die eigene Sprachausgabe hört.
- **Läuft bei ausgeschaltetem Bildschirm** — Vordergrunddienst mit Wake-Lock, die
  Sitzung läuft in der Hosentasche weiter.
- **Signaltöne** — ein kurzer Ton zeigt an, wann die App zuhört; eigene Töne für
  Erfolg und Fehler. So ist ohne Blick klar, wer gerade „dran" ist.
- **Timeline am Stück vorlesen** — Autor, Inhaltswarnung, Text und Bildbeschreibungen
  der Anhänge. Standardmäßig laufen alle Beiträge ohne Unterbrechung durch; erst am
  Ende fragt die App, wie es weitergehen soll.
- **Zwischenfragen optional** — wer freihändig navigieren will, schaltet mit
  „Mit Pausen" (oder dem Knopf) einen Modus ein, in dem das Mikrofon nach jedem
  Beitrag kurz aufgeht. „Am Stück" schaltet zurück. Die Einstellung wird gemerkt.
- **Beiträge diktieren** — diktieren, der Entwurf wird **zur Kontrolle vorgelesen**,
  und erst nach einem gesprochenen „Ja" veröffentlicht.
- **Sprechtempo per Stimme** — „schneller" / „langsamer", wird dauerhaft gemerkt.
- **Knöpfe bleiben optional** — dieselben Funktionen sind weiterhin antippbar, falls
  die Spracherkennung mal nicht verfügbar ist.
- **Dreisprachig** — Englisch, Deutsch und Japanisch, jeweils komplett: Oberfläche,
  gesprochene Ansagen und Sprachbefehle. Umschaltbar per Stimme („English",
  „Deutsch", „日本語") oder über den Knopf, der reihum durchschaltet. Nur die
  Beschriftung „Language" bleibt in jeder Sprache englisch, damit der Knopf auch
  dann auffindbar ist, wenn die App gerade Japanisch spricht.
- **Anmeldung per OAuth** — kein Passwort in der App, die Anmeldung läuft über die
  Weboberfläche deiner Instanz.

## Sprachbefehle

| Sagen | Wirkung |
| --- | --- |
| „Timeline vorlesen", „Was gibt es Neues" | Home-Timeline laden und vorlesen |
| „Nächster", „Überspringen" | nächster Beitrag |
| „Vorheriger", „Zurück" | vorheriger Beitrag |
| „Wiederholen", „Nochmal" | aktuellen Beitrag erneut vorlesen |
| „Pause" / „Weiter" | anhalten / fortsetzen |
| „Am Stück" / „Mit Pausen" | ohne bzw. mit Nachfrage zwischen den Beiträgen |
| „Neuer Beitrag", „Diktieren", „Posten" | Beitrag diktieren |
| „Ja", „Senden" / „Nein", „Verwerfen" | Entwurf senden oder verwerfen |
| „Schneller" / „Langsamer" | Sprechtempo ändern |
| „Wo bin ich" | aktuellen Stand ansagen |
| „Hilfe" | alle Befehle vorlesen |
| „Abmelden" | Konto abmelden |
| „Trotzdem vorlesen" | den Beitrag hinter der Inhaltswarnung doch hören |
| „Inhalte immer vorlesen" / „Nur die Warnung" | Inhaltswarnungen dauerhaft öffnen oder zulassen |
| „English" / „Deutsch" / „Japanisch" | Sprache umschalten |
| „Beenden", „Tschüss" | Sprachsitzung beenden |

Auf Englisch ist **„quit" oder „goodbye"** zuverlässiger als „end": Erkenner hören
ein einzelnes kurzes Wort wie „end" oft als „and" oder „in". „end" funktioniert
weiterhin, wenn es denn richtig ankommt.

### English commands

| Say | Effect |
| --- | --- |
| "read timeline", "what's new" | load and read the home timeline |
| "next", "skip" / "previous", "back" | move between posts |
| "repeat", "again" | read the current post again |
| "pause" / "continue" | stop and resume |
| "straight through" / "with pauses" | read without or with a prompt between posts |
| "new post", "dictate", "post something" | dictate a post |
| "yes", "send" / "no", "discard" | send or discard the draft |
| "faster" / "slower" | change the speaking rate |
| "where am I" | say the current position |
| "help" | read out all commands |
| "read anyway" | hear the post behind the content warning |
| "always read content" / "only the warning" | open content warnings for good, or keep them closed |
| "German" / "English" / "Japanese" | switch language |
| "sign out" | log out of the account |
| "quit", "goodbye", "finished" | end the voice session |

### 日本語のコマンド

| 言う | 動作 |
| --- | --- |
| タイムライン、新着 | ホームタイムラインを読み上げる |
| 次 / 前、戻って | 投稿を移動する |
| もう一度 | 今の投稿をもう一度読む |
| 一時停止 / 続けて | 止める・再開する |
| 続けて読んで / 間で聞いて | 止まらずに読む・投稿ごとに尋ねる |
| 新しい投稿、口述 | 投稿を口述する |
| はい、送信 / いいえ、キャンセル | 下書きを送る・破棄する |
| 速く / ゆっくり | 読み上げの速さを変える |
| 今どこ | 現在の位置を言う |
| ヘルプ | コマンドを読み上げる |
| それでも読んで | 警告の内容を今の投稿だけ聞く |
| 内容も読んで / 警告だけ | 警告の内容を常に読む・読まない |
| 英語 / ドイツ語 / 日本語 | 言語を切り替える |
| ログアウト | アカウントからログアウトする |
| 終了、さようなら | 音声操作を終了する |

日本語は分かち書きをしないため、コマンドは単語単位ではなく**部分一致**で
判定します。長い言い回しが短いものを飲み込まないよう、表の順序が意味を
持ちます（例: 「一時停止」は「停止」より先）。

## Inhaltswarnungen und Anhänge

Ein Beitrag mit **Inhaltswarnung** wird nur mit seiner Warnung vorgelesen — der
Text dahinter und seine Anhänge bleiben zu. Genau dafür ist die Warnung da: sie
soll ihre Leserin entscheiden lassen. Zwei Wege führen dahinter:

- „Trotzdem vorlesen" öffnet **den einen** Beitrag, bei dem die App gerade ist.
- „Inhalte immer vorlesen" schaltet es dauerhaft um; „Nur die Warnung" zurück.
  Denselben Schalter gibt es als Knopf in der App.

**Anhänge ohne Beschreibung werden gezählt statt aufgezählt.** Zwölf Fotos, die
niemand beschrieben hat, ergeben einen Satz — „Zwölf Bilder ohne Beschreibung." —
statt zwölfmal derselben Ansage. Anhänge **mit** Beschreibung werden weiterhin
einzeln vorgelesen: wer eine geschrieben hat, wollte sie gehört wissen.

## Sprache / Language

Beim ersten Start folgt die App der Gerätesprache: Deutsch auf einem deutschen
Gerät, Japanisch auf einem japanischen, sonst Englisch. Damit bekommen internationale
Nutzer ohne Zutun ihre Sprache. Danach gilt, was du zuletzt gewählt hast — per
Sprachbefehl („English", „Deutsch", „日本語") oder über den Knopf in der App.

**Diktieren folgt dieser Wahl automatisch.** Die App teilt dem Erkennungsdienst bei
jeder Aufnahme die gewünschte Sprache mit (`en-US`, `de-DE`, `ja-JP`) — in den
Systemeinstellungen ist dafür nichts zu tun. Zwei Vorbehalte: Der Dienst muss die
Sprache beherrschen (offline braucht es das jeweilige Sprachpaket, nachzuladen unter
*Spracheingabe → Google → Offline-Spracherkennung*), und einzelne Dienste ignorieren
die Angabe und nehmen die Systemsprache — Samsungs eigener ist dafür bekannt. Hilft
dann: als Spracheingabe Google wählen.

Die Wahl gilt für **alles**: Oberfläche, Ansagen, erkannte Befehle und die
Stimme, die die Timeline vorliest. Ein Wechsel mitten in der Sitzung verwirft die
geladene Timeline, weil deren Sprechtexte in der alten Sprache aufgebaut wurden —
einfach „Timeline vorlesen" bzw. „read timeline" erneut sagen.

Im Standardmodus („am Stück") hört das Mikrofon während des Vorlesens **nicht** zu —
zum Abbrechen dienen die Benachrichtigung oder der Knopf in der App. Wer mitten im
Vorlesen per Stimme steuern will, schaltet „Mit Pausen" ein.

Die Bestätigung eines Entwurfs ist bewusst ein reines Ja/Nein. Nach „Nein" ist der
Entwurf weg — für einen neuen Anlauf sagst du wieder „Neuer Beitrag".

Befehle werden auf **ganze Wörter** geprüft, nicht auf Teilzeichenketten — „Jahr"
gilt also nicht als „ja", und „nächster Beitrag" startet keinen neuen Beitrag.

Die App spricht mit der Mastodon-kompatiblen API von Pleroma
(`/api/v1/timelines/home`, `/api/v1/statuses`, `/oauth/*`) und funktioniert
deshalb auch mit Akkoma, Mastodon und anderen kompatiblen Servern.

## APK herunterladen

Fertige Release-APKs liegen unter
**[Releases](https://github.com/s-vlaude-netizen/pleroma-voice-client/releases)**.

1. `peroma-voice-<version>.apk` auf dem Android-Gerät herunterladen.
2. Beim Öffnen die Installation aus unbekannten Quellen für den Browser bzw. die
   Dateiverwaltung erlauben.
3. Installieren und starten.

Voraussetzung: **Android 7.0 (API 24)** oder neuer.

Jeder Build legt die APK zusätzlich als Workflow-Artefakt unter *Actions* ab.

### Versionen

| Version | Bedienung |
| --- | --- |
| **2.3.x** | zusätzlich Russisch; Updates ohne Deinstallieren |
| **2.2.x** | Englisch und Deutsch, umschaltbar |
| **2.1.x** | Timeline läuft am Stück durch, Zwischenfragen optional |
| **2.0.x** | Sprachsteuerung als Hauptbedienung, Nachfrage nach jedem Beitrag |
| **1.0.0** | reine Knopfbedienung, Diktat über Bestätigungsdialog |

Alle bleiben unter *Releases* verfügbar. Version 2 ändert die Bedienung grundlegend,
nutzt aber dieselben Konto- und Anmeldedaten — ein Update behält die Anmeldung bei.

## Benutzung

1. **Instanz eingeben** — z. B. `pleroma.example` (mit oder ohne `https://`).
2. Die App registriert sich per `POST /api/v1/apps` auf der Instanz und öffnet die
   Anmeldeseite im Browser.
3. Nach „Zugriff erlauben" kehrt der Browser über `peromavoice://oauth` zur App zurück.
4. **Sprachsteuerung starten** antippen und Mikrofon freigeben.
5. Bildschirm ausschalten. Ab hier geht alles per Stimme.

### Aktualisieren

Ab **2.2.2** lässt sich eine neue Version einfach über die alte installieren. Vorher
wurde jeder Build mit einem zufälligen Debug-Schlüssel signiert, weshalb Android
Updates mit „App wurde nicht installiert" ablehnte.

**Einmalig nötig:** Der Wechsel von 2.2.1 oder älter auf 2.2.2 verlangt noch ein
Deinstallieren, weil die installierte Version einen anderen Schlüssel trägt. Ab
2.2.2 aufwärts geht es dann direkt. Deine Anmeldung geht beim Deinstallieren
verloren — die Instanz einmal neu eingeben.

### Sprachausgabe verbessern

Die Sprachqualität kommt vom **TTS-Dienst des Geräts**, nicht aus der App — Android
erlaubt es nicht, ein eigenes Sprachmodell dafür mitzuliefern. Die App holt aber das
Beste aus dem heraus, was installiert ist: sie wählt automatisch die
höchstwertige Stimme der gewählten Sprache, die **offline** funktioniert.

Klingt es blechern, hilft ein besserer Dienst:

1. *Google Sprachausgabe* installieren bzw. aktualisieren.
2. Android-Einstellungen → *Bedienungshilfen → Sprachausgabe* → Modul wählen.
3. Dort unter *Sprachdaten installieren* für **Deutsch bzw. Englisch** die
   hochwertigen Stimmen laden (oft als „Hohe Qualität" / „Neural" gekennzeichnet).

Alternativ gibt es freie Engines wie RHVoice. Nach dem Wechsel die App neu starten.

Die Spracherkennung nutzt ebenfalls die Android-Bordmittel (`SpeechRecognizer`).
Ist keine eingerichtet, sagt die App das an und beendet die Sitzung — die Knöpfe
funktionieren dann weiter.

### Wenn die Sitzung im Hintergrund abbricht

Manche Hersteller (Xiaomi, Samsung, Huawei …) beenden Hintergrunddienste aggressiv.
Falls die Sprachsteuerung bei ausgeschaltetem Bildschirm stoppt, nimm die App in den
Android-Einstellungen unter *Akku → Akku-Optimierung* von der Optimierung aus.

## Selbst bauen

```bash
git clone https://github.com/s-vlaude-netizen/pleroma-voice-client
cd pleroma-voice-client
./gradlew assembleRelease
```

Die APK liegt danach unter `app/build/outputs/apk/release/`.
Benötigt JDK 17 und das Android SDK (Compile-SDK 34).

### Signierung

Standardmäßig signiert jeder Build mit `app/peroma-release.jks` — einem
Schlüssel, der **absichtlich im Repository liegt** (Alias `peroma`, Passwort
`peromavoice`). Er sorgt dafür, dass aufeinanderfolgende Releases dieselbe
Identität haben und einander ersetzen können. Ohne einen festen Schlüssel
erzeugen die Build-Tools auf einem CI-Runner bei jedem Lauf einen neuen, und
Android verweigert jedes Update.

> **Was das bedeutet:** Der Schlüssel ist öffentlich. Er belegt nicht, wer eine
> APK gebaut hat, und **jede** damit signierte APK wird von Android als Update
> einer bestehenden Installation akzeptiert — samt Zugriff auf den dort
> gespeicherten Pleroma-Token. Für eine selbst verteilte Privat-App ist das ein
> bewusster Kompromiss; für eine Veröffentlichung an Dritte oder in einem Store
> ist es keiner.

Für einen privaten Schlüssel diese Repository-Secrets setzen — sie haben Vorrang:

| Secret | Bedeutung |
| --- | --- |
| `KEYSTORE_BASE64` | Keystore-Datei, base64-kodiert |
| `KEYSTORE_PASSWORD` | Passwort des Keystores |
| `KEY_ALIAS` | Alias des Schlüssels |
| `KEY_PASSWORD` | Passwort des Schlüssels |

Lokal funktionieren dieselben Werte als Umgebungsvariablen (`KEYSTORE_FILE` als Pfad).
Ein Wechsel des Schlüssels verlangt erneut ein Deinstallieren auf jedem Gerät.

## Aufbau

| Datei | Aufgabe |
| --- | --- |
| `PleromaApi.kt` | OAuth-Registrierung, Token-Tausch, Timeline, Beitrag senden |
| `Post.kt` | Timeline-Eintrag, aufbereitet als Sprechtext |
| `SpeechText.kt` | HTML → vorlesbarer Text (Links, Hashtags, Erwähnungen, Entities) |
| `Strings.kt` | alle gesprochenen Texte je Sprache, plus Sprachauswahl |
| `VoiceCommands.kt` | Sprachbefehle je Sprache → Kommandos, wortweise geprüft |
| `VoiceService.kt` | Dialog-Zustandsautomat: sprechen ↔ zuhören, Wake-Lock, Töne |
| `TtsSetup.kt` | wählt die beste installierte deutsche Offline-Stimme |
| `MainActivity.kt` | Start der Sprachsitzung, optionale Knöpfe |
| `LoginActivity.kt` | Instanzauswahl und OAuth-Rückleitung |

Der Dialogablauf in `VoiceService` ist ein Zustandsautomat mit den Stufen
`MENU`, `READING`, `DICTATING` und `CONFIRMING`. Jede Äußerung wird abhängig von der
Stufe gedeutet: im Diktat ist „senden" Teil des Textes, in der Bestätigung ist es ein
Ja. Nach jeder Sprachausgabe entscheidet ein `After`-Wert, ob als Nächstes zugehört,
weitergelesen oder beendet wird.

Die App kommt ohne Netzwerk-Bibliothek aus (`HttpURLConnection` + `org.json`).

## Datenschutz

Zugangstoken und Instanzadresse liegen ausschließlich lokal in den app-privaten
`SharedPreferences`. Es gibt keinen Server der App, keine Analytik und keine
Drittanbieter-SDKs. Die Spracherkennung läuft über den auf dem Gerät
eingerichteten Android-Dienst; je nach Gerät kann dieser die Aufnahme zur
Erkennung an den jeweiligen Anbieter senden.

## Lizenz

MIT — siehe [LICENSE](LICENSE).
