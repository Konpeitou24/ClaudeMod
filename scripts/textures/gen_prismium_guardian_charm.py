#!/usr/bin/env python3
"""Generate the item icon for Prismium Guardian Charm (session 30), the
mod's first "cheat death" item - see PrismiumGuardianCharmItem /
PrismiumGuardianCharmHandler.

Visual language: unlike every other Prismium item so far (which use the
mod's teal PRISMIUM_BASE/PRISMIUM_ACCENT crystal-shard family, see
gen_prismium.py), this one is deliberately a pendant/amulet silhouette -
a closed loop (necklace ring) at the top connected to a hexagonal
gold-trimmed body with a small violet gem at its center - rather than
another crystal-shard shape, so it reads at a glance as "wearable
talisman", distinct from the mod's raw-material shards (Prismium Shard,
Rift Shard) despite sharing their 16x16 flat-icon format. The gold trim
is a deliberate nod to vanilla's own Totem of Undying (which this item
is a gameplay parallel to, see the Java class javadoc) while the violet
gem (reusing PRISMIUM_ACCENT/PRISMIUM_ACCENT_HILITE verbatim from
gen_prismium_grappling_hook.py / gen_prismium_rift_shard.py) keeps it
identifiably part of the Prismium family rather than a plain gold trinket.

Self-review note: an early draft made the loop's inner hole only 2x1
pixels, which at 16x16 read as a solid blob rather than an open ring
once previewed - widened it to a clear 2x2 hole with the ring itself
kept to a thin 1px band so the "necklace loop" reads unambiguously.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_guardian_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ----------------------------------------------------------
# Gold trim: a nod to vanilla's Totem of Undying (see module docstring).
GOLD_OUTLINE = "#3D2A08"
GOLD_SHADOW = "#8A6218"
GOLD_BASE = "#C99A2E"
GOLD_HILITE = "#F5D97A"

# Violet gem: the mod's existing energy-accent color, reused verbatim
# from gen_prismium_grappling_hook.py / gen_prismium_rift_shard.py so
# this item still reads as part of the Prismium family at a glance.
PRISMIUM_ACCENT = "#C97BFF"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"
PRISMIUM_ACCENT_DARK = "#7A3FA6"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


G_OUTLINE = hexrgb(GOLD_OUTLINE)
G_SHADOW = hexrgb(GOLD_SHADOW)
G_BASE = hexrgb(GOLD_BASE)
G_HILITE = hexrgb(GOLD_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_HILITE = hexrgb(PRISMIUM_ACCENT_HILITE)
ACCENT_DARK = hexrgb(PRISMIUM_ACCENT_DARK)

# The pendant body: a hexagonal silhouette, widest at rows 7-8, tapering
# to a point at the bottom (row 13) and narrowing into the loop's neck
# at the top (row 4).
BODY_ROWS = {
    4: (6, 9),
    5: (5, 10),
    6: (4, 11),
    7: (4, 11),
    8: (4, 11),
    9: (4, 11),
    10: (5, 10),
    11: (5, 10),
    12: (6, 9),
    13: (7, 8),
}

# The necklace loop: a thin ring sitting above the body, with a clear
# open hole so it reads as a loop rather than a solid blob (see
# self-review note in the module docstring).
LOOP_RING_PTS = {(6, 1), (7, 1), (8, 1), (9, 1),
                  (6, 2), (9, 2),
                  (6, 3), (9, 3)}
LOOP_HOLE_PTS = {(7, 2), (8, 2), (7, 3), (8, 3)}

# The central gem: a small diamond, plus a darker ring for depth and one
# bright glint pixel (same "accent gem" trick used on the grappling hook
# and Prismium tool set, per gen_prismium_locator.py's docstring).
GEM_CORE_PTS = {(7, 8), (8, 8)}
GEM_RING_PTS = {(7, 7), (8, 7), (6, 8), (9, 8), (7, 9), (8, 9)}
GEM_GLINT_PT = (7, 7)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_charm():
    img = new_img()
    px = img.load()

    body_pts = set()
    for y, (x0, x1) in BODY_ROWS.items():
        for x in range(x0, x1 + 1):
            body_pts.add((x, y))

    all_solid = body_pts | LOOP_RING_PTS

    # 1px outline around the combined silhouette (body + loop), same
    # "outline occupies empty pixels adjacent to a solid fill" technique
    # used by every other item in this mod.
    outline_pts = set()
    for (x, y) in all_solid:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_solid and (nx, ny) not in LOOP_HOLE_PTS \
                    and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))
    # The loop's inner hole also gets an outline ring so it reads as a
    # punched-through hole rather than transparent background bleeding
    # into the ring.
    for (x, y) in LOOP_HOLE_PTS:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) in LOOP_RING_PTS:
                outline_pts.discard((nx, ny))

    for (x, y) in outline_pts:
        px[x, y] = (*G_OUTLINE, 255)

    # Loop ring fill.
    for (x, y) in LOOP_RING_PTS:
        px[x, y] = (*G_BASE, 255)

    # Body fill with a simple left-shadow / right-highlight gradient
    # (same three-tone shading rule used across every other item script).
    for y, (x0, x1) in BODY_ROWS.items():
        width = x1 - x0
        for x in range(x0, x1 + 1):
            rel = (x - x0) / max(width, 1)
            if rel < 0.3:
                color = G_SHADOW
            elif rel < 0.75:
                color = G_BASE
            else:
                color = G_HILITE
            px[x, y] = (*color, 255)

    # Punch the gem into the body fill last, so it always wins over the
    # gold gradient underneath it.
    for (x, y) in GEM_RING_PTS:
        if (x, y) in body_pts:
            px[x, y] = (*ACCENT_DARK, 255)
    for (x, y) in GEM_CORE_PTS:
        if (x, y) in body_pts:
            px[x, y] = (*ACCENT, 255)
    if GEM_GLINT_PT in body_pts:
        px[GEM_GLINT_PT] = (*ACCENT_HILITE, 255)

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

    img = make_charm()
    out_path = out_dir / "prismium_guardian_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_guardian_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
