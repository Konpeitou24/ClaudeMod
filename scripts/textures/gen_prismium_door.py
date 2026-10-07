#!/usr/bin/env python3
"""Generate Prismium Door / Pale Prismium Door textures (bottom + top halves).

Derives each door's two 16x16 textures from that family's existing base
block texture (prismium_block.png / pale_prismium_block.png), reusing the
same diagonal-streak fill + darker frame + corner-accent-gem language the
rest of the family (trapdoor, fence, wall lamp, etc.) already uses, instead
of inventing a new unrelated palette. Adds a continuous 2px seam across the
bottom-of-top / top-of-bottom rows (same "paneled hatch" convention used for
the v0.68/v0.69 trapdoors) so the two halves read as one tall door when
stacked, plus a small handle-gem accent near the middle height.
"""
import sys
from PIL import Image

FAMILIES = {
    "prismium": {
        "src": "src/main/resources/assets/claudemod/textures/block/prismium_block.png",
        "out_bottom": "src/main/resources/assets/claudemod/textures/block/prismium_door_bottom.png",
        "out_top": "src/main/resources/assets/claudemod/textures/block/prismium_door_top.png",
        "out_item": "src/main/resources/assets/claudemod/textures/item/prismium_door.png",
        "frame": (0, 94, 92, 255),
        "seam": (0, 77, 75, 255),
        "accent": (157, 17, 187, 255),
        "accent_dark": (114, 0, 112, 255),
        "handle": (0, 255, 243, 255),
    },
    "pale_prismium": {
        "src": "src/main/resources/assets/claudemod/textures/block/pale_prismium_block.png",
        "out_bottom": "src/main/resources/assets/claudemod/textures/block/pale_prismium_door_bottom.png",
        "out_top": "src/main/resources/assets/claudemod/textures/block/pale_prismium_door_top.png",
        "out_item": "src/main/resources/assets/claudemod/textures/item/pale_prismium_door.png",
        "frame": (113, 157, 191, 255),
        "seam": (105, 141, 169, 255),
        "accent": (202, 186, 237, 255),
        "accent_dark": (232, 186, 237, 255),
        "handle": (246, 252, 255, 255),
    },
}


def build_half(src, frame, seam, accent, accent_dark, handle, is_top):
    """Build one 16x16 door-half texture from the base block texture."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            # Sample the diagonal fill pattern from the source block
            # texture (same coordinates, so the diagonal streak keeps its
            # established look), then overlay frame/seam/accent on top.
            img.putpixel((x, y), src.getpixel((x, y)))

    # Outer vertical edges (left/right) - darker frame, full height.
    for y in range(16):
        img.putpixel((0, y), frame)
        img.putpixel((15, y), frame)

    if is_top:
        # Outer top edge.
        for x in range(16):
            img.putpixel((x, 0), frame)
        # Seam at the bottom of the top half (touches the bottom half's
        # own seam row to form a visible 2px panel divider, matching the
        # trapdoor's two-seam-line convention).
        for x in range(16):
            img.putpixel((x, 14), seam)
            img.putpixel((x, 15), seam)
        # Corner accent gems (mirrors the 4-corner accent dots every other
        # family block uses - top half keeps the top two).
        for (cx, cy) in [(1, 1), (14, 1)]:
            img.putpixel((cx, cy), accent)
        for (cx, cy) in [(2, 1), (13, 1)]:
            img.putpixel((cx, cy), accent_dark)
    else:
        # Outer bottom edge.
        for x in range(16):
            img.putpixel((x, 15), frame)
        # Seam at the top of the bottom half.
        for x in range(16):
            img.putpixel((x, 0), seam)
            img.putpixel((x, 1), seam)
        # Corner accent gems - bottom half keeps the bottom two.
        for (cx, cy) in [(1, 14), (14, 14)]:
            img.putpixel((cx, cy), accent)
        for (cx, cy) in [(2, 14), (13, 14)]:
            img.putpixel((cx, cy), accent_dark)
        # Handle accent near hinge-opposite mid-height (purely cosmetic -
        # the real hinge side is picked by the HINGE blockstate at
        # placement time, not by the texture).
        img.putpixel((12, 6), handle)
        img.putpixel((12, 7), handle)

    return img


def build_item_icon(bottom, top, frame):
    """Flat 16x16 item icon: top half scaled into rows 0-7, bottom half
    into rows 8-15, matching vanilla's approach of a single flat drawing
    for the door item (distinct from the two in-world block textures)."""
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
        bottom.save(cfg["out_bottom"])
        top.save(cfg["out_top"])
        icon = build_item_icon(bottom, top, cfg["frame"])
        icon.save(cfg["out_item"])
        print(f"{name}: wrote {cfg['out_bottom']}, {cfg['out_top']}, {cfg['out_item']}")


if __name__ == "__main__":
    main()
