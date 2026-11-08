# Kalender-Assistent

Eine private iPhone-App (später auch Mac), die mehrere Outlook-Kalender zusammenführt, jeden Morgen ein Briefing erstellt und rechtzeitig an Termine erinnert.

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
