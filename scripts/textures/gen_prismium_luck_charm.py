#!/usr/bin/env python3
"""Generate the item icon for Prismium Luck Charm (scheduled session,
2026-10-12), the mod's ninth passive/always-on "just carry it"
accessory - see PrismiumLuckCharmHandler's TickEvent.PlayerTickEvent
listener (permanent Luck I while the item merely sits anywhere in
inventory, no equip slot, no right-click action). The second charm in
the family whose effect is not a damage-type mitigator, following
PrismiumHasteCharmItem's precedent (see that item's javadoc for why the
family moved past damage-reduction-only effects at v0.72.0).

Visual language: mirrors Featherstone/Emberguard/Vitastone/Aegis/
Frostguard/Antivenom/Haste's "stone + something diagonal/central +
small teal Prismium gem" composition (same pebble silhouette reused
verbatim, same GEM_RING/GEM_CORE/GEM_GLINT palette for family identity)
with a small pixel four-pointed sparkle/star standing in for the
shield/feather/flame/heart/bolt - a bold, symmetric "twinkle" shape
universally read as luck/magic/fortune in pixel art, distinct from
every other charm's motif. A fresh emerald-green accent palette,
deliberately more saturated/cooler than Antivenom's olive-yellow
"toxin" green so the two don't read as the same color at hotbar scale.
Two detached single-pixel highlight "glint" dashes flank the sparkle,
echoing Haste's electric-spark dashes but reading as distant twinkles
instead.

Self-review note: the first draft used a four-leaf clover silhouette
(a 7x7 rounded-diamond blob with a single-row notch between the two
top lobes). At 4x preview scale the notch was too subtle to read and
the whole motif collapsed into an ambiguous green leaf/shield blob
instead of a recognizable clover - the same failure mode Haste's own
self-review note describes for its discarded pickaxe draft. Replaced
it with a four-pointed sparkle (vertical spike + wide horizontal bar +
small diamond core), a shape whose bold cross silhouette stays legible
even this small - the same lesson as why every other charm in this
family uses a simple, bold, single-motif accent rather than something
with fine concave detail. Because every row of the sparkle is now a
single contiguous span, it is expressed as a plain x0/x1-per-row table
like the lightning bolt in gen_prismium_haste_charm.py, rather than the
explicit row-string table the clover draft needed for its concave notch.

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_luck_charm.py
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
# of these nine items, so keeping it identical across all of them keeps
# the family read tight and puts all the visual distinction into the
# accent motif/palette.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Emerald/clover-green accent - deliberately distinct from every other
# charm's accent palette, and in particular more saturated and cooler
# (less yellow) than Antivenom's olive "#7C9A2E" toxin tone so the two
# don't read as the same color at hotbar scale.
LUCK_SHADOW = "#1C5C2E"
LUCK_BASE = "#2EA84A"
LUCK_HILITE = "#8FFFAA"

# Prismium crystal accent, reused verbatim from the rest of the charm
# family so this item still reads as part of the Prismium family at a
# glance despite the green palette.
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
L_SHADOW = hexrgb(LUCK_SHADOW)
L_BASE = hexrgb(LUCK_BASE)
L_HILITE = hexrgb(LUCK_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as the rest of the charm
# family's pebble, reused verbatim so all nine passive items read as a
# matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Four-pointed sparkle/star silhouette: vertical spike (rows 1-2, 6-7),
# a small diamond core (rows 3, 5), and a wide horizontal bar (row 4) -
# the classic bold "twinkle" cross shape, same per-row x0/x1 span
# technique as the lightning bolt in gen_prismium_haste_charm.py (every
# row here is a single contiguous run, unlike the discarded clover
# draft - see self-review note above).
SPARK_ROWS = {
    1: (8, 8),
    2: (8, 8),
    3: (7, 9),
    4: (5, 11),
    5: (7, 9),
    6: (8, 8),
    7: (8, 8),
}

# Two detached single-pixel "glint" dashes flanking the sparkle's wide
# horizontal bar - distant twinkles, not debris/sparks (contrast with
# Haste's electric-spark framing of the same detached-dash device).
SPARKLE_PTS = {(3, 4), (13, 4)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    spark_pts = set()
    for y, (x0, x1) in SPARK_ROWS.items():
        for x in range(x0, x1 + 1):
            spark_pts.add((x, y))

    accent_pts = set(spark_pts) | SPARKLE_PTS

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. Glint dashes sit a
    # couple pixels away from the sparkle, so they each get their own
    # independent outline ring - same reasoning as Haste's spark dashes.
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

    # Sparkle fill: leftmost column of each row = highlight, rightmost =
    # shadow, everything else = base green - same leading-highlight/
    # trailing-shadow rule the rest of the family uses on their
    # diagonal/central accents (single-pixel rows are highlight only,
    # same as the bolt's narrowest rows).
    for y, (x0, x1) in SPARK_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == x0:
                color = L_HILITE
            elif x == x1:
                color = L_SHADOW
            else:
                color = L_BASE
            px[x, y] = (*color, 255)

    # Glint dashes: bright highlight dots, same treatment as the rest
    # of the family's detached accent pixels.
    for (x, y) in SPARKLE_PTS:
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
    out_path = out_dir / "prismium_luck_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_luck_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
