#!/usr/bin/env python3
"""Generate the texture for Pale Prismium Geode Cluster (scheduled
session, 2026-09-15): the Pale Prismium family's own crystal surface
decoration, mirroring Prismium Geode Cluster (scheduled session,
2026-09-13's gen_prismium_geode_cluster.py) exactly in silhouette/shape
but reskinned in the icy PALE_* palette established by
gen_pale_prismium_block.py (session #77).

This is the block/cross crystal-prop family's fifth member overall
(Bloom, Spike, Geode Cluster, Stalactite, and now this) but the first to
answer "what would this family's decorative crystal look like in the
Pale Prismium color line" rather than adding a new silhouette. Reuses
PrismiumGeodeClusterBlock (Java) verbatim - see ModBlocks.java's
PALE_PRISMIUM_GEODE_CLUSTER registration comment - since the shape/
canSurvive/VoxelShape logic is identical to the teal original; only the
texture, block/item IDs, loot table, recipe, and MapColor differ. This
keeps the session's risk minimal: no new Java code path is introduced,
only new data/asset files plus one new registration line reusing an
already-CI-verified Block subclass.

Same 16x16 canvas, jagged broken-rock base + five stubby crystal points
technique as gen_prismium_geode_cluster.py, but:
  - Crystal ramp swapped from PRISMIUM_* (teal) to PALE_* (icy blue-white,
    same five-stop palette as gen_pale_prismium_block.py) so it reads as
    a sibling of Pale Prismium Block/Lantern rather than a copy of the
    teal Geode Cluster.
  - Rocky base swapped from the near-black ROOT_* stone palette to a
    lighter icy-grey rock ramp, since a jet-black rock base under
    pale-white crystals would read as muddy/inconsistent rather than icy.

Deterministic (no randomness used). Run from repo root:
python3 scripts/textures/gen_pale_prismium_geode_cluster.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same icy palette as gen_pale_prismium_block.py (session #77) /
# gen_pale_prismium_lantern.py (session #79), for cross-texture
# consistency across the whole Pale Prismium family.
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"

# Lighter icy-grey rock ramp (distinct from gen_prismium_geode_cluster.py's
# near-black ROOT_* stone palette) so the rocky base reads as frost-rock
# rather than dark cave stone under the pale crystals.
ROOT_DARK = "#5B6B74"
ROOT_MID = "#7C8E97"
ROOT_HI = "#9FB0B8"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_pale_prismium_geode_cluster():
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

    # Jagged broken-rock base, identical column layout to
    # gen_prismium_geode_cluster.py so the two blocks share the same
    # "family" silhouette, just recolored.
    rock_top = {
        1: 13, 2: 12, 3: 13, 4: 12, 5: 13, 6: 12, 7: 13,
        8: 12, 9: 13, 10: 12, 11: 13, 12: 12, 13: 13, 14: 12,
    }
    rock_colors = [root_dark, root_mid, root_hi]
    for x, top_y in rock_top.items():
        for y in range(top_y, 16):
            c = rock_colors[(x + y) % 3]
            put(x, y, c)

    # Five short, stubby crystal points, identical geometry to
    # gen_prismium_geode_cluster.py's `points` list, recolored via the
    # PALE_* ramp below.
    points = [
        (2, 12, 8, 1),
        (5, 12, 6, 2),
        (8, 11, 4, 2),
        (11, 12, 7, 1),
        (13, 12, 9, 1),
    ]

    for cx, base_y, tip_y, half_w in points:
        height = base_y - tip_y
        for y in range(tip_y, base_y):
            t = (y - tip_y) / max(height, 1)  # 0 at tip, 1 at base
            w = max(0, round(half_w * t))
            for x in range(cx - w, cx + w + 1):
                dx = abs(x - cx)
                tip_bias = 1.0 - t
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
    img = make_pale_prismium_geode_cluster()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "pale_prismium_geode_cluster.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    # 24x scaled preview on a checkerboard background for self-review
    # (required step, see PROGRESS.md).
    scale = 24
    checker = Image.new("RGBA", (SIZE * scale, SIZE * scale), (0, 0, 0, 0))
    cpx = checker.load()
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            light = ((x // scale) + (y // scale)) % 2 == 0
            cpx[x, y] = (200, 200, 200, 255) if light else (150, 150, 150, 255)
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    checker.alpha_composite(big)
    preview_path = REPO_ROOT / "pale_prismium_geode_cluster_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
