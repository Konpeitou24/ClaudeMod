#!/usr/bin/env python3
"""Generate the item icon for Prismium Frostguard Charm (scheduled
session, 2026-09-20), the mod's sixth passive/always-on "just carry
it" accessory - see PrismiumFrostguardCharmHandler's LivingDamageEvent
listener (freeze/frostbite damage reduction while the item merely sits
anywhere in inventory, no equip slot, no right-click action).

Visual language: mirrors Featherstone/Emberguard/Vitastone/Aegis
Charm's "stone + something diagonal + small teal Prismium gem"
composition (same pebble silhouette reused verbatim), with a small
pixel icicle (wide jagged top tapering unevenly to a single-pixel
point, deliberately more irregular/jagged than Aegis Charm's smooth
shield taper to read as "ice", not "metal") standing in for the
feather/flame/heart/shield, plus two detached single-pixel "frost
sparkle" dots flanking the icicle's widest rows - echoing Aegis
Charm's deflected-fragment dashes but placed as isolated dots (rather
than a pair aligned to one row) to read as drifting snow rather than
debris blown outward from an impact.

Given a fifth, distinct palette (icy cyan-blue) that is deliberately
kept darker/more saturated than Featherstone's near-white/pale-teal
and further from Magnet Charm's brighter primary blue, so all five
passive charms with a "cool" accent (Featherstone, Magnet Charm,
Frostguard) still read as individually distinguishable in a hotbar
rather than blurring into "the blue ones".

Self-review note: an early mental draft considered a snowflake (6-arm
star) motif, but at 16x16 with only ~8 rows of vertical space a
readable 6-arm star needs more width than the pebble leaves available
without either shrinking to illegibility or overlapping the stone -
the icicle silhouette (already proven readable at this scale by
Aegis Charm's shield) reads more reliably small.

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_frostguard_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#241512"

# Same charred-rock tones as Emberguard/Vitastone/Aegis Charm's stone,
# reused verbatim - the stone itself is not the "hero" element on any
# of these items, so keeping it identical keeps the family read tight.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Icy cyan-blue, deliberately darker/more saturated than Featherstone's
# near-white/pale-teal and distinct from Magnet Charm's brighter
# primary blue.
FROST_SHADOW = "#1C4A63"
FROST_BASE = "#2E86AB"
FROST_HILITE = "#A8E6F0"

# Prismium crystal accent, reused verbatim from gen_prismium.py /
# gen_prismium_featherstone.py / gen_prismium_emberguard.py /
# gen_prismium_vitastone.py / gen_prismium_aegis_charm.py so this item
# still reads as part of the Prismium family at a glance.
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
F_SHADOW = hexrgb(FROST_SHADOW)
F_BASE = hexrgb(FROST_BASE)
F_HILITE = hexrgb(FROST_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as Featherstone/Emberguard/
# Vitastone/Aegis Charm's pebble, reused verbatim so all passive items
# read as a matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Icicle silhouette: wide, jagged top tapering unevenly to a single
# point - the asymmetric zigzag (rather than Aegis Charm's smooth
# even taper) is deliberate, communicating a rough ice shard rather
# than a manufactured shield.
ICICLE_ROWS = {
    1: (6, 9),
    2: (6, 9),
    3: (5, 10),
    4: (5, 9),
    5: (6, 9),
    6: (6, 8),
    7: (7, 8),
    8: (7, 7),
}
# Two detached single-pixel "frost sparkle" dots, placed at different
# rows (not aligned to one row like Aegis Charm's burst pair) to read
# as drifting snow rather than debris flung from an impact.
SPARKLE_PTS = {(2, 3), (13, 5)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    icicle_pts = set()
    for y, (x0, x1) in ICICLE_ROWS.items():
        for x in range(x0, x1 + 1):
            icicle_pts.add((x, y))

    accent_pts = set(icicle_pts) | SPARKLE_PTS

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. Sparkle dots sit at
    # least 2px away from the icicle, so they each get their own
    # independent outline ring - deliberate, see self-review note.
    outline_pts = set()
    for (x, y) in all_solid:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_solid and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))
    for (x, y) in outline_pts:
        px[x, y] = (*C_OUTLINE, 255)

    # Stone fill: darker at the edges, slightly lighter charred-brown
    # toward the center - identical shading rule to Emberguard/
    # Vitastone/Aegis Charm's stone.
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

    # Icicle fill: leftmost column of each row = highlight, rightmost
    # = shadow, everything else = base ice-blue - same leading-
    # highlight/trailing-shadow rule Featherstone/Emberguard/
    # Vitastone/Aegis Charm use on their diagonal accents.
    for y, (x0, x1) in ICICLE_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == x0:
                color = F_HILITE
            elif x == x1:
                color = F_SHADOW
            else:
                color = F_BASE
            px[x, y] = (*color, 255)

    # Sparkle dots: bright highlight dots, same treatment as
    # Emberguard's flicker / Vitastone's shimmer / Aegis Charm's burst.
    for (x, y) in SPARKLE_PTS:
        px[x, y] = (*F_HILITE, 255)

    # Gem punched into the stone last.
    gem_core_pts = {(7, 11), (8, 11)}
    gem_ring_pts = {(7, 10), (8, 10), (6, 11), (9, 11), (7, 12), (8, 12)}
    gem_ring_pts -= gem_core_pts
    gem_glint_pt = (7, 10)
    for (x, y) in gem_ring_pts:
        if (x, y) in stone_pts:
            px[x, y] = (*G_RING, 255)
    for (x, y) in gem_core_pts:
        if (x, y) in stone_pts:
            px[x, y] = (*G_CORE, 255)
    if gem_glint_pt in stone_pts:
        px[gem_glint_pt] = (*G_GLINT, 255)

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
    out_path = out_dir / "prismium_frostguard_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_frostguard_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
