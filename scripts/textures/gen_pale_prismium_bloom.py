#!/usr/bin/env python3
"""Generate the texture for Pale Prismium Bloom (scheduled session,
2026-09-17): the Pale Prismium family's third crystal-prop palette
sibling, after Pale Prismium Geode Cluster (2026-09-15) and Pale
Prismium Stalactite Crystal (2026-09-16). Mirrors Prismium Bloom
(session 17's gen_prismium_bloom.py) exactly in silhouette/shape but
reskinned in the icy PALE_* palette established by
gen_pale_prismium_block.py (session #77) and reused by every Pale
Prismium crystal prop since.

This is the block/cross crystal-prop family's seventh member overall
(Bloom, Spike, Geode Cluster, Stalactite, Pale Geode Cluster, Pale
Stalactite, and now this) and the THIRD "palette sibling" rather than a
new silhouette, so it introduces zero new Java code paths - reuses
PrismiumBloomBlock (Java) verbatim, since the shape/canSurvive/
VoxelShape logic (floor-standing, requires a sturdy block below) is
identical to the teal original. Only the texture, block/item IDs, loot
table, recipe, and MapColor differ - see ModBlocks.java's
PALE_PRISMIUM_BLOOM registration comment.

Unlike the original Prismium Bloom (worldgen-only, no crafting recipe -
see PrismiumBloomBlock's own javadoc), this Pale variant is deliberately
given a player-craftable shapeless recipe (Pale Prismium Block +
Prismium Shard). The 2026-09-16 HANDOFF.md flagged two options for a
Pale Bloom/Spike: (a) also wire up new worldgen biome-modifier/
configured-feature/placed-feature files to keep them worldgen-only like
their teal originals, or (b) give the Pale versions their own crafting
recipe instead. This session picks (b): it is the materially smaller,
lower-risk change (no new worldgen data files to get wrong), it matches
this family's general pattern of every OTHER crystal prop (Geode
Cluster, Stalactite, and both of their Pale siblings) already being
player-craftable, and it means Bloom/Spike are the only silhouettes in
the family without a Pale sibling once this and gen_pale_prismium_spike.py
ship. This is therefore the first time a Pale palette sibling gains a
recipe its own teal original never had (Bloom/Spike remain worldgen-only
in their teal form).

Same 16x16 canvas, same 2px stem + diamond-silhouette "flower head"
technique as gen_prismium_bloom.py, but:
  - Crystal ramp swapped from PRISMIUM_* (teal) to PALE_* (icy blue-white,
    same five-stop palette as every other Pale Prismium crystal prop) so
    it reads as a sibling of Pale Prismium Block/Lantern/Geode Cluster/
    Stalactite rather than a copy of the teal Bloom.
  - Stem swapped from the dark violet STEM_DARK/STEM_MID to the same
    lighter icy-grey ROOT_DARK/ROOT_MID rock ramp used by Pale Geode
    Cluster/Pale Stalactite, so all Pale-family crystal props share one
    consistent "frost" base material instead of Bloom's warmer violet
    stalk look.
  - No violet accent flecks (the teal Bloom's warm violet accent flecks
    would clash with the icy palette, and no Pale-family crystal prop so
    far uses an extra accent color - see gen_pale_prismium_geode_cluster.py
    / gen_pale_prismium_stalactite.py, both accent-free).

Deterministic (no randomness used). Run from repo root:
python3 scripts/textures/gen_pale_prismium_bloom.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same icy palette as gen_pale_prismium_block.py / gen_pale_prismium_
# lantern.py / gen_pale_prismium_geode_cluster.py / gen_pale_prismium_
# stalactite.py, for cross-texture consistency across the whole Pale
# Prismium family.
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"

# Same lighter icy-grey rock ramp as gen_pale_prismium_geode_cluster.py /
# gen_pale_prismium_stalactite.py, reused here for the stem instead of
# the teal Bloom's dark violet STEM_DARK/STEM_MID, so all Pale-family
# crystal props share the same frost-rock base material.
ROOT_DARK = "#5B6B74"
ROOT_MID = "#7C8E97"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_pale_prismium_bloom():
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

    # Stem: 2px wide, y=11..15, identical geometry to gen_prismium_bloom.py
    # but recolored to the icy-grey rock ramp.
    for y in range(11, 16):
        put(7, y, root_mid if y % 2 == 0 else root_dark)
        put(8, y, root_dark if y % 2 == 0 else root_mid)

    # Bloom head: identical diamond-silhouette geometry to
    # gen_prismium_bloom.py, recolored via the PALE_* ramp.
    cx, cy = 7.5, 7.0
    for y in range(0, 12):
        for x in range(0, 16):
            dx = abs(x - cx)
            dy = abs(y - cy)
            manhattan = dx + dy
            if manhattan > 6.5:
                continue
            if manhattan > 5.2:
                c = outline
            elif manhattan > 4.0:
                c = shadow
            elif manhattan > 2.7:
                c = base
            elif manhattan > 1.4:
                c = mid
            else:
                c = hilite
            put(x, y, c)

    return img


def main():
    img = make_pale_prismium_bloom()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "pale_prismium_bloom.png"
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
    preview_path = REPO_ROOT / "pale_prismium_bloom_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
