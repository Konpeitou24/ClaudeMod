#!/usr/bin/env python3
"""Generate the block texture for Prismium Stone (session 47), the plain
stone-equivalent fill block for the Prism Realm's new flat chunk generator
(data/claudemod/dimension/prism_realm.json).

Directly requested by the repo owner: ordinary overworld stone/dirt were
still generating throughout the dimension (the previous generator reused
minecraft:overworld noise settings wholesale - only the top-most surface
layer got repainted by the existing PrismiumSoilFeature, so underground and
any missed surface columns stayed vanilla stone/dirt), and the request was
specifically to build the replacement "off of the Prismium ore texture"
rather than invent a new palette from scratch.

Palette: sampled directly from the shipped prismium_ore.png (16x16) rather
than re-typed from memory/other scripts' constants, per the mod's "verify,
don't guess" texture practice - see the SAMPLED_FROM_ORE dict below and the
one-off sampling command in this docstring for how it was obtained:

    python3 -c "from PIL import Image; from collections import Counter; \
im = Image.open('src/main/resources/assets/claudemod/textures/block/prismium_ore.png').convert('RGBA'); \
print(Counter(im.getdata()).most_common(10))"

That printed five distinct mid-grey shades as the dominant colours (the
ore's plain stone matrix) plus the small cyan crystal-vein cluster. This
script reuses the five greys wholesale for a vanilla-stone-style mottled
fill, and reuses only a couple of the existing PRISMIUM_* teal accent
pixels (very sparingly - a handful of single pixels per tile, not a vein
cluster) so the block reads as "quarried near Prismium ore" without itself
looking like an ore block.

Self-review: writes a checkerboard-composited preview (1x/4x/8x scale, plus
a 4x4 tiled swatch to check seam continuity) to build/preview_prismium_stone.png
for Read-based visual inspection, matching the mod's established workflow.
Deterministic (fixed seed). Run from repo root:
    python3 scripts/textures/gen_prismium_stone.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260819
W, H = 16, 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Sampled directly from prismium_ore.png's dominant colours (see docstring).
STONE_SHADES = [
    (118, 118, 118, 255),
    (121, 121, 121, 255),
    (130, 130, 130, 255),
    (140, 140, 140, 255),
    (143, 143, 143, 255),
]
STONE_DARK_EDGE = (96, 96, 96, 255)   # a touch darker than the darkest
                                       # sampled shade, mortar-line only.
# A handful of the ore's own crystal-vein colours, reused very sparingly.
ACCENT_FLECKS = [
    (17, 187, 184, 255),
    (0, 130, 130, 255),
    (101, 245, 227, 255),
]


def make_stone_texture():
    rng = random.Random(SEED)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()

    for y in range(H):
        for x in range(W):
            px[x, y] = rng.choice(STONE_SHADES)

    # Sparse darker "mortar" specks (cheap way to break up flat noise into
    # something that reads as chunky stone rather than static, same trick
    # vanilla stone/andesite textures use).
    for _ in range((W * H) // 12):
        x, y = rng.randrange(W), rng.randrange(H)
        px[x, y] = STONE_DARK_EDGE

    # A handful of single-pixel teal flecks - deliberately few (3-5 per
    # tile) so this reads as "family resemblance to the ore" rather than
    # "this is secretly ore".
    for _ in range(rng.randint(3, 5)):
        x, y = rng.randrange(W), rng.randrange(H)
        px[x, y] = rng.choice(ACCENT_FLECKS)

    return img


def make_preview(img, scales=(1, 4, 8)):
    tile = 2
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * W for s in scales) + 8 * len(scales) + W * 4 * 4
    total_h = max(s * H for s in scales)
    preview = Image.new("RGBA", (total_w, total_h), (30, 30, 30, 255))

    x_off = 0
    for s in scales:
        board = Image.new("RGBA", (W * s, H * s))
        bpx = board.load()
        for y in range(H * s):
            for x in range(W * s):
                cx, cy = x // tile, y // tile
                bpx[x, y] = checker_light if (cx + cy) % 2 == 0 else checker_dark
        scaled = img.resize((W * s, H * s), Image.NEAREST)
        board.alpha_composite(scaled)
        preview.alpha_composite(board, (x_off, 0))
        x_off += W * s + 8

    # 4x4 tile swatch at 2x scale, to check seams read as continuous stone.
    tiled = Image.new("RGBA", (W * 4 * 2, H * 4 * 2))
    for ty in range(4):
        for tx in range(4):
            scaled_tile = img.resize((W * 2, H * 2), Image.NEAREST)
            tiled.paste(scaled_tile, (tx * W * 2, ty * H * 2))
    preview.alpha_composite(tiled, (x_off, 0))

    return preview


def main():
    ASSETS.joinpath("block").mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_stone_texture()
    out_path = ASSETS / "block" / "prismium_stone.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_stone.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
