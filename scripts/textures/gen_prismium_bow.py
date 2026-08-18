#!/usr/bin/env python3
"""Generate the 4 item-icon states for Prismium Bow (session 29, reworked
session 36 in response to direct user feedback with in-game screenshots:
"the way tools are held in general looks weird" - specifically, the bow
appeared to float away from the hand in third person and rendered as a
huge, oddly-oriented shape in first person).

Root cause (session 36 diagnosis): this mod's other handheld items -
Prismium's 5 basic tools (see gen_prismium_tools.py) and the Grappling
Hook - are all drawn along the same bottom-left-to-top-right *diagonal*
of the 16x16 canvas, which is the standard Minecraft convention for
"item/handheld"-style held items (it's how vanilla's own tool and bow
textures are drawn too) - that diagonal is what lines up correctly with
the game's built-in in-hand item display transforms. The original
session-29 bow, however, was explicitly drawn as a *vertical* longbow
(see the removed docstring text below, kept here for context: "a
vertical longbow silhouette (C-curve bulging left, tips at top/bottom)")
- a deliberate design choice at the time, but one that fights the
built-in hand transform instead of working with it, which is almost
certainly why it looked wrong once actually seen in hand (this sandbox
cannot render the game, so this mismatch went unnoticed for 7 sessions
until a real screenshot surfaced it).

This revision keeps the original's visual language (steel recurve limb,
small wood-wrapped grip at the middle, Prismium-accent-purple string,
string bending away from the grip + a wood/steel/accent arrow nocking
progressively further as the bow is drawn - see the session-29 self
-review notes below, which are still the correct animation logic and
were NOT the problem) but re-derives the limb/string/arrow geometry
along the same diagonal spine the rest of the mod's handheld items use:
base_point(t) = (1+t, 14-t) for t in 0..13, i.e. the exact same
bottom-left (1,14) grip anchor gen_prismium_tools.py's draw_handle()
starts from.

Because a diagonal "add o to both x and y" step covers sqrt(2)x the
on-screen distance of the original's single-axis-only offset, the old
bulge/pull magnitudes were divided by sqrt(2) (see INV_SQRT2 below) so
the curve keeps the same visual "reach" instead of overshooting into the
canvas corner - an early test draft that skipped this correction
produced a limb that flew out to a corner, disconnected from the string,
reading as a checkmark/slash rather than a bow (a case of the mistake
this docstring is now warning future edits about).

Original session-29 self-review notes (still accurate, kept verbatim
since the animation logic they describe is unchanged by this rework):

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
before calling a draw-state texture done. (In this session-36 diagonal
rework, "away from the grip" becomes "further in the +offset direction,
toward the lower-right" - see draw_string's `pull` parameter.)

Session-36 self-review: three drafts were tried before this one. Draft 1
reused the original single-axis bulge magnitudes unscaled, which sent
the limb flying out to the canvas corner, disconnected from the string
(read as a checkmark, not a bow). Draft 2 cut the bulge to a flat small
constant, which fixed the corner-overshoot but made the curve too subtle
to read as a bow at all (looked like a single messy diagonal smudge).
This final version derives the bulge by dividing the original vertical
design's already-tuned per-position magnitudes by sqrt(2) (the exact
correction for a two-axis diagonal step vs. the original's single-axis
step) - previewed at 1x/2x/4x/8x upscale, this reads as a recognizable
diagonal recurve bow at 4x/8x (the roughly hotbar-icon-equivalent
sizes) and as a plausible diagonal-with-crossbar smudge at 1x/2x, same
legibility ballpark as the mod's other 16x16 icons at those sizes.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_bow.py
"""
import math
from pathlib import Path

from PIL import Image

SIZE = 16
INV_SQRT2 = 1 / math.sqrt(2)
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette (shared with gen_prismium_shield.py / gen_prismium_tools.py
# for cross-item family consistency) ---------------------------------------
STEEL_OUTLINE = "#1B1B22"
STEEL_BASE = "#5B5B6B"
STEEL_HILITE = "#8A8A9C"

WOOD_OUTLINE = "#241407"
WOOD_BASE = "#8A5A30"
WOOD_HILITE = "#B07D48"

PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


S_OUTLINE = hexrgb(STEEL_OUTLINE)
S_BASE = hexrgb(STEEL_BASE)
S_HILITE = hexrgb(STEEL_HILITE)
W_OUTLINE = hexrgb(WOOD_OUTLINE)
W_BASE = hexrgb(WOOD_BASE)
W_HILITE = hexrgb(WOOD_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_HILITE = hexrgb(PRISMIUM_ACCENT_HILITE)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def set_px(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = (*color, 255)


def base_point(t):
    """The bow's diagonal spine, t in 0..13 - the same bottom-left grip
    anchor (1, 14) every other handheld item in this mod starts its
    handle/shaft from (see gen_prismium_tools.py's draw_handle)."""
    return 1 + t, 14 - t


# The original (session 29) vertical design's horizontal bulge-from-tip
# -baseline, kept as the source of truth for the curve's *shape* (it was
# already tuned to look like a recognizable recurve limb) and scaled by
# 1/sqrt(2) below since a diagonal (o, o) pixel step covers sqrt(2)x the
# on-screen distance of the original's single-axis o step.
_ORIGINAL_VERTICAL_BULGE = [0, 2, 3.5, 4.5, 5.5, 6.2, 6.5, 6.5, 6.2, 5.5, 4.5, 3.5, 2, 0]
BULGE = [round(v * INV_SQRT2) for v in _ORIGINAL_VERTICAL_BULGE]
GRIP_T = (6, 7)  # wood-wrapped grip, the middle of the spine - also
                  # where the bulge is largest, matching a recurve bow's
                  # riser sitting at the curve's belly.


def limb_band(t):
    """Perpendicular-offset(s) for this spine position, toward the
    upper-left (negative x, negative y) - 2px thick near the tips (t in
    0,1,12,13) like the original, 1px thick along the rest of the limb."""
    o = BULGE[t]
    if t in (0, 1, 12, 13):
        return [-o, -o - 1]
    return [-o]


def draw_limb(px):
    pts = set()
    per_t = {}
    for t in range(14):
        bx, by = base_point(t)
        band = []
        for o in limb_band(t):
            point = (bx + o, by + o)
            band.append(point)
            pts.add(point)
        per_t[t] = band

    for t in range(14):
        is_grip = t in GRIP_T
        for i, (x, y) in enumerate(per_t[t]):
            if is_grip:
                color = W_HILITE if i == 0 else W_BASE
            else:
                color = S_HILITE if i == 0 else S_BASE
            set_px(px, x, y, color)

    # Outline the limb silhouette (grip section gets the wood outline).
    grip_pts = {p for t in GRIP_T for p in per_t[t]}
    for (x, y) in pts:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in pts and 0 <= nx < SIZE and 0 <= ny < SIZE and px[nx, ny][3] == 0:
                outline = W_OUTLINE if (x, y) in grip_pts else S_OUTLINE
                px[nx, ny] = (*outline, 255)

    return pts


def draw_string(px, pull, limb_pts):
    """String from tip to tip along the spine, bending toward the
    lower-right (away from the limb's upper-left bulge, i.e. away from
    the grip/riser) as the bow is drawn further - `pull` grows 0 (rest)
    -> 1 -> 3 -> 5 across the four frames, same progression as the
    original vertical design's mid_x, scaled by 1/sqrt(2) for the same
    reason as the limb bulge. A resting offset of 1 (instead of 0) keeps
    the string a hair off the limb even unstrung, so the two don't visually
    merge into one line."""
    pts = {}
    for t in range(14):
        bx, by = base_point(t)
        tri = 1.0 - abs(t - 6.5) / 6.5  # 0 at the tips, 1 at the center
        o = 1 + round(pull * tri * INV_SQRT2)
        x, y = bx + o, by + o
        pts[t] = (x, y)
        if (x, y) in limb_pts:
            continue
        color = ACCENT_HILITE if t in (0, 13) else ACCENT
        set_px(px, x, y, color)
    return pts


def draw_arrow(px, pull, limb_pts):
    """A nocked arrow resting across the grip, perpendicular to the bow's
    diagonal spine: a steel head fixed just past the grip on the
    shot-direction (upper-left) side, a wood shaft, and a Prismium-accent
    fletching flare at the nock end, which sits on the string and tracks
    it outward (lower-right) as the draw increases - mirrors the original
    vertical design's head-fixed/nock-tracks-string logic exactly, just
    rotated onto the new diagonal axis."""
    bx, by = base_point(6)
    bx2, by2 = base_point(7)
    cx, cy = round((bx + bx2) / 2), round((by + by2) / 2)
    edge_o = round(2 * INV_SQRT2)
    head = (cx - BULGE[6] - edge_o, cy - BULGE[6] - edge_o)
    nock_o = round(pull * INV_SQRT2)
    nock = (cx + nock_o + 1, cy + nock_o + 1)

    steps = max(abs(nock[0] - head[0]), abs(nock[1] - head[1]))
    for i in range(steps + 1):
        t = i / steps
        x = round(head[0] + (nock[0] - head[0]) * t)
        y = round(head[1] + (nock[1] - head[1]) * t)
        if (x, y) not in limb_pts:
            set_px(px, x, y, W_BASE)
    set_px(px, head[0], head[1], S_HILITE)
    # Fletching flare either side of the nock end.
    set_px(px, nock[0], nock[1] - 1, ACCENT)
    set_px(px, nock[0] - 1, nock[1], ACCENT)


def make_frame(pull, arrow):
    img = new_img()
    px = img.load()
    limb_pts = draw_limb(px)
    draw_string(px, pull, limb_pts)
    if arrow:
        draw_arrow(px, pull, limb_pts)
    return img


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)

    frames = {
        "prismium_bow": make_frame(pull=0, arrow=False),
        "prismium_bow_pulling_0": make_frame(pull=1, arrow=False),
        "prismium_bow_pulling_1": make_frame(pull=3, arrow=True),
        "prismium_bow_pulling_2": make_frame(pull=5, arrow=True),
    }
    for name, img in frames.items():
        out_path = out_dir / f"{name}.png"
        img.save(out_path)
        print(f"wrote {out_path}")


if __name__ == "__main__":
    main()
