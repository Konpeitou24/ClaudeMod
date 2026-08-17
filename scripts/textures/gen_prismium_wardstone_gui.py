#!/usr/bin/env python3
"""Generate the background texture for Prismium Wardstone's GUI (session 27 -
see ModMenuTypes/PrismiumWardstoneMenu/PrismiumWardstoneScreen/PROGRESS.md).

Fifth GUI background after Prismium Cell (session 23, gen_prismium_cell_gui.py),
Prismium Generator (session 24, gen_prismium_generator_gui.py), Prismium Pylon
(session 25, gen_prismium_pylon_gui.py) and Prismium Restorer (session 26,
gen_prismium_restorer_gui.py). Structurally identical to Pylon's: a compact
176x90 panel with a single energy-bar track PLUS a small recessed lamp
socket for the boolean "warding right now" status (Wardstone, like Pylon,
has a ticking active/idle state driven by its LIT blockstate - see
PrismiumWardstoneBlockEntity#isActive - unlike Cell/Restorer which are
pure passive buffers with no such state), drawn on a full 256x256
transparent canvas per AbstractContainerScreen's blit-normalization
requirement (see gen_prismium_cell_gui.py's docstring for why).

Reuses the same slate-gray CASING_DARK/CASING_MID/CASING_HILITE casing
palette as every other GUI panel in the mod so far, but swaps the frame
outline to a dark blood-red derived from Wardstone's own "rune" block
texture accent (scripts/textures/gen_prismium_wardstone.py's
RUNE_LIT_EDGE/RUNE_LIT_MID, #B8221F/#FF4A3D), darkened toward the mod's
usual near-black outline value the same way PYLON_OUTLINE (session 25)
and RESTORER_OUTLINE (session 26) were derived from their own blocks'
accents. This completes the 4-color GUI-outline set flagged in
PROGRESS.md's session 25/26 "議論したい論点" ("消費ブロックの見分けやすさ、
GUIにも波及"): Cell/Generator teal, Pylon violet, Restorer gold, and now
Wardstone red - all four consumer/storage families should now be
distinguishable by GUI frame color alone.

Deterministic, no randomness needed (flat geometric shapes only). Run
from repo root: python3 scripts/textures/gen_prismium_wardstone_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_wardstone.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 90

# Dark blood-red outline derived from Wardstone's own RUNE_LIT_EDGE
# (#B8221F, see gen_prismium_wardstone.py), darkened toward the mod's
# usual near-black outline value so it still reads as a frame, not a
# glow, at rest - same darkening approach used for PYLON_OUTLINE (session
# 25) and RESTORER_OUTLINE (session 26).
WARDSTONE_OUTLINE = "#3D0D0B"
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"
CASING_HILITE = "#9BADAA"
TRACK_DARK = "#20302E"
LAMP_SOCKET_DARK = "#2E0E0C"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def build_panel():
    img = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    x0, y0, x1, y1 = 0, 0, PANEL_W - 1, PANEL_H - 1

    # Base casing fill.
    draw.rectangle([x0, y0, x1, y1], fill=hexrgb(CASING_MID) + (255,))

    # Outer 2px blood-red outline (see module doc for why this departs
    # from Cell/Generator's teal and Pylon/Restorer's violet/gold).
    for i in range(2):
        draw.rectangle([x0 + i, y0 + i, x1 - i, y1 - i], outline=hexrgb(WARDSTONE_OUTLINE) + (255,))

    # Upper-left highlight, same cheap fake-light trick as every other
    # panel in this family.
    draw.line([(x0 + 2, y0 + 2), (x1 - 2, y0 + 2)], fill=hexrgb(CASING_HILITE) + (255,))
    draw.line([(x0 + 2, y0 + 2), (x0 + 2, y1 - 2)], fill=hexrgb(CASING_HILITE) + (255,))

    # Recessed socket behind where PrismiumWardstoneScreen draws its
    # status lamp (LAMP_X=8, LAMP_Y=18, LAMP_SIZE=8 - identical geometry
    # to PrismiumPylonScreen's lamp) - a 2px-larger dark red inset so the
    # lamp (drawn at render time) always sits inside a visible "housing"
    # whether idle (dim gray fill) or active (glowing red fill).
    lamp_x, lamp_y, lamp_size = 8, 18, 8
    draw.rectangle([lamp_x - 2, lamp_y - 2, lamp_x + lamp_size + 1, lamp_y + lamp_size + 1],
                    fill=hexrgb(WARDSTONE_OUTLINE) + (255,))
    draw.rectangle([lamp_x - 1, lamp_y - 1, lamp_x + lamp_size, lamp_y + lamp_size],
                    fill=hexrgb(LAMP_SOCKET_DARK) + (255,))

    # Recessed energy-bar track, same geometry/style as every other
    # panel (BAR_X/BAR_Y/BAR_WIDTH/BAR_HEIGHT = 8, 34, 160, 14).
    bar_x, bar_y, bar_w, bar_h = 8, 34, 160, 14
    draw.rectangle([bar_x - 2, bar_y - 2, bar_x + bar_w + 1, bar_y + bar_h + 1],
                    fill=hexrgb(WARDSTONE_OUTLINE) + (255,))
    draw.rectangle([bar_x - 1, bar_y - 1, bar_x + bar_w, bar_y + bar_h],
                    fill=hexrgb(TRACK_DARK) + (255,))

    # Thin separator line below the bar, above where the FE text renders.
    draw.line([(x0 + 6, bar_y + bar_h + 8), (x1 - 6, bar_y + bar_h + 8)], fill=hexrgb(CASING_DARK) + (255,))

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = build_panel()
    img.save(OUT_PATH)
    print(f"Wrote {OUT_PATH} ({img.size[0]}x{img.size[1]})")


if __name__ == "__main__":
    main()
