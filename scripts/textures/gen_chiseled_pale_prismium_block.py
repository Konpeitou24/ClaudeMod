#!/usr/bin/env python3
"""Generate block/chiseled_pale_prismium_block.png (scheduled session,
2026-09-18): a decorative masonry variant for Pale Prismium Block,
extending the "plain block gets a chiseled/carved sibling" pattern
already established for Prismium Block (session 34) and Prismium Core
(session 37) to the Pale Prismium family. Pale Prismium Block already
has slab/wall/stairs building variants (session 78) but, like Prismium
Alloy Block, was still missing a chiseled detail block - see
gen_chiseled_prismium_alloy_block.py (done in the same session) for the
other half of that gap.

No new Java class or mechanic: same stats as PALE_PRISMIUM_BLOCK
(tool-gated, same hardness/resistance/sound/light), texture-only
variant exactly like its chiseled siblings.

Visual language: the same "carved panel" masonry structure (outer
outline ring, recessed inner ring 3px in, raised-bevel mid-tone band,
centered symmetric diamond rune) rebuilt with Pale Prismium Block's OWN
icy palette (PALE_* from gen_pale_prismium_block.py) instead of the
crystal-teal or alloy-metal palettes, with the diamond rune in the
family's own faint cyan PALE_ACCENT tone so it still reads as "Pale
Prismium, carved" rather than a re-tint of the other two chiseled
blocks. Deterministic (no RNG - every pixel placed explicitly).

Self-review: after writing, a 16x/8x/4x checkerboard preview is saved
under build/ and must be visually inspected (via the Read tool on a
copy in the outputs mount) for opacity/legibility before this is
treated as done.

Run from repo root: python3 scripts/textures/gen_chiseled_pale_prismium_block.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette (reused verbatim from gen_pale_prismium_block.py) -----------
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"
PALE_ACCENT = "#7EE6FF"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PALE_OUTLINE)
SHADOW = hexrgb(PALE_SHADOW)
BASE = hexrgb(PALE_BASE)
MID = hexrgb(PALE_MID)
HILITE = hexrgb(PALE_HILITE)
ACCENT = hexrgb(PALE_ACCENT)
# Darker variant of the accent for the diamond's edge pixels (the other
# two chiseled blocks use a distinct *_DARK constant for this; Pale
# Prismium Block's own gen script has no such constant, so derive one by
# blending the accent toward the outline tone rather than inventing an
# unrelated color).
ACCENT_DARK = tuple((a + o) // 2 for a, o in zip(ACCENT, OUTLINE))


def make_chiseled_pale_block():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    # Flat mid-tone field (flat/quiet vs. plain Pale Prismium Block's
    # diagonal band gradient + scattered sparkles, same plain-vs-
    # chiseled contrast as the other two chiseled blocks).
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*BASE, 255)

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
    for (x, y) in [(3, 3), (SIZE - 4, 3), (3, SIZE - 4), (SIZE - 4, SIZE - 4)]:
        px[x, y] = (*SHADOW, 255)

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

    # Centered diamond rune in the family's own faint cyan accent.
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

    # Single near-white glint pixel (PALE_HILITE), matching the icy
    # sparkle language of the plain block's own texture.
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

    img = make_chiseled_pale_block()
    out_path = out_dir / "chiseled_pale_prismium_block.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_chiseled_pale_prismium_block.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
