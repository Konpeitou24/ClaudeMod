#!/usr/bin/env python3
"""Generate the block texture for Prismium Chronoflame (scheduled session
#49) - see ModBlocks.PRISMIUM_CHRONOFLAME / PrismiumChronoflameBlock /
PROGRESS.md section 5 item 12(a)(ii).

Design goal: read at a glance as "a fixed shrine that controls time",
distinct from every existing lit block in the family (PRISMIUM_LANTERN's
metal cage lattice, the *_CORE family's smooth diagonal gradient). Reuses
two already-reviewed-safe techniques wholesale rather than inventing new
ones in a single unverified session:

  1. The exact same Chebyshev-distance "flat concentric glow bands"
     radial-core algorithm gen_prismium_lantern.py introduced (session 4)
     for the bright center -> cool edge falloff, since that technique is
     already confirmed to read clearly at 16x16 (PROGRESS.md session 4
     self-review notes) and to look good tiled across a full cube.
  2. The Prismium family's standard crystal palette constants
     (PRISMIUM_OUTLINE/SHADOW/BASE/MID/HILITE/CORE_WHITE/ACCENT), typed
     from gen_prismium_lantern.py's own copy rather than re-guessed.

New for this block only: a dark worked-stone surround (reusing
gen_prismium_stone.py's sampled grey shades so the "masonry" reads as the
same quarried material as PRISMIUM_STONE/the Prism Realm floor, not a new
unrelated grey), a ring of 12 small clock-tick marks in the ACCENT pink
around the glow core, and two thin off-center "hands" in CORE_WHITE
pointing from the center toward two of those ticks - the one new, clearly
legible shape this block needed to read as "clock", added deliberately
sparingly (a handful of pixels) rather than as a literal clock-face
illustration, matching the mod's established "restrained accents over
literal detail" texture philosophy (see gen_prismium_stone.py's own
docstring on the same point).

Self-review: writes a checkerboard-composited 1x/4x/8x preview plus a
2x2 tiled seam-check swatch to build/preview_prismium_chronoflame.png for
Read-based visual inspection, per the mod's texture workflow rules.
Deterministic (fixed seed). Run from repo root:
    python3 scripts/textures/gen_prismium_chronoflame.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ---------------------------------------------------------
# Same Prismium crystal family constants as gen_prismium_lantern.py.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#FF7CFC"

# Worked-stone surround: the same five sampled greys gen_prismium_stone.py
# pulled directly from prismium_ore.png (see that script's docstring for
# the sampling command), reused verbatim so this block's "masonry" reads
# as the same quarried material.
STONE_SHADES = [
    (118, 118, 118, 255),
    (121, 121, 121, 255),
    (130, 130, 130, 255),
    (140, 140, 140, 255),
    (143, 143, 143, 255),
]
STONE_DARK_EDGE = (96, 96, 96, 255)


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def make_chronoflame_texture():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    core_white = hexrgb(PRISMIUM_CORE_WHITE)
    accent = hexrgb(PRISMIUM_ACCENT)

    # 1. Worked-stone surround fills the whole tile first (deterministic
    # checkerboard-ish pick from the 5 shades by position, not RNG, so
    # this stays reproducible without needing a seeded Random instance).
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = STONE_SHADES[(x * 3 + y * 5) % len(STONE_SHADES)]
    # A handful of deterministic darker mortar specks.
    for (x, y) in [(1, 2), (14, 3), (2, 13), (13, 14), (4, 1), (12, 12)]:
        px[x, y] = STONE_DARK_EDGE

    # 2. Radial glow core, same flat concentric-band technique as
    # gen_prismium_lantern.py's make_prismium_lantern, but a smaller
    # radius (leaves the stone surround visible as a "housing" frame
    # around the glow, rather than filling the whole tile like the
    # Lantern does).
    cx, cy = 7.5, 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = max(abs(x - cx), abs(y - cy))
            if d < 1.5:
                px[x, y] = (*core_white, 255)
            elif d < 2.5:
                px[x, y] = (*hilite, 255)
            elif d < 3.5:
                px[x, y] = (*mid, 255)
            elif d < 4.5:
                px[x, y] = (*base, 255)
            elif d < 5.5:
                px[x, y] = (*shadow, 255)
            elif d < 6:
                px[x, y] = (*outline, 255)
            # d >= 6: leave the stone surround from step 1 untouched.

    # 3. Twelve clock ticks in ACCENT pink, one per hour position, placed
    # just outside the glow's outline ring (distance ~6-7 from center).
    # Listed explicitly (not computed via trig, to keep every placed
    # pixel deliberate/reviewable rather than relying on rounding).
    tick_points = [
        (7, 1), (8, 1),      # 12 o'clock
        (12, 3), (13, 4),    # ~2 o'clock
        (14, 7), (14, 8),    # 3 o'clock
        (12, 12), (13, 11),  # ~5 o'clock (mirrors 2 o'clock)
        (7, 14), (8, 14),    # 6 o'clock
        (2, 12), (3, 11),    # ~7 o'clock
        (1, 7), (1, 8),      # 9 o'clock
        (3, 4), (2, 3),      # ~10 o'clock
    ]
    for (x, y) in tick_points:
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*accent, 255)

    # 4. Two thin off-center "hands" in CORE_WHITE, pointing from just
    # past the core toward the 12 o'clock and ~2 o'clock ticks - the one
    # deliberately literal "clock" cue on an otherwise abstract glow.
    hand_points = [(7, 4), (7, 3), (9, 6), (10, 5)]
    for (x, y) in hand_points:
        px[x, y] = (*core_white, 255)

    return img


def make_preview(img, scales=(1, 4, 8)):
    tile = 2
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * SIZE for s in scales) + 8 * len(scales) + SIZE * 2 * 4
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

    # 2x2 tile swatch at 4x scale, to sanity-check seams (this block is a
    # cube_all so the same face repeats on all 6 sides, but seam
    # continuity still matters for e.g. a wall of them placed side by
    # side).
    tiled = Image.new("RGBA", (SIZE * 2 * 4, SIZE * 2 * 4))
    scaled_tile = img.resize((SIZE * 4, SIZE * 4), Image.NEAREST)
    for ty in range(2):
        for tx in range(2):
            tiled.paste(scaled_tile, (tx * SIZE * 4, ty * SIZE * 4))
    preview.alpha_composite(tiled, (x_off, 0))

    return preview


def main():
    ASSETS.joinpath("block").mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_chronoflame_texture()
    out_path = ASSETS / "block" / "prismium_chronoflame.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_chronoflame.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
