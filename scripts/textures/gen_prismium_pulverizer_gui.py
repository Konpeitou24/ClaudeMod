#!/usr/bin/env python3
"""Generate the background texture for Prismium Pulverizer's GUI (session
67 - see ModMenuTypes/PrismiumPulverizerMenu/PrismiumPulverizerScreen/
PROGRESS.md).

The mod's sixth GUI texture, and the first to bake in vanilla-style item
slot sockets *and* leave room for a full player-inventory grid below (see
PrismiumPulverizerMenu's class doc - every earlier GUI in this mod is
either a pure status display or has at most one slot, with no player
inventory shown at all). Same 256x256-canvas-with-artwork-only-in-the-
top-left-corner approach every earlier GUI texture in this mod uses (see
gen_prismium_generator_gui.py's docstring for the full explanation of
why 256x256 specifically), and the same casing palette (CASING_DARK/
CASING_MID/PRISMIUM_OUTLINE) reused from every machine's own block
texture so the panel reads as "the inside of the block you just opened".

Three baked elements, each with its real proportional fill drawn by
PrismiumPulverizerScreen#renderBg at runtime (never baked in) - same
split-responsibility convention as every earlier gauge in this mod:
- two 18x18 item-slot sockets (input at 56,20 - output at 116,20),
  reusing the exact same recessed-pocket construction (dark fill, shadow
  top-left edge, highlight bottom-right edge) gen_prismium_generator_gui.py
  established for its fuel slot socket, just at new coordinates and
  duplicated for both slots.
- a short horizontal progress-bar track between the two slots (matches
  PrismiumPulverizerScreen.PROGRESS_X/Y/WIDTH/HEIGHT = 82, 26, 28, 6).
- a wide horizontal energy-bar track below the slot row (matches
  PrismiumPulverizerScreen.BAR_X/Y/WIDTH/HEIGHT = 8, 46, 160, 10).

No player-inventory background tiles are baked into this texture - like
every other screen in this mod, individual slot icons are drawn by
vanilla's own AbstractContainerScreen machinery from the Slot list, not
by this script; the plain casing color already filling the rest of the
panel is sufficient background for them (this mod's convention of
skipping the usual light-gray vanilla inventory-panel look in favor of
its own casing palette throughout, not just in the machine-specific top
area).

Self-review performed on first generation (256x256 canvas, panel region
cropped and upscaled for inspection): confirmed both slot sockets are
vanilla-standard 18x18 and aligned with PrismiumPulverizerMenu's
SlotItemHandler coordinates, confirmed the progress/energy tracks don't
overlap the slot row or the player-inventory area below, confirmed the
panel outline/highlight bevel matches every earlier GUI texture's
language.

Run from repo root: python3 scripts/textures/gen_prismium_pulverizer_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_pulverizer.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 148

PRISMIUM_OUTLINE = "#024D4B"
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"
CASING_HILITE = "#9BADAA"
TRACK_DARK = "#20302E"

# Progress-track shadow - a near-black warm-magenta tone (distinct from
# the cool TRACK_DARK used for the energy bar) so the progress bar's
# empty state hints "this one fills magenta, not teal" even before any
# progress accumulates - mirrors gen_prismium_generator_gui.py's
# EMBER_TRACK_DARK doing the same job for its ember-colored flame gauge.
PROGRESS_TRACK_DARK = "#24122A"


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

    # Input/output item slot sockets (matches PrismiumPulverizerMenu's
    # SlotItemHandler positions exactly).
    draw_slot_socket(draw, 56, 20)
    draw_slot_socket(draw, 116, 20)

    # Progress-bar track (matches PrismiumPulverizerScreen.PROGRESS_X/Y/
    # WIDTH/HEIGHT = 82, 26, 28, 6).
    prog_x, prog_y, prog_w, prog_h = 82, 26, 28, 6
    draw.rectangle([prog_x - 2, prog_y - 2, prog_x + prog_w + 1, prog_y + prog_h + 1],
                    fill=hexrgb(PRISMIUM_OUTLINE) + (255,))
    draw.rectangle([prog_x - 1, prog_y - 1, prog_x + prog_w, prog_y + prog_h],
                    fill=hexrgb(PROGRESS_TRACK_DARK) + (255,))

    # Energy-bar track (matches PrismiumPulverizerScreen.BAR_X/Y/WIDTH/
    # HEIGHT = 8, 46, 160, 10).
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
