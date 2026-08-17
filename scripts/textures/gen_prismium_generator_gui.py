#!/usr/bin/env python3
"""Generate the background texture for Prismium Generator's GUI (session
24 - see ModMenuTypes/PrismiumGeneratorMenu/PrismiumGeneratorScreen/
PROGRESS.md).

The mod's second GUI/container-screen texture, following the same
approach established by scripts/textures/gen_prismium_cell_gui.py
(session 23): a full 256x256 canvas (required by AbstractContainerScreen's
7-int `blit` overload, which always normalizes against an assumed
256x256 source - see that script's docstring for the full explanation),
artwork only in the top-left imageWidth x imageHeight corner (176x110
here - taller than Cell's 176x90 to fit a second gauge, see
PrismiumGeneratorScreen's class doc), everything else left transparent.

Reuses the same casing palette as Prismium Generator's own block texture
(scripts/textures/gen_prismium_generator.py: CASING_DARK/CASING_MID +
PRISMIUM_OUTLINE) so this panel reads as "the inside of the block you
just opened", exactly like Cell's GUI does for its own block.

Two recessed "tracks" are baked into this texture (their actual
proportional fills are drawn by PrismiumGeneratorScreen#renderBg each
frame, not here - same split responsibility as Cell's energy bar):
- a narrow vertical flame-gauge track (matches FLAME_X/FLAME_Y/
  FLAME_WIDTH/FLAME_HEIGHT in PrismiumGeneratorScreen)
- a wide horizontal energy-bar track (matches BAR_X/BAR_Y/BAR_WIDTH/
  BAR_HEIGHT there), positioned lower than Cell's own bar to leave room
  for the flame gauge above it.

Deterministic, no randomness. Run from repo root:
python3 scripts/textures/gen_prismium_generator_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_generator.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 110

PRISMIUM_OUTLINE = "#0B3D3C"
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"
CASING_HILITE = "#9BADAA"
TRACK_DARK = "#20302E"

# Ember-track shadow - a near-black warm tone (distinct from the cool
# TRACK_DARK used for the energy bar) so the flame gauge's empty state
# still hints "this one glows warm, not teal" even before it fills.
EMBER_TRACK_DARK = "#241511"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def build_panel():
    img = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    x0, y0, x1, y1 = 0, 0, PANEL_W - 1, PANEL_H - 1

    draw.rectangle([x0, y0, x1, y1], fill=hexrgb(CASING_MID) + (255,))

    for i in range(2):
        draw.rectangle([x0 + i, y0 + i, x1 - i, y1 - i], outline=hexrgb(PRISMIUM_OUTLINE) + (255,))

    draw.line([(x0 + 2, y0 + 2), (x1 - 2, y0 + 2)], fill=hexrgb(CASING_HILITE) + (255,))
    draw.line([(x0 + 2, y0 + 2), (x0 + 2, y1 - 2)], fill=hexrgb(CASING_HILITE) + (255,))

    # Flame gauge track (matches PrismiumGeneratorScreen.FLAME_X/Y/WIDTH/
    # HEIGHT = 12, 20, 10, 32).
    flame_x, flame_y, flame_w, flame_h = 12, 20, 10, 32
    draw.rectangle([flame_x - 2, flame_y - 2, flame_x + flame_w + 1, flame_y + flame_h + 1],
                    fill=hexrgb(PRISMIUM_OUTLINE) + (255,))
    draw.rectangle([flame_x - 1, flame_y - 1, flame_x + flame_w, flame_y + flame_h],
                    fill=hexrgb(EMBER_TRACK_DARK) + (255,))

    # Energy-bar track (matches PrismiumGeneratorScreen.BAR_X/Y/WIDTH/
    # HEIGHT = 8, 62, 160, 14).
    bar_x, bar_y, bar_w, bar_h = 8, 62, 160, 14
    draw.rectangle([bar_x - 2, bar_y - 2, bar_x + bar_w + 1, bar_y + bar_h + 1],
                    fill=hexrgb(PRISMIUM_OUTLINE) + (255,))
    draw.rectangle([bar_x - 1, bar_y - 1, bar_x + bar_w, bar_y + bar_h],
                    fill=hexrgb(TRACK_DARK) + (255,))

    # Separator line between the two gauge zones and the bottom FE-text
    # zone, echoing Cell's panel structure.
    draw.line([(x0 + 6, bar_y + bar_h + 8), (x1 - 6, bar_y + bar_h + 8)], fill=hexrgb(CASING_DARK) + (255,))

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = build_panel()
    img.save(OUT_PATH)
    print(f"Wrote {OUT_PATH} ({img.size[0]}x{img.size[1]})")


if __name__ == "__main__":
    main()
