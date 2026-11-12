// Evaluates every ```strudel block of the app's lessons with Strudel's own pattern engine, without
// sound: each block must run, produce notes, and only use sounds strudel.cc actually loads.
//
//   node --import ./register.mjs check.mjs ../../app/src/main/assets/strudel.md

import { readFile } from 'node:fs/promises';
import * as core from '@strudel/core';
import * as mini from '@strudel/mini';
import * as tonal from '@strudel/tonal';
import { transpiler } from '@strudel/transpiler';

const lessonsFile = process.argv[2];
if (!lessonsFile) {
  console.error('usage: check.mjs <lessons.md>');
  process.exit(2);
}

// The sample maps strudel.cc loads on start (prebake in @strudel/repl), fetched live so a renamed
// sound upstream shows up here.
const SAMPLE_MAPS = [
  'https://raw.githubusercontent.com/felixroos/dough-samples/main/tidal-drum-machines.json',
  'https://raw.githubusercontent.com/felixroos/dough-samples/main/piano.json',
  'https://raw.githubusercontent.com/felixroos/dough-samples/main/Dirt-Samples.json',
  'https://raw.githubusercontent.com/felixroos/dough-samples/main/vcsl.json',
  'https://raw.githubusercontent.com/felixroos/dough-samples/main/mridangam.json',
  'https://raw.githubusercontent.com/tidalcycles/uzu-drumkit/main/strudel.json',
];
const CYCLES = 8;

async function knownSounds() {
  const sounds = new Map();
  for (const url of SAMPLE_MAPS) {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`${url}: HTTP ${response.status}`);
    for (const [name, files] of Object.entries(await response.json())) {
      if (!name.startsWith('_')) sounds.set(name, Array.isArray(files) ? files.length : Object.keys(files).length);
    }
  }
  const fixed = JSON.parse(await readFile(new URL('./sounds.json', import.meta.url), 'utf8'));
  for (const name of [...fixed.synths, ...fixed.soundfonts]) sounds.set(name, Infinity);
  return sounds;
}

/** Code blocks with the lesson heading above them, for messages. */
function blocks(markdown) {
  const found = [];
  let heading = '';
  const lines = markdown.split('\n');
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith('#')) heading = lines[i].replace(/^#+\s*/, '');
    if (lines[i].trim() === '```strudel') {
      const code = [];
      for (i++; i < lines.length && lines[i].trim() !== '```'; i++) code.push(lines[i]);
      found.push({ heading, code: code.join('\n') });
    }
  }
  return found;
}

await core.evalScope(core, mini, tonal);
const sounds = await knownSounds();
const lessons = blocks(await readFile(lessonsFile, 'utf8'));
let failures = 0;

for (const { heading, code } of lessons) {
  let error;
  const { evaluate, scheduler } = core.repl({
    defaultOutput: async () => {},
    getTime: () => 0,
    transpiler,
    onEvalError: (e) => (error = e),
  });
  const problems = [];
  const pattern = await evaluate(code, false);
  if (error || !pattern) {
    problems.push(`läuft nicht: ${error?.message ?? 'kein Muster'}`);
  } else {
    const haps = pattern.queryArc(0, CYCLES).filter((hap) => hap.hasOnset());
    if (haps.length === 0) problems.push('erzeugt keine Noten');
    const missing = new Set();
    for (const { value } of haps) {
      const base = value.s ?? 'triangle';
      const name = value.bank ? `${value.bank}_${base}` : base;
      const count = sounds.get(name);
      if (count === undefined) missing.add(name);
      else if (value.n !== undefined && value.n >= count) missing.add(`${name}:${value.n} (nur ${count} Samples)`);
    }
    if (missing.size) problems.push(`unbekannte Klänge: ${[...missing].join(', ')}`);
    const bpm = Math.round(scheduler.cps * 60 * 4);
    if (!problems.length) console.log(`ok    ${heading} (${haps.length} Noten in ${CYCLES} Takten, ${bpm} BPM)`);
  }
  if (problems.length) {
    failures++;
    console.log(`FEHLER ${heading}: ${problems.join('; ')}`);
  }
}

console.log(`${lessons.length - failures} von ${lessons.length} Beispielen in Ordnung`);
process.exit(failures ? 1 : 0);
