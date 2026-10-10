#!/usr/bin/env python3
"""Generate the item icon for Prismium Haste Charm (scheduled session,
2026-10-10), the mod's eighth passive/always-on "just carry it"
accessory - see PrismiumHasteCharmHandler's TickEvent.PlayerTickEvent
listener (permanent Haste I while the item merely sits anywhere in
inventory, no equip slot, no right-click action). Unlike the other
seven charms in this family, this one is not a damage-type mitigator -
see PrismiumHasteCharmItem's javadoc for why.

Visual language: mirrors Featherstone/Emberguard/Vitastone/Aegis's
"stone + something diagonal + small teal Prismium gem" composition
(same pebble silhouette reused verbatim, same GEM_RING/GEM_CORE/
GEM_GLINT palette for family identity) with a small pixel lightning
bolt (the standard zigzag pixel-art bolt silhouette) standing in for
the shield/feather/flame/heart - a bolt is a universally legible
"speed/haste" symbol, and reads unambiguously even at hotbar scale,
which a literal tiny pickaxe silhouette (tried first, see self-review
note) did not. Gold/amber accent palette, distinct from every other
charm's accent color, chosen to echo vanilla's own Haste effect icon
tone. Two detached single-pixel "spark" dashes flank the bolt's widest
row, echoing Aegis's detached burst-fragment dashes but reading as
electric sparks thrown off the bolt rather than debris.

Self-review note: the first draft used a small pixel pickaxe (curved
head + diagonal handle) instead of a bolt. At 4x/8x preview scale it
read as an ambiguous wishbone/antenna shape rather than a recognizable
tool, because a pickaxe's head needs more pixels than this icon's
8x8 accent budget to read correctly once combined with the stone
silhouette below it. Replaced it with a lightning bolt, a shape whose
zigzag silhouette stays legible even this small - the same lesson as
why every other charm in this family uses a simple, bold, single-motif
accent rather than something with fine detail.

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_haste_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#241512"

# Same charred-rock tones as Featherstone/Emberguard/Vitastone/Aegis's
# stone, reused verbatim - the stone itself is not the "hero" element
# on any of these eight items, so keeping it identical across all of
# them keeps the family read tight and puts all the visual distinction
# into the accent motif/palette.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Gold/amber "pickaxe" accent - deliberately distinct from every other
# charm's accent palette (violet Aegis, orange/red Emberguard, pink
# Vitastone, cool-white Featherstone, etc.) and chosen to echo vanilla's
# own Haste effect icon color.
HASTE_SHADOW = "#5C3A0A"
HASTE_BASE = "#C8900A"
HASTE_HILITE = "#FFDE7A"

# Prismium crystal accent, reused verbatim from the rest of the charm
# family so this item still reads as part of the Prismium family at a
# glance despite the gold palette.
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
H_SHADOW = hexrgb(HASTE_SHADOW)
H_BASE = hexrgb(HASTE_BASE)
H_HILITE = hexrgb(HASTE_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as the rest of the charm
# family's pebble, reused verbatim so all eight passive items read as
# a matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Lightning bolt silhouette: the standard pixel-art zigzag bolt (wide
# top bar, narrow waist, wide lower bar offset left, tapering tail) -
# see self-review note above for why this replaced an earlier pickaxe
# attempt.
BOLT_ROWS = {
    1: (8, 9),
    2: (7, 8),
    3: (6, 10),
    4: (8, 9),
    5: (7, 10),
    6: (7, 8),
    7: (6, 7),
    8: (5, 6),
}
# Two detached single-pixel "spark" dashes flanking the bolt's widest
# row (row 3) - electric sparks thrown off the bolt, not debris.
STREAK_PTS = {(4, 3), (12, 3)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    bolt_pts = set()
    for y, (x0, x1) in BOLT_ROWS.items():
        for x in range(x0, x1 + 1):
            bolt_pts.add((x, y))

    accent_pts = set(bolt_pts) | STREAK_PTS

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. Streak dashes sit 2px
    # away from the bolt, so they each get their own independent
    # outline ring - deliberate, same reasoning as Aegis's burst dashes.
    outline_pts = set()
    for (x, y) in all_solid:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_solid and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))
    for (x, y) in outline_pts:
        px[x, y] = (*C_OUTLINE, 255)

    # Stone fill: darker at the edges, slightly lighter charred-brown
    # toward the center - identical shading rule to the rest of the family.
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

    # Bolt fill: leftmost column of each row = highlight, rightmost =
    # shadow, everything else = base gold - same leading-highlight/
    # trailing-shadow rule the rest of the family uses on their
    # diagonal accents.
    for y, (x0, x1) in BOLT_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == x0:
                color = H_HILITE
            elif x == x1:
                color = H_SHADOW
            else:
                color = H_BASE
            px[x, y] = (*color, 255)

    # Streak dashes: bright highlight dots, same treatment as the rest
    # of the family's detached accent pixels.
    for (x, y) in STREAK_PTS:
        px[x, y] = (*H_HILITE, 255)

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
    out_path = out_dir / "prismium_haste_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_haste_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
