#!/usr/bin/env python3
"""Generate the background texture for Prismium Generator's GUI (session
24 - see ModMenuTypes/PrismiumGeneratorMenu/PrismiumGeneratorScreen/
PROGRESS.md).

The mod's second GUI/container-screen texture, following the same
approach established by scripts/textures/gen_prismium_cell_gui.py
(session 23): a full 256x256 canvas (required by AbstractContainerScreen's
7-int `blit` overload, which always normalizes against an assumed
256x256 source - see that script's docstring for the full explanation),
artwork only in the top-left imageWidth x imageHeight corner (176x186
here - see PrismiumGeneratorScreen's class doc: originally 176x110,
grown in a later session to make room for a player-inventory grid, the
same reason gen_prismium_pulverizer_gui.py's panel is taller than a
pure-gauge screen's), everything else left transparent.

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

Session 58 addition: a recessed 18x18 fuel-slot socket at (152, 8),
the panel's first-ever item slot (see PrismiumGeneratorMenu's matching
SlotItemHandler position and PrismiumGeneratorBlockEntity.fuelInventory).
A later session (TODO6 followup) grew this to a 2x2 block of four such
sockets and added the player-inventory grid below (not baked into this
texture - see gen_prismium_pulverizer_gui.py's docstring for why player
inventory tiles are never baked in across this mod's GUI textures).

Deterministic, no randomness. Run from repo root:
python3 scripts/textures/gen_prismium_generator_gui.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/gui/container/prismium_generator.png"

CANVAS_SIZE = 256
PANEL_W = 176
PANEL_H = 214

PRISMIUM_OUTLINE = "#024D4B"
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

    # Fuel slot (session 58, GitHub issue #15 comment - see
    # PrismiumGeneratorMenu's Slot at the same (152, 8) coordinates and
    # PrismiumGeneratorBlockEntity.fuelInventory's doc). Standard 18x18
    # vanilla slot footprint so vanilla's own item-icon rendering (drawn
    # by AbstractContainerScreen, not this script) lines up exactly.
    # Recessed look built from this panel's own palette rather than
    # vanilla's gray, so it reads as part of this GUI rather than a
    # pasted-in vanilla widget: medium CASING_DARK fill (kept lighter than
    # the near-black gauge tracks above so item icons stay readable
    # against it, unlike TRACK_DARK/EMBER_TRACK_DARK which are deliberately
    # near-black to contrast against their *bright* progress fills), a
    # 1px TRACK_DARK shadow on the top+left edge and a 1px CASING_HILITE
    # highlight on the bottom+right edge - the same "light from the
    # top-left, recessed pocket" language the panel's own outer bevel
    # already establishes (see the two draw.line calls right after the
    # outline loop above), just inverted since a *recessed* slot is dark
    # on the side facing the light and bright on the side away from it.
    # Session (TODO6 followup): grown from a single slot at (152, 8) to a
    # centered horizontal row of four (matches
    # PrismiumGeneratorBlockEntity.FUEL_SLOT_COUNT and
    # PrismiumGeneratorMenu.FUEL_SLOT_POS exactly) so bulk fuel-loading has
    # somewhere to land - see that class's doc for why this is its own row
    # below the energy bar rather than tucked next to the flame gauge/
    # status text.
    for slot_x, slot_y in [(52, 102), (70, 102), (88, 102), (106, 102)]:
        slot_size = 18
        draw.rectangle([slot_x, slot_y, slot_x + slot_size - 1, slot_y + slot_size - 1],
                        fill=hexrgb(CASING_DARK) + (255,))
        draw.line([(slot_x, slot_y), (slot_x + slot_size - 1, slot_y)], fill=hexrgb(TRACK_DARK) + (255,))
        draw.line([(slot_x, slot_y), (slot_x, slot_y + slot_size - 1)], fill=hexrgb(TRACK_DARK) + (255,))
        draw.line([(slot_x + slot_size - 1, slot_y), (slot_x + slot_size - 1, slot_y + slot_size - 1)],
                   fill=hexrgb(CASING_HILITE) + (255,))
        draw.line([(slot_x, slot_y + slot_size - 1), (slot_x + slot_size - 1, slot_y + slot_size - 1)],
                   fill=hexrgb(CASING_HILITE) + (255,))

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = build_panel()
    img.save(OUT_PATH)
    print(f"Wrote {OUT_PATH} ({img.size[0]}x{img.size[1]})")


if __name__ == "__main__":
    main()
