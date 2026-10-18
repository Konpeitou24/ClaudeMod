#!/usr/bin/env python3
"""Generate the item icon for Prismium Colossus Sigil Charm (scheduled
session, 2026-10-18), the mod's thirteenth passive/always-on "just
carry it" accessory - see PrismiumColossusSigilCharmHandler's
TickEvent.PlayerTickEvent listener (permanent Strength I while the
item merely sits anywhere in inventory, no equip slot, no right-click
action). The first charm in this family whose effect is combat offense
(Strength) rather than damage mitigation, detection, or a utility buff
- HANDOFF.md's 2026-10-17 note (option (x)) pointed out that Prismium
Colossus Core (the mod's first boss drop, v0.77.0/v0.78.0's Prismium
Colossus) had no crafting use at all yet, and that the Satiation Charm
(TODO47) had exhausted the family's "permanently apply an unused
vanilla buff" angle without a new direction - this item answers both
notes at once: a new Strength-themed effect direction, gated behind a
genuinely rare boss-drop ingredient so it reads as an actual trophy
reward rather than just another early-game pebble charm.

Visual language: deliberately keeps the family's "stone + diagonal
motif + small teal Prismium gem" composition (same pebble silhouette,
same GEM_RING/GEM_CORE/GEM_GLINT palette, reused verbatim) so this
still reads as part of the same charm family in a JEI grid - but
swaps the motif's accent palette to a dark violet/magenta pair lifted
directly from Prismium Colossus's own established colors (entity
texture + ForgeSpawnEggItem colors 0x0d0514/0xc34bf2, see ModItems.
PRISMIUM_COLOSSUS_SPAWN_EGG), a deliberate exception to the family's
usual "every charm gets its own unused hue" rule (PrismiumSatiationCharm
item's gen script tabulates the occupied hues) - this one is not meant
to read as a new independent color slot, it is meant to visually echo
the boss it comes from, the same way the spawn egg's own colors do.
The motif itself is a small upward-pointing arrow/rune (triangular
head tapering to a short stem) - a simple, unambiguous "power/boost"
glyph distinct from every other charm's silhouette in this family
(bolt/droplet/chevrons/wheat-ear/etc.), fitting both the "Sigil" name
and the Strength effect.

Self-review note (shape choice): a literal tiny fist/gauntlet was
considered first (most literal match for "Strength"), but a fist does
not reduce to a clean, unambiguous silhouette at 16x16 the way it does
at larger sizes - early sketches read as a shapeless blob once
outlined, the same problem Satiation's gen script flagged for a plain
food-plate glyph. An upward arrow keeps a crisp, legible outline at
icon scale and is a standard "boost/up" visual shorthand already
familiar from vanilla's own effect-icon arrows (Haste/Strength/
Jump Boost all show an upward chevron in their vanilla HUD icons).

Self-review note (color choice): confirmed the dark violet (~#2a0e33
range) / magenta (~#c34bf2) pair does not collide with Aegis's violet
accent (a cooler, more saturated blue-violet, hue ~258) or any other
existing charm hue at a glance - the Colossus palette sits closer to
true magenta (hue ~290) and is additionally distinguished by the dark,
near-black shadow tone no other charm in the family uses for its
accent shadow (every other charm's accent shadow is a darker shade of
its own hue, not a near-black).

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_colossus_sigil_charm.py
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
# reused verbatim - see gen_prismium_haste_charm.py's identical note.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Dark violet / magenta accent lifted directly from Prismium Colossus's
# own established palette (entity texture + spawn egg colors
# 0x0d0514/0xc34bf2) rather than a fresh hue slot - see module docstring.
SIGIL_SHADOW = "#1A0620"
SIGIL_BASE = "#7A1A8F"
SIGIL_HILITE = "#D978EC"

# Prismium crystal accent, reused verbatim from the rest of the charm
# family so this item still reads as part of the Prismium family at a
# glance despite the violet/magenta motif.
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
SG_SHADOW = hexrgb(SIGIL_SHADOW)
SG_BASE = hexrgb(SIGIL_BASE)
SG_HILITE = hexrgb(SIGIL_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as the rest of the charm
# family's pebble, reused verbatim so all thirteen passive items read
# as a matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Upward arrow/rune silhouette: a triangular head (rows 1-4, widening
# downward) tapering into a short stem (rows 5-8, width 3, centered) -
# see self-review note above for why this replaced an earlier fist
# attempt.
SIGIL_ROWS = {
    1: (8, 8),
    2: (7, 9),
    3: (6, 10),
    4: (5, 11),
    5: (7, 9),
    6: (7, 9),
    7: (7, 9),
    8: (7, 9),
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

    sigil_pts = set()
    for y, (x0, x1) in SIGIL_ROWS.items():
        for x in range(x0, x1 + 1):
            sigil_pts.add((x, y))

    all_solid = stone_pts | sigil_pts

    # 1px outline around the combined silhouette - identical rule to the
    # rest of the family.
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

    # Sigil fill: leftmost column of each row = highlight, rightmost =
    # shadow, everything else = base violet/magenta - same leading-
    # highlight/trailing-shadow rule the rest of the family uses on
    # their diagonal accents.
    for y, (x0, x1) in SIGIL_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == x0:
                color = SG_HILITE
            elif x == x1:
                color = SG_SHADOW
            else:
                color = SG_BASE
            px[x, y] = (*color, 255)

    # Gem punched into the stone last - identical coordinates to the
    # rest of the family.
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


def make_preview(img):
    s = 24
    checker_light = (70, 70, 70, 255)
    checker_dark = (55, 55, 55, 255)
    board = Image.new("RGBA", (SIZE * s, SIZE * s), (0, 0, 0, 0))
    bpx = board.load()
    for by in range(SIZE * s):
        for bx in range(SIZE * s):
            cx, cy = bx // s, by // s
            bpx[bx, by] = checker_light if (cx + cy) % 2 == 0 else checker_dark
    scaled = img.resize((SIZE * s, SIZE * s), Image.NEAREST)
    board.alpha_composite(scaled)
    return board


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_icon()
    out_path = out_dir / "prismium_colossus_sigil_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_colossus_sigil_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
