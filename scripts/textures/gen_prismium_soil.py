#!/usr/bin/env python3
"""Generate the block texture for Prismium Soil (scheduled session #45).

The mod's first dedicated Prism Realm ground block, addressing PROGRESS.md
section 5 item 10-a (long-flagged as the highest-priority remaining gap:
the dimension has had custom biome colors since session 39, custom ore/
crystal density since session 41, and three exclusive plants since
sessions 40/43/44, but the ground itself was still plain overworld grass_
block/dirt the whole time).

Rather than a full custom noise_settings/surface_rule rewrite (judged too
high-risk to get exactly right for 1.20.1 without a local build to verify
against - see PROGRESS.md for the research trail), this block is placed
onto the existing overworld-generated terrain by a small worldgen Feature
(see PrismiumSoilFeature.java) that swaps grass_block/dirt/coarse_dirt for
this block, restricted to the claudemod:prism_realm biome via the same
biome_modifier technique already proven for the three plants.

Palette: deliberately NOT the bright teal/magenta PRISMIUM_* crystal
palette used by ore/block/tools (that would make the entire ground read as
"a mile of polished gemstone", not soil). Instead this samples the Prism
Realm biome's own sky_color (#2B1A4D) / fog_color (#3A2360) - dark violet-
indigo - as the dominant ground tones, so the terrain visually belongs
under that sky. Sparse embedded flecks in the existing teal PRISMIUM_MID/
PRISMIUM_BASE and magenta PRISMIUM_ACCENT tie the new ground back to the
rest of the mod's crystal family (as if the crystals native to this realm
are naturally present, at low density, in its dirt).

Deterministic (fixed RNG seed, reused pattern from gen_prismium.py's
speckled_base/scatter_crystals). Run from repo root:
python3 scripts/textures/gen_prismium_soil.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260818
SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- soil palette (derived from the Prism Realm biome's own sky/fog
# colors, data/claudemod/worldgen/biome/prism_realm.json) -----------------
SOIL_OUTLINE = "#130B22"  # near-black violet, darker than sky_color
SOIL_SHADOW = "#241640"   # close to fog_color #3A2360, darkened
SOIL_BASE = "#3A2360"     # = biome fog_color, dominant fill tone
SOIL_MID = "#4F3080"      # lighter clump, between fog and sky color
SOIL_HILITE = "#6B4AA8"   # rare bright fleck, leans toward sky_color family

# ---- crystal accent palette (reused verbatim from gen_prismium.py so
# embedded flecks read as "the same Prismium crystal", not a new mineral)
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_DARK = "#720070"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def speckled_base(colors, rng):
    """Fill every pixel by weighted random choice from a flat color list
    (duplicate entries bias frequency) - identical technique to
    gen_prismium.py's speckled_base for the ore textures' stone/deepslate
    base, reused here for a soil/dirt-style noisy fill."""
    img = new_img()
    px = img.load()
    palette = [hexrgb(c) for c in colors]
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*rng.choice(palette), 255)
    return img


def scatter_pixels(img, rng, colors, count):
    """Scatter single-pixel flecks of the given colors at random
    positions (used for the sparse embedded Prismium crystal specks)."""
    px = img.load()
    palette = [hexrgb(c) for c in colors]
    placed = 0
    attempts = 0
    while placed < count and attempts < count * 20:
        attempts += 1
        x = rng.randint(0, SIZE - 1)
        y = rng.randint(0, SIZE - 1)
        px[x, y] = (*rng.choice(palette), 255)
        placed += 1


def draw_crack_lines(img, rng):
    """A couple of short 2-3px dark cracks, to break up the flat speckle
    fill and read as packed/cracked ground rather than a uniform carpet."""
    px = img.load()
    outline = hexrgb(SOIL_OUTLINE)
    cracks = [
        [(2, 3), (3, 4), (4, 4)],
        [(11, 2), (12, 3)],
        [(6, 12), (7, 13), (8, 13)],
        [(13, 9), (13, 10), (12, 11)],
    ]
    for crack in cracks:
        for (x, y) in crack:
            px[x, y] = (*outline, 255)


def make_prismium_soil():
    rng = random.Random(SEED)

    # Weighted base fill: SOIL_BASE dominant, SOIL_SHADOW/SOIL_MID as
    # secondary clumps, SOIL_OUTLINE sparse (deep pores), SOIL_HILITE
    # rare (catching a little ambient light).
    weighted = (
        [SOIL_BASE] * 6
        + [SOIL_SHADOW] * 4
        + [SOIL_MID] * 3
        + [SOIL_OUTLINE] * 2
        + [SOIL_HILITE] * 1
    )
    img = speckled_base(weighted, rng)

    draw_crack_lines(img, rng)

    # Sparse embedded Prismium crystal flecks: mostly teal (base material),
    # a couple of magenta accents, kept few (6 total across 256px) so the
    # ground reads as "soil with crystals in it" rather than "crystal
    # block with dirt on it".
    scatter_pixels(img, rng, [PRISMIUM_MID, PRISMIUM_BASE], 4)
    scatter_pixels(img, rng, [PRISMIUM_ACCENT], 2)

    return img


def make_preview(img, scales=(4, 8, 16)):
    tile = 2
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * SIZE for s in scales) + 8 * (len(scales) - 1)
    total_h = max(s * SIZE for s in scales)
    preview = Image.new("RGBA", (total_w, total_h), (30, 30, 30, 255))

    x_off = 0
    for s in scales:
        board = Image.new("RGBA", (SIZE * s, SIZE * s))
        bpx = board.load()
        for y in range(SIZE * s):
            for x in range(SIZE * s):
                cx, cy = x // tile, y // tile
                bpx[x, y] = checker_light if (cx + cy) % 2 == 0 else checker_dark
        scaled = img.resize((SIZE * s, SIZE * s), Image.NEAREST)
        board.alpha_composite(scaled)
        preview.alpha_composite(board, (x_off, 0))
        x_off += SIZE * s + 8

    return preview


def make_tiled_preview(img, tiles=4, scale=4):
    """4x4 tiled preview at 4x scale, so seams/repetition are visible the
    way they'd actually look covering ground in-game."""
    tile_size = SIZE * scale
    preview = Image.new("RGBA", (tile_size * tiles, tile_size * tiles))
    scaled = img.resize((tile_size, tile_size), Image.NEAREST)
    for ty in range(tiles):
        for tx in range(tiles):
            preview.alpha_composite(scaled, (tx * tile_size, ty * tile_size))
    return preview


def main():
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_prismium_soil()
    out_path = out_dir / "prismium_soil.png"
    img.save(out_path)
    print(f"wrote {out_path.relative_to(REPO_ROOT)}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_soil.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    tiled = make_tiled_preview(img)
    tiled_path = BUILD_DIR / "preview_prismium_soil_tiled.png"
    tiled.save(tiled_path)
    print(f"wrote {tiled_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
