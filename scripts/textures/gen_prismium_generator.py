#!/usr/bin/env python3
"""Generate textures for Prismium Generator (session 9's first
BlockEntityTicker block - see ModBlocks.PRISMIUM_GENERATOR /
PrismiumGeneratorBlock / PrismiumGeneratorBlockEntity / PROGRESS.md).

This block needs *two* textures, not one: it's the mod's first block to
use a blockstate property (BlockStateProperties.LIT, reused from vanilla)
to swap its model between an idle and an active look, matching furnace's
approach. Both variants share the same metal-casing "machine" visual
language introduced by Prismium Cell in session 8 (CASING_DARK/
CASING_MID against the shared PRISMIUM_OUTLINE border) - deliberately
reused rather than invented from scratch, since Generator and Cell are
meant to read as members of the same "machine" sub-family, paired with
each other in play. What differs between the two blocks is the recessed
socket in the middle: Cell shows a glass window with the Prismium glow,
Generator shows a furnace-style ember grate (three horizontal vents) that
is dark/cold in the idle (lit=false) texture and glowing orange in the
active (lit=true) one, plus small cyan corner accents on the active
texture hinting that FE is flowing out (tying it back to the shared
Prismium color language, distinct from the ember orange which represents
the burning fuel rather than the FE it produces).

Self-review performed on first generation (both textures, 16x/4x
upscaled previews): confirmed the ember grate reads clearly as three
distinct horizontal slits at small scale in both states, confirmed the
lit/unlit contrast is unambiguous side-by-side (this is the whole point
of the two-texture split - if they looked too similar it would defeat
the purpose), and confirmed all filled pixels are fully opaque (alpha
255) with no stray transparent pixels inside the casing. No redraw was
needed this time - reusing the already-validated Cell casing palette
(session 8 learned the hard way that a from-scratch metal palette can
clash with PRISMIUM_OUTLINE) meant the frame contrast problem that hit
Cell's first draft didn't recur here.

Run from repo root: python3 scripts/textures/gen_prismium_generator.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_ACCENT = "#C97BFF"

# Same casing palette as Prismium Cell (session 8) - deliberately reused,
# not reinvented, so the two machines read as a matched pair. See module
# docstring.
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"

# The recessed firebox cavity behind the ember grate - near-black so the
# grate's own colors (whether cold or glowing) read clearly against it.
SOCKET_SHADOW = "#12201F"

# Ember grate, idle (lit=false): cold, extinguished coals - dark
# red-browns, no brightness variation beyond the two-tone slit pattern.
EMBER_OFF = "#3A2420"
EMBER_OFF_MID = "#54332C"

# Ember grate, active (lit=true): hot glow gradient from the slit edges
# (warm/darker) to the center (hottest/brightest).
EMBER_LIT_CORE = "#FFE9B0"
EMBER_LIT_HOT = "#FF9A3C"
EMBER_LIT_WARM = "#C6501F"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def paint_casing(px):
    casing_dark = hexrgb(CASING_DARK)
    casing_mid = hexrgb(CASING_MID)
    outline = hexrgb(PRISMIUM_OUTLINE)

    # 1) Base fill: whole canvas starts as metal casing.
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*casing_dark, 255)

    # 2) Casing highlight, two pixels deep, top+left ("sunward" lighting
    # convention shared with Cell/Lantern).
    for x in range(SIZE):
        px[x, 0] = (*casing_mid, 255)
        px[x, 1] = (*casing_mid, 255)
    for y in range(SIZE):
        px[0, y] = (*casing_mid, 255)
        px[1, y] = (*casing_mid, 255)

    # 3) Rivets at the four inner corners where the casing meets the
    # recessed socket (socket occupies x/y 4..11).
    for (x, y) in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        px[x, y] = (*casing_mid, 255)

    # 4) Crisp 1px outline border, drawn last so it stays crisp over the
    # highlight.
    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)


def paint_grate(px, lit):
    socket = hexrgb(SOCKET_SHADOW)

    # Recessed firebox cavity, 8x8, well inset into the casing.
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = (*socket, 255)

    rows = [6, 8, 10]
    if lit:
        core = hexrgb(EMBER_LIT_CORE)
        hot = hexrgb(EMBER_LIT_HOT)
        warm = hexrgb(EMBER_LIT_WARM)
        for ry in rows:
            for x in range(5, 11):
                if x in (5, 10):
                    c = warm
                elif x in (6, 9):
                    c = hot
                else:
                    c = core
                px[x, ry] = (*c, 255)

        # Small cyan "FE flowing out" accents at the socket's inner
        # corners - only present while active, ties the ember glow (the
        # fuel burning) back to the Prismium energy it's producing.
        accent = hexrgb(PRISMIUM_ACCENT)
        for (x, y) in [(4, 4), (11, 4), (4, 11), (11, 11)]:
            px[x, y] = (*accent, 255)
    else:
        off = hexrgb(EMBER_OFF)
        off_mid = hexrgb(EMBER_OFF_MID)
        for ry in rows:
            for x in range(5, 11):
                c = off_mid if x in (6, 9) else off
                px[x, ry] = (*c, 255)


def make_generator(lit):
    img = new_img()
    px = img.load()
    paint_casing(px)
    paint_grate(px, lit)
    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_generator(False), "block/prismium_generator.png")
    save(make_generator(True), "block/prismium_generator_lit.png")
