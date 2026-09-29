#!/usr/bin/env python3
"""Generate block textures for Prismium Bricks / Cracked Prismium Bricks and
their Prismium Deepstone counterparts (scheduled session, 2026-09-29).

Context: the 2026-09-18 PROGRESS.md note explicitly parked Prismium
Stone/Deepstone out of the "Chiseled" decorative-block pattern that every
other plain block in the mod received, reasoning that these two blocks read
as quarried "stone material" and that a vanilla stone -> stone bricks style
cut (rather than a chiseled-slab decorative inlay) is the more natural next
step for them. This script is that follow-up: it adds the "quarried"
(Bricks) and "aged" (Cracked Bricks) variants for both Prismium Stone and
Prismium Deepstone, mirroring vanilla's own
stone -> stone_bricks -> cracked_stone_bricks family exactly (Cracked is
obtained by furnace-smelting the plain Bricks, same as vanilla).

Palette: reuses the exact, already-sampled shade lists from
gen_prismium_stone.py (STONE_SHADES/STONE_DARK_EDGE) and
gen_prismium_deepstone.py (DEEPSTONE_SHADES/DEEPSTONE_DARK_EDGE) rather than
re-deriving or guessing new colours, per the mod's "verify, don't guess"
texture practice - these four new blocks are meant to read as an obviously
related cut of the same stone, not a new material.

Brick layout: a simple 4-row running-bond pattern (each row 4px tall: 3px
of brick body + 1px mortar row), with a single vertical mortar seam per row
that alternates position every other row (offset by half the tile width),
the same "running bond" visual trick vanilla's own 16x16 stone_bricks.png
uses. Cracked variants start from the plain brick texture and additionally
draw a couple of short jagged dark crack lines across a subset of bricks
(same idea as vanilla cracked_stone_bricks - a few, not all, bricks show
wear).

Self-review: writes a checkerboard-composited preview (1x/4x/8x scale, plus
a 4x4 tiled swatch to check seam continuity) for each of the 4 textures to
build/preview_prismium_bricks_*.png for Read-based visual inspection,
matching the mod's established workflow. Deterministic (fixed seeds). Run
from repo root:
    python3 scripts/textures/gen_prismium_bricks.py
"""
import random
from pathlib import Path

from PIL import Image

W, H = 16, 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Sampled directly from prismium_ore.png via gen_prismium_stone.py - reused
# verbatim here, not re-typed from memory.
STONE_SHADES = [
    (118, 118, 118, 255),
    (121, 121, 121, 255),
    (130, 130, 130, 255),
    (140, 140, 140, 255),
    (143, 143, 143, 255),
]
STONE_MORTAR = (90, 90, 90, 255)  # a touch darker than gen_prismium_stone's
                                   # own mortar-speck colour, so the brick
                                   # seams read as deliberate lines rather
                                   # than the sparser random stone flecks.
STONE_CRACK = (58, 58, 58, 255)

# Sampled directly from deepslate_prismium_ore.png via
# gen_prismium_deepstone.py - reused verbatim here.
DEEPSTONE_SHADES = [
    (80, 80, 84, 255),
    (76, 76, 80, 255),
    (69, 69, 72, 255),
    (62, 62, 65, 255),
    (57, 58, 60, 255),
]
DEEPSTONE_MORTAR = (40, 40, 43, 255)
DEEPSTONE_CRACK = (24, 24, 26, 255)


def make_bricks_texture(seed, shades, mortar_color):
    """4 rows of running-bond bricks, each row 4px tall (3px body + 1px
    mortar row at the row's bottom edge), with one vertical seam per row
    alternating between x=8 (even rows) and x=4/x=12 (odd rows, i.e. offset
    by half the row's brick width) - the running-bond look."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()

    row_h = 4
    for row in range(H // row_h):
        y0 = row * row_h
        mortar_row_y = y0 + row_h - 1  # bottom pixel row of this course
        seam_xs = {8} if row % 2 == 0 else {4, 12}
        for y in range(y0, y0 + row_h):
            for x in range(W):
                if y == mortar_row_y or x in seam_xs:
                    px[x, y] = mortar_color
                else:
                    px[x, y] = rng.choice(shades)
    return img


def add_cracks(img, seed, crack_color):
    """Starting from a plain bricks texture, scratch a couple of short
    jagged crack lines across a subset of bricks - mirrors vanilla's
    cracked_stone_bricks treatment (wear on a few bricks, not all)."""
    rng = random.Random(seed)
    out = img.copy()
    px = out.load()

    # A small, fixed set of hand-picked jagged crack paths (relative to a
    # few of the brick cells), so cracks look like deliberate wear rather
    # than random static. Each path is a short list of (x, y) offsets that
    # get combined with a per-crack origin.
    crack_paths = [
        [(0, 0), (1, 1), (1, 2), (0, 3)],
        [(0, 0), (1, 0), (1, 1), (2, 2)],
        [(0, 0), (0, 1), (1, 2)],
    ]
    origins = [(1, 1), (9, 5), (5, 9), (11, 13)]
    rng.shuffle(origins)
    chosen_origins = origins[:3]  # a few bricks, not all

    for (ox, oy), path in zip(chosen_origins, crack_paths):
        for (dx, dy) in path:
            x, y = ox + dx, oy + dy
            if 0 <= x < W and 0 <= y < H:
                px[x, y] = crack_color
    return out


def make_preview(images_with_labels, scales=(1, 4, 8)):
    tile = 2
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * W for s in scales) + 8 * len(scales) + W * 4 * 4
    total_h = max(s * H for s in scales)

    previews = {}
    for label, img in images_with_labels:
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
        previews[label] = preview
    return previews


def main():
    ASSETS.joinpath("block").mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    bricks = make_bricks_texture(20260929, STONE_SHADES, STONE_MORTAR)
    cracked_bricks = add_cracks(bricks, 20260929, STONE_CRACK)
    deepstone_bricks = make_bricks_texture(20260930, DEEPSTONE_SHADES, DEEPSTONE_MORTAR)
    cracked_deepstone_bricks = add_cracks(deepstone_bricks, 20260930, DEEPSTONE_CRACK)

    outputs = [
        ("prismium_bricks", bricks),
        ("cracked_prismium_bricks", cracked_bricks),
        ("prismium_deepstone_bricks", deepstone_bricks),
        ("cracked_prismium_deepstone_bricks", cracked_deepstone_bricks),
    ]

    for name, img in outputs:
        out_path = ASSETS / "block" / f"{name}.png"
        img.save(out_path)
        print(f"Wrote {out_path}")
        alphas = set(img.getdata(3))
        print(f"  {name}: distinct alpha values present: {sorted(alphas)}")

    previews = make_preview(outputs)
    for name, preview in previews.items():
        preview_path = BUILD_DIR / f"preview_{name}.png"
        preview.save(preview_path)
        print(f"Wrote {preview_path}")


if __name__ == "__main__":
    main()
