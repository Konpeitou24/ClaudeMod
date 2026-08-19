#!/usr/bin/env python3
"""Generate the texture for Prismium Geyser (session 66 - see PROGRESS.md).

Prismium Geyser is the mod's second "gimmick" block (after Prismium
Snare, session 64) but the first with a *positive*, traversal-oriented
effect rather than a punishing one: stepping onto it (via the vanilla
Block#stepOn hook - the same one SlimeBlock/HoneyBlock/MagmaBlock use for
"something happens when you stand on this", confirmed against the
official 1.20.1 Mojang mappings at mappings.dev, session 66) launches the
entity upward, and it periodically bubbles via animateTick (also
confirmed against mappings.dev, session 66).

Visual language: reuses the family's core glow palette (same hex
constants as gen_prismium_lantern.py) for the radial "energy" background,
but swaps Lantern's dark-metal grid CAGE (bars running edge-to-edge) for
a short, thick 4-armed VALVE PLUS that floats in the middle of the tile
without touching the block's edges - deliberately chosen (after an
angle/atan2-wedge first draft looked noisy at 16x16 and was scrapped, see
PROGRESS.md session 66 self-review note) so the silhouette reads clearly
as "a vent with an open center," distinct from Lantern's full-bar grid at
a glance despite sharing a palette and the same cube_all model shape. The
vane color reuses Lantern's CAGE_DARK/CAGE_MID hex values verbatim
(deliberate, ties both into the same "Prismium machinery" sub-family).

Deterministic, no RNG needed (the geometry is fully hand-specified this
time - simpler and more predictable to review than the plant textures'
per-row range approach). Run from repo root:
python3 scripts/textures/gen_prismium_geyser.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_CORE_WHITE = "#EFFFFC"

# Same metal-accent hexes as gen_prismium_lantern.py's CAGE_DARK/CAGE_MID -
# reused deliberately (see module docstring).
VANE_DARK = "#26302E"
VANE_MID = "#3B4A47"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_geyser():
    img = new_img()
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    core_white = hexrgb(PRISMIUM_CORE_WHITE)
    vane_dark = hexrgb(VANE_DARK)
    vane_mid = hexrgb(VANE_MID)

    cx, cy = 7.5, 7.5

    # Radial glow background (same flat-band Chebyshev technique as
    # gen_prismium_lantern.py, kept identical on purpose for family
    # consistency).
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

    # 4-armed valve plus: each arm is 2px thick, spans radius [3,7] from
    # center along one axis only (so the center glow at radius<3 stays
    # open/bright, and the corners at radius>7 stay clear background) -
    # explicit coordinate lists, not a formula, so the exact shape is easy
    # to eyeball-review here rather than trusting trig at small scale.
    arm_span = range(3, 8)  # radius 3..7 inclusive
    for r in arm_span:
        # right arm
        px[int(cx + r - 0.5) if r > 0 else 8, 7] = (*vane_dark, 255)
        px[7 + r, 7] = (*vane_dark, 255)
        px[7 + r, 8] = (*vane_dark, 255)
        # left arm
        px[7 - r, 7] = (*vane_dark, 255)
        px[7 - r, 8] = (*vane_dark, 255)
        # down arm
        px[7, 7 + r] = (*vane_dark, 255)
        px[8, 7 + r] = (*vane_dark, 255)
        # up arm
        px[7, 7 - r] = (*vane_dark, 255)
        px[8, 7 - r] = (*vane_dark, 255)

    # Outer tip of each arm gets a single highlight pixel (radius 7, the
    # outermost ring of the arm) so the dark bars don't read as flat.
    for (x, y) in [(14, 7), (1, 8), (7, 14), (8, 1)]:
        px[x, y] = (*vane_mid, 255)

    # Crisp 1px outline border, matching the rest of the family.
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
    save(make_prismium_geyser(), "block/prismium_geyser.png")
