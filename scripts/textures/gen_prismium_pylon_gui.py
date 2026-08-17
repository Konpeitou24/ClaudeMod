#!/usr/bin/env python3
"""Generate the background texture for Prismium Pylon's GUI (session 25 -
see ModMenuTypes/PrismiumPylonMenu/PrismiumPylonScreen/PROGRESS.md).

Third GUI background after Prismium Cell (session 23,
gen_prismium_cell_gui.py) and Prismium Generator (session 24,
gen_prismium_generator_gui.py). Structurally closest to Cell's: a compact
176x90 panel (Pylon has no second gauge the way Generator's flame column
does - only a boolean "radiating" status, drawn as a small lamp square by
PrismiumPylonScreen itself, not baked into this texture) drawn on a full
256x256 transparent canvas per AbstractContainerScreen's blit-normalization
requirement (see gen_prismium_cell_gui.py's docstring for why).

Reuses the same slate-gray CASING_DARK/CASING_MID/CASING_HILITE casing
palette as every other GUI panel in the mod so far, but swaps the frame
outline from PRISMIUM_OUTLINE (dark teal, Cell/Generator) to a violet-
tinted outline derived from Pylon's own PRISMIUM_ACCENT color
(scripts/textures/gen_prismium_pylon.py) - a small, deliberate departure
so Pylon's GUI reads as visually distinct from Cell/Generator at a glance
even before the status lamp (drawn at runtime) shows its state, echoing
how the block's own lit-state texture already differs from Cell/
Generator's glass-window/ember-grate sockets with a faceted crystal
instead.

Deterministic, no randomness needed (flat geometric shapes only). Run
from repo root: python3 scripts/textures/gen_prismium_pylon_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_pylon.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 90

# Violet-tinted outline derived from Pylon's own PRISMIUM_ACCENT (#C97BFF,
# see gen_prismium_pylon.py), darkened toward the mod's usual near-black
# outline value so it still reads as a frame, not a glow, at rest.
PYLON_OUTLINE = "#3A1F52"
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"
CASING_HILITE = "#9BADAA"
TRACK_DARK = "#20302E"
LAMP_SOCKET_DARK = "#241433"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def build_panel():
    img = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    x0, y0, x1, y1 = 0, 0, PANEL_W - 1, PANEL_H - 1

    # Base casing fill.
    draw.rectangle([x0, y0, x1, y1], fill=hexrgb(CASING_MID) + (255,))

    # Outer 2px violet-tinted outline (see module doc for why this
    # departs from Cell/Generator's teal PRISMIUM_OUTLINE).
    for i in range(2):
        draw.rectangle([x0 + i, y0 + i, x1 - i, y1 - i], outline=hexrgb(PYLON_OUTLINE) + (255,))

    # Upper-left highlight, same cheap fake-light trick as every other
    # panel in this family.
    draw.line([(x0 + 2, y0 + 2), (x1 - 2, y0 + 2)], fill=hexrgb(CASING_HILITE) + (255,))
    draw.line([(x0 + 2, y0 + 2), (x0 + 2, y1 - 2)], fill=hexrgb(CASING_HILITE) + (255,))

    # Recessed socket behind where PrismiumPylonScreen draws its status
    # lamp (LAMP_X=8, LAMP_Y=18, LAMP_SIZE=8) - a 2px-larger dark violet
    # inset so the lamp (drawn at render time) always sits inside a
    # visible "housing" whether idle (dim gray fill) or active (glowing
    # violet/cyan fill).
    lamp_x, lamp_y, lamp_size = 8, 18, 8
    draw.rectangle([lamp_x - 2, lamp_y - 2, lamp_x + lamp_size + 1, lamp_y + lamp_size + 1],
                    fill=hexrgb(PYLON_OUTLINE) + (255,))
    draw.rectangle([lamp_x - 1, lamp_y - 1, lamp_x + lamp_size, lamp_y + lamp_size],
                    fill=hexrgb(LAMP_SOCKET_DARK) + (255,))

    # Recessed energy-bar track, same geometry/style as Cell's panel
    # (BAR_X/BAR_Y/BAR_WIDTH/BAR_HEIGHT = 8, 34, 160, 14).
    bar_x, bar_y, bar_w, bar_h = 8, 34, 160, 14
    draw.rectangle([bar_x - 2, bar_y - 2, bar_x + bar_w + 1, bar_y + bar_h + 1],
                    fill=hexrgb(PYLON_OUTLINE) + (255,))
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
