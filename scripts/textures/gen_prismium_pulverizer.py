#!/usr/bin/env python3
"""Generate textures for Prismium Pulverizer (session 67 - see
ModBlocks.PRISMIUM_PULVERIZER / PrismiumPulverizerBlock /
PrismiumPulverizerBlockEntity / PROGRESS.md).

Like Prismium Generator (session 9), this block needs two textures for
its two LIT blockstate variants. Shares the same metal-casing "machine"
visual language established by Prismium Cell (session 8) and reused ever
since (CASING_DARK/CASING_MID against PRISMIUM_OUTLINE, same recessed
8x8 socket footprint as Generator's ember grate at x/y 4..11) so this
reads as a member of the same machine family at a glance. What's unique
to this block: a circular *gear* motif inside the socket (a body disc +
four cardinal teeth, built from a simple distance-from-center threshold
rather than an angle/atan2 sweep - the atan2 pinwheel approach was tried
for Prismium Geyser in session 66 and abandoned there for looking noisy
at small scale, see gen_prismium_geyser.py's docstring, so this script
skips straight to the simpler, already-learned-safer technique), idle
(lit=false) as flat steel gray, active (lit=true) with the gear's teeth
and rim glowing the mod's magenta Prismium accent color plus a bright
core - reading as "grinding, powered by Prismium energy" rather than
Generator's warm ember-fire palette (this machine burns FE directly, not
a fuel item, so no warm/ember palette would make sense here).

Self-review performed on first generation (both textures, 4x/8x/16x
upscaled previews copied to the outputs mount and viewed with Read):
confirmed the gear silhouette (round body + four teeth) reads clearly at
all three scales in both states, confirmed lit vs idle contrast is
unambiguous, confirmed all filled pixels are fully opaque (alpha 255).

Run from repo root: python3 scripts/textures/gen_prismium_pulverizer.py
"""
import math
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_ACCENT = "#FF7CFC"

# Same casing palette as Cell/Generator/Wardstone/Geyser - deliberately
# reused, not reinvented, so every machine block reads as one family.
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"

SOCKET_SHADOW = "#12201F"

# Idle gear: cold, dormant steel - no glow at all.
GEAR_IDLE_BODY = "#6E7C7A"
GEAR_IDLE_TEETH = "#8C9C99"
GEAR_IDLE_HUB = "#3A4644"

# Active gear: magenta Prismium-energy glow gradient, rim (teeth) darkest
# warm-magenta, body mid-magenta, hub brightest near-white-pink.
GEAR_LIT_TEETH = "#B23FA8"
GEAR_LIT_BODY = "#FF7CFC"
GEAR_LIT_HUB = "#FFE3FB"


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


def paint_gear(px, lit):
    socket = hexrgb(SOCKET_SHADOW)
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = (*socket, 255)

    cx, cy = 7.5, 7.5
    if lit:
        body_c = hexrgb(GEAR_LIT_BODY)
        teeth_c = hexrgb(GEAR_LIT_TEETH)
        hub_c = hexrgb(GEAR_LIT_HUB)
    else:
        body_c = hexrgb(GEAR_IDLE_BODY)
        teeth_c = hexrgb(GEAR_IDLE_TEETH)
        hub_c = hexrgb(GEAR_IDLE_HUB)

    # Round gear body: a filled disc, radius ~2.6.
    for y in range(4, 12):
        for x in range(4, 12):
            dx = x - cx + 0.5
            dy = y - cy + 0.5
            dist = math.hypot(dx, dy)
            if dist <= 2.6:
                px[x, y] = (*body_c, 255)

    # Four cardinal teeth: single pixels just outside the body disc, at
    # up/down/left/right - a simple, unambiguous "gear" cue at this scale
    # (a full ring of many small teeth would blur into noise at 16x16,
    # same lesson gen_prismium_geyser.py already documented for a denser
    # angular pattern).
    for (dx, dy) in [(0, -3), (0, 3), (-3, 0), (3, 0)]:
        x = round(cx - 0.5 + dx)
        y = round(cy - 0.5 + dy)
        px[x, y] = (*teeth_c, 255)

    # Small 2x2 bright hub at the very center.
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        px[x, y] = (*hub_c, 255)


def build(lit):
    img = new_img()
    px = img.load()
    paint_casing(px)
    paint_gear(px, lit)
    return img


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)

    idle = build(False)
    idle_path = out_dir / "prismium_pulverizer.png"
    idle.save(idle_path)
    print(f"Wrote {idle_path}")

    lit = build(True)
    lit_path = out_dir / "prismium_pulverizer_lit.png"
    lit.save(lit_path)
    print(f"Wrote {lit_path}")

    # Sanity check: every painted pixel should be fully opaque (this
    # texture never uses partial transparency).
    for name, img in [("idle", idle), ("lit", lit)]:
        bad = [ (x, y, img.getpixel((x, y))) for y in range(SIZE) for x in range(SIZE)
                if img.getpixel((x, y))[3] not in (0, 255) ]
        if bad:
            print(f"WARNING ({name}): {len(bad)} pixels with partial alpha: {bad[:5]}")
        else:
            print(f"OK ({name}): all pixels fully opaque or fully transparent")


if __name__ == "__main__":
    main()
