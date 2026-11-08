# Auftrag: Kalender-Assistent (iPhone zuerst, danach Mac)

Du bist ein erfahrener Senior-Entwickler für Apple-Plattformen. Baue eine App, die mehrere Outlook-Kalender zusammenführt, jeden Morgen ein gebündeltes Briefing erzeugt und rechtzeitig an Termine erinnert. Das iPhone hat Priorität. Die Mac-App folgt erst, wenn die iPhone-App fertig und getestet ist.

## 0. Arbeitsweise (zuerst lesen)

1. Stelle vor dem Coden alle Rückfragen gesammelt in einer Nachricht (siehe Abschnitt 9). Rate bei offenen Punkten nicht.
2. Lege danach einen kurzen Plan vor (Architektur, Phasen, Risiken) und warte auf meine Freigabe.
3. Arbeite in kleinen, lauffähigen Phasen. Jede Phase hat Tests und eine kurze Beschreibung, wie ich sie ausprobiere.
4. Schreibe keine Funktionen, die nicht in diesem Prompt stehen, ohne sie vorher vorzuschlagen.
5. Wenn eine Anforderung technisch nicht oder nur eingeschränkt umsetzbar ist, sage das klar und schlage eine Alternative vor. Verschweige keine Einschränkung.

## 1. Plattform-Annahme (bitte bestätigen oder korrigieren)

- Annahme: Ich nutze ein **iPhone** und einen **Mac**. Daher: native App in **Swift/SwiftUI**, Mindestversion iOS 17, später macOS 14.
- Struktur: ein gemeinsames Swift-Package (`Core`) mit Kalenderlogik, Regeln, Briefing-Erzeugung und Scheduling-Planung. Die UI-Targets für iOS und macOS bleiben dünn.
- Falls ich ein **Android**-Handy habe, ändert sich viel: Es gibt dort keine einheitliche System-Erinnerungs-App. Stelle diese Frage in Abschnitt 9, bevor du anfängst.

## 2. Datenquellen: mehrere Outlook-Kalender

Prüfe diese Wege und empfiehl einen. Begründe die Empfehlung kurz.

- **A) Microsoft Graph API** (MSAL für iOS, OAuth2 mit PKCE). Mehrere Konten gleichzeitig möglich. Abgefragt wird `calendarView` (expandiert Serientermine zu Einzelterminen), Scopes minimal: `Calendars.Read`, `offline_access`, für geteilte Kalender `Calendars.Read.Shared`. Risiko: Firmen-Tenants verlangen oft Admin-Zustimmung oder sperren Drittanbieter-Apps.
- **B) EventKit** (Kalender, die schon in der iOS-Kalender-App eingerichtet sind). Kein Azure-Setup nötig, funktioniert auch bei gesperrten Tenants, solange das Konto auf dem Gerät eingerichtet werden darf. Einschränkung: weniger Metadaten (z. B. Teilnehmerstatus, Online-Meeting-Felder sind begrenzt).
- **C) ICS-Abo-URLs** (veröffentlichte Kalender). Nur als Notlösung, da oft deaktiviert und verzögert.

Anforderungen:
- Beliebig viele Konten/Kalender, pro Kalender ein- und ausschaltbar, mit Farbe und Kurzname.
- Tokens ausschließlich im Keychain. Keine Kalenderdaten an Drittserver senden. Kein Analytics-SDK.
- Abgesagte Termine und von mir abgelehnte Termine werden ignoriert. Vorläufige Termine ("tentative") werden markiert, nicht verworfen.
- Dieselbe Besprechung in mehreren Kalendern (gleiche `iCalUId`) wird nur einmal angezeigt und nur einmal erinnert.
- Zeitzonen und Sommerzeitwechsel korrekt behandeln. Ganztägige Termine und mehrtägige Termine getrennt behandeln.
- Offline-Cache (SwiftData oder SQLite), damit Briefing und Erinnerungen ohne Netz funktionieren.

## 3. Terminklassifikation (Kernlogik, getrennt testbar)

Jeder Termin bekommt genau einen Typ:

- **Online**: Graph-Feld `isOnlineMeeting == true` oder `onlineMeeting.joinUrl` vorhanden. Fallback: Erkennung von Teams-, Zoom-, Google-Meet-, Webex-Links in Ort/Beschreibung per Regex.
- **Präsenz**: nicht online UND Ort/Adresse vorhanden.
- **Sonstig**: weder noch (z. B. Blocker, Erinnerungen, Ganztägiges ohne Ort). Standardmäßig keine Erinnerung, aber im Briefing sichtbar.
- Hybride Termine (Link + physischer Ort): als Präsenz behandeln, aber Join-Link zusätzlich anzeigen. Das Verhalten ist in den Einstellungen umschaltbar.
- Pro Kalender und pro Termin manuell überschreibbar. Die Klassifikation muss in der UI erklärbar sein ("als Online erkannt wegen Teams-Link").

## 4. Morgen-Briefing

- Wird jeden Tag zu einer einstellbaren Uhrzeit bereitgestellt (Standard 07:00, Wochenende separat einstellbar oder aus).
- Inhalt, in dieser Reihenfolge:
  1. **Heute**: chronologische Liste über alle Kalender, mit Uhrzeit, Titel, Kalenderfarbe, Typ (Online/Präsenz), Join-Button bzw. Ort.
  2. **Warnhinweise**: Überschneidungen, Termine ohne Pause dazwischen, Präsenztermin direkt nach einem Online-Termin (Fahrzeit unrealistisch).
  3. **Freie Zeitfenster** des Tages (ab einer einstellbaren Mindestlänge).
  4. **Eigenes Kapitel: "Präsenztermine in 2 Tagen"**. Alle Präsenztermine, die übermorgen stattfinden, mit Ort, Uhrzeit, Fahrzeitschätzung (MapKit) und Hinweis, was vorzubereiten ist. Das Kapitel erscheint immer, bei Leere mit "Keine Präsenztermine übermorgen".
  5. Optional: kurze Vorschau auf morgen.
- Darstellung: in der App (Hauptansicht), als Benachrichtigung zur eingestellten Uhrzeit (Kurzfassung mit Anzahl der Termine und dem ersten Termin) und als Widget.
- Ehrliche Einschränkung, die du im Plan berücksichtigen musst: iOS erlaubt keinen garantierten Hintergrundlauf zu einer festen Uhrzeit. Lokale Benachrichtigungen haben statischen Inhalt. Entwirf deshalb: Die Morgen-Benachrichtigung wird am Vorabend und bei jedem Sync vorberechnet. Beim Antippen wird das Briefing frisch geladen. Beschreibe, wie aktuell der Inhalt im ungünstigsten Fall ist.

## 5. Erinnerungen

Regeln (alle Zeiten einstellbar, das sind die Standardwerte):

- **Online-Termin**: Erinnerung **15 Minuten vorher** als lokale Benachrichtigung mit Join-Button (öffnet den Link direkt).
- **Präsenztermin**: Erinnerung **1 Tag vorher** als Eintrag in der **System-Erinnerungen-App** (EventKit, `EKReminder` mit Fälligkeitsdatum und Alarm). Zusätzlich ist eine Benachrichtigung am Termintag selbst einstellbar, inklusive "Jetzt losfahren"-Hinweis anhand der Fahrzeit (MapKit).
- Pro Terminart ist wählbar: nur Benachrichtigung / nur System-Erinnerung / beides.

Technische Anforderungen:
- Eigene Erinnerungsliste in der Erinnerungen-App anlegen (z. B. "Kalender-Assistent"), damit nichts mit meinen übrigen Erinnerungen vermischt wird.
- Idempotenz: Speichere pro Termin die Zuordnung `Termin-ID → Erinnerungs-ID / Benachrichtigungs-ID`. Bei Änderung der Zeit wird die Erinnerung verschoben, bei Absage oder Löschung entfernt. Es dürfen keine Duplikate entstehen.
- Berücksichtige das iOS-Limit von 64 ausstehenden lokalen Benachrichtigungen: Plane nur die nächsten N Tage, priorisiere nach Startzeit und fülle bei jedem Sync nach.
- Berechtigungen (Kalender, Erinnerungen, Benachrichtigungen, Standort falls Fahrzeit) werden erst bei Bedarf angefragt, mit verständlicher Erklärung. Die App muss auch bei verweigerter Berechtigung sinnvoll degradieren.
- **Ruhezeiten** (vollständig in den App-Einstellungen konfigurierbar):
  - Ein oder mehrere Zeitfenster (Standard 22:00 bis 07:00), optional pro Wochentag unterschiedlich, komplett abschaltbar.
  - Pro Erinnerungsart (Online-Erinnerung, Präsenz-Vortag, Termintag-Hinweis, Morgenbriefing) getrennt wählbar, wie bei einer Erinnerung in der Ruhezeit verfahren wird:
    1. auf den Vorabend vorziehen, also auf den letzten Zeitpunkt vor Beginn der Ruhezeit (Standard für den Präsenz-Vortag),
    2. trotzdem zum geplanten Zeitpunkt senden (Standard für Online-Erinnerungen),
    3. auf das Ende der Ruhezeit verschieben, sofern der Termin dann noch nicht begonnen hat, sonst Verhalten 2,
    4. verwerfen.
  - Optional: Ausnahme für als "wichtig" markierte Termine (immer zum geplanten Zeitpunkt, als zeitkritische Benachrichtigung).
  - Die Einstellungsseite zeigt eine Vorschau, wie sich die aktuelle Konfiguration auf die nächsten anstehenden Erinnerungen auswirkt.
  - Die Ruhezeiten-Logik gehört in das testbare `Core`-Package (Unit-Tests für Fenster über Mitternacht, Sommerzeitwechsel und Termine knapp nach Ende der Ruhezeit). Zusätzlich gilt: iOS-Fokus-Modi bleiben unabhängig davon aktiv und werden nicht umgangen.

## 6. Aktualität und Synchronisation

- Sync beim App-Start, bei Rückkehr in den Vordergrund, per `BGAppRefreshTask` und `BGProcessingTask`. Sage offen, wie unzuverlässig der Hintergrundlauf ist.
- Zeige in der App "Zuletzt synchronisiert um …" und einen Warnhinweis, wenn der Stand zu alt ist, damit ich weiß, wann Erinnerungen veraltet sein könnten.
- Entwirf **optional** (Phase 3, nur nach meiner Freigabe) ein kleines Backend, das Graph-Änderungsbenachrichtigungen (Webhooks) per APNs-Silent-Push an das Gerät weitergibt. Nenne Aufwand, laufende Kosten und Datenschutzfolgen, bevor du es baust. Ohne dieses Backend gilt: Änderungen werden erst beim nächsten Sync bemerkt.

## 7. Weitere nützliche Funktionen (nach Priorität, nicht alles auf einmal)

Phase 1 gehört das Kernfeature. Die folgenden Punkte schlage ich vor, entscheide mit mir, was in welche Phase kommt:

- Fahrzeit und "Losfahren"-Hinweis bei Präsenzterminen (MapKit), optional Wetter am Zielort.
- Konflikt- und Doppelbuchungserkennung über alle Kalender.
- Vorbereitungsnotizen pro Termin (Teilnehmerliste, Agenda, Anhänge-Hinweis) im Briefing.
- Snooze ("in 5 Minuten erneut") und "Erledigt" direkt in der Benachrichtigung.
- Filter und Regeln: bestimmte Kalender, Kategorien oder Titel-Schlagwörter stumm schalten oder anders behandeln. Private Termine optional ausblenden oder anonymisieren.
- Wochenvorschau am Sonntagabend.
- Widget (Heute) und Sperrbildschirm-Widget, Live Activity für den nächsten Termin, später Apple Watch.
- Fokus-Modi von iOS respektieren (zeitkritische Benachrichtigung nur für Termine, die ich als wichtig markiere).
- Export des Tagesplans (Text/PDF) zum Teilen.
- Lokalisierung: Deutsch zuerst, Texte in String-Catalogs, damit Englisch später leicht ergänzt werden kann.

## 8. Phasenplan

1. **Phase 1, Kern (iPhone)**: Kalenderanbindung (Empfehlung aus Abschnitt 2), Klassifikation, Cache, Morgenbriefing in der App, Erinnerungen laut Abschnitt 5, Einstellungen, Tests.
2. **Phase 2, Komfort (iPhone)**: Benachrichtigungs-Aktionen, Widget, Fahrzeit, Konflikterkennung, Wochenvorschau.
3. **Phase 3, optional**: Backend für Push-Aktualisierung, Apple Watch.
4. **Phase 4, Mac**: macOS-App auf Basis des gemeinsamen `Core`-Packages. Menüleisten-App mit Tagesübersicht, native Benachrichtigungen, EventKit-Erinnerungen, Briefing-Fenster. Sync-Stand zwischen iPhone und Mac über iCloud (CloudKit) oder unabhängig pro Gerät. Entscheide und begründe, und achte darauf, dass keine doppelten Erinnerungen entstehen, wenn beide Geräte laufen.

## 9. Rückfragen, die du zuerst stellen musst

1. iPhone oder Android? (Bestimmt die komplette Architektur.)
2. Sind die Outlook-Konten privat (outlook.com/Microsoft 365 Privat) oder Firmenkonten? Darf ich in der Firma eigene Apps in Azure/Entra registrieren, und gibt es Einschränkungen durch den Administrator?
3. Wie viele Konten/Kalender, und sind geteilte Kalender dabei?
4. Wird die App nur privat genutzt oder soll sie im App Store erscheinen? (Bestimmt Signierung, Datenschutzerklärung, Review-Anforderungen.)
5. Welche Apple-Entwickleraccount-Situation gilt? (Ohne bezahlten Account verfällt eine selbst signierte App nach 7 Tagen, und Push/iCloud sind eingeschränkt.)
6. Gibt es Uhrzeiten, Wochentage oder Kalender, für die andere Regeln gelten?

## 10. Qualitätsanforderungen

- Klare Schichten: Datenquelle → Normalisierung → Klassifikation → Regelengine → Scheduling. Jede Schicht ist einzeln testbar.
- Unit-Tests für Klassifikation, Serientermine, Zeitzonen/Sommerzeit, Duplikaterkennung, Scheduling-Planung (inklusive 64er-Limit) und Briefing-Erzeugung. Nutze eine injizierbare Uhr (`Clock`-Protokoll), nie `Date()` direkt.
- Keine Secrets im Repository. Konfiguration (Client-ID, Redirect-URI) in einer Beispieldatei dokumentieren.
- README mit Einrichtung (Azure-App-Registrierung, Berechtigungen), Build-Anleitung und bekannten Einschränkungen.
- Fehlerzustände sind sichtbar und verständlich (Anmeldung abgelaufen, keine Berechtigung, kein Netz). Fehler dürfen nie dazu führen, dass Erinnerungen stillschweigend ausbleiben.

## 11. Akzeptanzkriterien Phase 1

- Zwei verschiedene Outlook-Konten sind verbunden, und das Briefing zeigt Termine aus beiden korrekt sortiert.
- Ein Online-Termin um 10:00 erzeugt um 09:45 eine Benachrichtigung mit funktionierendem Join-Button.
- Ein Präsenztermin übermorgen erscheint im Kapitel "Präsenztermine in 2 Tagen", und am Vortag steht ein Eintrag in der Erinnerungen-App.
- Verschiebe ich den Termin in Outlook, wird nach dem nächsten Sync die Erinnerung angepasst, ohne Duplikat. Sage ich den Termin ab, verschwindet sie.
- Dieselbe Besprechung in zwei Kalendern erzeugt genau eine Erinnerung.
- Bei verweigerter Erinnerungen-Berechtigung greift ein sichtbarer Fallback auf Benachrichtigungen.
- In den Einstellungen lassen sich Ruhezeiten (Zeitfenster, Wochentage, Verhalten pro Erinnerungsart) ändern, und die Änderung wirkt nach dem Speichern auf alle bereits geplanten Erinnerungen.
