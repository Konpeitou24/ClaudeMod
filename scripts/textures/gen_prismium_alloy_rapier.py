#!/usr/bin/env python3
"""Generate the item icon for Prismium Alloy Rapier (session 71,
scheduled - see ModItems.PRISMIUM_ALLOY_RAPIER /
PrismiumAlloyRapierHandler / PROGRESS.md session 70 handoff item 1,
"Prismium Alloy Ingot needs a real equipment use").

This is the mod's first item crafted from Prismium Alloy Ingot (session
70's further-refined material - Ingot -> Alloy Ingot via the Compressor -
which had no equipment use until now). Mirrors gen_prismium_warhammer.py's
material-storytelling idea (palette signals the crafting material) in the
opposite direction: Warhammer paired Prismium Ingot with a heavy/slow
silhouette and gen_prismium_ingot.py's warm bronze palette; this pairs the
even-more-refined Alloy Ingot with a light/fast silhouette and
gen_prismium_alloy_ingot.py's cool steel-blue/platinum palette
(METAL_SHADOW/BASE/MID/HILITE, copied verbatim from that script so the two
"cast metal" items read as the same family of material).

Silhouette: a real-rapier-inspired shape - a needle-thin 1px blade (the
thinnest in the mod's whole equipment set, distinct from the Sword's 2px
crystal blade at a glance) paired with a comparatively wide crossguard
(real-world rapiers pair thin blades with wide/ornate guards, unlike a
plain arming sword), reusing gen_prismium_tools.py's steel HILT_ palette
for guard/grip - the same "metal blade, steel hilt" family as the Sword,
not the Warhammer's wood-handle family.

Session note (first iteration, self-review caught this): the first draft
ran the blade through the shared draw_outline() helper, which frames
every filled pixel on all 8 neighbours - for a 1px-wide column that means
2 outline pixels for every 1 alloy-colored pixel, so the blade rendered
as a solid dark-teal bar instead of a slender blade. This revision skips
draw_outline() for the blade entirely (a lone shadow pixel on one edge is
enough depth cue at 16x16) and reserves the mod's usual teal outline for
the guard only, where it isn't the dominant color.

Self-review: generate at 16x16, write 4x/16x upscaled previews to the
outputs mount and inspect with Read before treating this as final -
confirm the blade actually reads as slender alloy color (not outline
color), confirm the guard/grip proportions read as a rapier hilt, confirm
every filled pixel is fully opaque.

Run from repo root: python3 scripts/textures/gen_prismium_alloy_rapier.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/item/prismium_alloy_rapier.png"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_ACCENT = "#FF7CFC"

# Blade/guard metal palette: copied verbatim from
# gen_prismium_alloy_ingot.py (cool steel-blue/platinum cast metal).
METAL_SHADOW = "#3E4E5C"
METAL_BASE = "#7C93A2"
METAL_MID = "#A6BAC5"
METAL_HILITE = "#EAF3F7"

# Hilt/grip palette: matches gen_prismium_tools.py's sword hilt exactly
# (muted steel-grey).
HILT_OUTLINE = "#1B1B22"
HILT_BASE = "#4A4A57"
HILT_HILITE = "#6E6E80"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
M_SHADOW = hexrgb(METAL_SHADOW)
M_BASE = hexrgb(METAL_BASE)
M_MID = hexrgb(METAL_MID)
M_HILITE = hexrgb(METAL_HILITE)
G_OUTLINE = hexrgb(HILT_OUTLINE)
G_BASE = hexrgb(HILT_BASE)
G_HILITE = hexrgb(HILT_HILITE)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def set_px(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = (*color, 255)


def draw_outline(px, pts, outline_color=OUTLINE):
    ptset = set(pts)
    for (x, y) in ptset:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, -1), (-1, 1), (1, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in ptset and 0 <= nx < SIZE and 0 <= ny < SIZE:
                if px[nx, ny][3] == 0:
                    px[nx, ny] = (*outline_color, 255)


def make_alloy_rapier():
    img = new_img()
    px = img.load()

    # Blade: a single 1px-wide column, rows 0-8. No draw_outline() call
    # here (see module docstring for why) - just a one-edge shadow pixel
    # per row for a hint of depth, so the alloy color itself stays
    # dominant instead of being swallowed by the mod's usual teal frame.
    blade_col = 9
    for y in range(0, 9):
        color = M_MID if y % 3 == 0 else M_BASE
        set_px(px, blade_col, y, color)
        set_px(px, blade_col + 1, y, M_SHADOW)
    set_px(px, blade_col, 0, M_HILITE)  # bright tip catches the light

    # Crossguard: wide relative to the blade (real rapiers pair a thin
    # blade with a broad guard) - 5px wide like the Sword's, but only
    # 1 row thick to stay visually lighter than the Sword's guard.
    for x in range(7, 12):
        set_px(px, x, 9, G_BASE)
    set_px(px, 7, 9, G_OUTLINE)
    set_px(px, 11, 9, G_OUTLINE)
    set_px(px, 9, 9, ACCENT)  # Prismium family accent, guard center

    # Grip + pommel: short steel hilt below the guard.
    for y in range(10, 13):
        set_px(px, 8, y, G_BASE)
        set_px(px, 9, y, G_BASE)
    set_px(px, 8, 10, G_HILITE)
    for (x, y) in [(7, 10), (10, 10), (7, 12), (10, 12)]:
        set_px(px, x, y, G_OUTLINE)
    set_px(px, 8, 13, G_OUTLINE)  # pommel cap
    set_px(px, 9, 13, G_OUTLINE)

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = make_alloy_rapier()
    img.save(OUT_PATH)
    print(f"Wrote {OUT_PATH}")

    bad = [(x, y, img.getpixel((x, y))) for y in range(SIZE) for x in range(SIZE)
           if img.getpixel((x, y))[3] not in (0, 255)]
    if bad:
        print(f"WARNING: {len(bad)} pixels with partial alpha: {bad[:5]}")
    else:
        print("OK: all pixels fully opaque or fully transparent")


if __name__ == "__main__":
    main()
