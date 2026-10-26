#!/usr/bin/env python3
"""Install the app on a running emulator and screenshot every screen.

Navigation goes through the accessibility tree: dump it, look up a visible label and
tap its centre. That way the app needs no test dependency at all. The screen list is
read out of Exercise.kt, so it cannot drift away from the app.

Usage (an emulator or device must already be connected):
    python3 tools/screenshots.py --apk path/to/app-release.apk --out screenshots
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

PACKAGE = "at.gigler.musictheorytrainer"
ACTIVITY = f"{PACKAGE}/.MainActivity"
EXERCISE_KT = Path("app/src/main/kotlin/at/gigler/musictheorytrainer/data/Exercise.kt")
SETTINGS_LABEL = "Einstellungen und Statistik"
STATS_LABEL = "Statistik und Übungszeit"


def adb(*args: str) -> str:
    done = subprocess.run(["adb", *args], capture_output=True, check=True)
    return done.stdout.decode("utf-8", errors="replace")


def adb_bytes(*args: str) -> bytes:
    return subprocess.run(["adb", *args], capture_output=True, check=True).stdout


# --- the app's own screen list ------------------------------------------------------


def read_screens() -> list[tuple[str, list[str]]]:
    """Categories in app order, each with the titles of its exercises."""
    source = EXERCISE_KT.read_text(encoding="utf-8")
    categories = re.findall(r'^ {4}(\w+)\("([^"]+)",', source, re.MULTILINE)
    exercises = re.findall(r'^ {4}\w+\(Category\.(\w+), "([^"]+)"', source, re.MULTILINE)
    if not categories or not exercises:
        sys.exit(f"Keine Übungen in {EXERCISE_KT} gefunden.")
    return [
        (title, [name for category, name in exercises if category == key])
        for key, title in categories
    ]


# --- reading and driving the screen -------------------------------------------------


def dump() -> list[tuple[str, tuple[int, int], int]]:
    """Every labelled node as (label, centre, area), largest area last."""
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb_bytes("exec-out", "cat", "/sdcard/ui.xml").decode("utf-8", errors="replace")
    nodes: list[tuple[str, tuple[int, int], int]] = []
    for node in ElementTree.fromstring(xml).iter("node"):
        label = node.get("text") or node.get("content-desc") or ""
        bounds = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if not label.strip() or not bounds:
            continue
        left, top, right, bottom = (int(value) for value in bounds.groups())
        centre = ((left + right) // 2, (top + bottom) // 2)
        nodes.append((label, centre, (right - left) * (bottom - top)))
    return sorted(nodes, key=lambda entry: entry[2])


def find(
    label: str,
    nodes: list[tuple[str, tuple[int, int], int]],
    exact: bool = False,
) -> tuple[int, int] | None:
    """Centre of the node matching label, from the most to the least specific match.

    Compose merges a card into a single node, so its label reads like
    "Notenlesen, Noten benennen, 80 %". Matching a whole segment first keeps
    "Notenlesen" from hitting "Notenlesen auf Zeit". With exact, only whole
    texts or whole segments count.
    """
    def segments(text: str) -> list[str]:
        return [part.strip() for part in re.split(r", |\n", text)]

    matchers = [
        lambda text: text.strip() == label,
        lambda text: label in segments(text),
    ]
    if not exact:
        matchers += [
            lambda text: text.strip().startswith(label),
            lambda text: label in text,
        ]
    for matches in matchers:
        # Nodes are sorted by area, so the first hit is the smallest, most specific one.
        for text, centre, _ in nodes:
            if matches(text):
                return centre
    return None


def tap(label: str, timeout: float = 20.0) -> None:
    """Wait for a label, tap it and wait until the screen has actually changed."""
    deadline = time.monotonic() + timeout
    while True:
        nodes = dump()
        centre = find(label, nodes)
        if centre:
            break
        if time.monotonic() > deadline:
            visible = " | ".join(sorted({text for text, _, _ in nodes}))
            sys.exit(f'"{label}" nicht gefunden. Sichtbar war: {visible}')
        time.sleep(0.5)

    before = [text for text, _, _ in nodes]
    adb("shell", "input", "tap", str(centre[0]), str(centre[1]))
    changed = time.monotonic() + 5.0
    while time.monotonic() < changed:
        time.sleep(0.4)
        if [text for text, _, _ in dump()] != before:
            break
    # Exercises draw a fretboard or staff right after; give that a moment to settle.
    time.sleep(0.6)


def back_until(label: str, attempts: int = 3) -> None:
    """Press back until label shows up.

    One press per level is not enough: an open soft keyboard takes the first
    press for itself and the screen stays where it is.
    """
    for _ in range(attempts):
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(0.8)
        if find(label, dump(), exact=True):
            return
    visible = " | ".join(sorted({text for text, _, _ in dump()}))
    sys.exit(f'Nach {attempts}x Zurück kein "{label}" zu sehen. Sichtbar war: {visible}')


def shot(out: Path, name: str) -> None:
    target = out / f"{name}.png"
    target.write_bytes(adb_bytes("exec-out", "screencap", "-p"))
    print(f"  {target} ({target.stat().st_size // 1024} kB)")


def slug(text: str) -> str:
    table = str.maketrans({"ä": "ae", "ö": "oe", "ü": "ue", "Ä": "ae", "Ö": "oe", "Ü": "ue", "ß": "ss"})
    return re.sub(r"[^a-z0-9]+", "-", text.lower().translate(table)).strip("-")


# --- the tour -----------------------------------------------------------------------


def capture(out: Path, prefix: str, screens: list[tuple[str, list[str]]]) -> None:
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", ACTIVITY)
    time.sleep(3.0)

    step = 0

    def numbered(name: str) -> str:
        nonlocal step
        step += 1
        return f"{prefix}{step:02d}-{slug(name)}"

    shot(out, numbered("start"))
    for category, exercises in screens:
        tap(category)
        shot(out, numbered(category))
        for exercise in exercises:
            tap(exercise)
            shot(out, numbered(exercise))
            # Another exercise's title only appears on the category screen, never on this one.
            back_until(next((title for title in exercises if title != exercise), category))
        back_until(SETTINGS_LABEL)

    tap(SETTINGS_LABEL)
    shot(out, numbered("einstellungen"))
    tap(STATS_LABEL)
    shot(out, numbered("statistik"))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--out", default=Path("screenshots"), type=Path)
    parser.add_argument("--theme", default="dark", choices=["dark", "light", "both"])
    args = parser.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    screens = read_screens()

    adb("wait-for-device")
    print(f"Installiere {args.apk}")
    adb("install", "-r", "-g", str(args.apk))

    themes = ["dark", "light"] if args.theme == "both" else [args.theme]
    for theme in themes:
        print(f"Farbschema: {theme}")
        adb("shell", "cmd", "uimode", "night", "yes" if theme == "dark" else "no")
        time.sleep(1.5)
        capture(args.out, f"{theme}-" if len(themes) > 1 else "", screens)

    print(f"{len(list(args.out.glob('*.png')))} Screenshots in {args.out}")


if __name__ == "__main__":
    main()
