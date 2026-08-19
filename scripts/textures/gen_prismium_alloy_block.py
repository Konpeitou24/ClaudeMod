#!/usr/bin/env python3
"""Generate the block texture for Prismium Alloy Block (session 70,
scheduled - see ModBlocks.PRISMIUM_ALLOY_BLOCK / PROGRESS.md).

Compact-storage block for Prismium Alloy Ingot (see
gen_prismium_alloy_ingot.py), the mod's second refined-material item -
the same role PRISMIUM_BLOCK plays for Prismium Shard. Deliberately
echoes the user-hand-drawn block/prismium_block.png's established
visual language (diagonal light/dark banding across the whole face,
four small magenta accent chips near the corners, a thin dark-teal
outline hugging the edges) rather than inventing a new block-texture
style, so the two storage blocks read as an obvious pair on a shelf -
only the body palette changes, from Prismium Block's bright cyan/teal
crystal gradient to Prismium Alloy Ingot's cooler steel-blue/platinum
metal gradient (same METAL_* colors as gen_prismium_alloy_ingot.py),
matching the ingot -> block relationship the mod's original
Shard -> Prismium Block pair already established.

Self-review performed on first generation (16x16, 8x/16x upscaled
previews copied to the outputs mount and viewed with Read): confirm
the diagonal banding reads as a subtle metallic texture rather than
noise at small scale, confirm the four corner accent chips are visible
but not overpowering, confirm every filled pixel is fully opaque.

Run from repo root: python3 scripts/textures/gen_prismium_alloy_block.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/block/prismium_alloy_block.png"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_ACCENT = "#D633B0"
PRISMIUM_ACCENT_DARK = "#8A1E73"

# Same cool steel-blue/platinum metal palette as gen_prismium_alloy_ingot.py.
METAL_SHADOW = "#3E4E5C"
METAL_BASE = "#7C93A2"
METAL_MID = "#A6BAC5"
METAL_HILITE = "#EAF3F7"

# Four small accent chips near the corners, echoing
# block/prismium_block.png's own corner-chip placement.
CORNER_CHIPS = [
    (1, 1), (2, 1), (1, 2),
    (13, 1), (14, 1), (13, 2),
    (1, 13), (2, 13), (1, 14),
    (13, 13), (14, 13), (13, 14),
]


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def build():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(METAL_SHADOW)
    base = hexrgb(METAL_BASE)
    mid = hexrgb(METAL_MID)
    hilite = hexrgb(METAL_HILITE)
    accent = hexrgb(PRISMIUM_ACCENT)
    accent_dark = hexrgb(PRISMIUM_ACCENT_DARK)

    # Diagonal 4-tone banding across the whole face (x+y mod 4), same
    # "cut, cast metal panel" idea as the original block texture's
    # diagonal cyan/teal bands.
    band_colors = [shadow, base, mid, hilite]
    for y in range(SIZE):
        for x in range(SIZE):
            band = (x + y) % 4
            px[x, y] = (*band_colors[band], 255)

    # Four corner accent chips (3px each, a small square + one extra
    # pixel), darker on the outer pixel for a touch of depth.
    for (x, y) in CORNER_CHIPS:
        px[x, y] = (*accent, 255)
    # Darken one pixel per corner cluster for depth (the outermost).
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*accent_dark, 255)

    # Thin outline hugging the full edge, matching every other Prismium
    # block/machine texture in this mod.
    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = build()
    img.save(OUT_PATH)
    print(f"Wrote {OUT_PATH}")

    bad = [(x, y, img.getpixel((x, y))) for y in range(SIZE) for x in range(SIZE)
           if img.getpixel((x, y))[3] not in (0, 255)]
    if bad:
        print(f"WARNING: {len(bad)} pixels with partial alpha: {bad[:5]}")
    else:
        print("OK: all pixels fully opaque or fully transparent")


if __name__ == "__main__":
    main()
