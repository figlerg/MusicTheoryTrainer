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
- Code-Kommentare und Commit-Nachrichten auf Englisch.
- Das Repo ist öffentlich: keine persönlichen Angaben (Arbeitgeber, Rechnernamen, private
  Mailadressen). Commits laufen über `40119252+figlerg@users.noreply.github.com`.
- Vor Abweichungen vom Besprochenen oder größeren Zusätzen nachfragen und eine Empfehlung
  mitgeben.

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

Signier-Secrets im Repo: `KEYSTORE_BASE64`, `STOREPASSWORD`, `KEYALIAS`, `KEYPASSWORD`. Ohne sie
signiert die CI mit dem Debug-Schlüssel und veröffentlicht nichts.
