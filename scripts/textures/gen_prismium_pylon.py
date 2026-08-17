#!/usr/bin/env python3
"""Generate textures for Prismium Pylon (session 19): the mod's first FE
*consumer* - see ModBlocks.PRISMIUM_PYLON / PrismiumPylonBlock /
PrismiumPylonBlockEntity / PROGRESS.md.

The Prismium Energy trio added in sessions 8-10 (Cell = storage,
Generator = source, Cable = relay) never gained an actual sink: nothing
in the mod ever *spent* stored FE for a gameplay effect, so a fully wired
Generator->Cable->Cell setup had no payoff beyond "a battery got full".
Prismium Pylon closes that loop: it drains its own buffer to grant
nearby players Regeneration, functioning as a small explorer-support
outpost fixture (conceptually parallel to vanilla's Beacon, but fed by
this mod's own FE network instead of a pyramid+star).

Visually it joins the established "machine casing" family introduced by
Prismium Cell (session 8) and reused by Generator (session 9) and Cable
(session 10): same CASING_DARK/CASING_MID metal palette against the
shared PRISMIUM_OUTLINE border, same recessed 8x8 socket cut into a
cube_all texture. What differs is the socket content - Cell shows a
glass window, Generator an ember grate, Pylon shows a faceted crystal
spike (echoing the crystal silhouettes already used by Prismium Bloom/
Spike, sessions 17-18) that is dim/desaturated while idle (lit=false)
and glows with the mod's two established Prismium accent colors while
active (lit=true): PRISMIUM_ACCENT (violet, the mod's long-standing
"charged crystal" color, e.g. the Wraith egg highlight) at the core and
a cyan (matching Prismium Spike's cool accent, session 18) at the outer
edge, plus four small cyan "pulse" marks at the casing's edge midpoints
hinting at the aura radiating outward - distinct from Generator's
"energy flowing out" corner accents (which sit at the socket's corners,
not the casing's edges) so the two don't read as the same motif.

Self-review performed on first generation: both variants (idle/lit)
rendered at 16x nearest-neighbour scale plus a dark "inventory slot"
background composite (same extra check introduced in session 18 for
Prismium Spike) and inspected via the Read tool. Confirmed: the crystal
silhouette reads clearly as a faceted diamond shape at small size, the
idle/lit contrast is unambiguous, the four edge pulse marks on the lit
variant don't blend into the casing highlight, and every filled pixel is
fully opaque (alpha 255) with no stray transparency. No redraw needed -
see PROGRESS.md for the outcome of that check.

Run from repo root: python3 scripts/textures/gen_prismium_pylon.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_ACCENT = "#C97BFF"
CYAN_ACCENT = "#39E6D6"

# Same casing palette as Cell/Generator/Cable (sessions 8-10) - reused on
# purpose so the whole machine family reads as one visual language.
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"

SOCKET_SHADOW = "#12201F"

# Crystal spike, idle (lit=false): dim, desaturated teal - "asleep".
CRYSTAL_OFF_CORE = "#3F7A77"
CRYSTAL_OFF_EDGE = "#2B5C5A"

# Crystal spike, active (lit=true): violet core fading to cyan edge.
CRYSTAL_LIT_CORE = "#F2E3FF"
CRYSTAL_LIT_MID = "#C97BFF"
CRYSTAL_LIT_EDGE = "#39E6D6"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def paint_casing(px, lit):
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

    # Pulse marks at the casing's edge midpoints - only while active. Sits
    # just inside the outline border (drawn after this, so the border
    # stays crisp) to read as "the aura reaching the edge of the block"
    # rather than a socket-adjacent detail like Generator's corner accents.
    if lit:
        cyan = hexrgb(CYAN_ACCENT)
        for (x, y) in [(7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)]:
            px[x, y] = (*cyan, 255)

    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)


def paint_crystal(px, lit):
    socket = hexrgb(SOCKET_SHADOW)
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = (*socket, 255)

    if lit:
        core = hexrgb(CRYSTAL_LIT_CORE)
        mid = hexrgb(CRYSTAL_LIT_MID)
        edge = hexrgb(CRYSTAL_LIT_EDGE)
    else:
        core = hexrgb(CRYSTAL_OFF_CORE)
        mid = hexrgb(CRYSTAL_OFF_EDGE)
        edge = hexrgb(CRYSTAL_OFF_EDGE)

    # Faceted diamond/spike silhouette, widest at the "shoulder" (y=7),
    # tapering to points above and below - reads as a crystal shard held
    # in the socket, distinct from Cell's flat window pane and Generator's
    # horizontal ember slits.
    rows = {
        4: (7, 8),
        5: (6, 9),
        6: (6, 9),
        7: (5, 10),
        8: (6, 9),
        9: (6, 9),
        10: (7, 8),
        11: (7, 8),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            if x in (x0, x1):
                c = edge
            elif y == 7:
                c = core
            else:
                c = mid
            px[x, y] = (*c, 255)
    # Bright single-pixel core at the shoulder's very center.
    px[7, 7] = (*core, 255)
    px[8, 7] = (*core, 255)


def make_pylon(lit):
    img = new_img()
    px = img.load()
    paint_casing(px, lit)
    paint_crystal(px, lit)
    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_pylon(False), "block/prismium_pylon.png")
    save(make_pylon(True), "block/prismium_pylon_lit.png")
