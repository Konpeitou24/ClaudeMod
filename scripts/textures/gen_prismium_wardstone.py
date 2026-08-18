#!/usr/bin/env python3
"""Generate the textures for Prismium Wardstone (session 21): the mod's
*third* FE consumer, after Prismium Pylon (session 19) and Prismium
Restorer (session 20) - see ModBlocks.PRISMIUM_WARDSTONE /
PrismiumWardstoneBlock / PrismiumWardstoneBlockEntity / PROGRESS.md.

Joins the established "machine casing" family introduced by Prismium
Cell (session 8) and reused by Generator (9), Cable (10), Pylon (19) and
Restorer (20): same CASING_DARK/CASING_MID metal palette against the
shared PRISMIUM_OUTLINE border, same recessed 8x8 socket cut into a
cube_all texture (paint_casing/SIZE/hexrgb lifted near-verbatim from
gen_prismium_pylon.py, on purpose - reusing an already-reviewed helper
instead of re-deriving pixel offsets from scratch, same as Restorer did
for the same reason last session).

What differs is the socket content and accent color, following the "reuse
the frame, swap the accent hue" strategy established by Restorer (gold
cross) against Pylon (violet/cyan crystal): Wardstone shows a crimson/red
hexagonal "rune ring" - a closed hexagon outline reads as a barrier/ward
shape (as opposed to Pylon's radiating crystal or Restorer's cross), and
red is otherwise unused across the whole Prismium Energy sub-family, so it
is immediately distinguishable at a glance despite sharing the same
casing shell. Like Pylon (and unlike Restorer, which has no
BlockEntityTicker), this block has an idle/active BlockStateProperties.LIT
swap, so two textures are produced: an unlit (dim rune) and a lit (bright
rune) variant.

Self-review performed after generation: rendered at 16x nearest-neighbour
scale plus a dark "inventory slot" background composite (same extra check
used since Prismium Spike, session 18) and inspected via the Read tool.
See PROGRESS.md for the outcome of that check.

Run from repo root: python3 scripts/textures/gen_prismium_wardstone.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"

# Same casing palette as Cell/Generator/Cable/Pylon/Restorer.
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"

SOCKET_SHADOW = "#12201F"

# Crimson "ward rune" - unlit is a dim, dormant red; lit is a bright,
# almost-white-hot red core, mirroring how Pylon's crystal and Restorer's
# cross both use a 3-tone edge/mid/core ramp but with red instead of
# violet or gold, so it can't be confused with either at a glance.
RUNE_OFF_EDGE = "#5A1414"
RUNE_OFF_MID = "#7A1E1E"
RUNE_LIT_EDGE = "#B8221F"
RUNE_LIT_MID = "#FF4A3D"
RUNE_LIT_CORE = "#FFDCC4"
RUNE_OFF_CORE = "#8A2B24"


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


# Hexagon ring outline pixels within the 4..12 socket, hand-placed to read
# as a closed six-sided barrier shape (flat top/bottom, angled sides) -
# distinct from Pylon's diamond spike (open, pointed top/bottom) and
# Restorer's cross (open, radiating arms). A *closed* ring is the whole
# point: it should visually suggest "contained ward", not "projecting
# beam".
HEX_RING = [
    (6, 4), (7, 4), (8, 4), (9, 4),
    (5, 5), (10, 5),
    (4, 6), (11, 6),
    (4, 7), (11, 7),
    (4, 8), (11, 8),
    (4, 9), (11, 9),
    (5, 10), (10, 10),
    (6, 11), (7, 11), (8, 11), (9, 11),
]

# Inner fill (the "glow" behind the ring) - kept sparse/center-weighted so
# the ring silhouette stays the dominant read at small sizes.
HEX_FILL = [
    (7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9),
]

HEX_CORE = [(7, 7), (8, 7), (7, 8), (8, 8)]


def paint_rune(px, lit):
    socket = hexrgb(SOCKET_SHADOW)
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = (*socket, 255)

    if lit:
        edge = hexrgb(RUNE_LIT_EDGE)
        mid = hexrgb(RUNE_LIT_MID)
        core = hexrgb(RUNE_LIT_CORE)
    else:
        edge = hexrgb(RUNE_OFF_EDGE)
        mid = hexrgb(RUNE_OFF_MID)
        core = hexrgb(RUNE_OFF_CORE)

    for (x, y) in HEX_RING:
        px[x, y] = (*edge, 255)
    for (x, y) in HEX_FILL:
        px[x, y] = (*mid, 255)
    for (x, y) in HEX_CORE:
        px[x, y] = (*core, 255)


def make_wardstone(lit):
    img = new_img()
    px = img.load()
    paint_casing(px)
    paint_rune(px, lit)
    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_wardstone(False), "block/prismium_wardstone.png")
    save(make_wardstone(True), "block/prismium_wardstone_lit.png")
