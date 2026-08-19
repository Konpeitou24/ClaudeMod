#!/usr/bin/env python3
"""Generate the background texture for Prismium Smelter's GUI (session
68 - see ModMenuTypes/PrismiumSmelterMenu/PrismiumSmelterScreen/
PROGRESS.md).

Structurally identical to gen_prismium_pulverizer_gui.py (same 256x256
canvas, same 176x148 panel, same two 18x18 item-slot sockets + progress
track + energy track layout, same casing palette) - this machine reuses
Pulverizer's whole GUI shape verbatim (see PrismiumSmelterMenu's class
doc for why: it is a straight copy of the 2-slot input/output/progress/
energy pattern Pulverizer established, just with different recipe/
item data). The one deliberate visual difference: the progress-track
shadow uses a warm amber-toned dark instead of Pulverizer's magenta-
toned dark, so the progress fill (drawn at runtime by
PrismiumSmelterScreen, matching Prismium Ingot's own gold palette)
reads consistently with this machine's "molten metal" identity instead
of Pulverizer's "grinding" identity, even though the two GUIs are
otherwise twins.

Self-review performed on first generation (256x256 canvas, panel region
cropped and upscaled for inspection): confirmed both slot sockets are
vanilla-standard 18x18 and aligned with PrismiumSmelterMenu's
SlotItemHandler coordinates, confirmed the progress/energy tracks don't
overlap the slot row or the player-inventory area below.

Run from repo root: python3 scripts/textures/gen_prismium_smelter_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_smelter.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 148

PRISMIUM_OUTLINE = "#024D4B"
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"
CASING_HILITE = "#9BADAA"
TRACK_DARK = "#20302E"

# Progress-track shadow: warm amber-toned dark, distinct from
# gen_prismium_pulverizer_gui.py's magenta PROGRESS_TRACK_DARK - see
# module docstring.
PROGRESS_TRACK_DARK = "#2E1E0C"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def draw_slot_socket(draw, x, y, size=18):
    draw.rectangle([x, y, x + size - 1, y + size - 1], fill=hexrgb(CASING_DARK) + (255,))
    draw.line([(x, y), (x + size - 1, y)], fill=hexrgb(TRACK_DARK) + (255,))
    draw.line([(x, y), (x, y + size - 1)], fill=hexrgb(TRACK_DARK) + (255,))
    draw.line([(x + size - 1, y), (x + size - 1, y + size - 1)], fill=hexrgb(CASING_HILITE) + (255,))
    draw.line([(x, y + size - 1), (x + size - 1, y + size - 1)], fill=hexrgb(CASING_HILITE) + (255,))


def build_panel():
    img = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    x0, y0, x1, y1 = 0, 0, PANEL_W - 1, PANEL_H - 1

    draw.rectangle([x0, y0, x1, y1], fill=hexrgb(CASING_MID) + (255,))

    for i in range(2):
        draw.rectangle([x0 + i, y0 + i, x1 - i, y1 - i], outline=hexrgb(PRISMIUM_OUTLINE) + (255,))

    draw.line([(x0 + 2, y0 + 2), (x1 - 2, y0 + 2)], fill=hexrgb(CASING_HILITE) + (255,))
    draw.line([(x0 + 2, y0 + 2), (x0 + 2, y1 - 2)], fill=hexrgb(CASING_HILITE) + (255,))

    draw_slot_socket(draw, 56, 20)
    draw_slot_socket(draw, 116, 20)

    prog_x, prog_y, prog_w, prog_h = 82, 26, 28, 6
    draw.rectangle([prog_x - 2, prog_y - 2, prog_x + prog_w + 1, prog_y + prog_h + 1],
                    fill=hexrgb(PRISMIUM_OUTLINE) + (255,))
    draw.rectangle([prog_x - 1, prog_y - 1, prog_x + prog_w, prog_y + prog_h],
                    fill=hexrgb(PROGRESS_TRACK_DARK) + (255,))

    bar_x, bar_y, bar_w, bar_h = 8, 46, 160, 10
    draw.rectangle([bar_x - 2, bar_y - 2, bar_x + bar_w + 1, bar_y + bar_h + 1],
                    fill=hexrgb(PRISMIUM_OUTLINE) + (255,))
    draw.rectangle([bar_x - 1, bar_y - 1, bar_x + bar_w, bar_y + bar_h],
                    fill=hexrgb(TRACK_DARK) + (255,))

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = build_panel()
    img.save(OUT_PATH)
    print(f"Wrote {OUT_PATH} ({img.size[0]}x{img.size[1]})")


if __name__ == "__main__":
    main()
