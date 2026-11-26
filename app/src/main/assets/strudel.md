# Grundlagen

Strudel ist eine Sprache, mit der man Musik als Muster schreibt. Jedes Beispiel öffnet sich mit „In Strudel öffnen“ auf strudel.cc. Dort auf ▶ (play) tippen, ändern, nochmal ▶. Für die Klänge braucht strudel.cc Internet.

## Der erste Schlag

Ein Bassdrum-Schlag. Strudel denkt in Zyklen: Ein Zyklus ist hier wie ein Takt, und das Muster füllt ihn aus.

```strudel
s("bd")
```

## Ein Muster

Vier Klänge teilen den Zyklus in vier gleiche Teile: Bassdrum, Snare, Bassdrum, Snare.

```strudel
s("bd sd bd sd")
```

## Wiederholen, Pause, gleichzeitig

`*4` spielt etwas viermal hintereinander, `~` ist eine Pause, und ein Komma lässt mehrere Muster gleichzeitig laufen.

```strudel
s("bd*4, ~ sd ~ sd, hh*8")
```

## Drumcomputer

`.bank` wählt einen klassischen Drumcomputer. Probier auch "RolandTR808" statt "RolandTR909".

```strudel
s("bd*4, ~ cp ~ cp, hh*8").bank("RolandTR909")
```

## Tempo und Spuren

`setcpm` setzt die Zyklen pro Minute. Bei vier Schlägen pro Zyklus ist das das Tempo in BPM geteilt durch 4. Jede Zeile mit `$:` ist eine eigene Spur.

```strudel
setcpm(120/4)
$: s("bd*4").bank("RolandTR909")
$: s("~ cp ~ cp").bank("RolandTR909")
$: s("hh*8").bank("RolandTR909").gain(0.5)
```

## Töne und Notennamen

Strudel benutzt englische Notennamen: `b` ist das deutsche H, `bb` das deutsche B, `#` ein Kreuz (`c#` = Cis). Die Zahl ist die Oktave, `c4` ist das eingestrichene c'.

```strudel
note("c3 e3 g3 b3 c4 b3 g3 e3").s("piano")
```

## Tonleitern

Mit `.scale` schreibst du Stufen statt Namen: 0 ist der Grundton, 7 die Oktave. Genau die Tonleitern aus der Tonleiter-Übung, hier C-Dur.

```strudel
n("0 1 2 3 4 5 6 7").scale("C4:major").s("piano")
```

## Akkorde

Töne mit Komma in eckigen Klammern klingen zusammen, `< >` nimmt pro Zyklus das nächste Element. Hier C-Dur, a-Moll, F-Dur, G-Dur, wie in der Akkord-Übung.

```strudel
note("<[c3,e3,g3] [a2,c3,e3] [f2,a2,c3] [g2,b2,d3]>").s("piano")
```

## Deine Instrumente

Alle meine Entchen auf der Blockflöte. Tausch den Klang gegen "gm_piano" oder "gm_acoustic_guitar_nylon". `@2` macht eine Note doppelt so lang.

```strudel
note("<[c4 d4 e4 f4] [g4@2 g4@2] [a4 a4 a4 a4] g4>").s("gm_recorder")
```

## Klang formen

`.lpf` ist ein Tiefpassfilter (kleinere Zahl = dumpfer), `.room` gibt Hall dazu, `.gain` regelt die Lautstärke.

```strudel
note("c2 c2 eb2 g2").s("sawtooth").lpf(600).room(0.3).gain(0.8)
```

# Techno

Techno läuft meist um 125 bis 135 BPM. Typisch: eine gerade Bassdrum auf jeder Viertel („four on the floor“), Hihats zwischen den Schlägen, viel Wiederholung, und Veränderung kommt eher über Filter als über neue Töne. Die Beispiele bauen Schritt für Schritt einen Track auf.

## Four on the floor

Die Bassdrum auf jeder Viertel ist das Fundament.

```strudel
setcpm(130/4)
$: s("bd*4").bank("RolandTR909")
```

## Offbeat-Hihat

Die offene Hihat genau zwischen den Bassdrums gibt den typischen Schub.

```strudel
setcpm(130/4)
$: s("bd*4").bank("RolandTR909")
$: s("[~ oh]*4").bank("RolandTR909").gain(0.6)
```

## Clap und Sechzehntel

Clap auf 2 und 4, dazu leise geschlossene Hihats in Sechzehnteln.

```strudel
setcpm(130/4)
$: s("bd*4").bank("RolandTR909")
$: s("[~ oh]*4").bank("RolandTR909").gain(0.6)
$: s("~ cp ~ cp").bank("RolandTR909")
$: s("hh*16").bank("RolandTR909").gain(0.3)
```

## Bass zwischen den Schlägen

Ein kurzer Bass auf den Offbeats lässt der Bassdrum Platz. `.decay` und `.sustain(0)` machen ihn kurz.

```strudel
setcpm(130/4)
$: s("bd*4").bank("RolandTR909")
$: s("[~ oh]*4").bank("RolandTR909").gain(0.6)
$: s("~ cp ~ cp").bank("RolandTR909")
$: s("hh*16").bank("RolandTR909").gain(0.3)
$: note("[~ c2]*4").s("sawtooth").lpf(400).decay(0.15).sustain(0)
```

## Bewegung mit dem Filter

`sine.range(300, 2000).slow(8)` öffnet und schließt den Filter langsam über acht Takte. So verändert sich der Track, ohne dass sich eine Note ändert. Dazu kommt leise eine Fläche.

```strudel
setcpm(130/4)
$: s("bd*4").bank("RolandTR909")
$: s("[~ oh]*4").bank("RolandTR909").gain(0.6)
$: s("~ cp ~ cp").bank("RolandTR909")
$: s("hh*16").bank("RolandTR909").gain(0.3)
$: note("[~ c2]*4").s("sawtooth").lpf(sine.range(300, 2000).slow(8)).decay(0.15).sustain(0)
$: note("<c3 c3 eb3 g2>").s("supersaw").lpf(1200).gain(0.25).room(0.4)
```

# Drum and Bass

Drum and Bass läuft um 170 bis 175 BPM. Das Schlagzeug ist schnell, Bass und Flächen wirken oft halb so schnell. Das Grundmuster heißt Two-Step: Bassdrum auf der 1 und kurz vor der 3, Snare auf 2 und 4.

## Two-Step

Sechzehn Schritte pro Takt: Bassdrum auf Schritt 1 und 11, Snare auf 5 und 13.

```strudel
setcpm(174/4)
$: s("bd ~ ~ ~ sd ~ ~ ~ ~ ~ bd ~ sd ~ ~ ~").bank("RolandTR909")
```

## Hihats und eine Geister-Snare

Achtel-Hihats treiben. Eine leise Snare auf Schritt 8, eine Geisternote, macht den Groove rund.

```strudel
setcpm(174/4)
$: s("bd ~ ~ ~ sd ~ ~ ~ ~ ~ bd ~ sd ~ ~ ~").bank("RolandTR909")
$: s("hh*8").bank("RolandTR909").gain(0.5)
$: s("~ ~ ~ ~ ~ ~ ~ sd ~ ~ ~ ~ ~ ~ ~ ~").bank("RolandTR909").gain(0.25)
```

## Sub-Bass

Ein tiefer Sinuston, eine Note pro Takt. Man spürt ihn mehr, als man ihn hört; auf Handylautsprechern fehlt er fast ganz.

```strudel
setcpm(174/4)
$: s("bd ~ ~ ~ sd ~ ~ ~ ~ ~ bd ~ sd ~ ~ ~").bank("RolandTR909")
$: s("hh*8").bank("RolandTR909").gain(0.5)
$: note("<e1 e1 g1 d1>").s("sine").gain(0.9)
```

## Reese-Bass

Ein Bass aus mehreren leicht verstimmten Sägezähnen, gefiltert. `supersaw` kommt dem nahe.

```strudel
setcpm(174/4)
$: s("bd ~ ~ ~ sd ~ ~ ~ ~ ~ bd ~ sd ~ ~ ~").bank("RolandTR909")
$: s("hh*8").bank("RolandTR909").gain(0.5)
$: note("<e1 e1 g1 d1>").s("sine").gain(0.9)
$: note("<e2 e2 g2 d2>").s("supersaw").lpf(500).gain(0.4)
```

## Flächen

Lange Akkorde mit viel Hall darüber, hier e-Moll und C-Dur über je zwei Takte.

```strudel
setcpm(174/4)
$: s("bd ~ ~ ~ sd ~ ~ ~ ~ ~ bd ~ sd ~ ~ ~").bank("RolandTR909")
$: s("hh*8").bank("RolandTR909").gain(0.5)
$: note("<e1 e1 g1 d1>").s("sine").gain(0.9)
$: note("<[e3,g3,b3] [c3,e3,g3]>").slow(2).s("piano").room(0.7).gain(0.5)
```

# Industrial und Aggrotech

Aggrotech kommt aus dem Umfeld von Industrial und EBM. Typisch: eine harte, verzerrte Bassdrum im geraden Takt, ein treibender Sequenzer-Bass in Sechzehnteln, Moll-Tonarten und schneidende Synth-Leads, das Tempo liegt meist zwischen 120 und 140 BPM. Industrial bringt dazu metallische Geräusche und Verzerrung.

## Harte Bassdrum

`.distort` verzerrt: Aus der sauberen 909-Bassdrum wird ein harter Schlag.

```strudel
setcpm(135/4)
$: s("bd*4").bank("RolandTR909").distort(3)
```

## Snare mit Raum

Snare auf 2 und 4, leicht verzerrt und mit Hall.

```strudel
setcpm(135/4)
$: s("bd*4").bank("RolandTR909").distort(3)
$: s("~ sd ~ sd").bank("RolandTR909").distort(1).room(0.4)
```

## Sequenzer-Bass

Sechzehntel auf dem Grundton, mit Sprüngen in die Oktave und zu anderen Tönen aus c-Moll. Kurz und verzerrt.

```strudel
setcpm(135/4)
$: s("bd*4").bank("RolandTR909").distort(3)
$: s("~ sd ~ sd").bank("RolandTR909").distort(1).room(0.4)
$: note("c2 c2 c3 c2 c2 c2 eb2 c2 c2 c2 c3 c2 bb1 c2 g1 c2").s("sawtooth").lpf(900).decay(0.12).sustain(0).distort(1)
```

## Metall

Metallische Schläge, hochgefiltert. `.crush` reduziert die Auflösung, das klingt kaputt und kalt.

```strudel
setcpm(135/4)
$: s("bd*4").bank("RolandTR909").distort(3)
$: s("~ sd ~ sd").bank("RolandTR909").distort(1).room(0.4)
$: note("c2 c2 c3 c2 c2 c2 eb2 c2 c2 c2 c3 c2 bb1 c2 g1 c2").s("sawtooth").lpf(900).decay(0.12).sustain(0).distort(1)
$: s("[~ metal]*4").n("<0 3 5 7>").hpf(1500).crush(6).gain(0.5)
```

## Lead

Ein verzerrter `supersaw` spielt die Töne des c-Moll-Akkords mit kleiner Septime, einen pro Takt.

```strudel
setcpm(135/4)
$: s("bd*4").bank("RolandTR909").distort(3)
$: s("~ sd ~ sd").bank("RolandTR909").distort(1).room(0.4)
$: note("c2 c2 c3 c2 c2 c2 eb2 c2 c2 c2 c3 c2 bb1 c2 g1 c2").s("sawtooth").lpf(900).decay(0.12).sustain(0).distort(1)
$: s("[~ metal]*4").n("<0 3 5 7>").hpf(1500).crush(6).gain(0.5)
$: note("<c4 eb4 g4 bb4>").s("supersaw").lpf(2500).distort(2).gain(0.3).room(0.3)
```
