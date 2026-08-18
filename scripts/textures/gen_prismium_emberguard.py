#!/usr/bin/env python3
"""Generate the item icon for Prismium Emberguard (session 32), the
mod's second passive/always-on "just carry it" accessory - see
PrismiumEmberguardHandler's LivingDamageEvent listener (fire/lava
damage reduction while the item merely sits anywhere in inventory, no
equip slot, no right-click action).

Visual language: mirrors Prismium Featherstone's "stone + something
diagonal + small teal Prismium gem" composition (same pebble silhouette
reused from gen_prismium_featherstone.py, same GEM_RING/GEM_CORE/
GEM_GLINT palette for family identity) but recolored dark, charred
obsidian instead of pale stone, with a small flame lick standing in for
the feather - "an ember caught in cooled rock", echoing the fire-damage
-softening gameplay effect. Kept deliberately distinct from Featherstone
at a glance via a warm (orange/red) vs. cool (white/teal) palette split,
even though the silhouette layout is intentionally the same family
shape.

Self-review note: an early draft used the exact same diagonal band width
as the feather for the flame, which read as "a orange feather" rather
than "fire" once previewed at 4x - narrowed the flame's tip to a single
pixel and added a one-pixel flicker offshoot near the top so the
silhouette breaks from a straight taper and reads as flickering flame
instead of a rigid quill.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_emberguard.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#241512"

STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

FLAME_SHADOW = "#B23A1B"
FLAME_BASE = "#F0752B"
FLAME_HILITE = "#FFD966"

# Prismium crystal accent, reused verbatim from gen_prismium.py /
# gen_prismium_featherstone.py so this item still reads as part of the
# Prismium family at a glance despite the warm/cool palette swap.
GEM_RING = "#008282"
GEM_CORE = "#11BBB8"
GEM_GLINT = "#CAFDF9"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


C_OUTLINE = hexrgb(OUTLINE)
S_SHADOW = hexrgb(STONE_SHADOW)
S_BASE = hexrgb(STONE_BASE)
S_HILITE = hexrgb(STONE_HILITE)
F_SHADOW = hexrgb(FLAME_SHADOW)
F_BASE = hexrgb(FLAME_BASE)
F_HILITE = hexrgb(FLAME_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as Featherstone's pebble,
# reused verbatim so the two passive items read as a matched pair on a
# shelf/in a JEI grid, just recolored dark.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Flame silhouette: narrow, flickering diagonal tongue from upper-right
# down to the stone - flares slightly wider near its base (closer to
# the stone) and tapers to a single-pixel tip at the top, with one
# flicker offshoot breaking the taper (see self-review note).
FLAME_ROWS = {
    1: (9, 9),
    2: (9, 10),
    3: (8, 10),
    4: (8, 9),
    5: (7, 9),
    6: (7, 8),
    7: (6, 8),
    8: (5, 8),
    9: (5, 7),
}
# A single detached ember spark above the main tongue - the "flicker".
FLICKER_PT = (10, 0)
# Core column per row = the leftmost column (hottest, highlight tone).
CORE_COL = {y: x0 for y, (x0, x1) in FLAME_ROWS.items()}

# Gem: small diamond embedded in the stone, same position/shape as
# Featherstone's for family consistency.
GEM_CORE_PTS = {(7, 11), (8, 11)}
GEM_RING_PTS = {(7, 10), (8, 10), (6, 11), (9, 11), (7, 12), (8, 12)}
GEM_GLINT_PT = (7, 10)
GEM_RING_PTS -= GEM_CORE_PTS


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    flame_pts = set()
    for y, (x0, x1) in FLAME_ROWS.items():
        for x in range(x0, x1 + 1):
            flame_pts.add((x, y))
    flame_pts.add(FLICKER_PT)

    all_solid = stone_pts | flame_pts

    # 1px outline around the combined silhouette.
    outline_pts = set()
    for (x, y) in all_solid:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_solid and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))
    for (x, y) in outline_pts:
        px[x, y] = (*C_OUTLINE, 255)

    # Stone fill: darker at the edges, slightly lighter charred-brown
    # toward the center (never as bright as Featherstone's pale stone).
    for y, (x0, x1) in STONE_ROWS.items():
        width = x1 - x0
        for x in range(x0, x1 + 1):
            rel = (x - x0) / max(width, 1)
            if rel < 0.25 or rel > 0.85:
                color = S_SHADOW
            elif rel < 0.6:
                color = S_BASE
            else:
                color = S_HILITE
            px[x, y] = (*color, 255)

    # Flame fill: hottest (highlight) at the leading/left edge, base
    # tone in the middle, shadow tone along the trailing/right edge -
    # mirrors Featherstone's rachis-shadow/leading-highlight split but
    # with fire's warm palette.
    for y, (x0, x1) in FLAME_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == CORE_COL[y]:
                color = F_HILITE
            elif x == x1:
                color = F_SHADOW
            else:
                color = F_BASE
            px[x, y] = (*color, 255)
    px[FLICKER_PT] = (*F_HILITE, 255)

    # Gem punched into the stone last.
    for (x, y) in GEM_RING_PTS:
        if (x, y) in stone_pts:
            px[x, y] = (*G_RING, 255)
    for (x, y) in GEM_CORE_PTS:
        if (x, y) in stone_pts:
            px[x, y] = (*G_CORE, 255)
    if GEM_GLINT_PT in stone_pts:
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
    out_path = out_dir / "prismium_emberguard.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_emberguard.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
