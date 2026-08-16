#!/usr/bin/env python3
"""Generate ClaudeMod's Prismium armor set textures (session 3).

Produces two kinds of assets:

1. Item icon textures (16x16), one per armor piece, drawn as flat 2D icons
   in the vanilla item-icon style (like diamond_helmet.png etc): a grey
   "socket frame" (steel palette borrowed from the tool hilts/guards) that
   holds visible Prismium crystal plates/gems. This ties armor into the
   same visual family as the tools (crystal + metal socket) established in
   gen_prismium_tools.py.

2. Worn-armor layer textures (64x32), using the standard vanilla biped/
   armor UV layout (same box-unwrap used by mob skins at 64x32 resolution):
     - head:      (0,0)-(32,16)   -> helmet
     - right leg: (0,16)-(16,32)  -> boots (layer 1) / leggings (layer 2)
     - body:      (16,16)-(40,32) -> chestplate (layer 1) / leggings waist (layer 2)
     - right arm: (40,16)-(56,32) -> chestplate arms (layer 1 only)
   Each box's 6 faces are laid out as top/bottom/right/front/left/back -
   see fill_box() below for the exact per-box math. Left arm/leg are
   mirrored from the right ones by the renderer, so they don't need their
   own UV space.

   NOTE: these UV coordinates are transcribed from documented/well-known
   Minecraft armor texture layout math, not verified by an in-game render
   (this sandbox cannot launch the game - see PROGRESS.md). The 16x16 icons
   were self-reviewed by eye; the 64x32 layer textures could only be sanity
   checked as flat sprite sheets, NOT as they will actually look wrapped
   onto a worn armor model. Flag any visual glitches in-game to the next
   session.

Run from repo root: python3 scripts/textures/gen_prismium_armor.py
"""
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette (matches gen_prismium.py / gen_prismium_tools.py) --------
PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_SHADOW = "#1E7A78"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"
PRISMIUM_ACCENT = "#C97BFF"

FRAME_OUTLINE = "#1B1B22"
FRAME_SHADOW = "#33333D"
FRAME_BASE = "#4A4A57"
FRAME_HILITE = "#6E6E80"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
SHADOW = hexrgb(PRISMIUM_SHADOW)
BASE = hexrgb(PRISMIUM_BASE)
MID = hexrgb(PRISMIUM_MID)
HILITE = hexrgb(PRISMIUM_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)

F_OUTLINE = hexrgb(FRAME_OUTLINE)
F_SHADOW = hexrgb(FRAME_SHADOW)
F_BASE = hexrgb(FRAME_BASE)
F_HILITE = hexrgb(FRAME_HILITE)


def new_img(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def fill_rows(px, rows, color):
    for y, spans in rows.items():
        for (x0, x1) in spans:
            for x in range(x0, x1 + 1):
                px[x, y] = (*color, 255)


def outline_nonzero(px, w, h, bg_outline=OUTLINE):
    """Add a 1px outline of bg_outline around any opaque pixel that borders
    transparency, without overwriting existing opaque pixels."""
    solid = set()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] != 0:
                solid.add((x, y))
    for (x, y) in list(solid):
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in solid:
                px[nx, ny] = (*bg_outline, 255)
                solid.add((nx, ny))


# ---------------------------------------------------------------------
# 16x16 item icons
# ---------------------------------------------------------------------

def make_helmet_icon():
    img = new_img()
    px = img.load()
    frame_rows = {
        1: [(6, 9)],
        2: [(5, 10)],
        3: [(4, 11)],
        4: [(4, 5), (10, 11)],
        5: [(4, 5), (10, 11)],
        6: [(4, 5), (10, 11)],
        7: [(4, 5), (10, 11)],
    }
    fill_rows(px, frame_rows, F_BASE)
    for x in range(6, 10):
        px[x, 1] = (*F_HILITE, 255)
    for x in (4, 5, 10, 11):
        px[x, 7] = (*F_SHADOW, 255)
    crystal_rows = {4: [(6, 9)], 5: [(6, 9)]}
    fill_rows(px, crystal_rows, BASE)
    px[7, 4] = (*MID, 255)
    px[8, 4] = (*HILITE, 255)
    px[7, 5] = (*ACCENT, 255)
    outline_nonzero(px, 16, 16, F_OUTLINE)
    return img


def make_chestplate_icon():
    img = new_img()
    px = img.load()
    frame_rows = {
        2: [(5, 6), (9, 10)],
        3: [(4, 11)],
        4: [(4, 5), (10, 11)],
        5: [(4, 5), (10, 11)],
        6: [(3, 5), (10, 12)],
        7: [(3, 5), (10, 12)],
        8: [(3, 5), (10, 12)],
        9: [(4, 11)],
    }
    fill_rows(px, frame_rows, F_BASE)
    for x in range(4, 12):
        px[x, 3] = (*F_HILITE, 255)
    for x in range(4, 12):
        px[x, 9] = (*F_SHADOW, 255)
    crystal_rows = {4: [(6, 9)], 5: [(6, 9)], 6: [(6, 9)], 7: [(6, 9)], 8: [(6, 9)]}
    fill_rows(px, crystal_rows, BASE)
    for y in (4, 5, 6, 7, 8):
        px[6, y] = (*SHADOW, 255)
        px[9, y] = (*MID, 255)
    px[7, 6] = (*ACCENT, 255)
    px[8, 6] = (*HILITE, 255)
    px[7, 7] = (*ACCENT, 255)
    outline_nonzero(px, 16, 16, F_OUTLINE)
    return img


def make_leggings_icon():
    img = new_img()
    px = img.load()
    # waistband bar, then two narrow leg columns with a wide (4px) gap
    # between them so outline_nonzero's 1px-per-side inward bleed can't
    # accidentally weld them into one shape (learned from a first attempt
    # with only a 2px gap, which did exactly that - see PROGRESS.md).
    frame_rows = {
        2: [(4, 11)],
        3: [(4, 5), (10, 11)],
        4: [(4, 5), (10, 11)],
        5: [(4, 5), (10, 11)],
        6: [(4, 5), (10, 11)],
        7: [(4, 5), (10, 11)],
        8: [(4, 5), (10, 11)],
        9: [(3, 5), (10, 12)],
    }
    fill_rows(px, frame_rows, F_BASE)
    for x in range(4, 12):
        px[x, 2] = (*F_HILITE, 255)
    for x in (3, 4, 5, 10, 11, 12):
        px[x, 9] = (*F_SHADOW, 255)
    # crystal centre stripe on the inner column of each leg
    for y in range(3, 9):
        px[5, y] = (*(MID if y % 2 == 0 else BASE), 255)
        px[10, y] = (*(MID if y % 2 == 0 else BASE), 255)
    px[7, 2] = (*ACCENT, 255)
    outline_nonzero(px, 16, 16, F_OUTLINE)
    return img


def make_boots_icon():
    img = new_img()
    px = img.load()
    # sideways boot silhouette: ankle cuff + foot/sole flaring right (toe)
    frame_rows = {
        5: [(6, 9)],
        6: [(6, 9)],
        7: [(6, 9)],
        8: [(6, 9)],
        9: [(5, 10)],
        10: [(5, 12)],
        11: [(4, 13)],
    }
    fill_rows(px, frame_rows, F_BASE)
    for x in range(6, 10):
        px[x, 5] = (*F_HILITE, 255)
    for x in range(4, 14):
        px[x, 11] = (*F_SHADOW, 255)
    # ankle crystal accent band
    fill_rows(px, {6: [(7, 8)], 7: [(7, 8)]}, BASE)
    px[7, 6] = (*ACCENT, 255)
    px[8, 7] = (*MID, 255)
    outline_nonzero(px, 16, 16, F_OUTLINE)
    return img


# ---------------------------------------------------------------------
# 64x32 worn-armor layer textures
# ---------------------------------------------------------------------

def fill_box(px, ux, uy, w, h, d, colors, front_extra=None):
    """Fill the 6-face UV unwrap of a box (w wide, h tall, d deep) whose
    unwrap origin is (ux, uy), using the standard Minecraft box UV layout:
        top:    (ux+d, uy)         size w x d
        bottom: (ux+d+w, uy)       size w x d
        right:  (ux, uy+d)         size d x h
        front:  (ux+d, uy+d)       size w x h
        left:   (ux+d+w, uy+d)     size d x h
        back:   (ux+d+w+d, uy+d)   size w x h
    """
    def rect(x0, y0, rw, rh, color):
        for yy in range(y0, y0 + rh):
            for xx in range(x0, x0 + rw):
                px[xx, yy] = (*color, 255)

    rect(ux + d, uy, w, d, colors["top"])
    rect(ux + d + w, uy, w, d, colors["bottom"])
    rect(ux, uy + d, d, h, colors["right"])
    rect(ux + d, uy + d, w, h, colors["front"])
    rect(ux + d + w, uy + d, d, h, colors["left"])
    rect(ux + d + w + d, uy + d, w, h, colors["back"])

    if front_extra:
        front_extra(px, ux + d, uy + d, w, h)


def gem_accent(px, x0, y0, w, h):
    cx = x0 + w // 2
    cy = y0 + h // 2
    for (dx, dy) in [(0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)]:
        x, y = cx + dx, cy + dy
        if x0 <= x < x0 + w and y0 <= y < y0 + h:
            px[x, y] = (*(ACCENT if (dx, dy) == (0, 0) else HILITE), 255)


PLATE_COLORS = {
    "top": F_SHADOW,
    "bottom": F_SHADOW,
    "right": F_BASE,
    "front": F_HILITE,
    "left": F_BASE,
    "back": F_SHADOW,
}

CRYSTAL_FRONT_COLORS = {
    "top": F_SHADOW,
    "bottom": F_SHADOW,
    "right": F_BASE,
    "front": BASE,
    "left": F_BASE,
    "back": F_SHADOW,
}


def crystal_front_detail(px, x0, y0, w, h):
    for y in range(y0, y0 + h):
        if (y - y0) % 3 == 0:
            px[x0, y] = (*MID, 255)
    gem_accent(px, x0, y0, w, h)


def make_layer1():
    img = new_img(64, 32)
    px = img.load()
    fill_box(px, 0, 0, 8, 8, 8, CRYSTAL_FRONT_COLORS, crystal_front_detail)
    fill_box(px, 0, 16, 4, 12, 4, CRYSTAL_FRONT_COLORS, crystal_front_detail)
    fill_box(px, 16, 16, 8, 12, 4, CRYSTAL_FRONT_COLORS, crystal_front_detail)
    fill_box(px, 40, 16, 4, 12, 4, PLATE_COLORS, gem_accent)
    return img


def make_layer2():
    img = new_img(64, 32)
    px = img.load()
    fill_box(px, 16, 16, 8, 12, 4, CRYSTAL_FRONT_COLORS, crystal_front_detail)
    fill_box(px, 0, 16, 4, 12, 4, CRYSTAL_FRONT_COLORS, crystal_front_detail)
    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_helmet_icon(), "item/prismium_helmet.png")
    save(make_chestplate_icon(), "item/prismium_chestplate.png")
    save(make_leggings_icon(), "item/prismium_leggings.png")
    save(make_boots_icon(), "item/prismium_boots.png")
    save(make_layer1(), "models/armor/prismium_layer_1.png")
    save(make_layer2(), "models/armor/prismium_layer_2.png")
