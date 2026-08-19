#!/usr/bin/env python3
"""Generate the item icon for Prismium Alloy Ingot (session 70,
scheduled - see ModItems.PRISMIUM_ALLOY_INGOT /
PrismiumCompressorBlockEntity / PROGRESS.md).

The mod's second refined-material item: Prismium Compressor (session
70's new machine) consumes 4 Prismium Ingots + FE to produce 1 of
these, extending the mod's production chain one step further than
gen_prismium_ingot.py's Prismium Ingot (Ore -> Shard -> Ingot -> Alloy
Ingot). Reuses the exact same vanilla-style ingot-bar silhouette/ROWS
shape as gen_prismium_ingot.py (same trapezoid bar, same proportions)
so the two items read as clearly related ("both cast metal bars") -
only the palette changes, from Ingot's warm bronze/gold to a cool
steel-blue/platinum for Alloy Ingot, signalling "further refined,
denser, colder-forged" without changing the recognizable silhouette. A
single 2px magenta accent chip (PRISMIUM_ACCENT, the same accent color
Prismium Warhammer's texture uses for its embedded gem) is added in
the middle of the bar to visually tie this higher-tier material back
to the Prismium family, the same "family accent embedded in a
different-palette base" trick Warhammer's texture already established.

Self-review: generate at 16x16, write 8x/16x upscaled previews to the
outputs mount and inspect with Read before treating this as final -
confirm the bar silhouette still reads clearly and distinctly from
Prismium Ingot at small size (palette contrast, not shape, should be
what tells them apart), confirm the accent chip doesn't muddy the
silhouette, confirm every filled pixel is fully opaque.

Run from repo root: python3 scripts/textures/gen_prismium_alloy_ingot.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/item/prismium_alloy_ingot.png"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_ACCENT = "#D633B0"

# Cool steel-blue/platinum metal palette - distinct from
# gen_prismium_ingot.py's warm bronze/gold, signalling a
# further-refined, denser material while keeping the same bar shape.
METAL_SHADOW = "#3E4E5C"
METAL_BASE = "#7C93A2"
METAL_MID = "#A6BAC5"
METAL_HILITE = "#EAF3F7"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# Same row-by-row silhouette as gen_prismium_ingot.py's ROWS.
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
    accent = hexrgb(PRISMIUM_ACCENT)

    for y, (x0, x1) in ROWS.items():
        for x in range(x0, x1 + 1):
            px[x, y] = (*base, 255)

    y_bottom = max(ROWS.keys())
    x0b, x1b = ROWS[y_bottom]
    for x in range(x0b, x1b + 1):
        px[x, y_bottom] = (*shadow, 255)
    for y, (x0, x1) in ROWS.items():
        px[x1, y] = (*shadow, 255)

    y_top = min(ROWS.keys())
    x0t, x1t = ROWS[y_top]
    for x in range(x0t, x1t + 1):
        px[x, y_top] = (*hilite, 255)
    for y, (x0, x1) in ROWS.items():
        px[x0, y] = (*mid, 255)

    # Prismium-family accent chip: a 2px magenta mark centered in the
    # bar body, tying this higher-tier material back to the mod's
    # crystal-family accent color (same idea as Warhammer's embedded
    # gem) without disrupting the ingot silhouette itself.
    px[7, 8] = (*accent, 255)
    px[8, 8] = (*accent, 255)

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
