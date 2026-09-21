#!/usr/bin/env python3
"""Generate the texture for Prismium Wall Lamp (scheduled session,
2026-09-21), the mod's first HORIZONTAL_FACING wall-mounted block - see
PrismiumWallLampBlock's class doc for the API/geometry rationale.

Unlike the mod's existing crystal-prop family (Bloom/Spike/Geode
Cluster/Stalactite, all "block/cross" transparent silhouettes on empty
backgrounds), this block's model is a solid flush-mounted plaque
(see assets/claudemod/models/block/prismium_wall_lamp.json), so the
texture is a FULL opaque 16x16 square depicting a wall-sconce fixture
head-on, not a transparent cutout sprite:
  - A dark iron-gray metal backplate fills the frame, with a simple
    top-lit/bottom-shadowed bevel tint (lighter rows near the top,
    darker rows near the bottom) to read as a slightly domed plate
    rather than a flat sticker, plus four small corner rivets.
  - A large diamond-shaped glowing Prismium gem dominates the center,
    using the same outline/shadow/base/mid/hilite teal ramp as the
    rest of the Prismium family (gen_prismium_geode_cluster.py etc.)
    for cross-texture consistency, sized to nearly fill the plate so
    it still reads clearly at a distance/small scale as "a lit lamp",
    not just a small icon lost in a big dark square.

Deterministic (no RNG - every pixel placed via a manhattan-distance
diamond formula, no random sampling). Run from repo root:
python3 scripts/textures/gen_prismium_wall_lamp.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same outline/shadow/base/mid/hilite teal ramp as the rest of the
# Prismium crystal-prop family (gen_prismium_geode_cluster.py onward).
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"

# Dark iron-gray metal backplate, distinct from the family's rocky
# ROOT_* browns (Bloom/Spike/Geode Cluster) since this is a fabricated
# fixture, not something growing out of stone.
METAL_OUTLINE = "#1B1C1F"
METAL_SHADOW = "#2A2D33"
METAL_BASE = "#3E424A"
METAL_HILITE = "#585D66"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_wall_lamp():
    img = new_img()
    px = img.load()

    m_outline = hexrgb(METAL_OUTLINE)
    m_shadow = hexrgb(METAL_SHADOW)
    m_base = hexrgb(METAL_BASE)
    m_hilite = hexrgb(METAL_HILITE)

    p_outline = hexrgb(PRISMIUM_OUTLINE)
    p_shadow = hexrgb(PRISMIUM_SHADOW)
    p_base = hexrgb(PRISMIUM_BASE)
    p_mid = hexrgb(PRISMIUM_MID)
    p_hilite = hexrgb(PRISMIUM_HILITE)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    # --- metal backplate, fully opaque (this is a solid mounted
    # fixture, not a walk-through prop, so no transparency) ---
    for y in range(SIZE):
        for x in range(SIZE):
            if y < 5:
                put(x, y, m_hilite)
            elif y < 11:
                put(x, y, m_base)
            else:
                put(x, y, m_shadow)

    # Outer frame ring, one pixel in from the true edge, reads as a
    # riveted plate border rather than a texture that bleeds to the
    # tile boundary.
    for x in range(SIZE):
        put(x, 0, m_outline)
        put(x, SIZE - 1, m_outline)
    for y in range(SIZE):
        put(0, y, m_outline)
        put(SIZE - 1, y, m_outline)

    # Four corner rivets, just inside the frame.
    for rx, ry in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(rx, ry, m_outline)

    # --- central glowing Prismium gem, diamond via manhattan distance
    # from (7, 7) so it stays perfectly symmetric ---
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
    out_path = out_dir / "prismium_wall_lamp.png"
    make_prismium_wall_lamp().save(out_path)
    print(f"Wrote {out_path}")


if __name__ == "__main__":
    main()
