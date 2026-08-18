#!/usr/bin/env python3
"""Generate the texture for Prismium Restorer (session 20): the mod's
*second* FE consumer, after Prismium Pylon (session 19) - see
ModBlocks.PRISMIUM_RESTORER / PrismiumRestorerBlock /
PrismiumRestorerBlockEntity / PROGRESS.md.

Joins the established "machine casing" family introduced by Prismium
Cell (session 8) and reused by Generator (session 9), Cable (session 10)
and Pylon (session 19): same CASING_DARK/CASING_MID metal palette against
the shared PRISMIUM_OUTLINE border, same recessed 8x8 socket cut into a
cube_all texture (paint_casing/SIZE/hexrgb lifted near-verbatim from
gen_prismium_pylon.py, session 19, on purpose - reusing a
already-reviewed helper instead of re-deriving pixel offsets from
scratch).

What differs is the socket content and, deliberately, the accent color:
every other energy-family block reads warm violet/cyan (the mod's
long-standing "charged Prismium crystal" palette, e.g. the Wraith egg
highlight, Bloom/Spike, Pylon). Prismium Restorer instead shows a bright
gold/amber "mending cross" - a plus-shaped glyph chosen to read instantly
as "repair station" (gold traditionally reads as "value/restoration" in
vanilla's own palette, e.g. enchanted golden apples) and, just as
importantly, to make this block visually distinguishable from Pylon at a
glance in a player's hotbar/world despite sharing the same casing shell -
a "reuse the frame, swap the accent hue" strategy that keeps this whole
machine sub-family coherent while still letting each block be identified
without reading a tooltip. Unlike Pylon this block has no lit/unlit
blockstate (it has no BlockEntityTicker, see PrismiumRestorerBlockEntity
javadoc) so only one static texture is produced.

Self-review performed after generation: rendered at 16x nearest-neighbour
scale plus a dark "inventory slot" background composite (same extra
check used since Prismium Spike, session 18) and inspected via the Read
tool. See PROGRESS.md for the outcome of that check.

Run from repo root: python3 scripts/textures/gen_prismium_restorer.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"

# Same casing palette as Cell/Generator/Cable/Pylon (sessions 8-10, 19).
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"

SOCKET_SHADOW = "#12201F"

# Gold/amber "mending cross" - deliberately warm, distinct from every
# other Prismium Energy block's violet/cyan accent (see module docstring).
CROSS_CORE = "#FFF3C4"
CROSS_MID = "#FFC94D"
CROSS_EDGE = "#B8791A"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def paint_casing(px):
    casing_dark = hexrgb(CASING_DARK)
    casing_mid = hexrgb(CASING_MID)
    outline = hexrgb(PRISMIUM_OUTLINE)

    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*casing_dark, 255)

    for x in range(SIZE):
        px[x, 0] = (*casing_mid, 255)
        px[x, 1] = (*casing_mid, 255)
    for y in range(SIZE):
        px[0, y] = (*casing_mid, 255)
        px[1, y] = (*casing_mid, 255)

    for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        px[x, y] = (*casing_mid, 255)

    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)


def paint_cross(px):
    socket = hexrgb(SOCKET_SHADOW)
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = (*socket, 255)

    core = hexrgb(CROSS_CORE)
    mid = hexrgb(CROSS_MID)
    edge = hexrgb(CROSS_EDGE)

    # Plus/cross glyph: a 2px-wide vertical bar and a 2px-wide horizontal
    # bar crossing at the socket's center, edge-shaded like the Pylon's
    # crystal (outer ring = edge color, inner = mid, dead-center = core)
    # so it reads as a faceted glyph rather than a flat sticker.
    vertical = [(x, y) for y in range(4, 12) for x in (7, 8)]
    horizontal = [(x, y) for x in range(4, 12) for y in (7, 8)]
    cross_pixels = set(vertical) | set(horizontal)

    for (x, y) in cross_pixels:
        on_tip = y in (4, 11) or x in (4, 11)
        px[x, y] = (*(edge if on_tip else mid), 255)

    # Bright core at dead-center, plus two small corner sparkle pixels on
    # the horizontal arm's outer edge to suggest a faint glint - kept far
    # fewer/smaller than the cross itself so it stays a secondary detail.
    px[7, 7] = (*core, 255)
    px[8, 7] = (*core, 255)
    px[7, 8] = (*core, 255)
    px[8, 8] = (*core, 255)
    px[4, 6] = (*core, 255)
    px[11, 9] = (*core, 255)


def make_restorer():
    img = new_img()
    px = img.load()
    paint_casing(px)
    paint_cross(px)
    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_restorer(), "block/prismium_restorer.png")
