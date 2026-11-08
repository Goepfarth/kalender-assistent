# Microsoft Entra: App registrieren und Anmeldung testen

> **Nur nötig für den Graph-Weg.** Zuerst kommt der Kontentest auf dem iPhone (PLAN.md, Phase 0a). Lassen sich beide Konten in iOS einrichten, liest die App sie über EventKit, und diese Anleitung brauchst du nicht.

Damit die App deine Kalender direkt über Microsoft Graph lesen darf, braucht sie eine App-Registrierung bei Microsoft. Daraus bekommt sie eine Client-ID. Dieser Teil läuft komplett im Browser und geht auch am Windows-PC. Dauer: etwa 20 Minuten.

Das Wichtigste daran ist Schritt 3. Er zeigt, ob dein Arbeitgeber die Anmeldung mit einer eigenen App zulässt, bevor wir Code schreiben.

## Schritt 1: Eigenen Entra-Tenant anlegen

Mit einem privaten Microsoft-Konto allein lässt sich keine App registrieren. Private Konten landen im Verzeichnis „Microsoft Services", in dem man nichts anlegen kann. Du brauchst ein eigenes Verzeichnis (Tenant).

**Empfohlen: kostenloses Azure-Konto**
1. Öffne <https://azure.microsoft.com/free> und melde dich mit deinem **privaten** Microsoft-Konto an.
2. Folge der Registrierung. Microsoft verlangt meist Telefonnummer und Kreditkarte zur Identitätsprüfung. Entra ID Free und App-Registrierungen kosten nichts. Ein kostenpflichtiges Upgrade brauchst du nicht, und die App-Registrierung funktioniert auch nach dem Ende des Testzeitraums weiter.
3. Dabei entsteht automatisch ein Verzeichnis (meist „Standardverzeichnis" bzw. „Default Directory").

**Alternative: im Tenant deines Arbeitgebers registrieren.** Das erhöht die Chance, dass die Anmeldung mit dem Arbeitskonto klappt. Mach das aber nur mit ausdrücklicher Erlaubnis deiner IT, denn es ist ein privates Projekt in einer Firmenumgebung. Wechselst du den Job, ist die Registrierung weg.

## Schritt 2: App registrieren

1. Öffne <https://entra.microsoft.com> und melde dich mit dem Konto aus Schritt 1 an.
2. Wähle links **App-Registrierungen** (je nach Version unter „Identität → Anwendungen" oder „Entra ID") und dann **Neue Registrierung**.
3. Fülle aus:
   - **Name:** `Kalender-Assistent`
   - **Unterstützte Kontotypen:** „Konten in einem beliebigen Organisationsverzeichnis (beliebiger Microsoft Entra ID-Mandant – mehrinstanzenfähig) und persönliche Microsoft-Konten" (englisch: „Accounts in any organizational directory … and personal Microsoft accounts")
   - **Umleitungs-URI:** Plattform „Öffentlicher Client/nativ (mobil und Desktop)" (englisch: „Public client/native (mobile & desktop)"), Adresse:
     `https://login.microsoftonline.com/common/oauth2/nativeclient`
4. Klicke **Registrieren**.
5. Auf der Übersichtsseite steht die **Anwendungs-ID (Client)**, eine lange Kennung wie `1a2b3c4d-…`. Notiere sie. Sie ist kein Geheimnis, kommt aber trotzdem nicht ins Repository, sondern später in `Config.xcconfig`.
6. Öffne **API-Berechtigungen** → **Berechtigung hinzufügen** → **Microsoft Graph** → **Delegierte Berechtigungen**. Suche `Calendars.Read`, setze den Haken und klicke **Berechtigungen hinzufügen**.
7. Lege **kein** Clientgeheimnis und kein Zertifikat an. Die App braucht beides nicht.

## Schritt 3: Anmeldung testen

Ersetze in dieser Adresse `DEINE-CLIENT-ID` durch die Anwendungs-ID aus Schritt 2:

```
https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=DEINE-CLIENT-ID&response_type=code&redirect_uri=https%3A%2F%2Flogin.microsoftonline.com%2Fcommon%2Foauth2%2Fnativeclient&scope=offline_access%20Calendars.Read&prompt=select_account
```

**Test A: Arbeitskonto** (der entscheidende Test)
1. Öffne ein privates Browserfenster (Edge: Strg+Umschalt+N) und füge die Adresse ein.
2. Melde dich mit deinem **Arbeitskonto** an.
3. Schau, was erscheint:

| Was du siehst | Bedeutung |
|---|---|
| Ein Fenster „Angeforderte Berechtigungen" mit dem Lesezugriff auf deine Kalender. Nach „Akzeptieren" folgt eine leere weiße Seite, deren Adresse `code=` enthält. | ✅ Das Arbeitskonto funktioniert. |
| „Genehmigung erforderlich", „Administratorgenehmigung erforderlich" oder „Need admin approval" | ❌ Der Arbeitgeber blockiert die App. Wir besprechen den Fallback (PLAN.md, Abschnitt 2). |
| Eine andere Fehlermeldung mit einem Code wie `AADSTS…` | ❓ Notiere den Code und die Meldung. |

**Test B: privates Konto**
Wiederhole das in einem neuen privaten Fenster mit deinem **privaten** Konto. Microsoft zeigt dabei eventuell den Hinweis „nicht überprüft" (unverified), weil die App keinen verifizierten Herausgeber hat. Das ist bei einer eigenen App normal.

Was hinter `code=` steht, brauchst du nicht und solltest du nicht weitergeben. Schick mir nur, welches Fenster bei Test A und B erschienen ist.

## Später auf dem Mac (Phase 0b)

Sobald die Bundle-ID feststeht, kommt in der App-Registrierung unter **Authentifizierung** → **Plattform hinzufügen** → **iOS/macOS** die Bundle-ID dazu. Entra erzeugt daraus die Redirect-URI `msauth.<bundle-id>://auth`, die MSAL braucht.
