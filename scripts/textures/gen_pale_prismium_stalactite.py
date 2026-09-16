#!/usr/bin/env python3
"""Generate the texture for Pale Prismium Stalactite Crystal (scheduled
session, 2026-09-16): the Pale Prismium family's ceiling-mounted crystal
decoration, mirroring Prismium Stalactite Crystal (scheduled session,
2026-09-14's gen_prismium_stalactite.py) exactly in silhouette/shape but
reskinned in the icy PALE_* palette established by
gen_pale_prismium_block.py (session #77) and already reused by
gen_pale_prismium_geode_cluster.py (scheduled session, 2026-09-15).

This is the block/cross crystal-prop family's sixth member overall
(Bloom, Spike, Geode Cluster, Stalactite, Pale Geode Cluster, and now
this) and the SECOND "palette sibling" (after Pale Geode Cluster) rather
than a new silhouette, so it introduces zero new Java code paths - reuses
PrismiumStalactiteBlock (Java) verbatim, since the shape/canSurvive/
VoxelShape logic (ceiling-mounted, hangs from a sturdy block above) is
identical to the teal original. Only the texture, block/item IDs, loot
table, recipe, and MapColor differ - see ModBlocks.java's
PALE_PRISMIUM_STALACTITE registration comment.

Same 16x16 canvas, jagged broken-rock ceiling (top of canvas) + three
crystal points hanging downward technique as gen_prismium_stalactite.py,
but:
  - Crystal ramp swapped from PRISMIUM_* (teal) to PALE_* (icy blue-white,
    same five-stop palette as gen_pale_prismium_block.py /
    gen_pale_prismium_geode_cluster.py) so it reads as a sibling of Pale
    Prismium Block/Lantern/Geode Cluster rather than a copy of the teal
    Stalactite.
  - Rocky base swapped from the near-black ROOT_* stone palette to the
    same lighter icy-grey rock ramp used by gen_pale_prismium_geode_
    cluster.py, so both Pale-family crystal props share one consistent
    "frost-rock" base material.

Deterministic (no randomness used). Run from repo root:
python3 scripts/textures/gen_pale_prismium_stalactite.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same icy palette as gen_pale_prismium_block.py / gen_pale_prismium_
# lantern.py / gen_pale_prismium_geode_cluster.py, for cross-texture
# consistency across the whole Pale Prismium family.
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"

# Same lighter icy-grey rock ramp as gen_pale_prismium_geode_cluster.py
# (distinct from gen_prismium_stalactite.py's near-black ROOT_* stone
# palette) so both Pale-family crystal props share the same frost-rock
# base material.
ROOT_DARK = "#5B6B74"
ROOT_MID = "#7C8E97"
ROOT_HI = "#9FB0B8"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_pale_prismium_stalactite():
    img = new_img()
    px = img.load()

    outline = hexrgb(PALE_OUTLINE)
    shadow = hexrgb(PALE_SHADOW)
    base = hexrgb(PALE_BASE)
    mid = hexrgb(PALE_MID)
    hilite = hexrgb(PALE_HILITE)
    root_dark = hexrgb(ROOT_DARK)
    root_mid = hexrgb(ROOT_MID)
    root_hi = hexrgb(ROOT_HI)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    # Jagged broken-rock ceiling, identical column layout to
    # gen_prismium_stalactite.py so the two blocks share the same
    # "family" silhouette, just recolored to the icy rock ramp.
    rock_bottom = {
        1: 3, 2: 4, 3: 3, 4: 4, 5: 3, 6: 4, 7: 3,
        8: 4, 9: 3, 10: 4, 11: 3, 12: 4, 13: 3, 14: 4,
    }
    rock_colors = [root_dark, root_mid, root_hi]
    for x, bottom_y in rock_bottom.items():
        for y in range(0, bottom_y):
            c = rock_colors[(x + y) % 3]
            put(x, y, c)

    # Three crystal points hanging down out of the rock, identical
    # geometry to gen_prismium_stalactite.py's `points` list, recolored
    # via the PALE_* ramp below.
    points = [
        (3, 4, 12, 2),
        (8, 3, 15, 2),
        (12, 4, 10, 1),
    ]

    for cx, base_y, tip_y, half_w in points:
        height = tip_y - base_y
        for y in range(base_y, tip_y):
            t = (y - base_y) / max(height, 1)  # 0 at base (top), 1 at tip (bottom)
            w = max(0, round(half_w * (1.0 - t)))
            for x in range(cx - w, cx + w + 1):
                dx = abs(x - cx)
                tip_bias = t
                score = dx - tip_bias * 1.3
                if score > 1.4:
                    c = outline
                elif score > 0.8:
                    c = shadow
                elif score > 0.1:
                    c = base
                elif score > -0.5:
                    c = mid
                else:
                    c = hilite
                put(x, y, c)

    return img


def main():
    img = make_pale_prismium_stalactite()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "pale_prismium_stalactite.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    # 24x scaled preview on a checkerboard background so transparency is
    # visible during self-review (required step, see PROGRESS.md).
    scale = 24
    checker = Image.new("RGBA", (SIZE * scale, SIZE * scale), (0, 0, 0, 0))
    cpx = checker.load()
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            light = ((x // scale) + (y // scale)) % 2 == 0
            cpx[x, y] = (200, 200, 200, 255) if light else (150, 150, 150, 255)
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    checker.alpha_composite(big)
    preview_path = REPO_ROOT / "pale_prismium_stalactite_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
