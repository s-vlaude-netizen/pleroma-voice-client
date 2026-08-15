# Peroma Voice — Audio-Client für Pleroma

Ein reiner **Audio-Client** für das Fediverse-Netzwerk [Pleroma](https://pleroma.social)
(oft „Peroma" ausgesprochen). Die App liest die Home-Timeline vor und nimmt neue
Beiträge per Mikrofon auf — gedacht für Nutzung ohne Blick auf den Bildschirm:
beim Kochen, Laufen, Autofahren oder mit Screenreader.

## Funktionen

- **Timeline vorlesen** — die Home-Timeline wird per Sprachausgabe (TTS) vorgelesen,
  inklusive Autor, Inhaltswarnung, Text und Bildbeschreibungen der Anhänge.
- **Weiterhören bei ausgeschaltetem Bildschirm** — die Wiedergabe läuft in einem
  Vordergrunddienst mit Benachrichtigung (Pause / Weiter / Stopp).
- **Steuerung** — nächster Beitrag, vorheriger Beitrag, wiederholen, pausieren, stoppen.
- **Beiträge diktieren** — Mikrofonaufnahme wird transkribiert, zur Kontrolle
  vorgelesen und erst nach Bestätigung veröffentlicht.
- **Anmeldung per OAuth** — kein Passwort in der App, die Anmeldung läuft über die
  Weboberfläche deiner Instanz.

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

## Benutzung

1. **Instanz eingeben** — z. B. `pleroma.example` (mit oder ohne `https://`).
2. Die App registriert sich per `POST /api/v1/apps` auf der Instanz und öffnet die
   Anmeldeseite im Browser.
3. Nach „Zugriff erlauben" kehrt der Browser über `peromavoice://oauth` zur App zurück.
4. **Timeline vorlesen** antippen — die Wiedergabe beginnt.
5. **Beitrag diktieren** antippen, sprechen, den vorgelesenen Text bestätigen.

Damit das Mikrofon nicht mit der Sprachausgabe kollidiert, pausiert die Wiedergabe
automatisch, sobald das Diktat startet.

### Sprachausgabe und Spracherkennung

Beides nutzt die Android-Bordmittel (`TextToSpeech` bzw. `SpeechRecognizer`),
voreingestellt auf Deutsch mit Rückfall auf die Gerätesprache. Fehlen die deutschen
Sprachdaten, lassen sie sich in den Android-Einstellungen unter
*Sprache & Eingabe → Sprachausgabe* nachinstallieren. Ist gar keine Spracherkennung
vorhanden, weicht die App auf den System-Diktierdialog aus.

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
| `PlaybackService.kt` | Vordergrunddienst mit TTS-Warteschlange und Benachrichtigung |
| `MainActivity.kt` | Große Schaltflächen, Diktat und Bestätigung |
| `LoginActivity.kt` | Instanzauswahl und OAuth-Rückleitung |

Die App kommt ohne Netzwerk-Bibliothek aus (`HttpURLConnection` + `org.json`).

## Datenschutz

Zugangstoken und Instanzadresse liegen ausschließlich lokal in den app-privaten
`SharedPreferences`. Es gibt keinen Server der App, keine Analytik und keine
Drittanbieter-SDKs. Die Spracherkennung läuft über den auf dem Gerät
eingerichteten Android-Dienst; je nach Gerät kann dieser die Aufnahme zur
Erkennung an den jeweiligen Anbieter senden.

## Lizenz

MIT — siehe [LICENSE](LICENSE).
