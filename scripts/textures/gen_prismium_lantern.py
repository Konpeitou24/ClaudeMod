#!/usr/bin/env python3
"""Generate the texture for Prismium Lantern (session 4's first purely
utility exploration block - see ModBlocks.PRISMIUM_LANTERN / PROGRESS.md).

Unlike prismium_block.png / prismium_core.png (smooth diagonal-gradient
crystal faces), this one introduces a new visual language for the family:
a dark metal cage/lattice (evoking a lantern frame) laid over the same
Prismium glow palette, brightest at the center to read as "light source"
at a glance even at small size. Kept the cage bars sparse (3 verticals x 3
horizontals, 1px each) and the accent flecks restrained after comparing
against a denser first pass that read as visual noise on small-scale
preview - same lesson as gen_prismium_core.py's self-review note in
PROGRESS.md (session 3): when in doubt, simplify back toward a clear
silhouette. Deterministic (fixed seed). Run from repo root:
python3 scripts/textures/gen_prismium_lantern.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260817
SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#FF7CFC"

# Dark cage frame color: distinct from PRISMIUM_OUTLINE (which is a very
# dark teal used as a thin silhouette border on every texture in the
# family) - this one is closer to neutral dark slate so the cage reads as
# "metal", not "crystal edge".
CAGE_DARK = "#26302E"
CAGE_MID = "#3B4A47"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_lantern():
    rng = random.Random(SEED + 41)
    img = new_img()
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    core_white = hexrgb(PRISMIUM_CORE_WHITE)
    accent = hexrgb(PRISMIUM_ACCENT)
    cage_dark = hexrgb(CAGE_DARK)
    cage_mid = hexrgb(CAGE_MID)

    # Radial-ish glow: brightest near the center, cooling toward the edges.
    # Uses Chebyshev distance (not true Euclidean) to stay in flat concentric
    # bands rather than a smooth AA circle, matching the rest of the mod's
    # "flat bands, no gradients" pixel-art style.
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

    # Cage lattice: 3 vertical + 3 horizontal 1px bars (dark metal), leaving
    # the glow visible in the "window" cells between them.
    bar_positions = (3, 7, 11)
    for i in bar_positions:
        for t in range(SIZE):
            px[i, t] = (*cage_dark, 255)
            px[t, i] = (*cage_dark, 255)
    # Lighter highlight pixel on the sunward (top-left) edge of every bar
    # segment so the cage doesn't read as flat black lines.
    for i in bar_positions:
        for t in range(0, SIZE, 4):
            if t > 0:
                px[i, t - 1] = (*cage_mid, 255)
                px[t - 1, i] = (*cage_mid, 255)

    # Small rivet dots at every bar intersection, for a "framed" read.
    for i in bar_positions:
        for j in bar_positions:
            px[i, j] = (*cage_dark, 255)

    # A few sparse violet energy flecks in two of the open windows only
    # (kept restrained - see module docstring).
    for (x, y) in [(5, 5), (9, 9), (5, 9)]:
        px[x, y] = (*accent, 255)

    # Crisp 1px border for a clean block silhouette, matching prismium_block
    # / prismium_core.
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
    save(make_prismium_lantern(), "block/prismium_lantern.png")
