#!/usr/bin/env python3
"""Generate the item icon for Prismium Aegis Charm (scheduled session,
2026-09-19), the mod's fifth passive/always-on "just carry it"
accessory - see PrismiumAegisCharmHandler's LivingDamageEvent listener
(explosion damage reduction while the item merely sits anywhere in
inventory, no equip slot, no right-click action).

Visual language: mirrors Featherstone/Emberguard/Vitastone's "stone +
something diagonal + small teal Prismium gem" composition (same pebble
silhouette reused verbatim from gen_prismium_featherstone.py, same
GEM_RING/GEM_CORE/GEM_GLINT palette for family identity) with a small
pixel shield (rounded top tapering to a point, the same silhouette
grammar a Minecraft player already reads as "block/shield" from
vanilla's own Shield item icon) standing in for the feather/flame/
heart, plus two detached single-pixel "deflected fragment" dashes
flanking the shield's widest row - "a ward stone that shrugs off a
blast", echoing the explosion-damage-softening gameplay effect. Given
a fourth, distinct palette (violet/lavender) that none of
Featherstone (cool white/teal), Emberguard (warm orange/red), or
Vitastone (pink/magenta) use, so all four passive charms still read as
a family at a glance (identical stone + identical gem) while staying
individually distinguishable in a hotbar.

Self-review note: an early draft placed the two deflection dashes
touching the shield's own outline, which after a 4x preview read as
"the shield has two broken corners" rather than "fragments flying
away from it" - moved them out to be clearly detached (2px gap) from
the shield silhouette so the outline pass gives them their own ring
and they read as separate debris, not damage to the shield itself.

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_aegis_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#241512"

# Same charred-rock tones as Emberguard/Vitastone's stone, reused
# verbatim - the stone itself is not the "hero" element on any of
# these four items, so keeping it identical across all four keeps the
# family read tight and puts all the visual distinction into the
# accent motif/palette.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

AEGIS_SHADOW = "#3A1F6B"
AEGIS_BASE = "#6B3FD1"
AEGIS_HILITE = "#B79CFF"

# Prismium crystal accent, reused verbatim from gen_prismium.py /
# gen_prismium_featherstone.py / gen_prismium_emberguard.py /
# gen_prismium_vitastone.py so this item still reads as part of the
# Prismium family at a glance despite the violet palette.
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
A_SHADOW = hexrgb(AEGIS_SHADOW)
A_BASE = hexrgb(AEGIS_BASE)
A_HILITE = hexrgb(AEGIS_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as Featherstone/Emberguard/
# Vitastone's pebble, reused verbatim so all four passive items read
# as a matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Shield silhouette: rounded flat top tapering evenly to a single-pixel
# point that meets the stone - the same "block/shield" reading as
# vanilla's own Shield item icon, kept simple so it communicates
# "protection" at a glance in a hotbar slot.
SHIELD_ROWS = {
    1: (5, 10),
    2: (4, 11),
    3: (4, 11),
    4: (5, 10),
    5: (6, 9),
    6: (6, 9),
    7: (7, 8),
    8: (7, 8),
}
# Two detached single-pixel "deflected fragment" dashes flanking the
# shield's widest row (rows 2-3) - echoes Emberguard's detached flicker
# ember / Vitastone's detached shimmer spark, but placed as a pair to
# read as debris blown outward from an impact rather than a single
# sparkle.
BURST_PTS = {(1, 2), (14, 2)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    shield_pts = set()
    for y, (x0, x1) in SHIELD_ROWS.items():
        for x in range(x0, x1 + 1):
            shield_pts.add((x, y))

    accent_pts = set(shield_pts) | BURST_PTS

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. Burst dashes sit 2px
    # away from the shield, so they each get their own independent
    # outline ring - deliberate, see self-review note above.
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
    # Vitastone's stone.
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

    # Shield fill: leftmost column of each row = highlight, rightmost
    # = shadow, everything else = base violet - same leading-highlight/
    # trailing-shadow rule Featherstone/Emberguard/Vitastone use on
    # their diagonal accents.
    for y, (x0, x1) in SHIELD_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == x0:
                color = A_HILITE
            elif x == x1:
                color = A_SHADOW
            else:
                color = A_BASE
            px[x, y] = (*color, 255)

    # Burst dashes: bright highlight dots, same treatment as
    # Emberguard's flicker / Vitastone's shimmer.
    for (x, y) in BURST_PTS:
        px[x, y] = (*A_HILITE, 255)

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
    out_path = out_dir / "prismium_aegis_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_aegis_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
