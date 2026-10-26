# Musiktheorie-Trainer

Kleine Android-App zum Üben von Musiktheorie auf der Gitarre. Offline, ohne Accounts, ohne Werbung.

- **Griffbrett-Töne:** Ton zu einem Punkt nennen oder alle Stellen eines Tons antippen (Bund 0–12, tiefe E- und A-Saite oder alle 6).
- **Intervalle:** „Fis + Halbton = ?“, „D − Ganzton = ?“ in drei Schwierigkeitsstufen.
- **Tonleitern:** Dur und natürliches Moll, nach Quintenzirkel oder zufällig. Der Grundton ist vorgegeben, du gibst die 7 folgenden Töne ein (Stufe 2 bis Oktave). Jede Eingabe prüft genau einen Schritt (Ganz/Halb). Danach siehst du die Tonleiter auf einer Saite als Griffbrett und Tab.
- **Akkorde:** Dur- und Moll-Dreiklänge. Entweder die drei Töne eingeben (auf der Gitarre höchstens ein Ton pro Saite) oder einen spielbaren Griff bauen, der nach Regeln geprüft und mit dem Standardgriff verglichen wird.
- **Notenlesen:** Violinschlüssel in Gitarrennotation (klingt eine Oktave tiefer). Bekannte Volkslieder oder Zufallszeilen, die Noten der Reihe nach benennen.

Eingabe per Text, Klaviertasten oder Gitarrengriffbrett (Bund 0–5). Nach einer falschen Antwort kannst du es nochmal probieren oder dir die Lösung oder eine Erklärung zeigen lassen (Halbtonleiste mit Pfeilen). Für die Trefferquote zählt der erste Versuch. Unübliche Schreibweisen wie Fes, His oder Hes zählen als Treffer, werden aber gelb markiert und mit dem üblichen Namen ergänzt.

Deutsche Notation (H, B = Bb) ist Standard, englische lässt sich in den Einstellungen wählen. Enharmonische Verwechslungen gelten als richtig.

## Aufbau

| Modul | Inhalt |
|---|---|
| `theory/` | Reine Kotlin-Musiklogik ohne Android: Tonnamen, Parser, Intervalle, Tonleitern (Dur/Moll, Quintenzirkel), Akkorde und Griffe, Notensystem und Melodien, Stimmung, Tab. Mit Unit-Tests. |
| `app/` | Jetpack Compose UI (eine Activity), DataStore für Einstellungen und Trefferquoten, AudioTrack-Synth. |

Abhängigkeiten der App: Compose (Foundation, Material 3), `activity-compose`, `datastore-preferences`. Keine Netzwerk-Berechtigung.

## Voraussetzungen

- **JDK 17 oder neuer** (getestet mit 21). `JAVA_HOME` muss darauf zeigen.
- **Android SDK** mit akzeptierter Lizenz. Compile-Platform und Build-Tools lädt Gradle beim ersten Build selbst.
- SDK-Pfad entweder in `local.properties` (`sdk.dir=...`, wird nicht eingecheckt) oder in der Umgebungsvariable `ANDROID_HOME`.

Gradle selbst musst du nicht installieren, der Wrapper (`gradlew` / `gradlew.bat`) lädt die passende Version.

### Windows 11

Einmalig (falls noch nicht vorhanden):

1. JDK 21 als ZIP laden, z. B. [Microsoft Build of OpenJDK](https://learn.microsoft.com/java/openjdk/download), und nach `%USERPROFILE%\.jdks\` entpacken.
2. [Android Command-line Tools](https://developer.android.com/studio#command-line-tools-only) laden und so entpacken, dass `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat` existiert.
3. In PowerShell:
   ```powershell
   $env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-21.0.12.1+1"   # an deine Version anpassen
   & "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools"
   ```
4. `local.properties` im Projektordner:
   ```properties
   sdk.dir=C\:\\Users\\DEINNAME\\AppData\\Local\\Android\\Sdk
   ```

Bauen:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-21.0.12.1+1"
.\gradlew.bat :theory:test          # Unit-Tests der Musiklogik
.\gradlew.bat :app:assembleRelease  # APK bauen
```

### macOS (Apple Silicon)

Am einfachsten mit Homebrew:

```bash
brew install --cask temurin@21
brew install --cask android-commandlinetools
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools   # Pfad mit "brew info android-commandlinetools" prüfen
sdkmanager "platform-tools"
```

Alternativ Android Studio installieren: Es bringt JDK und SDK mit (SDK liegt dann unter `~/Library/Android/sdk`).

Bauen:

```bash
./gradlew :theory:test
./gradlew :app:assembleRelease
```

### WSL

Funktioniert, aber das Repo sollte im Linux-Dateisystem liegen (`~/...`), denn Builds über `/mnt/c` sind sehr langsam. JDK per `sudo apt install openjdk-21-jdk`, Command-line Tools für Linux nach `~/Android/Sdk/cmdline-tools/latest` entpacken, `ANDROID_HOME=~/Android/Sdk`. USB-Zugriff aus WSL braucht `usbipd-win`. Einfacher: Die fertige APK mit dem Windows-adb installieren.

## APK aufs Handy bringen (Sideloading)

Ergebnis des Builds: `app/build/outputs/apk/release/app-release.apk` (minifiziert, einige MB).

Ohne eigenen Schlüssel wird die Release-APK mit dem Debug-Schlüssel deines Rechners signiert. Für private Nutzung reicht das.

### Vorbereitung auf dem Galaxy S25 Ultra

- **Automatische Sperre (Auto Blocker) ausschalten:** *Einstellungen → Sicherheit und Datenschutz → Automatische Sperre*. Sonst blockiert One UI sowohl USB-Befehle als auch Installationen aus unbekannten Quellen. Nach der Installation kannst du sie wieder einschalten.

### Variante A: per USB mit adb

1. Entwickleroptionen freischalten: *Einstellungen → Telefoninfo → Softwareinformationen →* 7× auf **Buildnummer** tippen.
2. *Einstellungen → Entwickleroptionen → USB-Debugging* einschalten.
3. Handy per USB-C anschließen und auf dem Handy „USB-Debugging zulassen?“ bestätigen.
4. Prüfen und installieren:
   ```powershell
   # Windows
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\release\app-release.apk
   ```
   ```bash
   # macOS
   $ANDROID_HOME/platform-tools/adb devices
   $ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
   ```
   Taucht unter Windows bei `adb devices` nichts auf, fehlt meist der [Samsung-USB-Treiber](https://developer.samsung.com/android-usb-driver). Steht dort `unauthorized`, hast du die Abfrage auf dem Handy noch nicht bestätigt.

### Variante B: als Datei

1. `app-release.apk` aufs Handy kopieren (USB-Dateiübertragung, Quick Share, Cloud, Mail an dich selbst).
2. In **Eigene Dateien** antippen. Beim ersten Mal fragt Android, ob die App „Eigene Dateien“ unbekannte Apps installieren darf. Das erlauben.
3. Installieren. Warnt Google Play Protect vor einer unbekannten App: *Weitere Details → Trotzdem installieren*.

### Updates und Signatur

Ein Update installiert sich nur über die bestehende App, wenn beide APKs mit demselben Schlüssel signiert sind. Der Debug-Schlüssel (`~/.android/debug.keystore`) ist aber auf jedem Rechner ein anderer. Wenn du mal auf Windows und mal auf dem Mac baust, schlägt das Update fehl („App nicht installiert“ bzw. `INSTALL_FAILED_UPDATE_INCOMPATIBLE`), und Deinstallieren löscht die Trefferquoten.

Abhilfe: einmal einen eigenen Schlüssel erzeugen und auf beide Rechner kopieren.

```bash
keytool -genkeypair -v -keystore release.jks -alias mtt -keyalg RSA -keysize 2048 -validity 10000
```

Dazu `keystore.properties` im Projektordner anlegen (beide Dateien sind per `.gitignore` ausgeschlossen):

```properties
storeFile=release.jks
storePassword=...
keyAlias=mtt
keyPassword=...
```

Existiert die Datei, signiert `assembleRelease` automatisch damit. Alternativ liest der Build die
gleichen Werte aus den Umgebungsvariablen `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` und
`KEY_PASSWORD` — so bekommt die CI den Schlüssel, ohne dass eine Datei im Repo liegt.

## Bauen ohne PC (GitHub Actions)

Zwei Workflows unter `.github/workflows/`:

| Workflow | Läuft | Ergebnis |
|---|---|---|
| `build.yml` | bei jedem Push auf jeden Branch und per Hand | Unit-Tests (`:theory:test`, `:app:testDebugUnitTest`) und eine installierbare Release-APK als Artefakt `apk-r<Nummer>`. Ist `versionName` neu, zusätzlich Tag und GitHub-Release mit der APK. |
| `screenshots.yml` | per Hand, und wenn sich das Skript oder der Workflow ändert | Emulator, APK installieren, jeden Bildschirm fotografieren, Artefakt `screenshots-r<Nummer>` |

### APK vom Handy holen

Fertige Versionen: auf der Repo-Seite **Releases** → oberster Eintrag → die `.apk` antippen und
installieren (siehe [Variante B](#variante-b-als-datei)).

Zwischenstände ohne Release:

1. Im Repo auf **Actions** → Workflow **Build** → den obersten Lauf öffnen.
2. Unten unter **Artifacts** `apk-r<Nummer>` antippen. Es lädt eine ZIP-Datei.
3. In **Eigene Dateien** die ZIP entpacken und die APK darin installieren.

Artefakte werden nach 90 Tagen gelöscht, Releases bleiben. Der Dateiname enthält Version,
Laufnummer und Commit, z. B. `musiktheorie-0.4.1-r12-a1b2c3d.apk`.

### Release anlegen

Es reicht, in `app/build.gradle.kts` `versionCode` um 1 zu erhöhen und `versionName` neu zu
setzen, z. B. `0.5.0`, und das auf irgendeinen Branch zu pushen. Gibt es den Tag `v0.5.0` noch
nicht, legt der Build nach Tests und Signieren Tag und Release mit `musiktheorie-0.5.0.apk` an.
Ohne neue Version wird nur gebaut. Eine Version mit Bindestrich (`0.5.0-beta1`) wird als
Vorabversion markiert.

Veröffentlicht wird nur, was mit dem eigenen Schlüssel signiert ist. Der Tag zeigt auf den
gebauten Commit, auch wenn der nicht auf `main` liegt. `main` holt man bei Gelegenheit per Pull
Request nach.

Scheitert der Release-Schritt mit 403: im Repo unter **Settings → Actions → General → Workflow
permissions** „Read and write permissions“ wählen.

### Signierschlüssel als Secret hinterlegen

Ohne Secret signiert die CI mit dem Debug-Schlüssel. Dann lässt sich die APK nicht über eine
vorhandene Installation legen, und Deinstallieren löscht die Statistik. Damit die CI-APKs echte
Updates deiner App sind, einmal (an dem Rechner, auf dem `release.jks` liegt):

```bash
base64 -w0 release.jks > release.jks.b64    # macOS: base64 -i release.jks -o release.jks.b64
```

Dann im Repo unter **Settings → Secrets and variables → Actions → New repository secret** vier
Secrets anlegen:

| Name | Inhalt |
|---|---|
| `KEYSTORE_BASE64` | der komplette Inhalt von `release.jks.b64` |
| `STOREPASSWORD` | Passwort des Keystores (`storePassword`) |
| `KEYALIAS` | Alias des Schlüssels (`keyAlias`), z. B. `mtt` |
| `KEYPASSWORD` | Passwort des Schlüssels (`keyPassword`) |

`release.jks.b64` danach löschen. Der Build-Log gibt den SHA-256-Fingerprint des Zertifikats aus:
stimmt der mit dem deiner installierten App überein, installiert sich die APK als Update.

### Screenshots

**Actions** → **Screenshots** → **Run workflow**. Wählbar sind Farbschema (`dark`, `light`, `both`)
und API-Level des Emulators. Der Lauf dauert gut 15 bis 25 Minuten.

`tools/screenshots.py` steuert den Emulator über `adb`: es liest den Accessibility-Baum, tippt die
Beschriftungen an und speichert nach jedem Schritt ein PNG. Die Liste der Bildschirme kommt aus
`Exercise.kt`, neue Übungen landen also automatisch in den Screenshots. Das Skript läuft auch
lokal gegen ein angeschlossenes Handy:

```bash
python3 tools/screenshots.py --apk app/build/outputs/apk/release/app-release.apk --out screenshots
```

## Lizenz

MIT, siehe [LICENSE](LICENSE).
