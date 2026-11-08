# Arbeitsanweisungen für Coding-Agenten

Lies vor jeder Arbeit:
1. [docs/PLAN.md](docs/PLAN.md): freigegebener Plan. Bei Widersprüchen zum Auftrag gilt der Plan.
2. [docs/AUFTRAG.md](docs/AUFTRAG.md): ursprünglicher Auftrag mit den Qualitätsanforderungen (Abschnitt 10)
3. [README.md](README.md): aktueller Status der Phasen

## Regeln

- Arbeite phasenweise nach PLAN.md, Abschnitt 6. Beginne nur eine neue Phase, wenn die vorherige abgeschlossen und freigegeben ist.
- Baue keine Funktionen, die nicht im Plan stehen, ohne sie vorher vorzuschlagen und eine Freigabe abzuwarten.
- Benenne technische Einschränkungen offen und schlage Alternativen vor.
- Was sinnvoll individuell einstellbar ist, wird einstellbar gebaut, mit gutem Standardwert. Neue Einstellungen kommen in die Tabelle in PLAN.md, Abschnitt 5. Häufige Einstellungen stehen oben, Schwellwerte unter „Erweitert".
- `Core` nutzt nur Foundation. Zeit kommt über das `Clock`-Protokoll, nie über `Date()`.
- Jede Logik in `Core` hat Unit-Tests (Swift Testing). Führe nach jeder Änderung `swift test` im `Core`-Package aus, für App-Targets `xcodebuild test`.
- Keine Secrets im Repository. Bundle-ID und gegebenenfalls Client-ID stehen nur in `Config.xcconfig`, das in `.gitignore` steht. Die Vorlage ist `Config.example.xcconfig`.
- Keine Kalenderdaten an Dritte, kein Analytics-SDK. Mit EventKit baut die App keine eigenen Netzwerkverbindungen auf. Mit Graph sind nur die Microsoft-Anmeldung und Microsoft Graph erlaubt.
- UI-Texte auf Deutsch, in String-Catalogs.
- Nach jedem abgeschlossenen Schritt: Tests grün, Status im README aktualisieren, kurze Anleitung „So probierst du es aus" schreiben.
