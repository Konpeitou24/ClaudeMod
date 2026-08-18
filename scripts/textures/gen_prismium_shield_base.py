#!/usr/bin/env python3
"""Generate the 3D in-hand/equipped texture for Prismium Shield
(session 38, GitHub issue #6: "the shield only ever looks like a flat
item, never like an equipped shield while blocking").

Background: PrismiumShieldItem (session 28) deliberately did not extend
vanilla ShieldItem and had no elements-based item model, so it always
rendered as a plain flat 2D sprite in-hand - this was a known, documented
trade-off (see PrismiumShieldItem's class doc) that a real player has now
filed a bug about. Session 38 fixes this by giving
models/item/prismium_shield.json real "elements" geometry (the same
two-box shape - a flat rectangular board plus a small central boss - that
vanilla's own item/shield.json uses, reverse-engineered coordinates
confirmed against a public gist since Mojang doesn't ship this file
decompiled in an easily greppable form) instead of a flat
"minecraft:item/generated" parent. That geometry expects a single 64x64
texture ("layer0") with UV regions in the standard 0-16 unit space (which
this script converts to 64x64 pixel rects by multiplying by 4). This
script paints that texture; it does not touch the older flat 16x16
`item/prismium_shield.png` icon (now unused by the model, left in place
harmlessly rather than deleted, in case a future session wants it back
for e.g. an achievement/advancement icon).

UV rects used (from-uv * 4 = pixel rect on a 64x64 canvas), copied from
the same reverse-engineered coordinates as
models/item/prismium_shield.json's "elements":
  main board (12w x 22h "front"/"back" faces, 1px edges, thin top/bottom):
    up    -> (1,0)-(13,1)      down  -> (13,0)-(25,1)
    north -> (14,1)-(26,23)    south -> (1,1)-(13,23)
    east  -> (13,1)-(14,23)    west  -> (0,1)-(1,23)
  boss (2w x 6h visible faces, 6x6 side faces):
    up    -> (32,0)-(34,6)     down  -> (34,0)-(36,6)  [drawn un-flipped;
                                                          not player-visible]
    north -> (40,6)-(42,12)    south -> (32,6)-(34,12)
    east  -> (34,6)-(40,12)    west  -> (26,6)-(32,12)

Design: reuses the mod-wide canonical palette (session 36/37, sampled
from the user-submitted Prismium Block art - see gen_prismium.py) for the
crystal/energy motif, plus the tool/armor family's neutral steel rim
(gen_prismium_tools.py STEEL_*) for the metal socket frame, matching this
item's existing 16x16 icon's material language (steel rim + a single
accent gem) rather than inventing a third one. Both the north and south
large faces (whichever ends up "front" facing the camera - not fully
certain from static analysis alone, see class script note) get the same
symmetric design so the shield looks correct regardless. The east/west
strips (1px, board thickness) and up/down strips get a plain steel rim
color, and the boss's four side faces get a steel body with the top-
facing (south, "front") getters an accent gem highlight; north (likely
the hidden back face) gets a plainer treatment.

Self-review: after generating, a checkerboard-backed preview PNG (raw
64x64 plus a 4x/8x upscale of just the two main UV rects, since the
whole 64x64 sheet is mostly unused padding and hard to judge at a glance)
is written for manual Read-tool inspection, per PROGRESS.md's established
process (session 34 onward) of copying preview images to the outputs
mount before viewing them.
"""
from pathlib import Path
from PIL import Image

SIZE = 64
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- mod-wide canonical palette (session 36/37, from user art) ----------
PRISMIUM_OUTLINE = "#024D4B"
SHADOW = "#008282"
BASE = "#11BBB8"
MID = "#65F5E3"
HILITE = "#CAFDF9"
ACCENT = "#FF7CFC"
ACCENT_DARK = "#720070"

# Steel rim, shared with tools/grappling hook/shield icon.
STEEL_OUTLINE = "#1B1B22"
STEEL_SHADOW = "#3A3A46"
STEEL_BASE = "#5B5B6B"
STEEL_HILITE = "#8A8A9C"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


C = {k: hexrgb(v) for k, v in dict(
    OUTLINE=PRISMIUM_OUTLINE, SHADOW=SHADOW, BASE=BASE, MID=MID,
    HILITE=HILITE, ACCENT=ACCENT, ACCENT_DARK=ACCENT_DARK,
    S_OUTLINE=STEEL_OUTLINE, S_SHADOW=STEEL_SHADOW, S_BASE=STEEL_BASE,
    S_HILITE=STEEL_HILITE,
).items()}


def fill_rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), color)


def draw_board_face(img, x0, y0, w, h):
    """12x22 board face: steel rim border, teal crystal field, a magenta
    accent diamond in the center - same visual language as the existing
    16x16 icon's wood-board-plus-boss idea, translated to this mod's
    canonical crystal palette instead of wood."""
    fill_rect(img, x0, y0, x0 + w, y0 + h, C["S_BASE"])
    # 1px steel rim
    for x in range(x0, x0 + w):
        img.putpixel((x, y0), C["S_OUTLINE"])
        img.putpixel((x, y0 + h - 1), C["S_OUTLINE"])
    for y in range(y0, y0 + h):
        img.putpixel((x0, y), C["S_OUTLINE"])
        img.putpixel((x0 + w - 1, y), C["S_OUTLINE"])
    # inner crystal field
    fill_rect(img, x0 + 2, y0 + 2, x0 + w - 2, y0 + h - 2, C["BASE"])
    fill_rect(img, x0 + 2, y0 + 2, x0 + w - 2, y0 + 3, C["MID"])
    # central diamond accent (rows relative to face)
    cx = x0 + w // 2
    cy = y0 + h // 2
    diamond = [
        (0, -3), (-1, -2), (0, -2), (1, -2),
        (-2, -1), (-1, -1), (0, -1), (1, -1), (2, -1),
        (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0),
        (-1, 1), (0, 1), (1, 1),
        (0, 2),
    ]
    for dx, dy in diamond:
        img.putpixel((cx + dx, cy + dy), C["ACCENT"])
    img.putpixel((cx - 1, cy - 2), C["HILITE"])
    img.putpixel((cx, cy - 3), C["HILITE"])
    for dx, dy in [(-2, -1), (2, -1), (-2, 0), (2, 0), (0, 2)]:
        img.putpixel((cx + dx, cy + dy), C["ACCENT_DARK"])


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    # ---- main board faces --------------------------------------------
    draw_board_face(img, 1, 1, 12, 22)   # south (px 1,1 - 13,23)
    draw_board_face(img, 14, 1, 12, 22)  # north (px 14,1 - 26,23)
    # thin edge strips (board thickness, 1px): plain steel
    fill_rect(img, 1, 0, 13, 1, C["S_SHADOW"])    # up
    fill_rect(img, 13, 0, 25, 1, C["S_SHADOW"])   # down
    fill_rect(img, 13, 1, 14, 23, C["S_OUTLINE"])  # east
    fill_rect(img, 0, 1, 1, 23, C["S_OUTLINE"])    # west

    # ---- boss (central knob) faces ------------------------------------
    # south face (px 32,6 - 34,12): front-facing gem highlight
    fill_rect(img, 32, 6, 34, 12, C["ACCENT"])
    img.putpixel((32, 6), C["HILITE"])
    img.putpixel((33, 11), C["ACCENT_DARK"])
    # north face (px 40,6 - 42,12): plainer back treatment
    fill_rect(img, 40, 6, 42, 12, C["S_SHADOW"])
    # east/west side faces (6x6): steel body
    fill_rect(img, 34, 6, 40, 12, C["S_BASE"])
    fill_rect(img, 26, 6, 32, 12, C["S_BASE"])
    for x in range(34, 40):
        img.putpixel((x, 6), C["S_HILITE"])
    for x in range(26, 32):
        img.putpixel((x, 6), C["S_HILITE"])
    # up/down (2x6 and mirrored region)
    fill_rect(img, 32, 0, 34, 6, C["S_HILITE"])
    fill_rect(img, 34, 0, 36, 6, C["S_SHADOW"])

    out = ASSETS / "item" / "prismium_shield_base.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out}")

    # transparency sanity check
    bad = 0
    for y in range(SIZE):
        for x in range(SIZE):
            a = img.getpixel((x, y))[3]
            if a not in (0, 255):
                bad += 1
    print(f"non-binary alpha pixels: {bad}")


if __name__ == "__main__":
    main()
