#!/usr/bin/env python3
"""Generate the texture for Pale Prismium Spike (scheduled session,
2026-09-17): the Pale Prismium family's fourth crystal-prop palette
sibling, after Pale Prismium Geode Cluster (2026-09-15), Pale Prismium
Stalactite Crystal (2026-09-16), and Pale Prismium Bloom (this same
session, see gen_pale_prismium_bloom.py for the full rationale on why
this session gives Pale Bloom/Spike their own crafting recipe even
though their teal originals are worldgen-only). Mirrors Prismium Spike
(session 18's gen_prismium_spike.py) exactly in silhouette/shape but
reskinned in the icy PALE_* palette established by
gen_pale_prismium_block.py (session #77) and reused by every Pale
Prismium crystal prop since.

This is the block/cross crystal-prop family's eighth member overall
(Bloom, Spike, Geode Cluster, Stalactite, Pale Geode Cluster, Pale
Stalactite, Pale Bloom, and now this) and the fourth "palette sibling"
rather than a new silhouette, so it introduces zero new Java code paths
- reuses PrismiumSpikeBlock (Java) verbatim, since the shape/canSurvive/
VoxelShape logic (floor-standing, requires a sturdy block below, taller
hitbox than Bloom) is identical to the teal original. Only the texture,
block/item IDs, loot table, recipe, and MapColor differ - see
ModBlocks.java's PALE_PRISMIUM_SPIKE registration comment.

Recipe: Pale Prismium Block + Amethyst Shard (shapeless) - deliberately
NOT the same ingredient multiset as Pale Bloom's own new recipe (Pale
Prismium Block + Prismium Shard), so the two new recipes don't collide,
and thematically fitting since Spike's silhouette (sharp jagged shards)
reads closer to "amethyst cluster" than Bloom's flower-head does.

Same 16x16 canvas, same three-triangular-taper "crystal shard cluster"
technique as gen_prismium_spike.py, but:
  - Crystal ramp swapped from PRISMIUM_* (teal) to PALE_* (icy blue-white,
    same five-stop palette as every other Pale Prismium crystal prop) so
    it reads as a sibling of Pale Prismium Block/Lantern/Geode Cluster/
    Stalactite/Bloom rather than a copy of the teal Spike.
  - Rocky base swapped from the near-black ROOT_* stone palette to the
    same lighter icy-grey rock ramp used by Pale Geode Cluster/Pale
    Stalactite/Pale Bloom, so all Pale-family crystal props share one
    consistent "frost-rock" base material.
  - No cyan accent flecks (the teal Spike's cyan accent flecks would be
    nearly invisible against the already-icy PALE_* ramp, and no other
    Pale-family crystal prop uses an extra accent color - see
    gen_pale_prismium_geode_cluster.py / gen_pale_prismium_stalactite.py
    / gen_pale_prismium_bloom.py, all accent-free).

Deterministic (no randomness used). Run from repo root:
python3 scripts/textures/gen_pale_prismium_spike.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same icy palette as gen_pale_prismium_block.py / gen_pale_prismium_
# lantern.py / gen_pale_prismium_geode_cluster.py / gen_pale_prismium_
# stalactite.py / gen_pale_prismium_bloom.py, for cross-texture
# consistency across the whole Pale Prismium family.
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"

# Same lighter icy-grey rock ramp as gen_pale_prismium_geode_cluster.py /
# gen_pale_prismium_stalactite.py / gen_pale_prismium_bloom.py, reused
# here for the rocky base instead of the teal Spike's near-black ROOT_*,
# so all Pale-family crystal props share the same frost-rock base
# material.
ROOT_DARK = "#5B6B74"
ROOT_MID = "#7C8E97"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_pale_prismium_spike():
    img = new_img()
    px = img.load()

    outline = hexrgb(PALE_OUTLINE)
    shadow = hexrgb(PALE_SHADOW)
    base = hexrgb(PALE_BASE)
    mid = hexrgb(PALE_MID)
    hilite = hexrgb(PALE_HILITE)
    root_dark = hexrgb(ROOT_DARK)
    root_mid = hexrgb(ROOT_MID)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    # Rocky base: identical thin 2px band at the very bottom as
    # gen_prismium_spike.py, recolored to the icy-grey rock ramp.
    for x in range(3, 13):
        put(x, 15, root_dark if x % 2 == 0 else root_mid)

    # Three crystal shards of different heights/widths/x-offsets,
    # identical geometry to gen_prismium_spike.py's `shards` list,
    # recolored via the PALE_* ramp.
    shards = [
        (5, 15, 3, 2),   # left shard, medium height
        (9, 15, 1, 2),   # right shard, tallest (near-full height)
        (7, 15, 7, 3),   # center shard, shortest and widest - anchors the
                          # cluster visually so it doesn't look like two
                          # separate plants side by side
    ]

    for cx, base_y, tip_y, half_w in shards:
        height = base_y - tip_y
        for y in range(tip_y, base_y):
            t = (y - tip_y) / max(height, 1)  # 0 at tip, 1 at base
            w = max(0, round(half_w * t))
            for x in range(cx - w, cx + w + 1):
                dx = abs(x - cx)
                tip_bias = 1.0 - t
                score = dx - tip_bias * 1.5
                if score > 1.6:
                    c = outline
                elif score > 0.9:
                    c = shadow
                elif score > 0.1:
                    c = base
                elif score > -0.6:
                    c = mid
                else:
                    c = hilite
                put(x, y, c)

    return img


def main():
    img = make_pale_prismium_spike()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "pale_prismium_spike.png"
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
    preview_path = REPO_ROOT / "pale_prismium_spike_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
