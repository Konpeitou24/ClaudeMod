#!/usr/bin/env python3
"""Generate the Door textures for the six block families the Door
silhouette is being rolled out to in this session (scheduled session,
2026-10-09): Prismium Core, Prismium Alloy Block, Prismium Stone,
Prismium Deepstone, Prismium Bricks, Prismium Deepstone Bricks. This is
the same horizontal-rollout step already done once for Fence/Fence Gate
(v0.66.0 -> v0.67.0) and once for Trapdoor (v0.68.0 -> v0.69.0), applied
to the newest "door" silhouette (v0.70.0, Prismium Block / Pale Prismium
Block only so far).

Technique: reuses gen_prismium_door.py's own "derive the two 16x16 door
halves (bottom/top) from the family's existing plain block texture, plus
a darker 2px frame on the outer edges and a 2px seam at the half-to-half
join" approach verbatim, rather than inventing a new per-family drawing
routine. Colours are taken only from palette constants each family's own
existing generation script already defines (gen_prismium_core.py's
PRISMIUM_OUTLINE/PRISMIUM_ACCENT/PRISMIUM_ACCENT_DARK/PRISMIUM_HILITE,
gen_prismium_alloy_block.py's PRISMIUM_OUTLINE/PRISMIUM_ACCENT/
PRISMIUM_ACCENT_DARK plus gen_prismium_alloy_ingot.py's METAL_HILITE, and
gen_prismium_stone.py's/gen_prismium_deepstone.py's own *_DARK_EDGE mortar
colours) - never a freshly invented colour.

Stone/Deepstone (and the Bricks pair cut from the same two palettes) are
deliberately given NO cyan/teal accent-gem or handle highlight: GitHub
issue #22 ("紛らわしいリソースパック") reported that sprinkling the
Prismium-family teal accent onto Stone made it hard to tell apart from
Prismium Ore, and gen_prismium_stone.py/gen_prismium_deepstone.py were
both corrected to stay plain-grey in response (see those two scripts'
own docstrings and PROGRESS.md "1. 約束や決まり事"). Repeating that same
accent on these doors would reopen the exact same complaint, so these
four families' doors use only their own existing grey mortar/edge shades
for the frame, seam and corner/handle marks - no new hue introduced.
Core and Alloy Block keep a small corner-accent-gem pair (matching the
visual language PRISMIUM_DOOR/PALE_PRISMIUM_DOOR already established),
since neither has ever been the subject of an "easily confused with
another block" complaint.

Self-review: run this script, then open the six *_door_bottom.png /
*_door_top.png / <item>.png files with Read (upscaled) before committing,
exactly like every prior texture-generation script in this mod.

Run from repo root: python3 scripts/textures/gen_doors_expansion.py
"""
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[2]
BLOCK_DIR = REPO_ROOT / "src/main/resources/assets/claudemod/textures/block"
ITEM_DIR = REPO_ROOT / "src/main/resources/assets/claudemod/textures/item"

# name -> (src block texture, frame, seam, accent-or-None, accent_dark-or-None, handle)
FAMILIES = {
    "prismium_core": {
        "src": BLOCK_DIR / "prismium_core.png",
        "frame": (2, 77, 75, 255),        # PRISMIUM_OUTLINE
        "seam": (1, 54, 53, 255),         # darker than the outline
        "accent": (255, 124, 252, 255),   # PRISMIUM_ACCENT
        "accent_dark": (114, 0, 112, 255),  # PRISMIUM_ACCENT_DARK
        "handle": (202, 253, 249, 255),   # PRISMIUM_HILITE
    },
    "prismium_alloy_block": {
        "src": BLOCK_DIR / "prismium_alloy_block.png",
        "frame": (2, 77, 75, 255),        # PRISMIUM_OUTLINE (alloy block reuses it too)
        "seam": (1, 54, 53, 255),
        "accent": (214, 51, 176, 255),    # D633B0
        "accent_dark": (138, 30, 115, 255),  # 8A1E73
        "handle": (234, 243, 247, 255),   # METAL_HILITE
    },
    "prismium_stone": {
        "src": BLOCK_DIR / "prismium_stone.png",
        "frame": (96, 96, 96, 255),       # STONE_DARK_EDGE
        "seam": (70, 70, 70, 255),
        "accent": None,
        "accent_dark": None,
        "handle": (150, 150, 150, 255),
    },
    "prismium_deepstone": {
        "src": BLOCK_DIR / "prismium_deepstone.png",
        "frame": (46, 46, 49, 255),       # DEEPSTONE_DARK_EDGE
        "seam": (30, 30, 32, 255),
        "accent": None,
        "accent_dark": None,
        "handle": (85, 85, 89, 255),
    },
    "prismium_bricks": {
        "src": BLOCK_DIR / "prismium_bricks.png",
        "frame": (96, 96, 96, 255),       # STONE_DARK_EDGE (same cut as Stone)
        "seam": (70, 70, 70, 255),
        "accent": None,
        "accent_dark": None,
        "handle": (150, 150, 150, 255),
    },
    "prismium_deepstone_bricks": {
        "src": BLOCK_DIR / "prismium_deepstone_bricks.png",
        "frame": (46, 46, 49, 255),       # DEEPSTONE_DARK_EDGE (same cut as Deepstone)
        "seam": (30, 30, 32, 255),
        "accent": None,
        "accent_dark": None,
        "handle": (85, 85, 89, 255),
    },
}


def build_half(src, frame, seam, accent, accent_dark, handle, is_top):
    """Build one 16x16 door-half texture from the base block texture.
    Mirrors gen_prismium_door.py's build_half() exactly."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), src.getpixel((x, y)))

    for y in range(16):
        img.putpixel((0, y), frame)
        img.putpixel((15, y), frame)

    if is_top:
        for x in range(16):
            img.putpixel((x, 0), frame)
        for x in range(16):
            img.putpixel((x, 14), seam)
            img.putpixel((x, 15), seam)
        if accent is not None:
            for (cx, cy) in [(1, 1), (14, 1)]:
                img.putpixel((cx, cy), accent)
            for (cx, cy) in [(2, 1), (13, 1)]:
                img.putpixel((cx, cy), accent_dark)
    else:
        for x in range(16):
            img.putpixel((x, 15), frame)
        for x in range(16):
            img.putpixel((x, 0), seam)
            img.putpixel((x, 1), seam)
        if accent is not None:
            for (cx, cy) in [(1, 14), (14, 14)]:
                img.putpixel((cx, cy), accent)
            for (cx, cy) in [(2, 14), (13, 14)]:
                img.putpixel((cx, cy), accent_dark)
        img.putpixel((12, 6), handle)
        img.putpixel((12, 7), handle)

    return img


def build_item_icon(bottom, top):
    icon = Image.new("RGBA", (16, 16))
    top_small = top.resize((16, 8), Image.NEAREST)
    bottom_small = bottom.resize((16, 8), Image.NEAREST)
    for x in range(16):
        for y in range(8):
            icon.putpixel((x, y), top_small.getpixel((x, y)))
        for y in range(8):
            icon.putpixel((x, y + 8), bottom_small.getpixel((x, y)))
    return icon


def main():
    for name, cfg in FAMILIES.items():
        src = Image.open(cfg["src"]).convert("RGBA")
        bottom = build_half(src, cfg["frame"], cfg["seam"], cfg["accent"], cfg["accent_dark"], cfg["handle"], is_top=False)
        top = build_half(src, cfg["frame"], cfg["seam"], cfg["accent"], cfg["accent_dark"], cfg["handle"], is_top=True)
        out_bottom = BLOCK_DIR / f"{name}_door_bottom.png"
        out_top = BLOCK_DIR / f"{name}_door_top.png"
        out_item = ITEM_DIR / f"{name}_door.png"
        bottom.save(out_bottom)
        top.save(out_top)
        build_item_icon(bottom, top).save(out_item)
        print(f"{name}: wrote {out_bottom.name}, {out_top.name}, {out_item.name}")


if __name__ == "__main__":
    main()
