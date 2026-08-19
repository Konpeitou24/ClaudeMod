#!/usr/bin/env python3
"""Generate block textures for Prismium Compressor (session 70,
scheduled - see ModBlocks.PRISMIUM_COMPRESSOR / PrismiumCompressorBlock
/ PROGRESS.md).

Third machine in the mod's item-processing chain (Ore -[Pulverizer]->
Shard -[Smelter]-> Ingot -[this block]-> Alloy Ingot). Shares the
established metal-casing "machine" visual language (CASING_DARK/
CASING_MID against PRISMIUM_OUTLINE, same recessed 8x8 socket at
x/y 4..11 used by every machine since Prismium Generator) so it reads
as a member of the same family at a glance, same approach as
gen_prismium_smelter.py / gen_prismium_pulverizer.py.

What's unique to this block: a compression-press motif inside the
socket - two horizontal jaw/plate shapes (top and bottom) closing in
on a thin gap in the middle, reading as "squeezing something flat"
rather than Pulverizer's circular grinding gear or Smelter's ingot
mold. Idle (lit=false) is dull, motionless steel-gray jaws with a dark
gap between them (nothing being pressed right now); active (lit=true)
uses a bright cyan-white "compression force" glow filling the gap -
deliberately a *cool* palette, unlike Generator's ember-red and
Smelter's amber-gold, and a different hue from Pulverizer's magenta,
so a player can tell all three/four active machines apart at a glance
purely by glow color.

Self-review performed on first generation (both textures, 4x/8x/16x
upscaled previews copied to the outputs mount and viewed with Read):
confirm the press-jaw silhouette reads clearly at small scale in both
states, confirm idle vs lit contrast is unambiguous, confirm every
filled pixel is fully opaque.

Run from repo root: python3 scripts/textures/gen_prismium_compressor.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"

# Same casing palette as every other machine block in this mod.
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"

SOCKET_SHADOW = "#12201F"

# Idle jaws: dull, motionless steel - no compression happening.
JAW_IDLE_SHADOW = "#3E4A52"
JAW_IDLE_BASE = "#6E7C86"
JAW_IDLE_HILITE = "#94A3AC"

# Active gap glow: bright cyan-white "compression force" - deliberately
# cool/electric, distinct from every other machine's warm or magenta
# active glow (see module docstring).
GAP_LIT_SHADOW = "#1E6E78"
GAP_LIT_BASE = "#4FD2E0"
GAP_LIT_HILITE = "#DFFBFF"

# Idle gap: just dark, nothing being compressed.
GAP_IDLE = "#0D1616"

# Press-jaw rows inside the 4..11 socket: a wide top jaw (rows 4-6), a
# wide bottom jaw (rows 9-11), and a narrow gap (rows 7-8) between them
# where the glow appears when active.
TOP_JAW_ROWS = (4, 6)
BOTTOM_JAW_ROWS = (9, 11)
GAP_ROWS = (7, 8)
JAW_X0, JAW_X1 = 4, 11


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


def paint_press(px, lit):
    socket = hexrgb(SOCKET_SHADOW)
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = (*socket, 255)

    jaw_shadow = hexrgb(JAW_IDLE_SHADOW)
    jaw_base = hexrgb(JAW_IDLE_BASE)
    jaw_hilite = hexrgb(JAW_IDLE_HILITE)

    # Top jaw: hilite on its outer (top) edge, base body, shadow on the
    # inner edge facing the gap.
    y0, y1 = TOP_JAW_ROWS
    for x in range(JAW_X0, JAW_X1 + 1):
        px[x, y0] = (*jaw_hilite, 255)
    for y in range(y0 + 1, y1):
        for x in range(JAW_X0, JAW_X1 + 1):
            px[x, y] = (*jaw_base, 255)
    for x in range(JAW_X0, JAW_X1 + 1):
        px[x, y1] = (*jaw_shadow, 255)

    # Bottom jaw: mirrored - shadow on the inner (top) edge facing the
    # gap, base body, hilite on its outer (bottom) edge.
    y0b, y1b = BOTTOM_JAW_ROWS
    for x in range(JAW_X0, JAW_X1 + 1):
        px[x, y0b] = (*jaw_shadow, 255)
    for y in range(y0b + 1, y1b):
        for x in range(JAW_X0, JAW_X1 + 1):
            px[x, y] = (*jaw_base, 255)
    for x in range(JAW_X0, JAW_X1 + 1):
        px[x, y1b] = (*jaw_hilite, 255)

    # Gap between the jaws: dark and empty when idle, bright
    # cyan-white "compression glow" when active.
    gap0, gap1 = GAP_ROWS
    if lit:
        gap_shadow = hexrgb(GAP_LIT_SHADOW)
        gap_base = hexrgb(GAP_LIT_BASE)
        gap_hilite = hexrgb(GAP_LIT_HILITE)
        for x in range(JAW_X0, JAW_X1 + 1):
            px[x, gap0] = (*gap_hilite, 255)
            px[x, gap1] = (*gap_base, 255)
        px[JAW_X0, gap0] = (*gap_shadow, 255)
        px[JAW_X1, gap0] = (*gap_shadow, 255)
    else:
        gap_idle = hexrgb(GAP_IDLE)
        for x in range(JAW_X0, JAW_X1 + 1):
            px[x, gap0] = (*gap_idle, 255)
            px[x, gap1] = (*gap_idle, 255)


def build(lit):
    img = new_img()
    px = img.load()
    paint_casing(px)
    paint_press(px, lit)
    return img


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)

    idle = build(False)
    idle_path = out_dir / "prismium_compressor.png"
    idle.save(idle_path)
    print(f"Wrote {idle_path}")

    lit = build(True)
    lit_path = out_dir / "prismium_compressor_lit.png"
    lit.save(lit_path)
    print(f"Wrote {lit_path}")

    for name, img in [("idle", idle), ("lit", lit)]:
        bad = [(x, y, img.getpixel((x, y))) for y in range(SIZE) for x in range(SIZE)
               if img.getpixel((x, y))[3] not in (0, 255)]
        if bad:
            print(f"WARNING ({name}): {len(bad)} pixels with partial alpha: {bad[:5]}")
        else:
            print(f"OK ({name}): all pixels fully opaque or fully transparent")


if __name__ == "__main__":
    main()
