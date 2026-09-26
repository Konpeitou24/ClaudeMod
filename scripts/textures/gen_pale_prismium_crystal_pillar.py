#!/usr/bin/env python3
"""Generate the textures for Pale Prismium Crystal Pillar (scheduled
session, 2026-09-26 - see ModBlocks.PALE_PRISMIUM_CRYSTAL_PILLAR).

The Pale Prismium family's own version of Prismium Crystal Pillar
(gen_prismium_crystal_pillar.py, v0.58.0): same vanilla RotatedPillarBlock
reused verbatim (no new Java code, no new subclass), same two-texture
layout (fluted side face + concentric-ring end cap), just reskinned in
the icy PALE_* palette already established by every other Pale sibling
(gen_pale_prismium_block.py onward) - the same low-risk "texture/ID/
recipe-only" pattern used for Lantern/Geode Cluster/Stalactite/Bloom/
Spike/Wall Lamp.

  - block/pale_prismium_crystal_pillar.png (the "side" face): same
    vertical fluted column pattern as the teal original, but built from
    PALE_SHADOW/PALE_BASE/PALE_MID with PALE_ACCENT (the family's
    established "faint cyan energy fleck" accent, gen_pale_prismium_
    block.py) standing in for PRISMIUM_ACCENT's magenta vein.
  - block/pale_prismium_crystal_pillar_top.png (the "end" face): same
    concentric-ring crystal cross-section, but in the PALE ramp. The
    original's inner ring uses PRISMIUM_ACCENT_DARK (a darkened version
    of its own accent) which the Pale family has no established
    equivalent for yet, so PALE_ACCENT_DARK is derived here the same
    way (same hue as PALE_ACCENT, darkened/desaturated) rather than
    reusing a color from an unrelated part of the ramp.

No new subclass, no new @Override surface - only two textures plus a
RotatedPillarBlock registration reusing the exact same blockstate/model
shape (cube_column / cube_column_horizontal parents) as the original.
Rotation values are the same vanilla quartz_pillar.json values already
verified for the teal version - no new lookup needed since the shape is
identical, only the textures differ.

Self-review required after generation (per PROGRESS.md/HANDOFF.md rules):
open the 24x upscaled previews with Read and confirm (a) the flutes read
as a column at a glance, not noise, (b) the top/bottom rings read as a
crystal cross-section, (c) every filled pixel is fully opaque, (d) the
palette matches the rest of the Pale Prismium family and is clearly
distinct from the teal original.

Run from repo root: python3 scripts/textures/gen_pale_prismium_crystal_pillar.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures/block"

# Same PALE_* ramp as the rest of the Pale Prismium family (gen_pale_
# prismium_block.py onward) for cross-texture consistency.
PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"
PALE_ACCENT = "#7EE6FF"
# Darkened/desaturated variant of PALE_ACCENT, following the same
# relationship PRISMIUM_ACCENT_DARK has to PRISMIUM_ACCENT in the teal
# original's own script - no equivalent existed yet in the Pale family.
PALE_ACCENT_DARK = "#1B6E8C"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_side():
    """Vertical fluted column face."""
    img = new_img()
    px = img.load()

    outline = hexrgb(PALE_OUTLINE)
    shadow = hexrgb(PALE_SHADOW)
    base = hexrgb(PALE_BASE)
    mid = hexrgb(PALE_MID)
    accent = hexrgb(PALE_ACCENT)

    # Same four-column repeat (4px period) as the teal original: shadow,
    # base, mid, base, with the accent vein replacing the "peak" column
    # every 8px (two veins total, not one per flute).
    col_pattern = [shadow, base, mid, base]
    for x in range(SIZE):
        c = col_pattern[x % 4]
        if x % 8 == 1:
            c = accent
        for y in range(SIZE):
            px[x, y] = (*c, 255)

    # Thin darker cap band at the very top and bottom edge, matching the
    # teal original's segment-joint treatment.
    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
        px[x, 1] = (*shadow, 255)
        px[x, SIZE - 2] = (*shadow, 255)

    return img


def make_top():
    """Concentric-ring crystal cross-section, used for the end caps."""
    img = new_img()
    px = img.load()

    outline = hexrgb(PALE_OUTLINE)
    shadow = hexrgb(PALE_SHADOW)
    base = hexrgb(PALE_BASE)
    mid = hexrgb(PALE_MID)
    hilite = hexrgb(PALE_HILITE)
    accent_dark = hexrgb(PALE_ACCENT_DARK)

    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x - cx
            dy = y - cy
            dist = (dx * dx + dy * dy) ** 0.5
            if dist > 7.6:
                c = outline
            elif dist > 6.6:
                c = shadow
            elif dist > 5.0:
                c = base
            elif dist > 3.2:
                c = accent_dark
            elif dist > 1.6:
                c = mid
            else:
                c = hilite
            px[x, y] = (*c, 255)

    return img


def save_preview(img, name):
    scale = 24
    checker = Image.new("RGBA", (SIZE * scale, SIZE * scale), (0, 0, 0, 0))
    cpx = checker.load()
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            light = ((x // scale) + (y // scale)) % 2 == 0
            cpx[x, y] = (200, 200, 200, 255) if light else (150, 150, 150, 255)
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    checker.alpha_composite(big)
    preview_path = REPO_ROOT / f"{name}_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)

    side = make_side()
    side_path = ASSETS / "pale_prismium_crystal_pillar.png"
    side.save(side_path)
    print(f"wrote {side_path}")
    save_preview(side, "pale_prismium_crystal_pillar_side")

    top = make_top()
    top_path = ASSETS / "pale_prismium_crystal_pillar_top.png"
    top.save(top_path)
    print(f"wrote {top_path}")
    save_preview(top, "pale_prismium_crystal_pillar_top")

    for img, label in [(side, "side"), (top, "top")]:
        bad = [(x, y, img.getpixel((x, y))) for y in range(SIZE) for x in range(SIZE)
               if img.getpixel((x, y))[3] not in (0, 255)]
        if bad:
            print(f"WARNING: {label} has {len(bad)} partial-alpha pixels: {bad[:5]}")
        else:
            print(f"OK: {label} fully opaque")


if __name__ == "__main__":
    main()
