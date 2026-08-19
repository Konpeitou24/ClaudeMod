#!/usr/bin/env python3
"""Generate the item icon for Prismium Warhammer (session 69, scheduled -
see ModItems.PRISMIUM_WARHAMMER / PrismiumWarhammerHandler / PROGRESS.md).

The mod's first weapon crafted from Prismium Ingot (session 68's refined
material, previously unused - see PROGRESS.md) rather than Prismium
Shard. Deliberately reuses gen_prismium_tools.py's warm dark-wood handle
palette (HANDLE_OUTLINE/SHADOW/BASE/HILITE) for the shaft, but - unlike
every existing Prismium tool/sword, whose heads are all cut from the
teal/magenta crystal palette (PRISMIUM_BASE/MID/HILITE) - gives the head
itself gen_prismium_ingot.py's warm bronze/gold cast-metal palette
(METAL_SHADOW/BASE/MID/HILITE). This is a deliberate visual signal: the
existing tool family is "crystal head on a wood handle" (raw Shard,
barely shaped), while the Warhammer is "cast metal head on a wood
handle" (refined Ingot, forged into a real weapon) - the same
material-progression story the item's own crafting recipe and lang
.details line tell, now also told through color.

Silhouette: a wide, symmetric double-sided mace/sledge head (unlike the
axe's single flat-topped block, this one is a thick rectangular block
that the handle visibly pierces through the middle of, with the handle
poking out slightly above the head too) - reads as "heavy, blunt,
two-handed" rather than "bladed", clearly distinct from every existing
Prismium tool/weapon's silhouette at a glance. A small Prismium gem
accent (PRISMIUM_ACCENT, same magenta used by every other Prismium
gear item - Sword/Featherstone/Emberguard/Vitastone/Magnet Charm) is
embedded in the head's center so it still reads as part of the mod's
Prismium family despite the different head material.

Self-review: generate at 16x16, write 4x/8x/16x upscaled previews to the
outputs mount and inspect with Read before treating this as final -
confirm the mace-head silhouette is legible and distinct from the axe/
pickaxe/sword at hotbar scale, confirm the metal head vs wood handle
contrast reads clearly, confirm every filled pixel is fully opaque.

Run from repo root: python3 scripts/textures/gen_prismium_warhammer.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_PATH = REPO_ROOT / "src/main/resources/assets/claudemod/textures/item/prismium_warhammer.png"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_ACCENT = "#FF7CFC"

# Handle palette: matches gen_prismium_tools.py exactly (warm dark wood).
HANDLE_OUTLINE = "#231208"
HANDLE_SHADOW = "#4A2A14"
HANDLE_BASE = "#6B3E1E"
HANDLE_HILITE = "#8F5A2E"

# Head palette: matches gen_prismium_ingot.py exactly (cast bronze/gold
# metal), not the crystal palette every other Prismium tool head uses -
# see module docstring for why.
METAL_SHADOW = "#8A5A1E"
METAL_BASE = "#C88A2E"
METAL_MID = "#E3A947"
METAL_HILITE = "#F6D488"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
H_OUTLINE = hexrgb(HANDLE_OUTLINE)
H_SHADOW = hexrgb(HANDLE_SHADOW)
H_BASE = hexrgb(HANDLE_BASE)
H_HILITE = hexrgb(HANDLE_HILITE)
M_SHADOW = hexrgb(METAL_SHADOW)
M_BASE = hexrgb(METAL_BASE)
M_MID = hexrgb(METAL_MID)
M_HILITE = hexrgb(METAL_HILITE)


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


def draw_handle(px, start, end):
    """2px-thick diagonal wood handle - identical technique to
    gen_prismium_tools.py's draw_handle()."""
    x0, y0 = start
    x1, y1 = end
    steps = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        color = H_HILITE if i % 4 == 0 else H_BASE
        set_px(px, x, y, color)
        set_px(px, x - 1, y + 1, H_SHADOW)
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        set_px(px, x + 1, y, H_OUTLINE)


def make_warhammer():
    img = new_img()
    px = img.load()

    # Handle: bottom-left grip up through the middle of the head, poking
    # out 1-2px above it (session docstring: "pierces through the head").
    draw_handle(px, (2, 14), (7, 3))

    # Head: a thick, wide, symmetric block straddling the handle - the
    # "mace/sledge" silhouette described in the module docstring.
    # Columns 3-12 (10 wide), rows 3-7 (5 tall): much thicker/wider than
    # the axe's block (rows 2-7, cols 8-13) so it reads as a distinct,
    # heavier shape even before color is considered.
    head = set()
    rows = {
        3: (4, 11),
        4: (3, 12),
        5: (3, 12),
        6: (3, 12),
        7: (4, 11),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            head.add((x, y))

    def shade(x, y):
        # Top-left lit, bottom-right in shadow - classic cast-metal
        # blockshading, matching gen_prismium_ingot.py's light direction.
        if y == 3:
            return M_HILITE
        if x <= 5:
            return M_MID
        if y >= 6:
            return M_SHADOW
        return M_BASE

    for (x, y) in head:
        px[x, y] = (*shade(x, y), 255)
    draw_outline(px, head)

    # Prismium gem accent, centered in the head, tying it back to the
    # mod's established family look (see docstring).
    set_px(px, 7, 5, ACCENT)
    set_px(px, 8, 5, ACCENT)

    return img


def main():
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = make_warhammer()
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
