#!/usr/bin/env python3
"""Generate the texture for Prismium Cell (session 8's first block entity -
see ModBlocks.PRISMIUM_CELL / PrismiumCellBlockEntity / PROGRESS.md).

New visual language for the family, third distinct one after the smooth
diagonal-gradient crystal faces (prismium_block/prismium_core) and the
dark cage lattice (prismium_lantern): a boxy "battery casing" - a thick
metal frame around a small glass window showing the Prismium glow, with a
couple of short accent segments hinting at a charge gauge.

Went through one redraw after generating and reviewing the first pass
(both 16x and 4x upscaled previews, per the mod's standard self-review
step): the initial version reused the lantern's near-black CAGE_DARK/
CAGE_MID metal tones and a thin 3px frame around a 10x10 window with
three full-width accent bars. On review it read as a flat teal square
with pink stripes, not a "cell" - the dark-gray casing visually blended
into PRISMIUM_OUTLINE's dark teal at small scale (nearly the same
lightness/saturation, just a different hue), so the frame that was
supposed to be the block's whole identity essentially disappeared, and
the full-width magenta bars were heavy enough to read as generic candy
stripes rather than a restrained gauge accent. This version fixes both:
the casing color was moved to a lighter, more neutral slate gray that
contrasts clearly against both the dark outline and the cyan glass, the
frame was thickened (4px per side instead of 3px) so it dominates the
silhouette the way a real battery casing would, and the glass window
shrank to 8x8 with only two short (not full-width) accent segments.
Deterministic (fixed seed - unused now, but kept for interface parity
with sibling scripts that still do use randomness). Run from repo root:
python3 scripts/textures/gen_prismium_cell.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#FF7CFC"

# Deliberately NOT the lantern's near-black CAGE_DARK/CAGE_MID (see module
# docstring for why the first draft using those failed self-review): a
# lighter, more neutral slate gray that stays visually distinct from both
# PRISMIUM_OUTLINE (dark teal) and the glass window's cyan glow.
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_cell():
    img = new_img()
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    core_white = hexrgb(PRISMIUM_CORE_WHITE)
    accent = hexrgb(PRISMIUM_ACCENT)
    casing_dark = hexrgb(CASING_DARK)
    casing_mid = hexrgb(CASING_MID)

    # 1) Base fill: the whole canvas starts as metal casing.
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*casing_dark, 255)

    # 2) Casing highlight: lighter metal on the top row(s) and left
    # column(s) of the casing only, "sunward" lighting convention shared
    # with the lantern's cage bars. Two pixels deep this time (not one)
    # since the casing itself is thicker.
    for x in range(SIZE):
        px[x, 0] = (*casing_mid, 255)
        px[x, 1] = (*casing_mid, 255)
    for y in range(SIZE):
        px[0, y] = (*casing_mid, 255)
        px[1, y] = (*casing_mid, 255)

    # 3) Glass window: x/y 4..11 inclusive (8x8), well inset into the now
    # much thicker casing so the frame reads clearly as the dominant shape.
    cx, cy = 7.5, 7.5
    for y in range(4, 12):
        for x in range(4, 12):
            d = max(abs(x - cx), abs(y - cy))
            if d < 1.5:
                c = core_white
            elif d < 2.75:
                c = hilite
            elif d < 3.5:
                c = mid
            else:
                c = base
            px[x, y] = (*c, 255)

    # 4) Two short accent segments (not full window width) suggesting a
    # charge gauge, kept deliberately restrained after the first draft's
    # full-width bars read as noisy stripes.
    for x in range(5, 9):
        px[x, 6] = (*accent, 255)
    for x in range(7, 11):
        px[x, 9] = (*accent, 255)

    # 5) Rivets at the four inner corners where the casing meets the
    # window frame.
    for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        px[x, y] = (*casing_mid, 255)

    # 6) A single accent "terminal" pixel on the top casing band, a small
    # nod to a battery's terminal without adding real clutter.
    px[7, 1] = (*accent, 255)

    # 7) Crisp 1px outline border, matching the rest of the family. Drawn
    # last so it stays crisp even where it overlaps the casing highlight.
    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)

    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_prismium_cell(), "block/prismium_cell.png")
