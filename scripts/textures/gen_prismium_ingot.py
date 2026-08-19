#!/usr/bin/env python3
"""Generate the item icon for Prismium Ingot (session 68 - see
ModItems.PRISMIUM_INGOT / PrismiumSmelterBlockEntity / PROGRESS.md).

The mod's first refined-material item: Prismium Smelter (session 68's
new machine, see PrismiumSmelterBlockEntity) consumes 4 Prismium Shards
+ FE to produce 1 of these, extending the mod's first production chain
one step further (Ore -[Silk Touch + Pulverizer, session 67]-> Shard
-[Smelter, session 68]-> Ingot). Deliberately drawn as a vanilla-style
ingot bar silhouette (the same general shape/proportions as vanilla's
iron/gold/copper ingot icons - a trapezoid bar, wider at the bottom,
angled top-left highlight edge) rather than reusing Prismium Shard's
faceted-crystal silhouette, so the two items read as visually distinct
categories at a glance in an inventory: shard = raw/uncut crystal,
ingot = refined/cast metal bar. Palette: a warm bronze/gold metal base
(distinct from the shard's own magenta/teal crystal palette and from
every existing machine's magenta Prismium-energy glow) with the mod's
usual PRISMIUM_OUTLINE dark-teal used only for the thin outline, so it
still visually belongs to the mod's overall item family without being
confused for another crystal/energy item.

Self-review: generate at 16x16, write 8x/16x upscaled previews to the
outputs mount and inspect with Read before treating this as final -
confirm the bar silhouette reads clearly at small size, confirm the
highlight/shadow give it a metallic (not flat) look, confirm every
filled pixel is fully opaque.

Run from repo root: python3 scripts/textures/gen_prismium_ingot.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/item/prismium_ingot.png"

PRISMIUM_OUTLINE = "#024D4B"

METAL_SHADOW = "#8A5A1E"
METAL_BASE = "#C88A2E"
METAL_MID = "#E3A947"
METAL_HILITE = "#F6D488"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# Row-by-row silhouette of a vanilla-style ingot bar: narrower top,
# wider bottom, in row-local (x_start, x_end_inclusive) pairs across a
# 16-wide canvas. Rows without an entry are left transparent.
ROWS = {
    5: (5, 10),
    6: (4, 11),
    7: (3, 12),
    8: (3, 12),
    9: (3, 12),
    10: (3, 12),
}


def build():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(METAL_SHADOW)
    base = hexrgb(METAL_BASE)
    mid = hexrgb(METAL_MID)
    hilite = hexrgb(METAL_HILITE)

    # Fill the bar body with the base tone first.
    for y, (x0, x1) in ROWS.items():
        for x in range(x0, x1 + 1):
            px[x, y] = (*base, 255)

    # Bottom row and right edge: shadow, for a cast-metal weight/depth cue.
    y_bottom = max(ROWS.keys())
    x0b, x1b = ROWS[y_bottom]
    for x in range(x0b, x1b + 1):
        px[x, y_bottom] = (*shadow, 255)
    for y, (x0, x1) in ROWS.items():
        px[x1, y] = (*shadow, 255)

    # Top row and left edge: bright highlight, for the classic
    # ingot "angled light catching the top face" look.
    y_top = min(ROWS.keys())
    x0t, x1t = ROWS[y_top]
    for x in range(x0t, x1t + 1):
        px[x, y_top] = (*hilite, 255)
    for y, (x0, x1) in ROWS.items():
        px[x0, y] = (*mid, 255)

    # A thin dark outline hugging the silhouette, one pixel outside the
    # filled area, so the bar separates cleanly from the inventory slot
    # background at small sizes.
    filled = set()
    for y, (x0, x1) in ROWS.items():
        for x in range(x0, x1 + 1):
            filled.add((x, y))

    for (x, y) in list(filled):
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if 0 <= nx < SIZE and 0 <= ny < SIZE and (nx, ny) not in filled:
                if px[nx, ny][3] == 0:
                    px[nx, ny] = (*outline, 255)

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = build()
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
