# Kalender-Assistent

Eine private iPhone-App (später auch Mac), die mehrere Outlook-Kalender zusammenführt, jeden Morgen ein Briefing erstellt und rechtzeitig an Termine erinnert.

## Was die App kann (Phase 1)

**Kalender**
- liest deine 3 Outlook-Kalender (privat und Arbeit) über die iOS-Konten, **nur lesend**
- pro Kalender an/aus, Farbe und Kurzname
- ignoriert abgesagte und abgelehnte Termine, markiert vorläufige
- zeigt dieselbe Besprechung aus zwei Kalendern nur einmal
- behandelt Zeitzonen, Sommerzeit, ganztägige und mehrtägige Termine richtig
- funktioniert auch ohne Netz

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
- kommt als Benachrichtigung mit Kurzfassung und Stand und steht ausführlich in der App; Abschnitte einzeln ein- und ausblendbar

**Erinnerungen** (als Benachrichtigung)
- Online-Termine: 15 Min. vorher, mit Button „Teilnehmen"
- Präsenztermine: am Vortag, optional zusätzlich am Termintag
- Sonstige Termine: standardmäßig keine
- verschobene Termine: Erinnerung wandert mit; abgesagte: Erinnerung verschwindet. Duplikate gibt es keine.
- Ruhezeiten mit eigenem Verhalten pro Erinnerungsart und Vorschau; wichtige Termine dürfen durch

**Aktualität**
- aktualisiert beim Start, beim Öffnen und im Hintergrund
- zeigt „Zuletzt synchronisiert um …" und warnt, wenn der Stand alt ist

**Einstellungen:** Alles, was sinnvoll ist, ist einstellbar (Liste in [docs/PLAN.md](docs/PLAN.md), Abschnitt 5).

**Was die App nicht tut**
- keine Termine erstellen, ändern, löschen oder Einladungen beantworten
- keine Daten ins Internet schicken, kein Tracking
- keine Fahrzeit (später nachrüstbar)
- die Outlook-Wichtigkeit nicht automatisch übernehmen
- ohne bezahlten Apple-Account nicht durch einen aktiven Fokus-Modus durchkommen

**Später:** Phase 2 bringt Snooze und „Erledigt" in der Benachrichtigung, Widgets, Konflikte über mehrere Tage und eine Wochenvorschau am Sonntagabend. Phase 4 bringt eine Mac-App in der Menüleiste.

## Status

| Phase | Stand |
|---|---|
| 0a Kontentest auf dem iPhone | erledigt: beide Konten in iOS, Datenquelle EventKit |
| 0b Projekt und Machbarkeit auf dem Mac | offen |
| 1 Kern iPhone | offen |
| 2 Komfort iPhone | offen |
| 3 Backend, Watch (optional) | offen |
| 4 Mac | offen |

## Dokumente

- [docs/AUFTRAG.md](docs/AUFTRAG.md): ursprünglicher Auftrag
- [docs/PLAN.md](docs/PLAN.md): freigegebener Plan mit Architektur, Phasen, Standardwerten und Grenzen. Bei Widersprüchen zum Auftrag gilt der Plan.
- [docs/ENTRA-EINRICHTUNG.md](docs/ENTRA-EINRICHTUNG.md): Microsoft-App-Registrierung, nur nötig, falls ein Konto über Graph laufen muss

## Einrichtung

Voraussetzungen:
- Mac mit aktuellem Xcode, mit deiner Apple-ID angemeldet (Xcode → Einstellungen → Accounts)
- iPhone mit iOS 17 oder neuer, Entwicklermodus eingeschaltet (Einstellungen → Datenschutz & Sicherheit → Entwicklermodus)
- Outlook-Konten in den iPhone-Einstellungen eingerichtet (Einstellungen → Kalender → Kalender-Accounts), siehe [docs/PLAN.md](docs/PLAN.md), Phase 0a
- Nur für den Graph-Weg: App-Registrierung in Microsoft Entra, siehe [docs/ENTRA-EINRICHTUNG.md](docs/ENTRA-EINRICHTUNG.md)

Konfiguration:
1. `Config.example.xcconfig` nach `Config.xcconfig` kopieren.
2. Bundle-ID eintragen, für den Graph-Weg zusätzlich die Client-ID. `Config.xcconfig` steht in `.gitignore` und kommt nie ins Repository.

Nach der Einrichtung: Mitteilungen der Outlook-App und von Apple Kalender abschalten, sonst kommen Erinnerungen doppelt (iOS-Einstellungen → Mitteilungen).

Build-Anleitung und Tests folgen mit Phase 0b.

## Bekannte Einschränkungen

Ausführlich in [docs/PLAN.md](docs/PLAN.md), Abschnitte 4 und 8. Kurz:
- iOS garantiert keinen Hintergrundlauf. Erinnerungen sind vorab geplant, ihr Inhalt entspricht aber dem letzten Sync.
- Mit kostenlosem Apple-Account muss die App alle 7 Tage aus Xcode neu installiert werden, und zeitkritische Benachrichtigungen sind nicht verfügbar.
- Änderungen in Outlook bemerkt die App erst beim nächsten Sync.
