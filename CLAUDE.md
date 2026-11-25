# Notizen für Claude

Android-App „Musiktheorie-Trainer“. `theory/` ist reine Kotlin-Musiklogik ohne Android, mit
Unit-Tests. `app/` ist die Compose-UI mit einer Activity. Der Nutzer arbeitet nur noch vom Handy
aus, ohne PC.

## Konventionen

- Texte in der App auf Deutsch. Keine Erklär- oder Tutorialtexte in den Übungen, die Theorie
  lernt der Nutzer woanders. Einzige Ausnahme sind die Grundwissen-Seiten (`KnowledgeScreen.kt`):
  nur gesichertes Standardwissen für Einsteiger, nichts Spekulatives.
- Die Flöte kennt nur die Griffe in `Recorder.kt`. Neue Griffe nur eintragen, wenn sie
  Standard sind; Flöten-Eingabe gibt es nur in Übungen, deren Fragen alle greifbar sind.
- Strudel-Kurs: Lektionen nur in `app/src/main/assets/strudel.md`, ein ```strudel-Block pro
  Lektion. `cd tools/strudel-check && npm ci && npm run check` prüft jedes Beispiel mit Strudels
  Musterlogik (läuft, erzeugt Noten, nur Klänge, die strudel.cc lädt); die CI tut das auch. Kein
  Strudel-Code in die App (AGPL): Beispiele öffnen auf strudel.cc.
- Code-Kommentare und Commit-Nachrichten auf Englisch.
- Das Repo ist öffentlich: keine persönlichen Angaben (Arbeitgeber, Rechnernamen, private
  Mailadressen). Commits laufen über `40119252+figlerg@users.noreply.github.com`.
- Vor Abweichungen vom Besprochenen oder größeren Zusätzen nachfragen und eine Empfehlung
  mitgeben.

## Integrationen mit den anderen Apps

Alles, was diese App mit anderen Apps des Nutzers teilt, steht in figlerg/design, Ordner
[`integrations/`](https://github.com/figlerg/design/tree/main/integrations) auf `main`. Das ist die
einzige Quelle.

- Vor jeder Arbeit, die App-Grenzen überschreitet (Daten einer anderen App lesen, ihr etwas
  schicken, etwas Neues anbieten): `integrations/` lesen und in `wishes.md` nach offenen Anfragen an
  diese App schauen.
- Einen Vertrag dort in derselben Änderung wie den Code ändern. Verträge nie in dieses Repo
  kopieren, nur verlinken.
- Anfragen an andere Apps kommen in `wishes.md`.
- Was diese App anbieten könnte:
  [`integrations/offers/music-theory.md`](https://github.com/figlerg/design/blob/main/integrations/offers/music-theory.md).

## Falle

Rückrufe wie `onTap` oder `onNote` dürfen keine Frage-Daten einfangen, sie müssen sie aus dem
State lesen. Ein eingefangener Wert überlebt den Fragenwechsel, und richtige Antworten werden als
falsch gewertet (siehe Commit 636fec9).

## Bauen und prüfen in der Cloud

- Die Cloud-Sitzung hat kein Android SDK. `./gradlew :theory:test` läuft mit dem JDK, alles unter
  `:app` nur in der CI.
- Nach jedem Push den Lauf von `build.yml` über die GitHub-Tools verfolgen (Läufe auflisten,
  Job-Logs lesen) und rote Läufe reparieren, bevor es weitergeht.
- Artefakte lassen sich aus der Sitzung nicht herunterladen, der Speicher-Host ist gesperrt.
  Screenshots also über das Log prüfen (Liste und Dateigrößen) und den Nutzer draufschauen lassen.
- `screenshots.yml` läuft, wenn sich `tools/screenshots.py` oder der Workflow ändert. Per Hand
  starten geht erst, wenn der Workflow auf `main` liegt.

## Release

1. In `app/build.gradle.kts` `versionCode` um 1 erhöhen und `versionName` neu setzen.
2. Auf den Arbeits-Branch pushen. Tags darf die Sitzung nicht pushen, das ist auch nicht nötig.
3. Die CI baut, testet und signiert. Ist `v<versionName>` neu, legt sie Tag und Release mit
   `musiktheorie-<versionName>.apk` an.
4. Mit den GitHub-Tools prüfen, ob der Release entstanden ist, und dem Nutzer den Link
   `https://github.com/figlerg/MusicTheoryTrainer/releases/tag/v<versionName>` schicken.

Signier-Secrets im Repo: `KEYSTORE_BASE64`, `STOREPASSWORD`, `KEYALIAS`, `KEYPASSWORD` (der eigene,
alte Schlüssel). Ohne sie signiert die CI mit dem Debug-Schlüssel und veröffentlicht nichts.

Alle Apps des Nutzers sollen mit einem gemeinsamen Schlüssel signiert sein (Kanäle mit
`signature`-Berechtigungen hängen daran). Sind zusätzlich `SIGNING_KEYSTORE_BASE64`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` gesetzt (dieselben Werte wie in
PushMe), baut Gradle unsigniert und `tools/sign-rotated.sh` signiert mit beiden Schlüsseln und einem
Rotationsnachweis alt zu neu. So bleibt jede Version ein Update der installierten App. Die alten
Secrets müssen dafür dauerhaft bleiben.
