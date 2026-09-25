#!/usr/bin/env python3
"""Generate the textures for Prismium Crystal Pillar (scheduled session,
2026-09-25 - see ModBlocks.PRISMIUM_CRYSTAL_PILLAR).

This is the mod's first axis-rotatable block (net.minecraft.world.level.
block.RotatedPillarBlock, used verbatim with no custom subclass - same
"lowest risk new block type" reasoning session 34's PROGRESS.md entry
gave for the original SlabBlock/WallBlock/StairBlock trio, since
RotatedPillarBlock is a stock vanilla class with a single
BlockBehaviour.Properties constructor, confirmed to exist with that exact
signature via mappings.dev/1.20.1 before use). It gives the "building
variety" pillar of Prismium Block that Slab/Wall/Stairs (session 34) never
covered - the vertical, load-bearing-column silhouette vanilla itself
gives Quartz/Purpur/Basalt but this mod's Prismium family didn't have
yet.

Two textures, both derived from the same PRISMIUM_* ramp gen_prismium.py
established (sampled from the user's own hand-drawn prismium_block.png,
see that script's docstring) so this reads as an obvious sibling of the
existing block/core/alloy-block family rather than a new material:

  - block/prismium_crystal_pillar.png (the "side" face): vertical fluted
    column, alternating light/dark vertical bands (mimicking a carved
    stone/crystal pillar's flutes) with a thin darker cap band at the very
    top and bottom edges (reads as a segment joint when stacked), and a
    single accent-colored vein down the center flute for a "this is
    crystal, not plain stone" tell - echoes PRISMIUM_ACCENT's use as a
    small color accent across the rest of the family (Alloy Block's
    corner chips, Block's gem clusters) without overusing it.
  - block/prismium_crystal_pillar_top.png (the "end" face, used for both
    the top/bottom of the vertical orientation via cube_column and the
    end caps of the two horizontal orientations via
    cube_column_horizontal): concentric rings from a bright center out to
    the dark outline, reading as a cut crystal cross-section - the same
    "look into the crystal" idea Prismium Core's own top-down light
    Level suggests, rendered explicitly here since a pillar's end cap is
    the one face where "what is this pillar made of, on the inside"
    actually shows.

No new subclass, no new @Override surface - only two textures plus a
RotatedPillarBlock registration, two blockstate model variants (vertical
"claudemod:block/prismium_crystal_pillar" parented on vanilla's own
minecraft:block/cube_column, horizontal
"claudemod:block/prismium_crystal_pillar_horizontal" parented on
minecraft:block/cube_column_horizontal) and the item model. Blockstate
rotation values (axis=x: x=90,y=90; axis=y: none; axis=z: x=90) copied
verbatim from two independently-fetched vanilla quartz_pillar.json
mirrors (raw.githubusercontent.com/InventivetalentDev/minecraft-assets,
1.20.1 branch) rather than guessed from memory, per the mod's standing
"don't invent vanilla rotation values" rule.

Self-review required after generation (per PROGRESS.md/HANDOFF.md rules):
open the 24x upscaled previews with Read and confirm (a) the flutes read
as a column at a glance, not noise, (b) the top/bottom rings read as a
crystal cross-section, (c) every filled pixel is fully opaque, (d) the
palette matches the rest of the Prismium family.

Run from repo root: python3 scripts/textures/gen_prismium_crystal_pillar.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures/block"

# Same outline/shadow/base/mid/hilite/accent ramp as the rest of the
# Prismium family (gen_prismium.py onward) for cross-texture consistency.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_DARK = "#720070"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_side():
    """Vertical fluted column face."""
    img = new_img()
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    accent = hexrgb(PRISMIUM_ACCENT)

    # Four repeating flute columns across the 16px width (4px period):
    # shadow, base, mid, base - a simple light/dark ripple that reads as
    # a carved fluted column, not a flat panel. x=1 and x=9 (the "peak"
    # column of each flute repeat) get the accent vein instead of mid,
    # so there are two thin crystal-colored veins running the height of
    # the block - deliberately not one per flute (four) since that would
    # read as too busy/gem-covered for a plain building block.
    col_pattern = [shadow, base, mid, base]
    for x in range(SIZE):
        c = col_pattern[x % 4]
        if x % 8 == 1:
            c = accent
        for y in range(SIZE):
            px[x, y] = (*c, 255)

    # Thin darker cap band at the very top and bottom edge - reads as a
    # segment joint when several pillars are stacked, matching how
    # vanilla quartz_pillar's side texture has a subtle top/bottom border.
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

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    accent_dark = hexrgb(PRISMIUM_ACCENT_DARK)

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
    side_path = ASSETS / "prismium_crystal_pillar.png"
    side.save(side_path)
    print(f"wrote {side_path}")
    save_preview(side, "prismium_crystal_pillar_side")

    top = make_top()
    top_path = ASSETS / "prismium_crystal_pillar_top.png"
    top.save(top_path)
    print(f"wrote {top_path}")
    save_preview(top, "prismium_crystal_pillar_top")

    for img, label in [(side, "side"), (top, "top")]:
        bad = [(x, y, img.getpixel((x, y))) for y in range(SIZE) for x in range(SIZE)
               if img.getpixel((x, y))[3] not in (0, 255)]
        if bad:
            print(f"WARNING: {label} has {len(bad)} partial-alpha pixels: {bad[:5]}")
        else:
            print(f"OK: {label} fully opaque")


if __name__ == "__main__":
    main()
