# Plan: Kalender-Assistent

Status: **Freigegeben (Version 3)** · Stand 08.11.2026 · Phase 0a erledigt: Beide Konten laufen in iOS, die Datenquelle ist EventKit.
Grundlage: [AUFTRAG.md](AUFTRAG.md) (der ursprüngliche Auftrag, Abschnittsnummern beziehen sich darauf). Wo dieser Plan vom Auftrag abweicht, gilt der Plan.

Änderungen gegenüber Version 2:
- **Datenquelle:** EventKit ist jetzt der bevorzugte Weg, ohne App-Registrierung bei Microsoft. Graph bleibt Ausweichweg (Abschnitt 2).
- **Phase 0a** ist jetzt ein Kontentest auf dem iPhone statt im Browser.
- **Grundsatz Einstellbarkeit:** Was sinnvoll individuell einstellbar ist, wird einstellbar gebaut (Abschnitt 5).
- **Morgenbriefing** pro Wochentag einstellbar. Die Vortags-Erinnerung gilt ausdrücklich nur für Präsenztermine.
- **Neue Grenze:** doppelte Benachrichtigungen durch die Outlook-App und Apple Kalender (Abschnitt 4).

Änderungen in Version 2: Die Erinnerungen-App von Apple entfällt, die App erinnert selbst per Benachrichtigung. Akzeptanzkriterien angepasst (Abschnitt 7).

## 1. Entscheidungen

| Thema | Entscheidung |
|---|---|
| Plattform | iPhone zuerst (iOS 17+), danach Mac (macOS 14+). Swift/SwiftUI. Gebaut wird auf dem Mac mit Xcode (macOS 27). |
| Konten | 2 Microsoft-Konten derselben Person (privat und Arbeit), zusammen 3 Kalender. |
| Datenquelle | **EventKit**, entschieden nach Phase 0a. Graph wird nicht gebaut und bleibt nur als Ausweichweg beschrieben. |
| Kalenderzugriff | **Nur lesen.** Die App erstellt, ändert oder löscht nie Termine und beantwortet keine Einladungen. Eigene Markierungen (Terminart, „wichtig") bleiben nur in der App. |
| Geteilte Kalender | Nicht genannt. Annahme: keine. |
| Verteilung | Nur für den Eigengebrauch, kein App Store. |
| Entwickleraccount | Start mit kostenlosem Account (Abschnitt 8). |
| Sonderregeln | Keine. |
| Erinnerungen-App | Entfällt. Alle Erinnerungen sind Benachrichtigungen der App. |
| Einstellbarkeit | Alles, was sinnvoll individuell einstellbar ist, wird einstellbar, mit gutem Standardwert (Abschnitt 5). |
| Fahrzeit | Gestrichen: keine MapKit-Fahrzeit, kein „Jetzt losfahren", keine Fahrzeit im Briefing. Später nachrüstbar. |
| „Wichtig" | Manuell in der App markierbar. Die Outlook-Wichtigkeit liefert EventKit nicht. |
| Vorbereitungshinweis | Entfällt in der ersten Version. |
| Design | Minimalistisch, native iOS-Optik (Abschnitt 11). |
| Ablage | Privates GitHub-Repository. |

Phasenzuordnung:
- Join-Button in der Online-Benachrichtigung: Phase 1. Snooze und „Erledigt": Phase 2.
- Warnhinweise im Tagesbriefing (Überschneidung, keine Pause, Präsenz direkt nach Online): Phase 1. Konfliktauswertung über mehrere Tage: Phase 2.
- Widget: Phase 2.

### Folgen des Wegfalls der Erinnerungen-App

- **Einfacher:** kein Zugriff auf die Erinnerungen-App, keine doppelten Einträge über iCloud zwischen iPhone und Mac.
- **Nachteil:** Ein Eintrag in der Erinnerungen-App bleibt stehen, bis du ihn abhakst. Eine weggewischte Benachrichtigung ist dagegen weg. Ausgleich: Benachrichtigungen bleiben in der Mitteilungszentrale, bis du sie entfernst, und die App zeigt anstehende Präsenztermine im Briefing. Die Apple Watch spiegelt iPhone-Benachrichtigungen ohnehin.
- **64er-Limit:** Die Präsenz-Erinnerungen zählen jetzt mit zum Budget (Abschnitt 4).

## 2. Datenquelle

Jede App, die selbst mit Microsofts Servern spricht, braucht eine App-Registrierung bei Microsoft, auch eine rein private. Das lässt sich nur umgehen, wenn iOS die Verbindung zu Microsoft übernimmt und die App die Termine vom iPhone liest.

**Entschieden: EventKit.** Beide Konten ließen sich in iOS einrichten (Phase 0a, 08.11.2026).

### Gewählt: EventKit (Weg B), ohne App-Registrierung

Beide Konten werden einmal in den iPhone-Einstellungen eingerichtet: das private als „Outlook.com", das Arbeitskonto als „Microsoft Exchange". iOS synchronisiert sie selbst, die App liest sie über EventKit.

Vorteile:
- keine App-Registrierung, kein Azure-Konto, keine Kreditkarte
- keine Microsoft-Anmeldung in der App, kein MSAL, keine Tokens
- iOS hält die Termine selbst aktuell, auch wenn die App nicht läuft
- funktioniert genauso auf dem Mac (Phase 4)

Nachteile:
- Die Outlook-Wichtigkeit fehlt. „Wichtig" geht dann nur manuell in der App.
- Online-Termine werden nur über Links in Ort, URL-Feld und Notizen erkannt (Regex), nicht über ein eigenes Feld. Teams-Einladungen enthalten den Link normalerweise in der Beschreibung. Phase 0a prüft das.
- Duplikate: EventKit liefert die externe ID des Kalenderservers. Ob sie für dieselbe Besprechung in zwei Kalendern gleich ist, prüft Phase 0b. Rückfallebene: gleicher Titel, gleicher Beginn und gleiches Ende.
- Die App braucht zusätzlich die Berechtigung „Kalender: Vollzugriff". iOS kennt keinen reinen Lesezugriff, „Vollzugriff" erlaubt technisch auch Schreiben. Die App nutzt das nie: Nur `EventKitSource` greift auf `EKEventStore` zu, und zwar ausschließlich mit Lesemethoden. Ein Test durchsucht den Quellcode nach Schreibaufrufen (`save`, `remove`, `commit`).
- Der Arbeitgeber muss erlauben, dass das Arbeitskonto in der iOS-Kalender-App läuft.

Abgesagte, abgelehnte und vorläufige Termine erkennt EventKit über Terminstatus und eigenen Teilnahmestatus. Die Regeln aus dem Auftrag gelten unverändert.

### Ausweichweg: Microsoft Graph (Weg A), mit App-Registrierung

Nur nötig, wenn sich ein Konto nicht in iOS einrichten lässt, und dann nur für dieses Konto. Ehrlicher Hinweis: Wenn der Arbeitgeber das Arbeitskonto in der iOS-Kalender-App sperrt, sperrt er oft auch fremde Apps. Dann bleibt nur, die IT zu fragen.

- Liefert zusätzlich `isOnlineMeeting`, `onlineMeeting.joinUrl`, `importance` und `iCalUId`. `calendarView` expandiert Serientermine serverseitig.
- Scopes: `Calendars.Read`, `offline_access`. Anmeldung über MSAL als Public Client mit PKCE, ohne Client-Secret. Tokens liegen nur im Keychain.
- App-Registrierung in einem eigenen Entra-Tenant: [ENTRA-EINRICHTUNG.md](ENTRA-EINRICHTUNG.md).

Beide Wege stecken hinter dem Protokoll `CalendarSource`. Mischen geht auch, zum Beispiel privat über EventKit und Arbeit über Graph. ICS-Abos sind nicht eingeplant (verzögert, und wer den Link kennt, sieht die Termine).

## 3. Architektur

```
kalender-assistent/
├── Core/                 Swift-Package, nur Foundation, keine Apple-UI-Frameworks
│   ├── Model             Termin, Kalender, Konto (normalisiert, quellenunabhängig)
│   ├── Normalization     Quelldaten → Termin, Zeitzonen, ganztägig/mehrtägig, Duplikate
│   ├── Classification    Online / Präsenz / Sonstig, mit Begründung und Overrides
│   ├── Rules             Erinnerungsregeln, Ruhezeiten
│   ├── Settings          alle Einstellungen mit Standardwerten (Abschnitt 5)
│   ├── Scheduling        Soll-Plan aller Benachrichtigungen, Abgleich mit Ist, 64er-Budget
│   └── Briefing          Heute, Warnungen, freie Zeitfenster, Präsenz in N Tagen, Vorschau morgen
├── Adapters/             Plattformcode hinter Protokollen aus Core
│   ├── EventKitSource    Termine aus den in iOS eingerichteten Konten
│   ├── GraphSource       nur falls nötig: MSAL, calendarView, Token im Keychain
│   ├── Store             SwiftData: Einstellungen, Overrides, Sync-Stand, Cache (für Graph)
│   ├── Notifications     UNUserNotificationCenter, Aktion „Teilnehmen"
│   └── BackgroundSync    BGAppRefreshTask, BGProcessingTask
├── App-iOS/              SwiftUI, dünn
├── App-macOS/            Phase 4
└── docs/
```

Schichten wie in Abschnitt 10 gefordert: Datenquelle → Normalisierung → Klassifikation → Regelengine → Scheduling. `Core` enthält nur reine Logik und ist mit `swift test` ohne iPhone testbar. Zeit kommt immer über ein `Clock`-Protokoll, nie über `Date()`. Tests nutzen Swift Testing, der Code den Swift-6-Sprachmodus.

**Kernideen**

- **Soll/Ist-Abgleich:** Bei jedem Sync berechnet `Core` die vollständige Liste aller Benachrichtigungen, die existieren sollen. Der Adapter vergleicht sie mit den bei iOS ausstehenden Benachrichtigungen und legt an, ersetzt oder löscht. Verschobene Termine, Absagen und geänderte Einstellungen laufen damit über denselben Weg und wirken sofort auf bereits geplante Erinnerungen.
- **Stabile IDs:** Die Benachrichtigungs-ID setzt sich aus Erinnerungsart und Duplikatschlüssel zusammen, zum Beispiel `online:<Schlüssel>`. iOS ersetzt eine Benachrichtigung mit gleicher ID, statt eine zweite anzulegen. Die IDs lassen sich jederzeit aus iOS auslesen, ein verlorener Speicher führt also nicht zu Duplikaten.
- **Sync mit EventKit:** Die App liest beim Start, bei Rückkehr in den Vordergrund, bei Änderungsmeldungen von EventKit (während sie läuft) und in den Hintergrund-Tasks. Neue Termine holt iOS selbst. Die App muss trotzdem laufen, um Erinnerungen neu zu planen.
- **Zeit:** Termine werden in der Gerätezeitzone dargestellt. Ganztägige Termine bleiben Datumsangaben ohne Uhrzeit und verrutschen deshalb beim Sommerzeitwechsel nicht.
- **Speicher:** Dateischutz „bis zur ersten Entsperrung", damit der Hintergrund-Sync nach einem Neustart lesen kann. Briefing und geplante Erinnerungen funktionieren ohne Netz.
- **Datenschutz:** Die App selbst baut keine Verbindungen auf. Mit Graph sind die einzigen Ziele die Microsoft-Anmeldung und Graph. Kein Analytics-SDK. Protokolle bleiben lokal (`os.Logger`).
- **Texte:** von Anfang an in String-Catalogs (Deutsch), Englisch später ergänzbar.

## 4. Erinnerungen und Briefing

Welche Erinnerung für welche Terminart gilt:

| Terminart | Erinnerung (Standard) |
|---|---|
| Online | 15 Min. vorher, mit Button „Teilnehmen". Keine Vortags-Erinnerung. |
| Präsenz | am Vortag, dazu optional ein Hinweis am Termintag |
| Sonstig | keine (einschaltbar), im Briefing sichtbar |

Die Vortags-Erinnerung gibt es **nur für Präsenztermine**. Alle Zeiten, Schalter und das Verhalten in der Ruhezeit sind einstellbar (Abschnitt 5).

- **64er-Limit:** Von den 64 Plätzen gehören bis zu 7 den Morgenbriefings der nächsten 7 Tage und einer der Wächter-Benachrichtigung (falls freigegeben, Abschnitt 9). Die übrigen werden nach Auslösezeit mit Terminerinnerungen gefüllt. Bei jedem Sync wird nachgefüllt. Die App zeigt „Erinnerungen geplant bis …".
- **Ruhezeiten:** wie in Abschnitt 5 des Auftrags. Die Logik ist eine reine Funktion in `Core`: Sie bekommt geplanten Zeitpunkt, Terminbeginn, Erinnerungsart und Konfiguration und liefert den neuen Zeitpunkt oder „verworfen", jeweils mit Begründung. Dieselbe Funktion erzeugt die Vorschau in den Einstellungen. Tests decken Fenster über Mitternacht, beide Sommerzeitwechsel und Termine knapp nach Ende der Ruhezeit ab.
- **Berechtigungen:** Kalender (nur bei EventKit) und Benachrichtigungen, jeweils erst beim ersten Bedarf angefragt, mit kurzer Erklärung vorher.
  - Ohne Kalenderzugriff zeigt die App, wie man ihn erteilt, und kann sonst nichts anzeigen.
  - Ohne Benachrichtigungen zeigt die App einen dauerhaften, deutlichen Hinweis mit Weg zu den iOS-Einstellungen. Das Briefing in der App funktioniert weiter.

### Ehrliche Grenzen

1. **Hintergrundlauf:** iOS entscheidet selbst, wann `BGAppRefreshTask` läuft. Bei regelmäßiger Nutzung meist mehrmals am Tag, garantiert ist das nicht. Nach dem Wegwischen der App, im Stromsparmodus oder bei abgeschalteter Hintergrundaktualisierung läuft gar nichts. `BGProcessingTask` läuft meist nachts am Ladegerät. Erinnerungen sind deshalb immer vorab geplant und kommen auch ohne Hintergrundlauf, allerdings auf dem Stand des letzten Syncs.
2. **Morgen-Benachrichtigung:** Ihr Inhalt ist statisch und entspricht dem letzten Sync. Im ungünstigsten Fall (App länger nicht geöffnet, kein Hintergrundlauf) ist er mehrere Tage alt. Deshalb steht im Text der Stand („Stand: Sa 21:40"), und beim Antippen synchronisiert die App sofort. Geplant werden die nächsten 7 Tage, damit kein Morgen ohne Benachrichtigung bleibt.
3. **Änderungen in Outlook** wirken erst, wenn die App das nächste Mal läuft. Bis dahin kommt die Erinnerung eines verschobenen Termins zur alten Zeit, und die eines abgesagten Termins kommt trotzdem.
4. **Sync-Stand:** Die App zeigt „Zuletzt synchronisiert um …" und warnt ab einer einstellbaren Zeit (Standard 12 Stunden).
5. **Fokus-Modi** bleiben unangetastet. Ohne bezahlten Account gibt es keine zeitkritischen Benachrichtigungen. Wichtige Termine umgehen dann zwar die eigenen Ruhezeiten, ein aktiver Fokus kann sie aber zurückhalten.
6. **Doppelte Benachrichtigungen:** Die Outlook-App und Apple Kalender senden eigene Terminerinnerungen. Sobald die App läuft, solltest du deren Mitteilungen abschalten (iOS-Einstellungen → Mitteilungen → Outlook bzw. Kalender). Die App weist beim Einrichten darauf hin. Selbst abschalten kann sie das nicht.
7. **Anmeldung (nur Graph):** Die Microsoft-Anmeldung kann ablaufen, beim Arbeitskonto auch durch Richtlinien des Arbeitgebers. Dann zeigt die App einen deutlichen Hinweis, und bereits geplante Erinnerungen bleiben bestehen.

## 5. Einstellungen und Standardwerte

**Grundsatz:** Was sinnvoll individuell einstellbar ist, ist einstellbar. Jede Einstellung hat einen Standardwert, mit dem die App ohne Einrichten funktioniert. Damit die Einstellungen übersichtlich bleiben, stehen die häufigen oben und die Schwellwerte unter „Erweitert". Neue Funktionen bekommen ihre Einstellungen gleich mit und werden in dieser Tabelle ergänzt.

| Bereich | Einstellung | Standard |
|---|---|---|
| **Morgenbriefing** | pro Wochentag an/aus und eigene Uhrzeit | Mo–Fr 07:00, Sa und So aus |
| | Abschnitte einzeln ein- und ausblenden (Heute, Warnungen, freie Zeitfenster, Präsenztermine, Vorschau morgen) | alle an |
| | Vorlauf des Präsenz-Kapitels | 2 Tage (übermorgen) |
| | Verhalten in der Ruhezeit | auf Ende der Ruhezeit verschieben |
| **Online-Erinnerung** | an/aus, Vorlaufzeit | an, 15 Min. |
| | Verhalten in der Ruhezeit | trotzdem senden |
| **Präsenz-Vortag** (nur Präsenztermine) | an/aus | an |
| | Zeitpunkt: gleiche Uhrzeit wie der Termin oder feste Uhrzeit | gleiche Uhrzeit; ganztägige Termine 18:00 |
| | Verhalten in der Ruhezeit | auf den Vorabend vorziehen |
| **Präsenz-Termintag** | an/aus, Vorlaufzeit | aus, 60 Min. |
| | Verhalten in der Ruhezeit | auf Ende der Ruhezeit verschieben |
| **Sonstige Termine** | Erinnerung an/aus, Vorlaufzeit | aus, 15 Min. |
| | Verhalten in der Ruhezeit | auf Ende der Ruhezeit verschieben |
| **Ruhezeiten** | an/aus; ein oder mehrere Zeitfenster; pro Wochentag unterschiedlich | an, 22:00–07:00 täglich |
| | wichtige Termine ignorieren die Ruhezeit | an |
| **Termine** | vorläufige Termine erinnern | an, mit „(vorläufig)" im Text |
| | hybride Termine behandeln als | Präsenz, Join-Link zusätzlich |
| **Pro Kalender** | an/aus, Farbe, Kurzname | an, Kalenderfarbe, Kalendername |
| | feste Terminart statt automatischer Erkennung | automatisch |
| | Erinnerungsarten abweichend an/aus | wie oben |
| **Pro Termin** | Terminart überschreiben, als wichtig markieren, Erinnerungen aus | – |
| **Erweitert** | freie Zeitfenster: Tagesrahmen pro Wochentag, Mindestlänge | 08:00–18:00, 30 Min. |
| | Warnung „Überschneidung" an/aus | an |
| | Warnung „keine Pause" an/aus, Schwelle | an, unter 5 Min. |
| | Warnung „Präsenz nach Online" an/aus, Schwelle | an, unter 30 Min. |
| | Sync-Warnung ab | 12 Stunden |
| | Planungshorizont | 14 Tage |

Feste Regeln (keine Einstellung nötig):
- Ganztägige Termine stehen in einer eigenen Zeile oben im Briefing. Eine Erinnerung bekommen sie nur als Präsenz-Vortag, wenn ein Ort angegeben ist.
- Mehrtägige Termine stehen an jedem Tag im Briefing („Tag 2 von 3"). Erinnert wird nur einmal zum Beginn.

## 6. Phasen

Jede Phase und jeder Schritt endet mit Tests und einer kurzen Anleitung „So probierst du es aus".

**Phase 0a: Kontentest auf dem iPhone** (erledigt am 08.11.2026: beide Konten klappen, alle 3 Kalender sichtbar)

1. iOS-Einstellungen → Kalender → Kalender-Accounts → Account hinzufügen. Das private Konto als „Outlook.com" einrichten, den Schalter „Kalender" einschalten.
2. Das Arbeitskonto als „Microsoft Exchange" einrichten, nur wenn dein Arbeitgeber das erlaubt.
3. In Apple Kalender prüfen: Sind alle 3 Kalender mit ihren Terminen sichtbar? Einen Teams-Termin öffnen: Steht der Teilnahme-Link in den Notizen?

Ergebnis:
- Beide Konten klappen: EventKit, keine App-Registrierung nötig.
- Das Arbeitskonto scheitert: Graph für das Arbeitskonto ([ENTRA-EINRICHTUNG.md](ENTRA-EINRICHTUNG.md)) oder die IT fragen.

**Phase 0b: Projekt und Machbarkeit auf dem Mac**

- Xcode-Projekt, `Core`-Package, Test-Targets, `Config.xcconfig`, Bundle-ID festlegen
- Termine der nächsten 7 Tage aus der gewählten Quelle lesen
- Prüfen: Wie verhalten sich Termin-IDs bei Serien und bei derselben Besprechung in zwei Kalendern?
- So probierst du es aus: App auf dem iPhone starten, Kalenderzugriff erlauben. Die Termine der nächsten 7 Tage aus allen 3 Kalendern erscheinen als einfache Liste.

**Phase 1: Kern iPhone**

- 1a: `Core`-Logik mit Unit-Tests: Normalisierung, Klassifikation mit Begründung, Duplikate, Serientermine, Zeitzonen und Sommerzeit, Einstellungen, Ruhezeiten, Scheduling mit 64er-Budget, Briefing
- 1b: Quellen-Adapter, Speicher, Sync (App-Start, Vordergrund, Änderungsmeldungen, Hintergrund), Sync-Status
- 1c: Benachrichtigungen mit „Teilnehmen", Soll/Ist-Abgleich, Berechtigungen und Hinweise
- 1d: Oberfläche: Briefing, Kalender, alle Einstellungen aus Abschnitt 5, Ruhezeiten mit Vorschau, Overrides pro Kalender und Termin. Vorher bekommst du ein Design-Mockup zur Abstimmung.
- Abschluss: Akzeptanzkriterien (Abschnitt 7) als Checkliste

**Phase 2: Komfort iPhone**

Snooze und „Erledigt" in der Benachrichtigung, Widget und Sperrbildschirm-Widget, Konflikte über mehrere Tage, Wochenvorschau am Sonntagabend.

**Phase 3: optional**

Backend für Push-Aktualisierung, Apple Watch. Das Backend braucht einen bezahlten Account (Push) und Graph. Aufwand, laufende Kosten und Datenschutzfolgen lege ich vorher vor.

**Phase 4: Mac**

Menüleisten-App mit Tagesübersicht, Briefing-Fenster und Benachrichtigungen auf Basis von `Core`. Mit EventKit liest der Mac die Konten aus den macOS-Internetaccounts. Jedes Gerät plant seine Erinnerungen selbst, ohne CloudKit (gibt es mit kostenlosem Account nicht). Damit nicht beide Geräte gleichzeitig klingeln, lässt sich jede Erinnerungsart pro Gerät an- und abschalten. Vorschlag: Online-Erinnerungen auf beiden Geräten (Teilnehmen am Mac ist praktisch), alles andere nur auf dem iPhone. Endgültig entschieden wird in Phase 4.

**Noch nicht eingeplant** (aus Abschnitt 7 des Auftrags, später nach Absprache): Fahrzeit und Wetter, Vorbereitungsnotizen, Filter und Regeln (Stummschalten, private Termine anonymisieren), Live Activity, Export als Text/PDF.

## 7. Akzeptanzkriterien Phase 1 (angepasst)

- Zwei verschiedene Outlook-Konten sind verbunden, und das Briefing zeigt Termine aus beiden korrekt sortiert.
- Ein Online-Termin um 10:00 erzeugt um 09:45 eine Benachrichtigung mit funktionierendem Button „Teilnehmen".
- Ein Präsenztermin übermorgen erscheint im Kapitel „Präsenztermine in 2 Tagen", und am Vortag kommt eine Benachrichtigung. *(geändert: vorher Eintrag in der Erinnerungen-App)*
- Verschiebst du den Termin in Outlook, wird nach dem nächsten Sync die Erinnerung angepasst, ohne Duplikat. Sagst du den Termin ab, verschwindet sie.
- Dieselbe Besprechung in zwei Kalendern erzeugt genau eine Erinnerung.
- Bei verweigerter Benachrichtigungs-Berechtigung zeigt die App einen dauerhaften Hinweis mit Weg zu den Einstellungen, und das Briefing funktioniert weiter. *(geändert: vorher Fallback der Erinnerungen-App)*
- In den Einstellungen lassen sich Ruhezeiten (Zeitfenster, Wochentage, Verhalten pro Erinnerungsart) ändern, und die Änderung wirkt nach dem Speichern auf alle bereits geplanten Erinnerungen.
- Das Morgenbriefing lässt sich pro Wochentag an- und abschalten und mit eigener Uhrzeit versehen. *(neu)*

## 8. Apple-Entwickleraccount

Der kostenlose Account reicht für den Start. Laut Apples Übersicht der Capabilities (developer.apple.com, geprüft am 08.11.2026):

| Funktion | Kostenlos | Wofür gebraucht |
|---|---|---|
| Keychain Sharing | ja | MSAL-Tokens (nur Graph) |
| Background Modes | ja | Hintergrund-Sync |
| App Groups | ja | Widget (Phase 2) |
| Time Sensitive Notifications | nein | „Wichtig" trotz aktivem Fokus |
| Push Notifications | nein | Backend (Phase 3) |
| iCloud / CloudKit | nein | Sync zwischen iPhone und Mac |
| MapKit-Routen | nein | spätere Fahrzeit |

Dazu kommt: Eine mit kostenlosem Account signierte App muss alle 7 Tage aus Xcode neu installiert werden. Die Daten bleiben dabei erhalten. Wenn dich das stört oder eine der gesperrten Funktionen nötig wird, lohnt sich das Apple Developer Program (99 €/Jahr).

Eine selbst erstellte IPA umgeht das nicht. Jede App auf dem iPhone braucht eine Signatur, und die des kostenlosen Accounts gilt 7 Tage, egal wie installiert wird. Tools wie AltStore oder SideStore erneuern sie automatisch, die Einschränkungen in der Tabelle bleiben aber. Über den Account entscheiden wir am Ende von Phase 1. Während der Entwicklung installiert Xcode die App ohnehin ständig neu.

## 9. Vorschlag außerhalb des Auftrags (offen, braucht Freigabe)

**Wächter-Benachrichtigung:** Jeder Sync plant eine Benachrichtigung 24 Stunden in die Zukunft und schiebt sie beim nächsten Sync weiter. Sie kommt also nur, wenn die Syncs ausbleiben: „Termine seit … nicht aktualisiert. Bitte App kurz öffnen." Das sichert die Vorgabe ab, dass Erinnerungen nie stillschweigend ausbleiben dürfen. Die Wartezeit wäre einstellbar.

## 10. Risiken

| Risiko | Auswirkung | Gegenmaßnahme |
|---|---|---|
| Arbeitgeber sperrt das Arbeitskonto in iOS | Arbeitskalender fehlt | Phase 0a testet das zuerst, dann Graph oder IT fragen |
| Teams-Link fehlt in den Notizen (EventKit) | Online-Termin als Präsenz oder Sonstig erkannt | Phase 0a prüft das, Override pro Kalender und Termin |
| Hintergrundlauf bleibt aus | Erinnerungen auf altem Stand | Vorab planen, Sync-Stand sichtbar, Wächter (Abschnitt 9) |
| Doppelte Benachrichtigungen von Outlook und Apple Kalender | Mehrfaches Klingeln | Hinweis beim Einrichten, deren Mitteilungen abschalten |
| 7-Tage-Signatur | App startet nicht mehr | Wöchentlich aus Xcode installieren oder bezahlter Account |
| Benachrichtigung weggewischt | Präsenz-Erinnerung übersehen | Mitteilungszentrale, Briefing zeigt anstehende Präsenztermine |
| Zu viele Einstellungen | unübersichtlich | gute Standardwerte, Schwellwerte unter „Erweitert" |
| Mehr anstehende Terminerinnerungen als Budget | spätere Tage noch nicht geplant | Anzeige „geplant bis", Nachfüllen bei jedem Sync |

## 11. Design-Richtung

- Eine Hauptansicht: das Briefing als ruhige, scrollbare Seite in der Reihenfolge aus dem Auftrag.
- Systemschrift (SF Pro), viel Weißraum, Graustufen und genau eine Akzentfarbe.
- Kalenderfarbe als schmaler Streifen links am Termin, Terminart als SF Symbol (Kamera für Online, Pin für Präsenz, Kreis für Sonstig).
- Vorläufige Termine: gestrichelter Streifen und der Hinweis „vorläufig".
- Der Button „Teilnehmen" ist die einzige farbige Fläche am Termin.
- Einstellungen in klaren Gruppen nach Abschnitt 5, jede Option mit einem Satz Erklärung, „Erweitert" eingeklappt.
- Dark Mode, Dynamic Type und VoiceOver von Anfang an.
- Vor Phase 1d gibt es ein Mockup zur Abstimmung.

## 12. Offen

- Rest von Phase 0a: Steht bei Teams-Terminen der Teilnahme-Link in den Notizen?
- Geteilte Kalender: wirklich keine?
- Wächter-Benachrichtigung (Abschnitt 9): ja oder nein?
