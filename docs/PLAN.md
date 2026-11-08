# Plan: Kalender-Assistent

Status: **Freigegeben (Version 4)** · Stand 08.11.2026
Grundlage: [AUFTRAG.md](AUFTRAG.md) (der ursprüngliche Auftrag, Abschnittsnummern beziehen sich darauf). Wo dieser Plan vom Auftrag abweicht, gilt der Plan.

Änderungen gegenüber Version 3:
- **Zwei Plattformen:** Android (Handy der Mutter, zuerst zum Testen mit den eigenen Kalendern) und iPhone, danach Mac. **Android kommt zuerst.**
- **Gemeinsamer Kern in Kotlin Multiplatform** statt in Swift. Die Oberfläche bleibt pro Plattform nativ: Jetpack Compose auf Android, SwiftUI auf iPhone und Mac.
- Android-Besonderheiten und -Grenzen ergänzt (Abschnitt 4).
- Phasen neu geordnet (Abschnitt 6).

Frühere Versionen: Die Erinnerungen-App von Apple entfällt (V2). Die Datenquelle ist der Kalender des Geräts, keine App-Registrierung bei Microsoft (V3). Die App liest Kalender nur (V3).

## 1. Entscheidungen

| Thema | Entscheidung |
|---|---|
| Plattformen | Android (minSdk 26, Android 8) zuerst, dann iPhone (iOS 17+), dann Mac (macOS 14+) |
| Technik | Kern in Kotlin Multiplatform (`core`), Oberfläche nativ: Jetpack Compose (Android), SwiftUI (iPhone, Mac) |
| Nutzer | Du (iPhone) und deine Mutter (Android). Jedes Gerät arbeitet für sich, es gibt keinen Austausch zwischen den Geräten. |
| Datenquelle | Der Kalender des Geräts: Android-Kalenderspeicher (bei deiner Mutter Samsung Kalender), iOS EventKit. Keine App-Registrierung bei Microsoft. |
| Kalenderzugriff | **Nur lesen.** Die App erstellt, ändert oder löscht nie Termine und beantwortet keine Einladungen. Eigene Markierungen (Terminart, „wichtig") bleiben nur in der App. |
| Internet | Die App baut keine Verbindungen auf. Die Android-App hat nicht einmal die Berechtigung dazu. |
| Konten | Bei dir 2 Microsoft-Konten (privat und Arbeit) mit zusammen 3 Kalendern. Deine Mutter: ihre Kalender im Samsung Kalender. |
| Verteilung | Privat, kein App Store, kein Google Play. Android: APK direkt aufs Handy. iPhone: über Xcode. |
| Entwickleraccount | Android: keiner nötig. Apple: Start mit kostenlosem Account (Abschnitt 8). |
| Erinnerungen-App | Entfällt. Alle Erinnerungen sind Benachrichtigungen der App. |
| Einstellbarkeit | Alles, was sinnvoll individuell einstellbar ist, wird einstellbar, mit gutem Standardwert (Abschnitt 5). |
| Fahrzeit | Gestrichen. Später nachrüstbar. |
| „Wichtig" | Manuell in der App markierbar. Die Outlook-Wichtigkeit liefern die Gerätekalender nicht. |
| Vorbereitungshinweis | Entfällt in der ersten Version. |
| Sonderregeln, geteilte Kalender | Keine (Annahme). |
| Design | Minimalistisch, auf jeder Plattform mit nativer Optik (Abschnitt 11). |
| Ablage | Privates GitHub-Repository `Goepfarth/kalender-assistent` |

Phasenzuordnung: Join-Button in der Online-Benachrichtigung in Phase 1, Snooze und „Erledigt" in Phase 2. Warnhinweise im Tagesbriefing in Phase 1, Konflikte über mehrere Tage in Phase 2. Widget in Phase 2.

## 2. Datenquelle

Die App liest die Termine aus dem Kalender des Geräts. Das Gerät selbst hält die Verbindung zu Microsoft, Google oder Samsung. Deshalb braucht die App keine App-Registrierung bei Microsoft, keine Anmeldung und keine Tokens.

**Android:** Kalenderspeicher (`CalendarContract`). Alles, was im Samsung Kalender oder Google Kalender erscheint, kann die App lesen, auch Outlook-Konten, wenn sie im Samsung Kalender eingebunden sind.
- Berechtigung: „Kalender lesen" (`READ_CALENDAR`). Android hat, anders als iOS, eine echte Nur-Lese-Berechtigung.
- Serientermine expandiert der Kalenderspeicher selbst (`Instances`).
- Duplikate: über die iCalendar-UID (`UID_2445`), falls der Kalender sie liefert. Phase A0 prüft das. Rückfallebene: gleicher Titel, gleicher Beginn und gleiches Ende.

**iPhone und Mac:** EventKit, mit den in den Einstellungen eingerichteten Konten (Phase 0a erledigt: beide Konten klappen). iOS kennt keinen reinen Lesezugriff, „Kalender: Vollzugriff" erlaubt technisch auch Schreiben. Die App nutzt das nie.

**Für beide gilt:**
- Online-Termine werden über Links in Ort und Beschreibung erkannt: Teams, Zoom, Google Meet, Webex, auch hinter Microsoft-„Safe Links". Ob die Links in den Gerätekalendern ankommen, prüfen Phase A0 (Android) und Phase 0a (iPhone, noch offen).
- Abgesagte, abgelehnte und vorläufige Termine erkennt die App über Terminstatus und eigenen Teilnahmestatus.
- Nur der jeweilige Kalender-Adapter greift auf den Kalender zu, ausschließlich mit Lesemethoden. Ein Test durchsucht den Quellcode nach Schreibaufrufen.

**Ausweichweg Microsoft Graph:** nur für den Fall, dass ein Konto nicht im Gerätekalender auftaucht. Wird nicht gebaut. Anleitung für den Notfall: [ENTRA-EINRICHTUNG.md](ENTRA-EINRICHTUNG.md).

## 3. Architektur

```
kalender-assistent/
├── core/                 Kotlin Multiplatform, gemeinsamer Kern ohne Plattformcode
│   └── src/commonMain    Model, Normalisierung, Klassifikation, Regeln, Einstellungen,
│                         Ruhezeiten, Scheduling, Briefing
├── androidApp/           Android: Jetpack Compose und Adapter
│                         (Kalenderspeicher, Alarme, Benachrichtigungen, WorkManager)
├── iosApp/               iPhone: SwiftUI und Adapter (EventKit, Benachrichtigungen, BGTasks)
├── macApp/               Mac: SwiftUI-Menüleisten-App (Phase 4)
└── docs/
```

Schichten wie in Abschnitt 10 des Auftrags: Datenquelle → Normalisierung → Klassifikation → Regelengine → Scheduling.
- **`core`** enthält nur reine Logik. Er läuft auf der JVM und wird hier auf Windows mit `gradlew :core:jvmTest` getestet. Die Apple-Ziele kommen in der iPhone-Phase dazu, sie lassen sich nur auf dem Mac bauen. Für iPhone und Mac wird `core` als XCFramework eingebunden, Swift ruft ihn direkt auf.
- Zeit kommt immer über ein `Clock`-Interface, nie über die Systemuhr direkt.
- Die Adapter pro Plattform sind dünn: Sie lesen Termine, übergeben sie an `core` und setzen dessen Plan um (Benachrichtigungen planen, ersetzen, löschen).

**Kernideen**

- **Soll/Ist-Abgleich:** Bei jedem Sync berechnet `core` die vollständige Liste aller Benachrichtigungen, die existieren sollen. Der Adapter gleicht sie mit dem ab, was geplant ist. Verschobene Termine, Absagen und geänderte Einstellungen laufen damit über denselben Weg und wirken sofort auf bereits geplante Erinnerungen.
- **Stabile IDs:** Die Benachrichtigungs-ID setzt sich aus Erinnerungsart und Duplikatschlüssel zusammen. Ersetzen statt neu anlegen, deshalb keine Duplikate.
- **Zeit:** Termine werden in der Gerätezeitzone dargestellt. Ganztägige Termine bleiben Datumsangaben ohne Uhrzeit und verrutschen deshalb beim Sommerzeitwechsel nicht.
- **Datenschutz:** keine Netzwerkverbindungen, kein Analytics, Protokolle nur lokal.
- **Texte:** Deutsch, auf Android in `strings.xml`, auf iOS in String-Catalogs. Englisch später ergänzbar.

## 4. Erinnerungen und Briefing

| Terminart | Erinnerung (Standard) |
|---|---|
| Online | 15 Min. vorher, mit Button „Teilnehmen". Keine Vortags-Erinnerung. |
| Präsenz | am Vortag, dazu optional ein Hinweis am Termintag |
| Sonstig | keine (einschaltbar), im Briefing sichtbar |

Die Vortags-Erinnerung gibt es **nur für Präsenztermine**. Alle Zeiten, Schalter und das Verhalten in der Ruhezeit sind einstellbar (Abschnitt 5).

- **Ruhezeiten:** wie in Abschnitt 5 des Auftrags, als reine Funktion in `core` mit Begründung für die Vorschau. Tests decken Fenster über Mitternacht, beide Sommerzeitwechsel und Termine knapp nach Ende der Ruhezeit ab.
- **Budget:** `core` plant nach Auslösezeit bis zu einem Limit pro Plattform: iOS 64 ausstehende Benachrichtigungen (davon bis zu 7 Morgenbriefings und eine Wächter-Benachrichtigung), Android 400 (Android erlaubt höchstens 500 Alarme pro App). Bei jedem Sync wird nachgefüllt.
- **Berechtigungen** werden erst bei Bedarf angefragt, mit kurzer Erklärung vorher. Ohne Benachrichtigungs-Berechtigung zeigt die App einen dauerhaften Hinweis, das Briefing funktioniert weiter.

### Android

- **Morgenbriefing frisch:** Zur eingestellten Uhrzeit weckt ein Alarm die App, sie liest die Termine neu und erzeugt die Benachrichtigung erst dann. Der Inhalt ist also aktuell.
- **Änderungen:** Die App reagiert auf Änderungen im Kalenderspeicher (WorkManager mit Content-URI-Trigger) und prüft zusätzlich regelmäßig. Verschobene und abgesagte Termine werden damit meist innerhalb von Minuten nachgeführt.
- **Exakte Zeiten:** Die Erinnerungen laufen über exakte Alarme (`setExactAndAllowWhileIdle`, Berechtigung `USE_EXACT_ALARM`, die Android Kalender-Apps beim Installieren gewährt).
- **Neustart und Zeitumstellung:** Android löscht Alarme beim Neustart. Die App plant nach dem Hochfahren, bei Zeit- und Zeitzonenwechsel neu.
- **Benachrichtigungskanäle** pro Erinnerungsart. Ton und Wichtigkeit lassen sich zusätzlich in den Android-Einstellungen anpassen. Für „wichtige Termine" gibt es einen eigenen Kanal, den du bei „Nicht stören" als Ausnahme erlauben kannst. Die App selbst umgeht „Nicht stören" nie.
- **Akku-Optimierung:** Samsung schickt selten genutzte Apps in den Tiefschlaf, dann kommen Erinnerungen zu spät oder gar nicht. Die App erkennt das, erklärt, wie man sie auf „Nicht eingeschränkt" stellt, und zeigt einen Hinweis, solange das nicht erledigt ist.

### iPhone

1. **Hintergrundlauf:** iOS entscheidet selbst, wann `BGAppRefreshTask` läuft, garantiert ist das nicht. Erinnerungen sind deshalb immer vorab geplant und kommen auch ohne Hintergrundlauf, allerdings auf dem Stand des letzten Syncs.
2. **Morgen-Benachrichtigung:** Ihr Inhalt ist statisch und entspricht dem letzten Sync, im ungünstigsten Fall ist er mehrere Tage alt. Deshalb steht im Text der Stand („Stand: Sa 21:40"), und beim Antippen synchronisiert die App sofort.
3. **Änderungen in Outlook** wirken erst, wenn die App das nächste Mal läuft.
4. **Fokus-Modi** bleiben unangetastet. Ohne bezahlten Account gibt es keine zeitkritischen Benachrichtigungen, ein aktiver Fokus kann auch wichtige Termine zurückhalten.

### Für beide

- **Sync-Stand:** „Zuletzt synchronisiert um …", Warnung ab einer einstellbaren Zeit (Standard 12 Stunden).
- **Doppelte Benachrichtigungen:** Outlook-App, Samsung Kalender und Apple Kalender senden eigene Terminerinnerungen. Sobald die App läuft, sollten deren Mitteilungen aus sein. Die App weist beim Einrichten darauf hin, abschalten kann sie das nicht.

## 5. Einstellungen und Standardwerte

**Grundsatz:** Was sinnvoll individuell einstellbar ist, ist einstellbar. Jede Einstellung hat einen Standardwert, mit dem die App ohne Einrichten funktioniert. Häufige Einstellungen stehen oben, Schwellwerte unter „Erweitert". Neue Funktionen bekommen ihre Einstellungen gleich mit und werden in dieser Tabelle ergänzt. Die Einstellungen sind auf beiden Plattformen gleich, sie leben in `core`.

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

**Phase 0a: Kontentest auf dem iPhone** · erledigt (beide Konten klappen). Offen: Steht bei Teams-Terminen der Link in den Notizen?

**Phase A0: Android-Grundgerüst und Machbarkeit** · jetzt, auf dem Windows-PC
- Gradle-Projekt mit `core` (Kotlin Multiplatform) und `androidApp` (Compose)
- erste Kernfunktion mit Tests: Erkennung von Meeting-Links
- Testansicht: Kalender und Termine der nächsten 7 Tage, mit Prüfwerten (UID vorhanden, Online-Link erkannt, Status)
- So probierst du es aus: App per USB aufs Handy, Kalenderzugriff erlauben, die Liste mit Samsung Kalender vergleichen.

**Phase 1: Kern auf Android**
- 1a: `core`-Logik mit Unit-Tests: Normalisierung, Klassifikation mit Begründung, Duplikate, Serientermine, Zeitzonen und Sommerzeit, Einstellungen, Ruhezeiten, Scheduling mit Budget, Briefing
- 1b: Android-Adapter: Kalenderspeicher, Speicher für Einstellungen und Overrides, Sync (Start, Änderungen, regelmäßig, Neustart)
- 1c: Benachrichtigungen mit „Teilnehmen", exakte Alarme, Kanäle, Soll/Ist-Abgleich, Berechtigungen und Hinweise (inkl. Akku-Optimierung)
- 1d: Oberfläche: Briefing, Kalender, alle Einstellungen aus Abschnitt 5, Ruhezeiten mit Vorschau, Overrides. Vorher bekommst du ein Design-Mockup zur Abstimmung.
- Abschluss: Akzeptanzkriterien (Abschnitt 7) auf Android

**Phase 1i: iPhone** · auf dem Mac
- Apple-Ziele für `core`, XCFramework, Xcode-Projekt, `Config.xcconfig`, Bundle-ID
- iOS-Adapter: EventKit, Benachrichtigungen, BGTasks; SwiftUI-Oberfläche nach demselben Mockup
- Abschluss: Akzeptanzkriterien auf dem iPhone

**Phase 2: Komfort** (beide): Snooze und „Erledigt", Widgets, Konflikte über mehrere Tage, Wochenvorschau am Sonntagabend.

**Phase 3: optional:** Apple Watch. (Ein Push-Backend ist ohne Microsoft-Anbindung und ohne bezahlten Apple-Account nicht sinnvoll.)

**Phase 4: Mac:** Menüleisten-App mit Tagesübersicht, Briefing-Fenster und Benachrichtigungen auf Basis von `core` und EventKit. Damit Mac und iPhone nicht gleichzeitig klingeln, lässt sich jede Erinnerungsart pro Gerät an- und abschalten. Vorschlag: Online-Erinnerungen auf beiden Geräten, alles andere nur auf dem iPhone.

**Noch nicht eingeplant** (aus Abschnitt 7 des Auftrags, später nach Absprache): Fahrzeit und Wetter, Vorbereitungsnotizen, Filter und Regeln (Stummschalten, private Termine anonymisieren), Live Activity, Export als Text/PDF.

## 7. Akzeptanzkriterien Phase 1 (pro Plattform)

- Zwei verschiedene Konten sind eingebunden, und das Briefing zeigt Termine aus beiden korrekt sortiert.
- Ein Online-Termin um 10:00 erzeugt um 09:45 eine Benachrichtigung mit funktionierendem Button „Teilnehmen".
- Ein Präsenztermin übermorgen erscheint im Kapitel „Präsenztermine in 2 Tagen", und am Vortag kommt eine Benachrichtigung.
- Wird der Termin verschoben, passt sich die Erinnerung nach dem nächsten Sync an, ohne Duplikat. Wird er abgesagt, verschwindet sie.
- Dieselbe Besprechung in zwei Kalendern erzeugt genau eine Erinnerung.
- Bei verweigerter Benachrichtigungs-Berechtigung zeigt die App einen dauerhaften Hinweis mit Weg zu den Einstellungen, und das Briefing funktioniert weiter.
- Ruhezeiten (Zeitfenster, Wochentage, Verhalten pro Erinnerungsart) lassen sich ändern, und die Änderung wirkt nach dem Speichern auf alle bereits geplanten Erinnerungen.
- Das Morgenbriefing lässt sich pro Wochentag an- und abschalten und mit eigener Uhrzeit versehen.
- Nur Android: Nach einem Neustart des Handys kommen die Erinnerungen weiter.

## 8. Apple-Entwickleraccount

Der kostenlose Account reicht für den Start. Laut Apples Übersicht der Capabilities (developer.apple.com, geprüft am 08.11.2026):

| Funktion | Kostenlos | Wofür gebraucht |
|---|---|---|
| Background Modes | ja | Hintergrund-Sync |
| App Groups | ja | Widget (Phase 2) |
| Time Sensitive Notifications | nein | „Wichtig" trotz aktivem Fokus |
| iCloud / CloudKit | nein | nicht geplant |
| MapKit-Routen | nein | spätere Fahrzeit |

Eine mit kostenlosem Account signierte App muss alle 7 Tage aus Xcode neu installiert werden. Die Daten bleiben dabei erhalten. Eine selbst erstellte IPA umgeht das nicht, AltStore oder SideStore erneuern die Signatur nur automatisch. Über den bezahlten Account (99 €/Jahr) entscheiden wir am Ende von Phase 1i.

Android braucht nichts davon: Die APK läuft unbegrenzt.

## 9. Vorschlag außerhalb des Auftrags (offen, braucht Freigabe)

**Wächter-Benachrichtigung:** Jeder Sync plant eine Benachrichtigung 24 Stunden in die Zukunft und schiebt sie beim nächsten Sync weiter. Sie kommt also nur, wenn die Syncs ausbleiben: „Termine seit … nicht aktualisiert. Bitte App kurz öffnen." Vor allem auf dem iPhone sinnvoll. Die Wartezeit wäre einstellbar.

## 10. Risiken

| Risiko | Auswirkung | Gegenmaßnahme |
|---|---|---|
| Samsung schickt die App in den Tiefschlaf | Erinnerungen kommen zu spät | Hinweis und Anleitung „Nicht eingeschränkt", Prüfung in der App |
| Meeting-Links fehlen in den Gerätekalendern | Online-Termin falsch erkannt | Phase A0 und 0a prüfen das; Override pro Kalender und Termin |
| UID fehlt im Android-Kalender | Duplikate schwerer erkennbar | Rückfallebene Titel und Zeiten |
| Hintergrundlauf auf dem iPhone bleibt aus | Erinnerungen auf altem Stand | Vorab planen, Sync-Stand sichtbar, Wächter (Abschnitt 9) |
| Doppelte Benachrichtigungen anderer Kalender-Apps | mehrfaches Klingeln | Hinweis beim Einrichten |
| 7-Tage-Signatur auf dem iPhone | App startet nicht mehr | wöchentlich aus Xcode installieren oder bezahlter Account |
| Kotlin-Kern auf iOS | zusätzlicher Build-Schritt auf dem Mac | XCFramework per Gradle-Task, in Phase 1i eingerichtet |
| Zu viele Einstellungen | unübersichtlich | gute Standardwerte, Schwellwerte unter „Erweitert" |

## 11. Design-Richtung

- Eine Hauptansicht: das Briefing als ruhige, scrollbare Seite in der Reihenfolge aus dem Auftrag.
- Systemschrift, viel Weißraum, Graustufen und genau eine Akzentfarbe. Auf Android Material 3, auf iOS die Apple-Optik, mit denselben Inhalten und derselben Ordnung.
- Kalenderfarbe als schmaler Streifen links am Termin, Terminart als Symbol (Kamera für Online, Pin für Präsenz, Kreis für Sonstig).
- Vorläufige Termine: gestrichelter Streifen und der Hinweis „vorläufig".
- Der Button „Teilnehmen" ist die einzige farbige Fläche am Termin.
- Einstellungen in klaren Gruppen nach Abschnitt 5, jede Option mit einem Satz Erklärung, „Erweitert" eingeklappt.
- Dunkler Modus, große Schrift und Screenreader von Anfang an.
- Vor Phase 1d gibt es ein Mockup zur Abstimmung.

## 12. Offen

- Ergebnisse von Phase A0: UID und Meeting-Links im Android-Kalender
- Phase 0a: Teams-Link in den Notizen auf dem iPhone
- Wächter-Benachrichtigung (Abschnitt 9): ja oder nein?
