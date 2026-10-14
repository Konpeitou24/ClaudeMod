#!/usr/bin/env python3
"""Generate the item icon for Prismium Aqualung Charm (scheduled
session, 2026-10-14), the mod's tenth passive/always-on "just carry it"
accessory - see PrismiumAqualungCharmHandler's TickEvent.PlayerTickEvent
listener (permanent Water Breathing while the item merely sits anywhere
in inventory, no equip slot, no right-click action). The third charm in
the family whose effect is not a damage-type mitigator, following
PrismiumHasteCharmItem and PrismiumLuckCharmItem's precedent.

Visual language: mirrors Featherstone/Emberguard/Vitastone/Aegis/
Frostguard/Antivenom/Haste/Luck's "stone + something diagonal/central +
small teal Prismium gem" composition (same pebble silhouette reused
verbatim, same GEM_RING/GEM_CORE/GEM_GLINT palette for family identity)
with three small rising air bubbles standing in for the shield/feather/
flame/heart/bolt/sparkle - bubbles are a universally legible "breathing
underwater" symbol, distinct from every other charm's motif (in
particular Antivenom's single yellow-green droplet: three bubbles in a
diagonal rising cluster reads as a completely different silhouette). A
fresh deep royal-blue accent palette, deliberately darker/more
saturated than Frostguard's lighter icy cyan-blue ("#2E86AB" family) so
the two don't read as the same color at hotbar scale.

Self-review note (shape choice): the first instinct was a single
teardrop (matching Antivenom's droplet silhouette but in blue), but
that would collide visually with Antivenom at a glance despite the
different hue - the family's own standing lesson (see Luck's
self-review note on Antivenom vs Luck green) is to pick a motif shape,
not just a color, that no sibling charm already uses. Three rising
bubbles of decreasing size (large at the bottom-right, small at the
top-left) are instead unique in this family and reinforce the
"breathing/air" concept more directly than a single drop would.

Self-review note (bubble shapes): the first draft expressed the
smallest (topmost) bubble as a single stray pixel plus a separate
detached highlight dot above it. At 4x preview scale this read as two
unrelated specks rather than a bubble, and the medium bubble's own
single-pixel top/bottom rows (with no blank row separating it from the
small bubble above) made the whole cluster blur into one ambiguous
streak. Redrawn with a solid 2x2 square for the small bubble and an
explicit blank row of gap between every pair of bubbles, so all three
read as distinct circles even at the smallest preview size - the same
"keep every detached chunk unambiguously separate" lesson Aegis's burst
dashes and Haste's spark dashes already established for this family.

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_aqualung_charm.py
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
# of these ten items, so keeping it identical across all of them keeps
# the family read tight and puts all the visual distinction into the
# accent motif/palette.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Deep royal-blue "water" accent - deliberately distinct from
# Frostguard's lighter icy cyan-blue ("#2E86AB" base) by being darker,
# more saturated, and less cyan (closer to vanilla's own Water
# Breathing potion tint) so the two don't read as the same color at
# hotbar scale.
WATER_SHADOW = "#15295C"
WATER_BASE = "#2A4FB0"
WATER_HILITE = "#8FC7FF"

# Prismium crystal accent, reused verbatim from the rest of the charm
# family so this item still reads as part of the Prismium family at a
# glance despite the blue palette.
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
W_SHADOW = hexrgb(WATER_SHADOW)
W_BASE = hexrgb(WATER_BASE)
W_HILITE = hexrgb(WATER_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as the rest of the charm
# family's pebble, reused verbatim so all ten passive items read as a
# matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Three rising bubbles of decreasing size (large at bottom-right,
# small at top-left), each expressed as its own few rows, with a full
# empty row of gap between every pair so each bubble is unambiguously
# a separate detached circle rather than a single blobby shape - see
# self-review note above for why bubbles (not a single droplet) were
# chosen, and the second self-review note below for why the first
# attempt (a single stray pixel for the smallest bubble) was redrawn.
BUBBLE_ROWS = {
    0: (8, 9),
    1: (8, 9),
    3: (6, 6),
    4: (5, 7),
    5: (6, 6),
    6: (10, 11),
    7: (9, 12),
    8: (10, 11),
}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    bubble_pts = set()
    for y, (x0, x1) in BUBBLE_ROWS.items():
        for x in range(x0, x1 + 1):
            bubble_pts.add((x, y))

    accent_pts = set(bubble_pts)

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. The three bubbles and
    # the glint dot are each detached, so each gets its own independent
    # outline ring - same reasoning as Aegis's burst dashes/Haste's
    # spark dashes.
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

    # Bubble fill: leftmost column of each row = highlight, rightmost =
    # shadow, everything else = base blue - same leading-highlight/
    # trailing-shadow rule the rest of the family uses on their accents,
    # applied independently per bubble (rows never collide between the
    # three bubbles).
    for y, (x0, x1) in BUBBLE_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == x0:
                color = W_HILITE
            elif x == x1:
                color = W_SHADOW
            else:
                color = W_BASE
            px[x, y] = (*color, 255)

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
    out_path = out_dir / "prismium_aqualung_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_aqualung_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
