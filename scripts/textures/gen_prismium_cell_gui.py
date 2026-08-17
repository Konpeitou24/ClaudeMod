#!/usr/bin/env python3
"""Generate the background texture for Prismium Cell's GUI (session 23 -
see ModMenuTypes/PrismiumCellMenu/PrismiumCellScreen/PROGRESS.md).

This is the mod's first GUI/container-screen texture, a different
category from every earlier script here (block/item/entity textures).
Per Forge's own docs (confirmed while implementing PrismiumCellScreen),
AbstractContainerScreen's plain 7-int `blit` overload always normalizes
its u/v/width/height against an assumed 256x256 source image, even
though only the top-left `imageWidth`x`imageHeight` (176x90 here, a
compact "status only" panel - see PrismiumCellScreen's class doc for why
it's smaller than vanilla's usual 176x166) corner is ever drawn. So this
script always produces a full 256x256 canvas; everything outside the
176x90 corner is transparent and simply never sampled.

Reuses the same casing palette as the block itself
(scripts/textures/gen_prismium_cell.py: CASING_DARK/CASING_MID slate gray
+ PRISMIUM_OUTLINE dark teal frame) so the GUI panel reads as "the inside
of the block you just opened" rather than a generic unrelated UI skin.
The energy bar itself is NOT drawn here - only its recessed dark "track"
is part of this texture; the actual proportional fill color is drawn by
PrismiumCellScreen#renderBg each frame (see that class's doc for why).

Deterministic, no randomness needed (this is all flat geometric shapes,
not organic pixel art). Run from repo root:
python3 scripts/textures/gen_prismium_cell_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_cell.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 90

PRISMIUM_OUTLINE = "#0B3D3C"
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

    # Outer 2px dark-teal outline (matches the mod's established
    # PRISMIUM_OUTLINE frame language from every block texture).
    for i in range(2):
        draw.rectangle([x0 + i, y0 + i, x1 - i, y1 - i], outline=hexrgb(PRISMIUM_OUTLINE) + (255,))

    # A single 1px highlight just inside the outline along the top/left
    # edges only, to fake a light source from the upper-left - a cheap
    # trick reused from the block casing scripts to keep the flat fill
    # from reading as completely flat.
    draw.line([(x0 + 2, y0 + 2), (x1 - 2, y0 + 2)], fill=hexrgb(CASING_HILITE) + (255,))
    draw.line([(x0 + 2, y0 + 2), (x0 + 2, y1 - 2)], fill=hexrgb(CASING_HILITE) + (255,))

    # Recessed energy-bar track: a dark inset rectangle with its own
    # thin outline, positioned to match PrismiumCellScreen's BAR_X/BAR_Y/
    # BAR_WIDTH/BAR_HEIGHT constants (8, 34, 160, 14). The screen draws
    # the actual proportional fill on top of this at render time.
    bar_x, bar_y, bar_w, bar_h = 8, 34, 160, 14
    draw.rectangle([bar_x - 2, bar_y - 2, bar_x + bar_w + 1, bar_y + bar_h + 1],
                    fill=hexrgb(PRISMIUM_OUTLINE) + (255,))
    draw.rectangle([bar_x - 1, bar_y - 1, bar_x + bar_w, bar_y + bar_h],
                    fill=hexrgb(TRACK_DARK) + (255,))

    # Thin separator line below the bar, above where the FE text renders,
    # echoing the same PRISMIUM_OUTLINE frame language once more so the
    # panel doesn't read as three unrelated zones stacked together.
    draw.line([(x0 + 6, bar_y + bar_h + 8), (x1 - 6, bar_y + bar_h + 8)], fill=hexrgb(CASING_DARK) + (255,))

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = build_panel()
    img.save(OUT_PATH)
    print(f"Wrote {OUT_PATH} ({img.size[0]}x{img.size[1]})")


if __name__ == "__main__":
    main()
