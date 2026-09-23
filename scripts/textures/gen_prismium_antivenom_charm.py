#!/usr/bin/env python3
"""Generate the item icon for Prismium Antivenom Charm (scheduled
session, 2026-09-23), the mod's seventh passive/always-on "just carry
it" accessory - see PrismiumAntivenomCharmHandler's LivingDamageEvent
listener (poison/wither damage reduction while the item merely sits
anywhere in inventory, no equip slot, no right-click action).

Visual language: mirrors Featherstone/Emberguard/Vitastone/Aegis
Charm/Frostguard Charm's "stone + something diagonal + small teal
Prismium gem" composition (same pebble silhouette reused verbatim),
with a small pixel toxin droplet (narrow point at top widening to a
rounded bulge at the bottom, i.e. a raindrop pointing upward) standing
in for the feather/flame/heart/shield/icicle, plus two detached
single-pixel "bubbling toxin" sparkle dots - echoing Frostguard
Charm's frost sparkle dots but placed closer to the droplet's bulge to
read as bubbles rising off a toxic liquid rather than drifting snow.

Given a sickly yellow-green palette (evoking both poison potions and
wither roses) that is deliberately darker/more desaturated than any
existing charm's accent color, so all seven passive charms remain
individually distinguishable in a hotbar/JEI grid.

Self-review note: an early mental draft considered a skull motif (to
read as "poison" more literally), but at 16x16 a legible skull needs
carved-out eye sockets that would require punching transparent/dark
holes into the accent shape itself - every other charm in this family
keeps its accent shape solid (outline + fill only, no internal cutouts)
so a skull would break that consistency. The droplet keeps the same
solid-silhouette structure as Featherstone's feather / Frostguard's
icicle while still reading clearly as "toxic liquid" via color alone.

Deterministic (no RNG - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_antivenom_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#241512"

# Same charred-rock tones as Emberguard/Vitastone/Aegis Charm/
# Frostguard Charm's stone, reused verbatim - the stone itself is not
# the "hero" element on any of these items, so keeping it identical
# keeps the family read tight.
STONE_SHADOW = "#241F1E"
STONE_BASE = "#3B3230"
STONE_HILITE = "#5C4E4A"

# Sickly yellow-green toxin palette - darker/more desaturated than any
# existing charm accent (Featherstone's near-white/pale-teal, Magnet
# Charm's bright blue, Frostguard's cyan-blue, Aegis Charm's violet).
TOXIN_SHADOW = "#4A5A1E"
TOXIN_BASE = "#7C9A2E"
TOXIN_HILITE = "#C4E86A"

# Prismium crystal accent, reused verbatim from gen_prismium.py /
# gen_prismium_featherstone.py / gen_prismium_emberguard.py /
# gen_prismium_vitastone.py / gen_prismium_aegis_charm.py /
# gen_prismium_frostguard_charm.py so this item still reads as part of
# the Prismium family at a glance.
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
T_SHADOW = hexrgb(TOXIN_SHADOW)
T_BASE = hexrgb(TOXIN_BASE)
T_HILITE = hexrgb(TOXIN_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Charred-rock silhouette: same squat oval as Featherstone/Emberguard/
# Vitastone/Aegis Charm/Frostguard Charm's pebble, reused verbatim so
# all passive items read as a matched set on a shelf/in a JEI grid.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Droplet silhouette: narrow point at top, widening to a rounded bulge
# at the bottom - a raindrop shape pointing upward.
DROPLET_ROWS = {
    1: (7, 8),
    2: (7, 8),
    3: (6, 9),
    4: (5, 10),
    5: (4, 11),
    6: (4, 11),
    7: (5, 10),
    8: (6, 9),
}
# Two detached single-pixel "bubbling toxin" sparkle dots, placed near
# the droplet's widest rows to read as bubbles rising off a toxic
# liquid rather than debris or drifting snow.
SPARKLE_PTS = {(3, 6), (12, 5)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    droplet_pts = set()
    for y, (x0, x1) in DROPLET_ROWS.items():
        for x in range(x0, x1 + 1):
            droplet_pts.add((x, y))

    accent_pts = set(droplet_pts) | SPARKLE_PTS

    all_solid = stone_pts | accent_pts

    # 1px outline around the combined silhouette. Sparkle dots sit at
    # least 1px away from the droplet, so they each get their own
    # independent outline ring.
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
    # Vitastone/Aegis Charm/Frostguard Charm's stone.
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

    # Droplet fill: leftmost column of each row = highlight, rightmost
    # = shadow, everything else = base toxin-green - same leading-
    # highlight/trailing-shadow rule Featherstone/Emberguard/
    # Vitastone/Aegis Charm/Frostguard Charm use on their diagonal
    # accents.
    for y, (x0, x1) in DROPLET_ROWS.items():
        for x in range(x0, x1 + 1):
            if x == x0:
                color = T_HILITE
            elif x == x1:
                color = T_SHADOW
            else:
                color = T_BASE
            px[x, y] = (*color, 255)

    # Sparkle dots: bright highlight dots, same treatment as
    # Emberguard's flicker / Vitastone's shimmer / Aegis Charm's burst
    # / Frostguard Charm's frost sparkle.
    for (x, y) in SPARKLE_PTS:
        px[x, y] = (*T_HILITE, 255)

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
    out_path = out_dir / "prismium_antivenom_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_antivenom_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
