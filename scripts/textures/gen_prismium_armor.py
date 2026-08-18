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

SESSION 9 UPDATE (GitHub issue #1): a player reported that wearing the
helmet hides the whole face, and asked for it to stay visible ("the face
is an important part that identifies the player"). The original
make_layer1() filled the head box's front face fully opaque like vanilla
helmets do - correct per vanilla convention, but not what was asked for
here. open_face()/helmet_front() now punch the front face transparent
below a thin 2-row "brim" band at the top, so the skin layer's actual
face shows through underneath while the piece still reads as a helmet
from the front (brim) and fully as one from every other angle (top/
sides/back are untouched). Only the helmet's worn-layer front face is
affected - the item icon and the other three armor pieces are unchanged.
Self-reviewed with a checkerboard-backed upscaled preview (see
PROGRESS.md session 9) to confirm the cutout lands exactly where
intended and doesn't leak into neighboring UV regions; not yet confirmed
in an actual in-game render, same caveat as the note above.

SESSION 39 UPDATE (user feedback relayed session 37, still outstanding
as of session 38): the worn armor was reported as looking "flat"
("のっぺりしている" - lacking shading/depth). The 64x32 layer sheet
previously filled each box face with a single flat color (only varying
color *between* faces via PLATE_COLORS/CRYSTAL_FRONT_COLORS), so any one
face read as a solid block with almost no internal contrast. Two changes
address this without touching the UV layout (still unverified in-game,
same caveat as above):
  - fill_box()'s rect() now applies a 1px lighten on a face's first row
    and a 1px darken on its last row (a simple top-lit bevel), for every
    face on every box, so even the plain metal plates (arms, chestplate
    back, leggings back) pick up a bit of directional shading instead of
    being perfectly flat.
  - crystal_front_detail() (used for helmet/chest/legs front faces) now
    fills the whole front face with alternating 2-row BASE/MID bands
    (a faceted-crystal look) plus SHADOW/HILITE side edge columns,
    instead of a flat BASE fill with only a single sparse accent line.
    The center gem_accent() cross is kept on top as a focal highlight.
  Self-reviewed via the same upscaled-preview method as session 9
  (see PROGRESS.md session 39): bevel edges are visible without reading
  as noise, and the crystal bands read clearly as faceted at both 8x and
  16x scale. Still not verified as an actual worn 3rd-person render.

Run from repo root: python3 scripts/textures/gen_prismium_armor.py
"""
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette (matches gen_prismium.py / gen_prismium_tools.py) --------
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_ACCENT = "#FF7CFC"

FRAME_OUTLINE = "#1B1B22"
FRAME_SHADOW = "#262630"
FRAME_BASE = "#4A4A57"
FRAME_HILITE = "#82829C"


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


def clamp255(v):
    return max(0, min(255, v))


def lighten(color, amt=30):
    return tuple(clamp255(c + amt) for c in color)


def darken(color, amt=30):
    return tuple(clamp255(c - amt) for c in color)


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

    SESSION 39: each rect now gets a 1px lighten on its first row and a
    1px darken on its last row (when tall enough) - a cheap top-lit bevel
    so flat metal plate faces aren't a single uniform color anymore. See
    module docstring "SESSION 39 UPDATE" for the motivating feedback.
    """
    def rect(x0, y0, rw, rh, color):
        for yy in range(y0, y0 + rh):
            if rh >= 3 and yy == y0:
                row_color = lighten(color)
            elif rh >= 3 and yy == y0 + rh - 1:
                row_color = darken(color)
            else:
                row_color = color
            for xx in range(x0, x0 + rw):
                px[xx, yy] = (*row_color, 255)

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
    """SESSION 39: replaced the old flat-BASE-fill + single sparse accent
    line with alternating 2-row BASE/MID bands across the whole front
    face (a faceted-crystal look) plus SHADOW/HILITE side edge columns
    for a touch of side-lighting. gem_accent()'s center cross is kept as
    a focal highlight on top. See module docstring for context."""
    for y in range(y0, y0 + h):
        band = ((y - y0) // 2) % 2
        row_color = MID if band == 0 else BASE
        for x in range(x0, x0 + w):
            px[x, y] = (*row_color, 255)
        px[x0, y] = (*SHADOW, 255)
        if w > 1:
            px[x0 + w - 1, y] = (*HILITE, 255)
    gem_accent(px, x0, y0, w, h)


def open_face(px, x0, y0, w, h, brim_rows=2):
    """Punches the player's face open on a helmet's front-face UV region
    (GitHub issue #1, addressed session 9): fill_box() draws the front
    face of the head box fully opaque, which - combined with the vanilla
    armor layer always rendering on top of the skin layer - completely
    hides the wearer's face. Vanilla helmets do the same thing, but a
    player specifically flagged it as unwelcome for this mod ("the face
    is an important part that identifies the player; it seems wrong for
    armor to hide it"), so unlike vanilla we deliberately leave most of
    the front face transparent so the skin layer's face shows through,
    keeping only a thin opaque "brim" band across the top few rows (a
    coronet/circlet look) so the piece still visually reads as a helmet
    from the front rather than looking like nothing was drawn there at
    all. x0/y0/w/h are the front face's own rect as passed to fill_box's
    front_extra callback (i.e. already offset by ux+d, uy+d)."""
    for y in range(y0 + brim_rows, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = (0, 0, 0, 0)


def helmet_front(px, x0, y0, w, h):
    crystal_front_detail(px, x0, y0, w, h)
    open_face(px, x0, y0, w, h)


def make_layer1():
    img = new_img(64, 32)
    px = img.load()
    fill_box(px, 0, 0, 8, 8, 8, CRYSTAL_FRONT_COLORS, helmet_front)
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
