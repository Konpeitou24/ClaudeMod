#!/usr/bin/env python3
"""Generate the item icon for Prismium Magnet Charm (session 65), the
mod's fourth purely passive accessory - see PrismiumMagnetCharmItem's
javadoc for the gameplay side (pulls nearby dropped items toward the
carrying player, no equip slot needed).

Visual language: a classic horseshoe/toy magnet silhouette (a "U" shape
with red and blue pole tips) - the most immediately-recognizable "magnet"
shape available, chosen deliberately over anything more abstract so the
icon reads correctly even at inventory-slot size with no prior context.
A small Prismium crystal gem (reusing GEM_RING/GEM_CORE/GEM_GLINT
verbatim from gen_prismium.py, same values gen_prismium_featherstone.py
and gen_prismium_emberguard.py already reuse) is embedded in the top
curve, keeping this item legible as part of the mod's existing passive-
accessory family at a glance rather than reading as a generic vanilla-
style magnet.

Deterministic (no RNG - every pixel is placed explicitly, following the
same row/segment-range + auto-outline technique gen_prismium_featherstone.py
established). Run from repo root:
    python3 scripts/textures/gen_prismium_magnet_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#2B3336"

STEEL_SHADOW = "#5A6268"
STEEL_BASE = "#8B949B"
STEEL_HILITE = "#C7CFD4"

RED_SHADOW = "#7A1414"
RED_BASE = "#C22626"
RED_HILITE = "#F05A5A"

BLUE_SHADOW = "#12376B"
BLUE_BASE = "#2461B8"
BLUE_HILITE = "#5FA0F0"

# Prismium crystal accent, reused verbatim from gen_prismium.py (same
# values gen_prismium_featherstone.py already copies) so this item still
# reads as part of the Prismium family at a glance.
GEM_RING = "#008282"
GEM_CORE = "#11BBB8"
GEM_GLINT = "#CAFDF9"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


C_OUTLINE = hexrgb(OUTLINE)
ST_SHADOW = hexrgb(STEEL_SHADOW)
ST_BASE = hexrgb(STEEL_BASE)
ST_HILITE = hexrgb(STEEL_HILITE)
R_SHADOW = hexrgb(RED_SHADOW)
R_BASE = hexrgb(RED_BASE)
R_HILITE = hexrgb(RED_HILITE)
B_SHADOW = hexrgb(BLUE_SHADOW)
B_BASE = hexrgb(BLUE_BASE)
B_HILITE = hexrgb(BLUE_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Horseshoe silhouette, defined per-row as a list of inclusive (x0, x1)
# segments (rows 4-12 have two disjoint segments - the left/right arms -
# with a hollow gap between them, unlike gen_prismium_featherstone.py's
# single-range-per-row shapes).
#   rows 1-3: the rounded top cap connecting both arms (progressively
#             wider going down, for a rounded-dome silhouette).
#   rows 4-9: the two arms, steel-colored, gap between them transparent.
#   rows 10-12: the same two arms, recolored as the magnet's poles
#               (red = left, blue = right).
CAP_ROWS = {
    1: [(6, 9)],
    2: [(5, 10)],
    3: [(4, 11)],
}
ARM_SEGMENTS = [(4, 6), (9, 11)]
STEEL_ARM_ROWS = [4, 5, 6, 7, 8, 9]
POLE_ROWS = [10, 11, 12]

# Small gem embedded in the top cap (row 2-3, the two center columns
# that would otherwise be steel) - same 2x2 diamond-ish arrangement
# gen_prismium_featherstone.py uses (ring pixels + core pixels + one
# glint pixel brighter than the rest).
GEM_RING_PTS = {(7, 2), (8, 3)}
GEM_CORE_PTS = {(8, 2), (7, 3)}
GEM_GLINT_PT = (7, 2)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def shade_for_segment(x, x0, x1, shadow, base, hilite):
    width = x1 - x0
    rel = (x - x0) / max(width, 1)
    if rel < 0.3:
        return shadow
    if rel < 0.75:
        return base
    return hilite


def make_icon():
    img = new_img()
    px = img.load()

    solid = set()
    for y, segs in CAP_ROWS.items():
        for (x0, x1) in segs:
            for x in range(x0, x1 + 1):
                solid.add((x, y))
    for y in STEEL_ARM_ROWS:
        for (x0, x1) in ARM_SEGMENTS:
            for x in range(x0, x1 + 1):
                solid.add((x, y))
    for y in POLE_ROWS:
        for (x0, x1) in ARM_SEGMENTS:
            for x in range(x0, x1 + 1):
                solid.add((x, y))

    # 1px outline around the combined silhouette (same technique as
    # gen_prismium_featherstone.py).
    outline_pts = set()
    for (x, y) in solid:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in solid and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))
    for (x, y) in outline_pts:
        px[x, y] = (*C_OUTLINE, 255)

    # Cap fill (steel, gradient per segment).
    for y, segs in CAP_ROWS.items():
        for (x0, x1) in segs:
            for x in range(x0, x1 + 1):
                color = shade_for_segment(x, x0, x1, ST_SHADOW, ST_BASE, ST_HILITE)
                px[x, y] = (*color, 255)

    # Arm fill (steel, gradient per arm segment).
    for y in STEEL_ARM_ROWS:
        for (x0, x1) in ARM_SEGMENTS:
            for x in range(x0, x1 + 1):
                color = shade_for_segment(x, x0, x1, ST_SHADOW, ST_BASE, ST_HILITE)
                px[x, y] = (*color, 255)

    # Pole fill: left arm segment -> red, right arm segment -> blue.
    left_seg, right_seg = ARM_SEGMENTS
    for y in POLE_ROWS:
        x0, x1 = left_seg
        for x in range(x0, x1 + 1):
            color = shade_for_segment(x, x0, x1, R_SHADOW, R_BASE, R_HILITE)
            px[x, y] = (*color, 255)
        x0, x1 = right_seg
        for x in range(x0, x1 + 1):
            color = shade_for_segment(x, x0, x1, B_SHADOW, B_BASE, B_HILITE)
            px[x, y] = (*color, 255)

    # Gem punched into the cap last.
    for (x, y) in GEM_RING_PTS:
        if (x, y) in solid:
            px[x, y] = (*G_RING, 255)
    for (x, y) in GEM_CORE_PTS:
        if (x, y) in solid:
            px[x, y] = (*G_CORE, 255)
    if GEM_GLINT_PT in solid:
        px[GEM_GLINT_PT] = (*G_GLINT, 255)

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
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_icon()
    out_path = out_dir / "prismium_magnet_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_magnet_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
