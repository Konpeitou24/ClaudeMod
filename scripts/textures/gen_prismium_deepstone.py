#!/usr/bin/env python3
"""Generate the block texture for Prismium Deepstone (session 72,
GitHub issue #23 follow-up), the dark deep-layer fill block for the
Prism Realm's flat chunk generator (data/claudemod/dimension/prism_realm.json),
used below y=0 - the same "stone gets darker/denser once you go deep
enough" idea vanilla expresses with Stone -> Deepslate, requested
explicitly by the repo owner ("0付近より下の高さには、専用の深層岩を置いて
ほしいです" - "please place a dedicated deep-layer stone below around
y=0").

Palette: sampled directly from the shipped deepslate_prismium_ore.png
(16x16) the same way gen_prismium_stone.py sampled prismium_ore.png -
see that script's docstring for the one-off sampling command this reused
verbatim (only the source filename changed). That printed five dark-grey
shades as the block's own stone matrix (deepslate_prismium_ore.png's
non-crystal pixels) plus its small cyan crystal-vein cluster.

Unlike gen_prismium_stone.py's *original* version, this script does NOT
sprinkle in any of the ore's cyan/teal accent pixels - GitHub issue #22
("紛らわしいリソースパック") reported that exact "family resemblance"
technique made Prismium Stone hard to tell apart from Prismium Ore at a
glance, and gen_prismium_stone.py's own flecks were removed this same
session in direct response. Deepstone is written plain-dark-grey from
the start instead of repeating the mistake and having to walk it back
again.

Self-review: writes a checkerboard-composited preview (1x/4x/8x scale,
plus a 4x4 tiled swatch to check seam continuity) to
build/preview_prismium_deepstone.png for Read-based visual inspection,
matching the mod's established workflow. Deterministic (fixed seed).
Run from repo root:
    python3 scripts/textures/gen_prismium_deepstone.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260826
W, H = 16, 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Sampled directly from deepslate_prismium_ore.png's dominant colours
# (see docstring) - the block's own dark stone matrix, no crystal-vein
# colours included this time (see docstring for why).
DEEPSTONE_SHADES = [
    (80, 80, 84, 255),
    (76, 76, 80, 255),
    (69, 69, 72, 255),
    (62, 62, 65, 255),
    (57, 58, 60, 255),
]
DEEPSTONE_DARK_EDGE = (46, 46, 49, 255)  # a touch darker still, mortar-line only.


def make_deepstone_texture():
    rng = random.Random(SEED)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()

    for y in range(H):
        for x in range(W):
            px[x, y] = rng.choice(DEEPSTONE_SHADES)

    # Sparse darker "mortar" specks, same trick as gen_prismium_stone.py.
    for _ in range((W * H) // 12):
        x, y = rng.randrange(W), rng.randrange(H)
        px[x, y] = DEEPSTONE_DARK_EDGE

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

    img = make_deepstone_texture()
    out_path = ASSETS / "block" / "prismium_deepstone.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_deepstone.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
