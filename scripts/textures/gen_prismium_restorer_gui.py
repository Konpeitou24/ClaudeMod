#!/usr/bin/env python3
"""Generate the background texture for Prismium Restorer's GUI (session 26 -
see ModMenuTypes/PrismiumRestorerMenu/PrismiumRestorerScreen/PROGRESS.md).

Fourth GUI background after Prismium Cell (session 23,
gen_prismium_cell_gui.py), Prismium Generator (session 24,
gen_prismium_generator_gui.py) and Prismium Pylon (session 25,
gen_prismium_pylon_gui.py). Structurally identical to Cell's: a compact
176x90 panel with a single energy-bar track and no second gauge/lamp
socket (Restorer has no ticking "active" state or burn gauge to
visualize, see PrismiumRestorerMenu's class doc - the GUI is a pure
status display, same as Cell), drawn on a full 256x256 transparent canvas
per AbstractContainerScreen's blit-normalization requirement (see
gen_prismium_cell_gui.py's docstring for why).

Reuses the same slate-gray CASING_DARK/CASING_MID/CASING_HILITE casing
palette as every other GUI panel in the mod so far, but swaps the frame
outline from PRISMIUM_OUTLINE (dark teal, Cell/Generator) to a dark
gold/amber outline derived from Restorer's own "mending cross" block
texture accent (scripts/textures/gen_prismium_restorer.py's CROSS_EDGE,
#B8791A, darkened toward near-black the same way Pylon's PYLON_OUTLINE
was derived from its own PRISMIUM_ACCENT in session 25) - continuing the
"let each consumer block's GUI frame echo its own block accent" strategy
floated in PROGRESS.md's session 25 "議論したい論点"
("消費ブロックの見分けやすさ、GUIにも波及"). This is deliberately the
*only* visual departure from Cell's panel: no lamp socket, no second bar,
same bar geometry - Restorer's GUI is meant to read as "a Cell-family
panel wearing gold trim", not a fourth unrelated layout.

Deterministic, no randomness needed (flat geometric shapes only). Run
from repo root: python3 scripts/textures/gen_prismium_restorer_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_restorer.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 90

# Dark gold/amber outline derived from Restorer's own CROSS_EDGE (#B8791A,
# see gen_prismium_restorer.py), darkened toward the mod's usual
# near-black outline value so it still reads as a frame, not a glow, at
# rest - same darkening approach used for PYLON_OUTLINE in session 25.
RESTORER_OUTLINE = "#4A2E08"
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"
CASING_HILITE = "#9BADAA"
TRACK_DARK = "#20302E"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def build_panel():
    img = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    x0, y0, x1, y1 = 0, 0, PANEL_W - 1, PANEL_H - 1

    # Base casing fill.
    draw.rectangle([x0, y0, x1, y1], fill=hexrgb(CASING_MID) + (255,))

    # Outer 2px gold-tinted outline (see module doc for why this departs
    # from Cell/Generator's teal PRISMIUM_OUTLINE).
    for i in range(2):
        draw.rectangle([x0 + i, y0 + i, x1 - i, y1 - i], outline=hexrgb(RESTORER_OUTLINE) + (255,))

    # Upper-left highlight, same cheap fake-light trick as every other
    # panel in this family.
    draw.line([(x0 + 2, y0 + 2), (x1 - 2, y0 + 2)], fill=hexrgb(CASING_HILITE) + (255,))
    draw.line([(x0 + 2, y0 + 2), (x0 + 2, y1 - 2)], fill=hexrgb(CASING_HILITE) + (255,))

    # Recessed energy-bar track, same geometry/style as Cell/Pylon's
    # panel (BAR_X/BAR_Y/BAR_WIDTH/BAR_HEIGHT = 8, 34, 160, 14).
    bar_x, bar_y, bar_w, bar_h = 8, 34, 160, 14
    draw.rectangle([bar_x - 2, bar_y - 2, bar_x + bar_w + 1, bar_y + bar_h + 1],
                    fill=hexrgb(RESTORER_OUTLINE) + (255,))
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
