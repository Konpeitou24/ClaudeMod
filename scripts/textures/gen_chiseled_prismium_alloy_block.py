#!/usr/bin/env python3
"""Generate block/chiseled_prismium_alloy_block.png (scheduled session,
2026-09-18): a decorative masonry variant for Prismium Alloy Block,
extending the "plain block gets a chiseled/carved sibling" pattern
already established for Prismium Block (session 34,
gen_prismium_chiseled_block.py) and Prismium Core (session 37,
gen_prismium_chiseled_core.py) to the mod's third simple full block.
Prismium Alloy Block and Pale Prismium Block were the only two
BLOCKS.register("...", () -> new Block(...)) storage/decorative blocks
in ModBlocks.java that did NOT yet have a chiseled counterpart, despite
already having slab/wall/stairs building variants - this closes that
gap for the Alloy Block half (see gen_chiseled_pale_prismium_block.py
for the Pale half, done in the same session).

No new Java class or mechanic: same stats as PRISMIUM_ALLOY_BLOCK
(tool-gated, same hardness/resistance/sound/light), texture-only
variant exactly like its two chiseled siblings.

Visual language: reuses the "carved panel" masonry structure from
gen_prismium_chiseled_block.py's original programmatic design (outer
outline ring, a second recessed ring 3px in, a raised-bevel mid-tone
band between the two rings, and a centered symmetric diamond "rune"
motif) but rebuilt with Prismium Alloy Block's OWN palette - the cool
steel-blue/platinum METAL_* tones from gen_prismium_alloy_block.py for
the field/frame, and that same block's magenta PRISMIUM_ACCENT /
PRISMIUM_ACCENT_DARK for the diamond motif - so it reads as "the alloy
block, carved" rather than borrowing the crystal-teal chiseled blocks'
palette. Deterministic (no RNG - every pixel placed explicitly).

Self-review: after writing, a 16x/8x/4x checkerboard preview is saved
under build/ and must be visually inspected (via the Read tool on a
copy in the outputs mount) for opacity/legibility before this is
treated as done.

Run from repo root: python3 scripts/textures/gen_chiseled_prismium_alloy_block.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette (reused verbatim from gen_prismium_alloy_block.py) ----------
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_ACCENT = "#D633B0"
PRISMIUM_ACCENT_DARK = "#8A1E73"

METAL_SHADOW = "#3E4E5C"
METAL_BASE = "#7C93A2"
METAL_MID = "#A6BAC5"
METAL_HILITE = "#EAF3F7"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
SHADOW = hexrgb(METAL_SHADOW)
BASE = hexrgb(METAL_BASE)
MID = hexrgb(METAL_MID)
HILITE = hexrgb(METAL_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_DARK = hexrgb(PRISMIUM_ACCENT_DARK)


def make_chiseled_alloy_block():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    # Flat mid-tone metal field (flat/quiet, unlike the diagonal 4-tone
    # banding of plain Prismium Alloy Block, so the two don't read as
    # near-duplicates side by side - same contrast gen_prismium_chiseled
    # _block.py used against plain Prismium Block).
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*BASE, 255)

    # Outer masonry border.
    for x in range(SIZE):
        px[x, 0] = (*OUTLINE, 255)
        px[x, SIZE - 1] = (*OUTLINE, 255)
    for y in range(SIZE):
        px[0, y] = (*OUTLINE, 255)
        px[SIZE - 1, y] = (*OUTLINE, 255)

    # Inner recessed ring (3px in) using the darker metal shadow tone.
    for i in range(3, SIZE - 3):
        px[i, 3] = (*SHADOW, 255)
        px[i, SIZE - 4] = (*SHADOW, 255)
        px[3, i] = (*SHADOW, 255)
        px[SIZE - 4, i] = (*SHADOW, 255)
    for (x, y) in [(3, 3), (SIZE - 4, 3), (3, SIZE - 4), (SIZE - 4, SIZE - 4)]:
        px[x, y] = (*SHADOW, 255)

    # Raised-bevel band between the two rings, lighter metal tone.
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

    # Centered diamond "rune" motif in the block's own magenta accent
    # (echoes Prismium Alloy Block's four corner accent chips, but
    # arranged as one symmetric centered motif like the other two
    # chiseled blocks' rune, for the same plain-vs-chiseled contrast).
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

    # Single glint pixel in the metal hilite tone, tying the rune back
    # to the block's own metallic material instead of pure magenta.
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

    img = make_chiseled_alloy_block()
    out_path = out_dir / "chiseled_prismium_alloy_block.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_chiseled_prismium_alloy_block.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
