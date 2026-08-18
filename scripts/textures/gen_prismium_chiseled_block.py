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


def use_user_submitted_chiseled_block_texture():
    """Copy the user's hand-drawn Chiseled Prismium Block art in as the real asset.

    Same-day follow-up to the Chiseled Prismium Core art adoption
    (gen_prismium_chiseled_core.py's use_user_submitted_chiseled_core_texture(),
    session/commit b581f54): the user proposed reusing Chiseled Core's new
    concentric-ring frame for Chiseled Block too, but with the center gem
    recolored to magenta (Block's own accent color, echoing its corner
    gem clusters) instead of Core's white/light "glowing core" center -
    keeping the plain-vs-chiseled visual language consistent between the
    two families while still telling Block and Core apart by center hue.
    They then hand-drew the actual texture (not a simple palette-swap of
    the Core file - about 100 of 256 pixels differ from
    chiseled_prismium_core.png, mostly minor shading variation in the
    inner rings) and asked for it to replace make_chiseled_block()'s
    programmatic diamond-rune design. Converts to RGBA (source PNG is
    plain RGB) but does not otherwise touch a single pixel.
    """
    src = REPO_ROOT / "scripts/textures/reference/user_submitted_chiseled_prismium_block_2026-08-18.png"
    img = Image.open(src).convert("RGBA")
    assert img.size == (SIZE, SIZE), f"expected {SIZE}x{SIZE}, got {img.size}"
    out = ASSETS / "block/chiseled_prismium_block.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)} (from user-submitted art, not make_chiseled_block())")


def main():
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    use_user_submitted_chiseled_block_texture()
    img = Image.open(ASSETS / "block/chiseled_prismium_block.png")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_chiseled_prismium_block.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
