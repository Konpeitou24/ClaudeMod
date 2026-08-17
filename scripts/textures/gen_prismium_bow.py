#!/usr/bin/env python3
"""Generate the 4 item-icon states for Prismium Bow (session 29).

The mod's first ranged weapon and the companion piece to Prismium Shield
(session 28) - see PrismiumBowItem and PROGRESS.md session 28 handoff
item 6(a). Extending vanilla's BowItem means the item needs the same
4-texture "pull state" set vanilla's own bow uses: a resting icon plus
three progressively-more-drawn frames, switched between at runtime by
the "pulling"/"pull" item-model overrides in prismium_bow.json (see
ClientModEvents for the matching ItemProperties registration).

Visual language: a vertical longbow silhouette (C-curve bulging left,
tips at top/bottom) built from the tool/armor family's neutral steel
(STEEL_*, identical hex values to gen_prismium_shield.py /
gen_prismium_tools.py for cross-item consistency - this is a metal-cored
bow, not a plain wood one) with a small wood-wrapped grip at the
midpoint (WOOD_*, same palette as the shield's face) for material
variety. The bowstring uses the mod's Prismium accent purple
(PRISMIUM_ACCENT/_HILITE) instead of plain white/grey - an "energy
string" that reads as part of the Prismium family at a glance, the same
"one accent color ties it together" strategy used by every other
accessory item in the mod. The string's draw point moves right (AWAY
from the grip/limb) across the four frames to sell the pull motion, and
a wood (shaft) + steel (head) + prismium-accent (fletching) arrow
appears, progressively further nocked, in the two most-drawn frames -
the head stays fixed just past the grip on the shot-direction (left)
side while the fletched nock end tracks the string outward (right).

Self-review note (round 1): an early draft kept the string perfectly
straight in all four frames and only moved the arrow, which read as
"arrow floating in front of a static bow" rather than "bow being drawn"
once previewed at 16x16 - bending the string's own midpoint per frame
(in addition to the arrow) was necessary to sell the motion.

Self-review note (round 2, caught after a user looked at the rendered
preview): that same early draft bent the string toward the grip (left,
same side as the limb's belly) as the draw increased, which reads as
physically backwards - drawing a bow pulls the string away from the
grip/riser toward the archer, not into the bow. The fix was purely a
sign flip (draw_string's mid_x now grows past the resting/brace value of
10 instead of shrinking toward the grip's x=3), plus swapping which end
of the arrow is fixed (head, near the grip) versus which end moves with
the string (the fletched nock). This is the mistake to watch for if this
script is ever copied as a template for another bow-like item: always
sanity-check "does the string bend away from the grip, not into it?"
before calling a draw-state texture done.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_bow.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette (shared with gen_prismium_shield.py / gen_prismium_tools.py
# for cross-item family consistency) ---------------------------------------
STEEL_OUTLINE = "#1B1B22"
STEEL_SHADOW = "#3A3A46"
STEEL_BASE = "#5B5B6B"
STEEL_HILITE = "#8A8A9C"

WOOD_OUTLINE = "#241407"
WOOD_SHADOW = "#5C3A1E"
WOOD_BASE = "#8A5A30"
WOOD_HILITE = "#B07D48"

PRISMIUM_ACCENT = "#C97BFF"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


S_OUTLINE = hexrgb(STEEL_OUTLINE)
S_SHADOW = hexrgb(STEEL_SHADOW)
S_BASE = hexrgb(STEEL_BASE)
S_HILITE = hexrgb(STEEL_HILITE)
W_OUTLINE = hexrgb(WOOD_OUTLINE)
W_SHADOW = hexrgb(WOOD_SHADOW)
W_BASE = hexrgb(WOOD_BASE)
W_HILITE = hexrgb(WOOD_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_HILITE = hexrgb(PRISMIUM_ACCENT_HILITE)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def set_px(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = (*color, 255)


# The limb: a vertical C-curve, one or two pixels wide per row, bulging
# left toward x=3 at the vertical middle and narrowing to tips at the
# very top/bottom rows. (row, [x, ...]) - explicit per-row pixel lists
# rather than a formula, so the curve can be hand-tuned.
LIMB_ROWS = {
    1: [9, 10],
    2: [7, 8],
    3: [6],
    4: [5],
    5: [4],
    6: [3],
    7: [3],
    8: [3],
    9: [3],
    10: [4],
    11: [5],
    12: [6],
    13: [7, 8],
    14: [9, 10],
}
GRIP_ROWS = (7, 8)  # wood-wrapped grip section, middle of the limb


def draw_limb(px):
    limb_pts = set()
    for y, xs in LIMB_ROWS.items():
        for x in xs:
            limb_pts.add((x, y))

    for y, xs in LIMB_ROWS.items():
        for x in xs:
            if y in GRIP_ROWS:
                color = W_HILITE if x == max(xs) else W_BASE
            else:
                color = S_HILITE if (x + y) % 3 == 0 else S_BASE
            set_px(px, x, y, color)

    # Outline the limb silhouette.
    for (x, y) in limb_pts:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in limb_pts and 0 <= nx < SIZE and 0 <= ny < SIZE:
                if px[nx, ny][3] == 0:
                    outline = W_OUTLINE if y in GRIP_ROWS else S_OUTLINE
                    px[nx, ny] = (*outline, 255)

    return limb_pts


def draw_string(px, mid_x, limb_pts):
    """String from the top tip (row 1, x=10) to the bottom tip (row 14,
    x=10), smoothly bending toward mid_x at the vertical center (between
    rows 7/8). Uses a continuous linear interpolation (by distance from
    the center row) rather than a handful of hand-picked per-row values,
    so the line has no discontinuities/gaps - an early draft picked
    per-row x values by hand and produced a viscerally "broken dashed
    line" look once previewed at 16x16 (see this script's module
    docstring self-review note) because the values did not change
    monotonically row-to-row."""
    center = 7.5
    half_span = 6.5  # distance from center to row 1 or row 14
    string_x = {}
    for y in range(1, 15):
        t = 1.0 - abs(y - center) / half_span  # 0 at the tips, 1 at the center
        x = round(10 - t * (10 - mid_x))
        string_x[y] = x
        if (x, y) in limb_pts:
            continue
        color = ACCENT_HILITE if y in (1, 14) else ACCENT
        set_px(px, x, y, color)
    return string_x


def draw_arrow(px, head_x, nock_x, nock_y_range, limb_pts):
    """A horizontal arrow resting across the grip: a steel head at the
    far (left) end pointing past the limb toward the shot direction, a
    wood shaft, and a Prismium-accent fletching flare at the nock end
    (right), which sits on the string and therefore must be the end that
    moves as the bow is drawn further - see draw_string / make_frame for
    why "further drawn" means "nock_x grows", not shrinks."""
    mid_row = (nock_y_range[0] + nock_y_range[-1]) // 2
    for x in range(head_x, nock_x):
        # Unlike draw_string, the arrow is drawn *over* the grip rather
        # than stopping at it - a nocked arrow visually rests on top of
        # the riser, it does not vanish behind it.
        set_px(px, x, mid_row, W_BASE)
    set_px(px, head_x, mid_row, S_HILITE)
    set_px(px, head_x + 1, mid_row - 1, S_BASE)
    set_px(px, head_x + 1, mid_row + 1, S_BASE)
    # Fletching flare at the nock end (2 pixels, above/below the shaft).
    if (nock_x + 1, mid_row - 1) not in limb_pts:
        set_px(px, nock_x + 1, mid_row - 1, ACCENT)
    if (nock_x + 1, mid_row + 1) not in limb_pts:
        set_px(px, nock_x + 1, mid_row + 1, ACCENT)


def make_frame(mid_x, arrow=None):
    img = new_img()
    px = img.load()
    limb_pts = draw_limb(px)
    draw_string(px, mid_x, limb_pts)
    if arrow is not None:
        head_x, nock_x = arrow
        draw_arrow(px, head_x, nock_x, GRIP_ROWS, limb_pts)
    return img


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)

    # mid_x grows (string bends further right, AWAY from the grip at
    # x=3) as the draw progresses - see draw_string's docstring and the
    # session's self-review note above for why the earlier draft had
    # this backwards. head_x/nock_x for the arrow: the head stays fixed
    # just past the grip (pointing toward the shot direction, left) while
    # nock_x tracks the string's own rightward pull.
    frames = {
        "prismium_bow": make_frame(mid_x=10, arrow=None),
        "prismium_bow_pulling_0": make_frame(mid_x=11, arrow=None),
        "prismium_bow_pulling_1": make_frame(mid_x=13, arrow=(1, 13)),
        "prismium_bow_pulling_2": make_frame(mid_x=15, arrow=(1, 15)),
    }
    for name, img in frames.items():
        out_path = out_dir / f"{name}.png"
        img.save(out_path)
        print(f"wrote {out_path}")


if __name__ == "__main__":
    main()
