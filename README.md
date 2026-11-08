# Kalender-Assistent

Eine private App für Android und iPhone (später auch Mac). Sie führt die Kalender auf dem Gerät zusammen, erstellt jeden Morgen ein Briefing und erinnert rechtzeitig an Termine.

## Was die App kann (Phase 1)

**Kalender**
- liest alle Kalender des Geräts (Samsung/Google Kalender auf Android, die iOS-Konten auf dem iPhone), **nur lesend**
- pro Kalender an/aus, Farbe und Kurzname
- ignoriert abgesagte und abgelehnte Termine, markiert vorläufige
- zeigt dieselbe Besprechung aus zwei Kalendern nur einmal
- behandelt Zeitzonen, Sommerzeit, ganztägige und mehrtägige Termine richtig
- funktioniert ohne Internet

**Terminarten**
- erkennt automatisch Online, Präsenz oder Sonstig und sagt, warum („wegen Teams-Link")
- hybride Termine zählen als Präsenz, der Link wird trotzdem angezeigt
- Terminart pro Kalender oder Termin überschreibbar, Termine als „wichtig" markierbar

**Morgenbriefing** (pro Wochentag an/aus und eigene Uhrzeit)
- Heute: alle Termine chronologisch, mit Button „Teilnehmen" oder Ort
- Warnungen: Überschneidungen, keine Pause, Präsenz direkt nach Online
- freie Zeitfenster
- Präsenztermine in 2 Tagen (immer da, auch wenn leer)
- Vorschau auf morgen
- kommt als Benachrichtigung und steht ausführlich in der App; Abschnitte einzeln ein- und ausblendbar

**Erinnerungen** (als Benachrichtigung)
- Online-Termine: 15 Min. vorher, mit Button „Teilnehmen"
- Präsenztermine: am Vortag, optional zusätzlich am Termintag
- Sonstige Termine: standardmäßig keine
- verschobene Termine: Erinnerung wandert mit; abgesagte: Erinnerung verschwindet. Duplikate gibt es keine.
- Ruhezeiten mit eigenem Verhalten pro Erinnerungsart und Vorschau; wichtige Termine dürfen durch

**Aktualität**
- zeigt „Zuletzt synchronisiert um …" und warnt, wenn der Stand alt ist
- Android: reagiert selbst auf Kalenderänderungen, das Morgenbriefing ist immer frisch

**Einstellungen:** Alles, was sinnvoll ist, ist einstellbar (Liste in [docs/PLAN.md](docs/PLAN.md), Abschnitt 5).

**Was die App nicht tut**
- keine Termine erstellen, ändern, löschen oder Einladungen beantworten
- keine Verbindung ins Internet, kein Tracking
- keine Fahrzeit (später nachrüstbar)
- die Outlook-Wichtigkeit nicht automatisch übernehmen
- „Nicht stören" bzw. Fokus-Modi umgehen

**Später:** Phase 2 bringt Snooze und „Erledigt" in der Benachrichtigung, Widgets, Konflikte über mehrere Tage und eine Wochenvorschau am Sonntagabend. Phase 4 bringt eine Mac-App in der Menüleiste.

## Status

| Phase | Stand |
|---|---|
| 0a Kontentest auf dem iPhone | erledigt: beide Konten in iOS |
| A0 Android-Grundgerüst und Machbarkeit | erledigt: App liest alle Kalender auf dem Galaxy S24+ |
| 1 Kern auf Android | 1a in Arbeit: Terminarten, Duplikate und Warnungen fertig (31 Tests grün) |
| 1i iPhone | offen |
| 2 Komfort | offen |
| 3 Apple Watch (optional) | offen |
| 4 Mac | offen |

### So probierst du den aktuellen Stand aus

Die App zeigt noch eine Testansicht, das eigentliche Briefing kommt in Phase 1d.
1. App öffnen. Unter „Termine der nächsten 7 Tage" steht bei jedem Termin die erkannte Terminart (Online, Präsenz, Sonstig) und der Grund dafür.
2. Im Samsung Kalender zwei Termine anlegen, die sich überschneiden, und einen dritten direkt danach. Zurück in der App erscheinen unter dem Tag die Warnungen „Überschneidung" und „Keine Pause".
3. Einen Termin mit Ort direkt nach einem Teams-Termin: Warnung „Präsenz direkt nach Online".
4. Steht dieselbe Besprechung in zwei Kalendern, erscheint sie nur einmal, mit „in 2 Kalendern".

## Projektaufbau

```
core/         gemeinsamer Kern in Kotlin Multiplatform (Logik, Tests)
androidApp/   Android-App (Jetpack Compose)
iosApp/       iPhone-App (SwiftUI), ab Phase 1i
macApp/       Mac-App (SwiftUI), ab Phase 4
docs/         Auftrag, Plan, Anleitungen
```

## Dokumente

- [docs/AUFTRAG.md](docs/AUFTRAG.md): ursprünglicher Auftrag
- [docs/PLAN.md](docs/PLAN.md): freigegebener Plan mit Architektur, Phasen, Einstellungen und Grenzen. Bei Widersprüchen zum Auftrag gilt der Plan.
- [docs/ENTRA-EINRICHTUNG.md](docs/ENTRA-EINRICHTUNG.md): Microsoft-App-Registrierung, nur für den Notfall

## Android: bauen und installieren

Voraussetzungen (Windows, macOS oder Linux):
- JDK 17 (`JAVA_HOME` gesetzt)
- Android SDK mit Plattform 36; der Pfad steht in `local.properties` als `sdk.dir=…` (nicht im Repository)

Tests und Build:
```
gradlew :core:jvmTest
gradlew :androidApp:assembleDebug
```

Aufs Handy (USB-Debugging an, Handy per Kabel verbunden):
```
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Auf dem Handy danach:
- Kalenderzugriff erlauben
- in den Android-Einstellungen unter Apps → Kalender-Assistent → Akku: „Nicht eingeschränkt"
- Mitteilungen von Samsung Kalender bzw. Outlook abschalten, sonst kommen Erinnerungen doppelt

## iPhone: Einrichtung (ab Phase 1i)

- Mac mit aktuellem Xcode, mit deiner Apple-ID angemeldet (Xcode → Einstellungen → Accounts)
- iPhone mit iOS 17 oder neuer, Entwicklermodus an (Einstellungen → Datenschutz & Sicherheit → Entwicklermodus)
- Outlook-Konten in den iPhone-Einstellungen eingerichtet (Einstellungen → Kalender → Kalender-Accounts)
- `Config.example.xcconfig` nach `Config.xcconfig` kopieren und die Bundle-ID eintragen

## Bekannte Einschränkungen

Ausführlich in [docs/PLAN.md](docs/PLAN.md), Abschnitte 4 und 8. Kurz:
- Android: Die Akku-Optimierung von Samsung muss für die App aus sein, sonst kommen Erinnerungen zu spät.
- iPhone: iOS garantiert keinen Hintergrundlauf. Erinnerungen sind vorab geplant, ihr Inhalt entspricht aber dem letzten Sync.
- iPhone mit kostenlosem Apple-Account: Die App muss alle 7 Tage aus Xcode neu installiert werden.
