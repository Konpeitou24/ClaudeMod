#!/usr/bin/env python3
"""Generate the item icon for Prismium Vitastone (session 33), the
mod's third passive/always-on "just carry it" accessory - see
PrismiumVitastoneHandler's LivingHealEvent listener (amplifies
incoming healing while the item merely sits anywhere in inventory,
no equip slot, no right-click action).

Visual language: mirrors Prismium Featherstone/Emberguard's "stone +
something diagonal + small teal Prismium gem" composition (same
pebble silhouette reused verbatim from gen_prismium_featherstone.py/
gen_prismium_emberguard.py, same GEM_RING/GEM_CORE/GEM_GLINT palette
for family identity) with a small pixel heart standing in for the
feather/flame, trailing a short sparkle tail down to the stone - "a
spark of life caught in stone", echoing the healing-amplification
gameplay effect. Kept a third, distinct palette (pink/magenta) from
both Featherstone (cool white/teal) and Emberguard (warm orange/red)
so all three read as a family at a glance but are still individually
distinguishable in a hotbar.

Self-review note: an early mental draft considered a literal anatomical
heart outline, but a plain small pixel heart (the same silhouette
vanilla already uses for the health HUD, just recolored) reads
instantly as "healing" to any Minecraft player and avoids inventing an
unfamiliar shape at 16x16 - familiarity was prioritized over novelty
here specifically because the icon needs to communicate its effect at
a glance in a hotbar slot.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_vitastone.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#241512"

# Same charred-rock tones as Emberguard's stone, reused verbatim - the
# stone itself is not the "hero" element on any of these three items,
# so keeping it identical across all three keeps the family read tight
# and puts all the visual distinction into the accent motif/palette.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

LIFE_SHADOW = "#7A1F45"
LIFE_BASE = "#E8447F"
LIFE_HILITE = "#FFC7DE"

# Prismium crystal accent, reused verbatim from gen_prismium.py /
# gen_prismium_featherstone.py / gen_prismium_emberguard.py so this
# item still reads as part of the Prismium family at a glance despite
# the pink/magenta palette swap.
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
L_SHADOW = hexrgb(LIFE_SHADOW)
L_BASE = hexrgb(LIFE_BASE)
L_HILITE = hexrgb(LIFE_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as Featherstone/Emberguard's
# pebble, reused verbatim so all three passive items read as a matched
# set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Heart silhouette: a plain small pixel heart (the same basic shape as
# vanilla's HUD health icon), explicit column lists per row since row 1
# has a gap between the two top bumps.
HEART_ROWS = {
    1: [5, 6, 9, 10],
    2: [5, 6, 7, 8, 9, 10],
    3: [6, 7, 8, 9],
    4: [7, 8],
}
# Short sparkle tail trailing from the heart's tip down to the stone -
# echoes Emberguard's single-pixel-wide flame taper/flicker, but here
# reads as "drifting spark" rather than "flickering flame".
TAIL_ROWS = {
    5: [7],
    6: [6],
    7: [6],
    8: [5, 6],
}
# A single detached highlight spark above/right of the heart - the
# "shimmer", same trick as Emberguard's flicker ember.
SHIMMER_PT = (11, 0)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    life_pts = set()
    for y, cols in HEART_ROWS.items():
        for x in cols:
            life_pts.add((x, y))
    for y, cols in TAIL_ROWS.items():
        for x in cols:
            life_pts.add((x, y))
    life_pts.add(SHIMMER_PT)

    all_solid = stone_pts | life_pts

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
    # toward the center - identical shading rule to Emberguard's stone.
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

    # Heart fill: leftmost column of each row = highlight (light
    # catching the rounded left lobe), rightmost = shadow, everything
    # else = base pink - same leading-highlight/trailing-shadow rule
    # Featherstone/Emberguard use on their diagonal accents.
    for y, cols in HEART_ROWS.items():
        x0, x1 = min(cols), max(cols)
        for x in cols:
            if x == x0:
                color = L_HILITE
            elif x == x1:
                color = L_SHADOW
            else:
                color = L_BASE
            px[x, y] = (*color, 255)

    # Sparkle tail: bright highlight dots, same treatment as
    # Emberguard's single-pixel flame core/flicker.
    for y, cols in TAIL_ROWS.items():
        for x in cols:
            px[x, y] = (*L_HILITE, 255)
    px[SHIMMER_PT] = (*L_HILITE, 255)

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
    out_path = out_dir / "prismium_vitastone.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_vitastone.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
