# Arbeitsanweisungen für Coding-Agenten

Lies vor jeder Arbeit:
1. [docs/PLAN.md](docs/PLAN.md): freigegebener Plan. Bei Widersprüchen zum Auftrag gilt der Plan.
2. [docs/AUFTRAG.md](docs/AUFTRAG.md): ursprünglicher Auftrag mit den Qualitätsanforderungen (Abschnitt 10)
3. [README.md](README.md): aktueller Status der Phasen und Build-Befehle

## Regeln

- Arbeite phasenweise nach PLAN.md, Abschnitt 6. Beginne nur eine neue Phase, wenn die vorherige abgeschlossen und freigegeben ist.
- Baue keine Funktionen, die nicht im Plan stehen, ohne sie vorher vorzuschlagen und eine Freigabe abzuwarten.
- Benenne technische Einschränkungen offen und schlage Alternativen vor.
- Was sinnvoll individuell einstellbar ist, wird einstellbar gebaut, mit gutem Standardwert. Neue Einstellungen kommen in die Tabelle in PLAN.md, Abschnitt 5. Häufige Einstellungen stehen oben, Schwellwerte unter „Erweitert".
- Die App liest Kalender nur. Nur die Kalender-Adapter greifen auf den Kalender zu, ausschließlich lesend: auf Android nur `query` auf `CalendarContract`, auf iOS keine Aufrufe von `save`, `remove` oder `commit` auf `EKEventStore`. Ein Test prüft das im Quellcode.
- Die App baut keine Netzwerkverbindungen auf. Die Android-App bekommt keine `INTERNET`-Berechtigung. Kein Analytics-SDK.
- Alle Logik gehört in `core` (Kotlin Multiplatform, `commonMain`): nur Kotlin-Standardbibliothek und Multiplatform-Bibliotheken, kein Android- oder iOS-Code. Die Plattform-Apps bleiben dünn.
- Zeit kommt über das `Clock`-Interface, nie direkt von der Systemuhr.
- Jede Logik in `core` hat Unit-Tests (`kotlin.test`). Nach jeder Änderung: `gradlew :core:jvmTest`. Für Android zusätzlich `gradlew :androidApp:assembleDebug`, auf dem Mac für iOS `xcodebuild test`.
- Keine Secrets im Repository. `local.properties`, `Config.xcconfig` und Signierschlüssel stehen in `.gitignore`.
- UI-Texte auf Deutsch: Android in `strings.xml`, iOS in String-Catalogs.
- Nach jedem abgeschlossenen Schritt: Tests grün, Status im README aktualisieren, kurze Anleitung „So probierst du es aus" schreiben.
