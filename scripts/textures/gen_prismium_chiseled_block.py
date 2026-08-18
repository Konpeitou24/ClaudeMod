#!/usr/bin/env python3
"""Generate the block texture for Chiseled Prismium Block (session 34).

The mod's first purely decorative masonry variant - no new mechanics or
block class, just a second look for Prismium Block so builders have a
detail texture, mirroring vanilla's stone_bricks/chiseled_stone_bricks
pairing. Reuses the exact PRISMIUM_* palette from gen_prismium.py so it
reads unambiguously as "the same material, cut differently" rather than a
new mineral.

Visual language: a carved stone panel - a 1px darker frame (masonry
border, like chiseled stone bricks' border) around a flat mid-tone field,
with a centered diamond "rune" motif in the violet accent color (echoing
the tiny energy flecks scattered across the plain Prismium Block texture,
but here deliberately arranged/symmetric instead of scattered - that
contrast is what should read as "chiseled" vs "rough" at a glance) and a
single teal glint pixel for a bit of sparkle.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_chiseled_block.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette (reused verbatim from gen_prismium.py) ----------------------
PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_SHADOW = "#1E7A78"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"
PRISMIUM_ACCENT = "#C97BFF"
PRISMIUM_ACCENT_DARK = "#7A3FA6"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
SHADOW = hexrgb(PRISMIUM_SHADOW)
BASE = hexrgb(PRISMIUM_BASE)
MID = hexrgb(PRISMIUM_MID)
HILITE = hexrgb(PRISMIUM_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_DARK = hexrgb(PRISMIUM_ACCENT_DARK)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_chiseled_block():
    img = new_img()
    px = img.load()

    # Flat mid-tone field fill (deliberately flat/quiet, unlike the
    # diagonal-gradient facet look of plain Prismium Block, so the two
    # textures don't read as near-duplicates side by side).
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*BASE, 255)

    # Outer masonry border: darker outline ring at the very edge, plus a
    # second darker ring 3px in to suggest a recessed panel (mirrors
    # vanilla chiseled_stone_bricks' framed-panel look).
    for x in range(SIZE):
        px[x, 0] = (*OUTLINE, 255)
        px[x, SIZE - 1] = (*OUTLINE, 255)
    for y in range(SIZE):
        px[0, y] = (*OUTLINE, 255)
        px[SIZE - 1, y] = (*OUTLINE, 255)

    for i in range(3, SIZE - 3):
        px[i, 3] = (*SHADOW, 255)
        px[i, SIZE - 4] = (*SHADOW, 255)
        px[3, i] = (*SHADOW, 255)
        px[SIZE - 4, i] = (*SHADOW, 255)
    # corners of the inner frame
    for (x, y) in [(3, 3), (SIZE - 4, 3), (3, SIZE - 4), (SIZE - 4, SIZE - 4)]:
        px[x, y] = (*SHADOW, 255)

    # Between the two rings: slightly lighter tone so the frame reads as
    # a raised bevel rather than a flat outline.
    for y in range(1, 3):
        for x in range(1, SIZE - 1):
            px[x, y] = (*MID, 255)
    for y in range(SIZE - 3, SIZE - 1):
        for x in range(1, SIZE - 1):
            px[x, y] = (*MID, 255)
    for x in range(1, 3):
        for y in range(3, SIZE - 3):
            px[x, y] = (*MID, 255)
    for x in range(SIZE - 3, SIZE - 1):
        for y in range(3, SIZE - 3):
            px[x, y] = (*MID, 255)

    # Centered diamond "rune" motif (violet accent, symmetric - the
    # "chiseled/carved" contrast to plain Prismium Block's scattered
    # flecks). Diamond spans rows 5-10, columns 5-10, widest at the
    # middle row.
    diamond_rows = {
        5: (7, 8),
        6: (6, 9),
        7: (5, 10),
        8: (5, 10),
        9: (6, 9),
        10: (7, 8),
    }
    for y, (x0, x1) in diamond_rows.items():
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1)
            px[x, y] = (*(ACCENT_DARK if edge else ACCENT), 255)

    # Single glint pixel, slightly off-center in the diamond's upper-left
    # quadrant, in the teal hilite tone (ties back to the crystal family
    # rather than reading as a pure violet gem).
    px[6, 7] = (*HILITE, 255)

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


def main():
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_chiseled_block()
    out_path = out_dir / "chiseled_prismium_block.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_chiseled_prismium_block.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
