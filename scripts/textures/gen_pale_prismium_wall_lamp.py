#!/usr/bin/env python3
"""Generate the texture for Pale Prismium Wall Lamp (scheduled session,
2026-09-22): the Pale Prismium family's own wall-mounted fixture,
mirroring Prismium Wall Lamp (gen_prismium_wall_lamp.py, scheduled
session 2026-09-21) exactly in silhouette/layout but reskinned in the
icy PALE_* palette established by gen_pale_prismium_block.py (session
#77) / gen_pale_prismium_geode_cluster.py (2026-09-15).

Reuses PrismiumWallLampBlock (Java) verbatim - see ModBlocks.java's
PALE_PRISMIUM_WALL_LAMP registration comment - since the shape/
canSurvive/VoxelShape/HORIZONTAL_FACING logic is identical to the teal
original; only the texture, block/item IDs, loot table, recipe, and
MapColor differ. Same full-opaque metal-plaque-plus-diamond-gem layout
as the teal original (not a block/cross transparent sprite), but:
  - The metal backplate keeps the same dark iron-gray METAL_* ramp as
    the teal original (a physical fixture body shouldn't change color
    just because its gem does - only the gem itself carries each
    family's identity color, matching how e.g. Pale Prismium Lantern's
    cage stays the same metal-dark tone as Prismium Lantern's cage).
  - The central diamond gem is swapped from the teal PRISMIUM_* ramp to
    the icy PALE_* ramp so it reads as a sibling of Pale Prismium
    Block/Lantern/Geode Cluster rather than a recolored copy of the
    teal Wall Lamp.

Deterministic (no RNG - every pixel placed via the same manhattan-
distance diamond formula as gen_prismium_wall_lamp.py). Run from repo
root: python3 scripts/textures/gen_pale_prismium_wall_lamp.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same icy palette as gen_pale_prismium_block.py / gen_pale_prismium_geode_cluster.py,
# for cross-texture consistency across the whole Pale Prismium family.
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"

# Same dark iron-gray metal backplate as gen_prismium_wall_lamp.py - the
# fixture body itself doesn't change with the gem's color family.
METAL_OUTLINE = "#1B1C1F"
METAL_SHADOW = "#2A2D33"
METAL_BASE = "#3E424A"
METAL_HILITE = "#585D66"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_pale_prismium_wall_lamp():
    img = new_img()
    px = img.load()

    m_outline = hexrgb(METAL_OUTLINE)
    m_shadow = hexrgb(METAL_SHADOW)
    m_base = hexrgb(METAL_BASE)
    m_hilite = hexrgb(METAL_HILITE)

    p_outline = hexrgb(PALE_OUTLINE)
    p_shadow = hexrgb(PALE_SHADOW)
    p_base = hexrgb(PALE_BASE)
    p_mid = hexrgb(PALE_MID)
    p_hilite = hexrgb(PALE_HILITE)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    # --- metal backplate, fully opaque ---
    for y in range(SIZE):
        for x in range(SIZE):
            if y < 5:
                put(x, y, m_hilite)
            elif y < 11:
                put(x, y, m_base)
            else:
                put(x, y, m_shadow)

    # Outer frame ring.
    for x in range(SIZE):
        put(x, 0, m_outline)
        put(x, SIZE - 1, m_outline)
    for y in range(SIZE):
        put(0, y, m_outline)
        put(SIZE - 1, y, m_outline)

    # Four corner rivets.
    for rx, ry in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(rx, ry, m_outline)

    # --- central glowing Pale Prismium gem, diamond via manhattan
    # distance from (7, 7) so it stays perfectly symmetric ---
    cx, cy = 7, 7
    for y in range(SIZE):
        for x in range(SIZE):
            d = abs(x - cx) + abs(y - cy)
            if d <= 1:
                put(x, y, p_hilite)
            elif d == 2:
                put(x, y, p_mid)
            elif d <= 4:
                put(x, y, p_base)
            elif d == 5:
                put(x, y, p_shadow)
            elif d == 6:
                put(x, y, p_outline)
            # d > 6 stays metal backplate from the pass above.

    return img


def main():
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "pale_prismium_wall_lamp.png"
    make_pale_prismium_wall_lamp().save(out_path)
    print(f"Wrote {out_path}")


if __name__ == "__main__":
    main()
