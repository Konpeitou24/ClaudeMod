#!/usr/bin/env python3
"""Generate block textures for Prismium Smelter (session 68 - see
ModBlocks.PRISMIUM_SMELTER / PrismiumSmelterBlock / PROGRESS.md).

Second machine in the mod's item-processing chain (Ore -[Pulverizer,
session 67]-> Shard -[this block]-> Ingot). Shares the established
metal-casing "machine" visual language (CASING_DARK/CASING_MID against
PRISMIUM_OUTLINE, same recessed 8x8 socket at x/y 4..11 used by every
machine since Prismium Generator) so it reads as a member of the same
family at a glance, same as gen_prismium_pulverizer.py's approach.

What's unique to this block: an ingot-mold motif inside the socket (a
small trapezoid bar shape, matching Prismium Ingot's own item-icon
silhouette so a player can visually connect "this machine" with "this
output item") rather than Pulverizer's circular gear. Idle (lit=false)
is cast/cooled metal in cool grays; active (lit=true) uses a warm
amber/gold "molten metal" glow - deliberately different from both
Generator's ember-red fuel-fire palette and Pulverizer's magenta
FE-energy palette, since this machine's whole identity is "refining
metal", and warm gold reads unambiguously as "hot metal" even though,
like Pulverizer, it is powered by FE rather than a burning fuel item.

Self-review performed on first generation (both textures, 4x/8x/16x
upscaled previews copied to the outputs mount and viewed with Read):
confirm the ingot-mold silhouette reads clearly at small scale in both
states, confirm idle vs lit contrast is unambiguous, confirm every
filled pixel is fully opaque.

Run from repo root: python3 scripts/textures/gen_prismium_smelter.py
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

# Idle mold: cooled, cast metal - dull cool gray-bronze, no glow.
MOLD_IDLE_SHADOW = "#5B5147"
MOLD_IDLE_BASE = "#8A7B67"
MOLD_IDLE_HILITE = "#A79A87"

# Active mold: molten amber/gold glow, brightest at the top (where the
# "pour" would be), darker amber at the base.
MOLD_LIT_SHADOW = "#8A5A1E"
MOLD_LIT_BASE = "#E3A947"
MOLD_LIT_HILITE = "#FFE9A8"

# Ingot-mold silhouette, same row-shape family as
# gen_prismium_ingot.py's item icon (narrower top, wider bottom),
# positioned inside the 4..11 socket.
ROWS = {
    6: (6, 9),
    7: (5, 10),
    8: (5, 10),
    9: (5, 10),
    10: (5, 10),
}


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


def paint_mold(px, lit):
    socket = hexrgb(SOCKET_SHADOW)
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = (*socket, 255)

    if lit:
        shadow_c = hexrgb(MOLD_LIT_SHADOW)
        base_c = hexrgb(MOLD_LIT_BASE)
        hilite_c = hexrgb(MOLD_LIT_HILITE)
    else:
        shadow_c = hexrgb(MOLD_IDLE_SHADOW)
        base_c = hexrgb(MOLD_IDLE_BASE)
        hilite_c = hexrgb(MOLD_IDLE_HILITE)

    for y, (x0, x1) in ROWS.items():
        for x in range(x0, x1 + 1):
            px[x, y] = (*base_c, 255)

    y_top = min(ROWS.keys())
    x0t, x1t = ROWS[y_top]
    for x in range(x0t, x1t + 1):
        px[x, y_top] = (*hilite_c, 255)

    y_bottom = max(ROWS.keys())
    x0b, x1b = ROWS[y_bottom]
    for x in range(x0b, x1b + 1):
        px[x, y_bottom] = (*shadow_c, 255)


def build(lit):
    img = new_img()
    px = img.load()
    paint_casing(px)
    paint_mold(px, lit)
    return img


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)

    idle = build(False)
    idle_path = out_dir / "prismium_smelter.png"
    idle.save(idle_path)
    print(f"Wrote {idle_path}")

    lit = build(True)
    lit_path = out_dir / "prismium_smelter_lit.png"
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
