#!/usr/bin/env python3
"""Generate block/pale_prismium_lantern.png (session #79, scheduled):
a new exploration light source for the Pale Prismium family (see
gen_pale_prismium_block.py, session #77), following the same "dark metal
cage over a glowing core" visual language established by
gen_prismium_lantern.py (session 4) for the original teal-cyan Prismium
Lantern.

Deliberately reuses that cage-lattice *technique* (radial glow core +
3x3 dark metal bars + rivets + sparse accent flecks + solid border) but
swaps in the PALE_* icy blue-white palette from gen_pale_prismium_block.py
so this reads as "the pale family's lantern", not a recolor of the
original lantern with a different name. The cage color itself is also
shifted from gen_prismium_lantern.py's near-neutral slate (CAGE_DARK/MID)
to a cooler blue-black so it still contrasts against the (much lighter)
pale glow instead of nearly matching PALE_OUTLINE.

16x16, limited flat palette, no smooth gradients/AA (Chebyshev-distance
concentric bands, same as gen_prismium_lantern.py), matching this mod's
established pixel-art convention. Deterministic (fixed seed).

Run from repo root: python3 scripts/textures/gen_pale_prismium_lantern.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260829
SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same PALE_* constants as gen_pale_prismium_block.py, kept duplicated
# here (not imported) to match this repo's existing convention of each
# gen_*.py script being self-contained (see gen_prismium_lantern.py,
# which also duplicates gen_prismium.py's PRISMIUM_* constants rather
# than importing them).
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"
PALE_ACCENT = "#7EE6FF"

# Cage frame: cool blue-black, distinct from PALE_OUTLINE so the cage
# still reads as a separate "metal" layer over the icy glow rather than
# blending into the border.
CAGE_DARK = "#1B2A38"
CAGE_MID = "#2E4356"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_pale_prismium_lantern():
    rng = random.Random(SEED + 41)
    img = new_img()
    px = img.load()
    outline = hexrgb(PALE_OUTLINE)
    shadow = hexrgb(PALE_SHADOW)
    base = hexrgb(PALE_BASE)
    mid = hexrgb(PALE_MID)
    hilite = hexrgb(PALE_HILITE)
    core_white = (255, 255, 255)
    accent = hexrgb(PALE_ACCENT)
    cage_dark = hexrgb(CAGE_DARK)
    cage_mid = hexrgb(CAGE_MID)

    # Radial-ish glow, brightest near the center, cooling toward the
    # edges - identical banding technique to gen_prismium_lantern.py.
    cx, cy = 7.5, 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = max(abs(x - cx), abs(y - cy))
            if d < 2:
                c = core_white
            elif d < 3.5:
                c = hilite
            elif d < 5.5:
                c = mid
            elif d < 7:
                c = base
            else:
                c = shadow
            px[x, y] = (*c, 255)

    # Cage lattice: 3 vertical + 3 horizontal 1px bars.
    bar_positions = (3, 7, 11)
    for i in bar_positions:
        for t in range(SIZE):
            px[i, t] = (*cage_dark, 255)
            px[t, i] = (*cage_dark, 255)
    for i in bar_positions:
        for t in range(0, SIZE, 4):
            if t > 0:
                px[i, t - 1] = (*cage_mid, 255)
                px[t - 1, i] = (*cage_mid, 255)

    # Rivet dots at every bar intersection.
    for i in bar_positions:
        for j in bar_positions:
            px[i, j] = (*cage_dark, 255)

    # Sparse cyan energy flecks in two open windows, tying into the
    # mod's energy theme (same restrained density as the original
    # lantern - see gen_prismium_lantern.py's self-review note).
    for (x, y) in [(5, 5), (9, 9), (5, 9)]:
        px[x, y] = (*accent, 255)

    # Crisp 1px border, matching pale_prismium_block.png.
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
    save(make_pale_prismium_lantern(), "block/pale_prismium_lantern.png")
