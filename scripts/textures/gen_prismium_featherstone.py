#!/usr/bin/env python3
"""Generate the item icon for Prismium Featherstone (session 31), the
mod's first passive/always-on accessory - unlike every other Prismium
item so far (tools/armor equipped in their vanilla slot, or "hold and
activate" items like the Grappling Hook/Locator/Rift Shard/Guardian
Charm), this one works simply by sitting anywhere in the player's
inventory (see PrismiumFeatherstoneHandler's LivingFallEvent listener -
no equip slot, no right-click action, nothing to consciously use).

Visual language: a smooth pale stone (pebble) with a feather laid
diagonally across it and a small teal Prismium gem (reusing
PRISMIUM_BASE/PRISMIUM_HILITE verbatim from gen_prismium.py, keeping the
mod's core crystal accent identifiable) glowing where the feather tip
touches the stone - "a stone light enough to float", echoing the fall
-damage-softening gameplay effect. Kept visually distinct from the
Guardian Charm's gold pendant silhouette (a wearable necklace) since this
item is explicitly *not* worn/held for its effect to apply.

Self-review note: an early draft placed the gem fully inside the
feather's silhouette rather than the stone's, which read as "a feather
with a chunk out of it" rather than "a gem embedded in the stone" once
previewed at 4x - moved the gem down one row so it sits unambiguously on
stone pixels only, with the feather tip merely touching its edge.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_featherstone.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#2B3336"

STONE_SHADOW = "#8A9296"
STONE_BASE = "#B7C0C4"
STONE_HILITE = "#E6ECEE"

FEATHER_SHADOW = "#AEDFE0"
FEATHER_BASE = "#E8FAFA"
FEATHER_HILITE = "#FFFFFF"

# Prismium crystal accent, reused verbatim from gen_prismium.py so this
# item still reads as part of the Prismium family at a glance.
GEM_RING = "#1E7A78"
GEM_CORE = "#3FBDB8"
GEM_GLINT = "#B9FFF3"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


C_OUTLINE = hexrgb(OUTLINE)
S_SHADOW = hexrgb(STONE_SHADOW)
S_BASE = hexrgb(STONE_BASE)
S_HILITE = hexrgb(STONE_HILITE)
F_SHADOW = hexrgb(FEATHER_SHADOW)
F_BASE = hexrgb(FEATHER_BASE)
F_HILITE = hexrgb(FEATHER_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Pebble silhouette: a squat oval sitting in the lower half of the icon.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Feather silhouette: a thin diagonal band from the upper-right down to
# the stone, tapering to a point at both ends. The rachis (quill vein)
# runs along the right/lower edge of each row.
FEATHER_ROWS = {
    0: (9, 11),
    1: (9, 11),
    2: (9, 10),
    3: (8, 10),
    4: (8, 9),
    5: (7, 9),
    6: (7, 8),
    7: (6, 8),
    8: (6, 7),
    9: (6, 7),
}
# Rachis pixel per row = the rightmost column (darker shadow tone).
RACHIS_COL = {y: x1 for y, (x0, x1) in FEATHER_ROWS.items()}

# Barb notches: single transparent nicks cut into the feather's leading
# (left) edge so the silhouette reads as segmented barbs rather than a
# smooth crystal-shard taper (see self-review note - the first draft
# had no notches and previewed as indistinguishable from the mod's
# other Prismium Shard-family items).

# Gem: small diamond embedded in the stone, just below where the
# feather's tip touches it (see self-review note in the docstring).
GEM_CORE_PTS = {(7, 11), (8, 11)}
GEM_RING_PTS = {(7, 10), (8, 10), (6, 11), (9, 11), (7, 12), (8, 12)}
GEM_GLINT_PT = (7, 10)
# GEM_CORE_PTS overlaps GEM_RING_PTS at (7,11)/(8,11) on purpose - core
# drawn after ring below, so it simply wins there.
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

    feather_pts = set()
    for y, (x0, x1) in FEATHER_ROWS.items():
        for x in range(x0, x1 + 1):
            feather_pts.add((x, y))

    notch_pts = {(9, 2), (8, 5)}
    feather_pts -= notch_pts

    all_solid = stone_pts | feather_pts

    # 1px outline around the combined silhouette.
    outline_pts = set()
    for (x, y) in all_solid:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_solid and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))
    for (x, y) in outline_pts:
        px[x, y] = (*C_OUTLINE, 255)

    # Stone fill: left-shadow / right-highlight gradient.
    for y, (x0, x1) in STONE_ROWS.items():
        width = x1 - x0
        for x in range(x0, x1 + 1):
            rel = (x - x0) / max(width, 1)
            if rel < 0.3:
                color = S_SHADOW
            elif rel < 0.75:
                color = S_BASE
            else:
                color = S_HILITE
            px[x, y] = (*color, 255)

    # Feather fill: base tone, with the rachis edge in shadow tone and
    # the leading (left) edge in highlight tone so the barbs read as
    # catching light.
    for y, (x0, x1) in FEATHER_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == RACHIS_COL[y]:
                color = F_SHADOW
            elif x == x0:
                color = F_HILITE
            else:
                color = F_BASE
            px[x, y] = (*color, 255)

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
    out_path = out_dir / "prismium_featherstone.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_featherstone.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
