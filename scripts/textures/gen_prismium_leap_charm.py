#!/usr/bin/env python3
"""Generate the item icon for Prismium Leap Charm (scheduled session,
2026-10-15), the mod's eleventh passive/always-on "just carry it"
accessory - see PrismiumLeapCharmHandler's TickEvent.PlayerTickEvent
listener (permanent Jump Boost I while the item merely sits anywhere in
inventory, no equip slot, no right-click action). The fourth charm in
the family whose effect is not a damage-type mitigator, following
PrismiumHasteCharmItem, PrismiumLuckCharmItem and
PrismiumAqualungCharmItem's precedent.

Visual language: mirrors Featherstone/Emberguard/Vitastone/Aegis/
Frostguard/Antivenom/Haste/Luck/Aqualung's "stone + something diagonal/
central + small teal Prismium gem" composition (same pebble silhouette
reused verbatim, same GEM_RING/GEM_CORE/GEM_GLINT palette for family
identity) with two stacked upward chevrons ("^" over a larger "^",
like a jump-arc/level-up glyph) standing in for the shield/feather/
flame/heart/bolt/droplet/sparkle/bubbles - a pair of upward chevrons is
a universally legible "boost/rise/jump" symbol, distinct in silhouette
from every other charm's motif in this family (no other charm uses a
pointed, V-shaped accent). A fresh violet-magenta accent palette,
deliberately a different hue from Aegis's blue-leaning purple
(hue ~258) and Vitastone's red-leaning rose (hue ~338) by sitting in
the gap between them (hue ~290-300, true magenta) so none of the three
read as the same color at hotbar scale.

Self-review note (shape choice): the first instinct was a single coiled
spring (a zigzag squiggle), but at this icon's resolution a spring
squiggle and Haste's lightning bolt zigzag would be nearly
indistinguishable silhouettes once both are reduced to a handful of
offset pixels. Two stacked chevrons avoids that collision entirely -
a chevron's converging-diagonals-to-a-point shape reads as fundamentally
different from a bolt's offset zigzag bars, and doubling it (small over
large) reinforces "upward motion/growth" more clearly than a single
chevron would.

Self-review note (color choice): tabulated every existing charm's
accent hue before picking this one (red ~0, orange ~16, amber ~39,
yellow-green ~79, green ~137, cyan-blue ~198, blue ~212/225, purple
~258, rose-pink ~338) and chose the one clear unused gap, magenta
~290-300, straddling the purple/pink boundary without landing on
either - see the module docstring above.

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_leap_charm.py
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
# of these eleven items, so keeping it identical across all of them
# keeps the family read tight and puts all the visual distinction into
# the accent motif/palette.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Violet-magenta "chevron" accent - deliberately a different hue from
# Aegis's blue-leaning purple and Vitastone's red-leaning rose (see
# module docstring's self-review note on color choice).
LEAP_SHADOW = "#6B1B74"
LEAP_BASE = "#B82EC4"
LEAP_HILITE = "#F2A8FF"

# Prismium crystal accent, reused verbatim from the rest of the charm
# family so this item still reads as part of the Prismium family at a
# glance despite the magenta palette.
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
L_SHADOW = hexrgb(LEAP_SHADOW)
L_BASE = hexrgb(LEAP_BASE)
L_HILITE = hexrgb(LEAP_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as the rest of the charm
# family's pebble, reused verbatim so all eleven passive items read as
# a matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Two stacked upward chevrons ("^" over a bigger "^"): a small chevron
# (apex + two single-pixel legs, rows 1-3) above a larger chevron (apex
# + two double-pixel-thick legs, rows 5-8), separated by a blank gap
# row (row 4) so the two are unambiguously distinct shapes rather than
# one blurred zigzag - same "keep every tier visually separate" lesson
# Aqualung's bubble cluster and Haste's bolt/spark dashes already
# established for this family. Each row is colored explicitly below
# (left leg = highlight, right leg = shadow, consistent "lit from
# upper-left" convention), since a chevron's two disjoint diagonal legs
# per row can't be expressed as a single contiguous (x0, x1) span the
# way the stone/bolt/bubble rows above are.
CHEVRON_PIXELS = {
    # small top chevron
    (7, 1): L_HILITE, (8, 1): L_SHADOW,
    (6, 2): L_HILITE, (9, 2): L_SHADOW,
    (5, 3): L_HILITE, (10, 3): L_SHADOW,
    # row 4 deliberately blank - separates the two chevrons
    # large bottom chevron
    (7, 5): L_HILITE, (8, 5): L_SHADOW,
    (6, 6): L_HILITE, (9, 6): L_SHADOW,
    (5, 7): L_HILITE, (6, 7): L_BASE, (9, 7): L_BASE, (10, 7): L_SHADOW,
    (4, 8): L_HILITE, (5, 8): L_BASE, (10, 8): L_BASE, (11, 8): L_SHADOW,
}

# Two detached single-pixel "liftoff" dashes flanking the big chevron's
# base row (row 8) - kicked-up motion dust, echoing Haste's detached
# spark dashes/Aegis's detached burst-fragment dashes for this family.
DASH_PTS = {(2, 8), (13, 8)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    chevron_pts = set(CHEVRON_PIXELS.keys())
    accent_pts = chevron_pts | DASH_PTS

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. The two chevron legs
    # per row and the two dash dots are each their own detached chunk,
    # so each gets its own independent outline ring - same reasoning as
    # Aqualung's bubble cluster/Haste's spark dashes.
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

    # Chevron fill: colors given explicitly per pixel above (left leg =
    # highlight, right leg = shadow/base), since each row's two legs are
    # disjoint rather than a single contiguous span.
    for (x, y), color in CHEVRON_PIXELS.items():
        px[x, y] = (*color, 255)

    # Liftoff dashes: bright highlight dots, same treatment as the rest
    # of the family's detached accent pixels.
    for (x, y) in DASH_PTS:
        px[x, y] = (*L_HILITE, 255)

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
    out_path = out_dir / "prismium_leap_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_leap_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
