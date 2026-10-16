#!/usr/bin/env python3
"""Generate the item icon for Prismium Satiation Charm (scheduled
session, 2026-10-16), the mod's twelfth passive/always-on "just carry
it" accessory - see PrismiumSatiationCharmHandler's
TickEvent.PlayerTickEvent listener (permanent Saturation I while the
item merely sits anywhere in inventory, no equip slot, no right-click
action). The fifth charm in the family whose effect is not a
damage-type mitigator, following PrismiumHasteCharmItem,
PrismiumLuckCharmItem, PrismiumAqualungCharmItem and
PrismiumLeapCharmItem's precedent.

Visual language: mirrors Featherstone/Emberguard/Vitastone/Aegis/
Frostguard/Antivenom/Haste/Luck/Aqualung/Leap's "stone + something
diagonal/central + small teal Prismium gem" composition (same pebble
silhouette reused verbatim, same GEM_RING/GEM_CORE/GEM_GLINT palette
for family identity) with a tapering wheat-ear motif (a central stalk
with alternating left/right grain nubs, like a stylized ear of wheat)
standing in for the shield/feather/flame/heart/bolt/droplet/sparkle/
bubbles/chevrons - a wheat ear is a universally legible "food/harvest/
satiation" symbol, and its vertical-stalk-with-alternating-side-nubs
silhouette is distinct from every other charm's motif in this family
(no other charm uses a single central spine with symmetric side
branches). A fresh golden-wheat accent palette, deliberately a
different hue from Haste's amber/gold lightning (hue ~39) and Luck's
yellow-green sparkle (hue ~79) by sitting in the gap between them
(hue ~54-58) so none of the three read as the same color at hotbar
scale.

Self-review note (shape choice): the first instinct was a simple
"plate of food" glyph, but at 16x16 a plate silhouette reads as a
featureless blob once outlined and is easily confused with the stone
pebble itself. A wheat ear's alternating side-nubs give it a
distinctive, non-blob silhouette even at icon scale, and ties directly
to the "never go hungry" theme of the effect (Saturation) rather than a
generic food icon.

Self-review note (color choice): tabulated every existing charm's
accent hue before picking this one (red ~0, orange ~16, amber ~39,
yellow-green ~79, green ~137, cyan-blue ~198, blue ~212/225, purple
~258, magenta ~290-300, rose-pink ~338) and chose the one clear unused
gap between amber (~39) and yellow-green (~79): a golden-wheat hue of
~54-58, roughly equidistant from both neighbors (about 15-25 degrees of
separation either way) and thematically apt (wheat/bread are
golden-yellow in real life).

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_satiation_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#241512"

# Same charred-rock tones as the rest of the charm family's stone,
# reused verbatim - the stone itself is not the "hero" element on any
# of these twelve items, so keeping it identical across all of them
# keeps the family read tight and puts all the visual distinction into
# the accent motif/palette.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Golden-wheat "grain ear" accent - deliberately a different hue from
# Haste's amber/gold and Luck's yellow-green (see module docstring's
# self-review note on color choice).
WHEAT_SHADOW = "#8C7A0A"
WHEAT_BASE = "#E0CC22"
WHEAT_HILITE = "#FAF078"

# Prismium crystal accent, reused verbatim from the rest of the charm
# family so this item still reads as part of the Prismium family at a
# glance despite the golden-wheat palette.
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
W_SHADOW = hexrgb(WHEAT_SHADOW)
W_BASE = hexrgb(WHEAT_BASE)
W_HILITE = hexrgb(WHEAT_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as the rest of the charm
# family's pebble, reused verbatim so all twelve passive items read as
# a matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# A tapering wheat ear: a two-pixel-wide central stalk (x=7 hilite /
# x=8 shadow) running rows 1-7, with a single-pixel grain nub
# alternating left then right off the stalk at rows 2, 4 and 6 (three
# pairs of nubs, narrowing in effect toward the tip since the topmost
# row is the bare stalk tip alone) - a clearly different silhouette
# from every other charm's motif in this family (no other motif is a
# single spine with symmetric side branches).
STALK_PIXELS = {
    (7, 1): W_HILITE,
    (7, 2): W_HILITE, (8, 2): W_SHADOW,
    (6, 3): W_BASE,
    (7, 3): W_HILITE, (8, 3): W_SHADOW,
    (9, 3): W_BASE,
    (7, 4): W_HILITE, (8, 4): W_SHADOW,
    (5, 5): W_BASE,
    (7, 5): W_HILITE, (8, 5): W_SHADOW,
    (10, 5): W_BASE,
    (7, 6): W_HILITE, (8, 6): W_SHADOW,
    (6, 7): W_BASE,
    (7, 7): W_HILITE, (8, 7): W_SHADOW,
    (9, 7): W_BASE,
}

# Two detached single-pixel "fallen grain" dots flanking the stalk's
# base row - loose grains at the foot of the ear, echoing Leap's
# detached "liftoff" dashes/Haste's detached spark dashes for this
# family.
DASH_PTS = {(3, 8), (12, 8)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    stalk_pts = set(STALK_PIXELS.keys())
    accent_pts = stalk_pts | DASH_PTS

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. The stalk and the two
    # dash dots are each their own detached chunk, so each gets its own
    # independent outline ring - same reasoning as Aqualung's bubble
    # cluster/Haste's spark dashes/Leap's chevron legs.
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

    # Stalk fill: colors given explicitly per pixel above (center column
    # split hilite/shadow, side nubs a mid-tone base), since the nubs are
    # single detached pixels rather than a contiguous span.
    for (x, y), color in STALK_PIXELS.items():
        px[x, y] = (*color, 255)

    # Fallen-grain dashes: bright highlight dots, same treatment as the
    # rest of the family's detached accent pixels.
    for (x, y) in DASH_PTS:
        px[x, y] = (*W_HILITE, 255)

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
    out_path = out_dir / "prismium_satiation_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_satiation_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
