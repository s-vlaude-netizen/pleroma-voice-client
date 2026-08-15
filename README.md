# Peroma Voice — Audio-Client für Pleroma

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
- **Timeline vorlesen** — Autor, Inhaltswarnung, Text und Bildbeschreibungen der
  Anhänge. Zwischen zwei Beiträgen hört die App kurz zu: sagst du nichts, geht es
  automatisch weiter; sagst du etwas, wird der Befehl ausgeführt.
- **Beiträge diktieren** — diktieren, der Entwurf wird **zur Kontrolle vorgelesen**,
  und erst nach einem gesprochenen „Ja" veröffentlicht.
- **Sprechtempo per Stimme** — „schneller" / „langsamer", wird dauerhaft gemerkt.
- **Knöpfe bleiben optional** — dieselben Funktionen sind weiterhin antippbar, falls
  die Spracherkennung mal nicht verfügbar ist.
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
| „Neuer Beitrag", „Diktieren", „Posten" | Beitrag diktieren |
| „Ja", „Senden" / „Nein", „Verwerfen" / „Nochmal" | Entwurf bestätigen, verwerfen, neu diktieren |
| „Schneller" / „Langsamer" | Sprechtempo ändern |
| „Wo bin ich" | aktuellen Stand ansagen |
| „Hilfe" | alle Befehle vorlesen |
| „Abmelden" | Konto abmelden |
| „Beenden", „Tschüss" | Sprachsitzung beenden |

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
| **2.0.0** | Sprachsteuerung als Hauptbedienung, Knöpfe optional |
| **1.0.0** | reine Knopfbedienung, Diktat über Bestätigungsdialog |

Beide bleiben unter *Releases* verfügbar. 2.0.0 ändert die Bedienung grundlegend,
nutzt aber dieselben Konto- und Anmeldedaten — ein Update behält die Anmeldung bei.

## Benutzung

1. **Instanz eingeben** — z. B. `pleroma.example` (mit oder ohne `https://`).
2. Die App registriert sich per `POST /api/v1/apps` auf der Instanz und öffnet die
   Anmeldeseite im Browser.
3. Nach „Zugriff erlauben" kehrt der Browser über `peromavoice://oauth` zur App zurück.
4. **Sprachsteuerung starten** antippen und Mikrofon freigeben.
5. Bildschirm ausschalten. Ab hier geht alles per Stimme.

### Sprachausgabe verbessern

Die Sprachqualität kommt vom **TTS-Dienst des Geräts**, nicht aus der App — Android
erlaubt es nicht, ein eigenes Sprachmodell dafür mitzuliefern. Die App holt aber das
Beste aus dem heraus, was installiert ist: sie wählt automatisch die
höchstwertige deutsche Stimme, die **offline** funktioniert.

Klingt es blechern, hilft ein besserer Dienst:

1. *Google Sprachausgabe* installieren bzw. aktualisieren.
2. Android-Einstellungen → *Bedienungshilfen → Sprachausgabe* → Modul wählen.
3. Dort unter *Sprachdaten installieren → Deutsch* die **hochwertigen** Stimmen laden
   (oft als „Hohe Qualität" / „Neural" gekennzeichnet).

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

Ohne konfigurierten Schlüssel wird die Release-APK mit dem Debug-Schlüssel
signiert — installierbar, aber nicht für eine Veröffentlichung im Store gedacht.
Für eigene signierte Builds setzt der CI-Workflow diese Repository-Secrets aus:

| Secret | Bedeutung |
| --- | --- |
| `KEYSTORE_BASE64` | Keystore-Datei, base64-kodiert |
| `KEYSTORE_PASSWORD` | Passwort des Keystores |
| `KEY_ALIAS` | Alias des Schlüssels |
| `KEY_PASSWORD` | Passwort des Schlüssels |

Sind sie gesetzt, signiert der Workflow damit; sonst greift der Debug-Schlüssel.
Lokal funktionieren dieselben Werte als Umgebungsvariablen (`KEYSTORE_FILE` als Pfad).

## Aufbau

| Datei | Aufgabe |
| --- | --- |
| `PleromaApi.kt` | OAuth-Registrierung, Token-Tausch, Timeline, Beitrag senden |
| `Post.kt` | Timeline-Eintrag, aufbereitet als Sprechtext |
| `SpeechText.kt` | HTML → vorlesbarer Text (Links, Hashtags, Erwähnungen, Entities) |
| `VoiceCommands.kt` | deutsche Sprachbefehle → Kommandos, wortweise geprüft |
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
