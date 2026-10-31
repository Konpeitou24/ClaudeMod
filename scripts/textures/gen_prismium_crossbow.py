#!/usr/bin/env python3
"""Generate the 6 item-icon states for Prismium Crossbow (scheduled
session, 2026-10-31): standby, pulling_0/1/2, arrow (loaded with a bolt),
and firework (loaded with a firework rocket).

Deliberately an all-original design, not a trace of vanilla's own
crossbow texture (that PNG was never opened in this session - only its
*model* JSON was fetched, for the override/predicate wiring, which is
functional data rather than creative expression; see PrismiumCrossbowItem
's class javadoc). Reuses the same diagonal spine convention as this
mod's other handheld items (bow/tools/grappling hook - see
gen_prismium_bow.py's session-36 history for why: the generic
minecraft:item/generated parent's built-in hand transforms assume a
bottom-left-to-top-right diagonal, and departing from that convention is
exactly what made the original Prismium Bow look wrong in a real
player's hands) and the same palette constants gen_prismium_bow.py uses
for cross-item family consistency (steel barrel/prod, wood grip, Prismium
-accent string/fletching).

Silhouette: a thin steel "stock" runs the full diagonal spine (the same
base_point(t) the bow uses), with a wood grip at the rear (near the
canvas's bottom-left corner, t 0-2, matching the same resting-hand anchor
every other handheld item in this mod uses). A steel "prod" (the bow-like
springy part) crosses the stock near its front third, extending along the
*other* diagonal (perpendicular to the stock) in both directions - this
is what makes the silhouette read as a crossbow rather than a reskinned
bow/sword even at a small, low-detail 16x16 icon size. A Prismium-accent
string spans the prod's two tips, bowing forward (away from the grip) at
rest and bending backward toward the grip as the draw progresses -
mirroring the bow's own string logic (round 2 self-review note in
gen_prismium_bow.py: "does the string bend away from the grip, not into
it?" - for a crossbow the equivalent sanity check is "does drawing it pull
the string *toward* the stock/trigger, not away from it?", which is the
opposite direction from an in-hand bow's string because the archer draws
a bow's string toward themselves while a crossbow's string is cranked
back along the weapon's own body toward the trigger mechanism).

A loaded bolt (arrow state) or a stylised firework rocket (firework
state) rests in a shallow groove just above the stock, visible only once
the crossbow is "charged" - at that point the string itself is shown
fully drawn (same bend magnitude as pulling_2), matching how a real
crossbow stays cocked/ready once loaded rather than relaxing back to
rest, unlike a bow (which this mod's existing pulling_2/arrow handling
for PrismiumBowItem never needed to represent, since a drawn bow is never
"left loaded").

<b>Unverified</b> (same standing sandbox limitation as every other visual
asset in this mod): whether this new, larger, cross-shaped silhouette
(stock + perpendicular prod) still reads clearly at actual in-game icon
size, and whether relying on the generic item/generated parent's default
hand transform (no custom "display" block copied from vanilla, see
PrismiumCrossbowItem's class javadoc for why that was a deliberate choice
to avoid combining unverified geometry with this mod's own texture
convention) looks acceptable for this particular silhouette - flagged in
PROGRESS.md §3 alongside the item's other new-content caveats.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_crossbow.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette (shared with gen_prismium_bow.py / gen_prismium_shield.py /
# gen_prismium_tools.py for cross-item family consistency) ---------------
STEEL_OUTLINE = "#1B1B22"
STEEL_BASE = "#5B5B6B"
STEEL_HILITE = "#8A8A9C"

WOOD_OUTLINE = "#241407"
WOOD_BASE = "#8A5A30"
WOOD_HILITE = "#B07D48"

PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"

# Firework rocket colours (gold/orange, distinct from the mod's cool
# steel/accent palette so a loaded firework reads as "something warm and
# explosive" at a glance - this mod has no existing firework-rocket-like
# asset to match against, so this is a fresh but small palette addition
# scoped to this one texture state).
ROCKET_PAPER = "#C94A1F"
ROCKET_PAPER_HILITE = "#F2863D"
ROCKET_FUSE = "#3A2A1A"


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
R_PAPER = hexrgb(ROCKET_PAPER)
R_HILITE = hexrgb(ROCKET_PAPER_HILITE)
R_FUSE = hexrgb(ROCKET_FUSE)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def set_px(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = (*color, 255)


def base_point(t):
    """The stock's diagonal spine, t in 0..13 - the same bottom-left grip
    anchor (1, 14) every other handheld item in this mod starts its
    handle/shaft from (see gen_prismium_tools.py's draw_handle and
    gen_prismium_bow.py's base_point)."""
    return 1 + t, 14 - t


GRIP_T = (0, 1, 2)
PROD_MOUNT_A = 9   # base_point(9) = (10, 5)
PROD_MOUNT_B = 10  # base_point(10) = (11, 4)
PROD_REACH = 4


def draw_stock(px):
    """The barrel/stock: a thin steel rail along the spine, wood-wrapped
    at the grip end, 1px thick with a 1px outline - same weight as this
    mod's existing tool shafts, just following the bow's full 14-point
    spine instead of the tools' shorter handle."""
    pts = set()
    for t in range(14):
        pts.add(base_point(t))

    for t in range(14):
        x, y = base_point(t)
        color = W_HILITE if t == 1 else (W_BASE if t in GRIP_T else
                                          (S_HILITE if t % 3 == 0 else S_BASE))
        set_px(px, x, y, color)

    for (x, y) in list(pts):
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in pts and 0 <= nx < SIZE and 0 <= ny < SIZE and px[nx, ny][3] == 0:
                is_grip_neighbor = any(
                    (nx, ny) == (bx + ddx, by + ddy)
                    for ddx, ddy in [(0, 0)]
                    for (bx, by) in [base_point(t) for t in GRIP_T]
                )
                outline = W_OUTLINE if is_grip_neighbor else S_OUTLINE
                px[nx, ny] = (*outline, 255)
    return pts


def draw_prod(px, stock_pts):
    """The crossbow's springy 'prod', crossing the stock near the front
    third and extending in both perpendicular directions (the (1,1) axis,
    perpendicular to the stock's (1,-1) spine) - this cross shape is what
    reads as "crossbow" rather than a reskinned bow/sword. Returns the two
    outer tip coordinates for draw_string to anchor to."""
    mount_a = base_point(PROD_MOUNT_A)
    mount_b = base_point(PROD_MOUNT_B)

    limb_a_pts = []
    for k in range(1, PROD_REACH + 1):
        limb_a_pts.append((mount_a[0] + k, mount_a[1] + k))
    limb_b_pts = []
    for k in range(1, PROD_REACH + 1):
        limb_b_pts.append((mount_b[0] - k, mount_b[1] - k))

    all_limb_pts = set(limb_a_pts) | set(limb_b_pts)
    for i, (x, y) in enumerate(limb_a_pts):
        color = S_HILITE if i == len(limb_a_pts) - 1 else S_BASE
        if (x, y) not in stock_pts:
            set_px(px, x, y, color)
    for i, (x, y) in enumerate(limb_b_pts):
        color = S_HILITE if i == len(limb_b_pts) - 1 else S_BASE
        if (x, y) not in stock_pts:
            set_px(px, x, y, color)

    occupied = stock_pts | all_limb_pts
    for (x, y) in all_limb_pts:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in occupied and 0 <= nx < SIZE and 0 <= ny < SIZE and px[nx, ny][3] == 0:
                px[nx, ny] = (*S_OUTLINE, 255)

    return limb_a_pts[-1], limb_b_pts[-1], all_limb_pts


def draw_string(px, pull, tip_a, tip_b, occupied_pts):
    """String spanning the prod's two tips. At rest (pull=0) it bows
    forward slightly (away from the stock); as pull increases it bends
    backward toward the stock/grip instead - the crossbow equivalent of
    the bow's own "string bends away from the grip when drawn" rule,
    just mirrored because a crossbow's string is cranked back along the
    weapon's body rather than drawn toward the archer's own hand."""
    ax, ay = tip_a
    bx, by = tip_b
    steps = max(abs(ax - bx), abs(ay - by))
    for i in range(steps + 1):
        s = i / steps
        x = round(ax + (bx - ax) * s)
        y = round(ay + (by - ay) * s)
        tri = 1.0 - abs(s - 0.5) / 0.5  # 0 at the tips, 1 at the midpoint
        # Forward bow (away from stock) at rest, backward bend (toward
        # the grip, i.e. the -(1,-1) direction = (-1, 1)) once drawn.
        forward_bow = round(1 * tri)
        back_bend = round(pull * tri)
        fx, fy = x + forward_bow, y - forward_bow
        px_, py_ = fx - back_bend, fy + back_bend
        if (px_, py_) in occupied_pts:
            continue
        color = ACCENT_HILITE if i in (0, steps) else ACCENT
        set_px(px, px_, py_, color)


def draw_bolt(px, occupied_pts):
    """A loaded bolt resting in a shallow groove just above the stock
    (offset by one perpendicular step, direction (-1,-1) i.e. toward the
    canvas's top-left, away from the prod which sits on the (1,1) side) -
    wood shaft, steel head pointing toward the front (large t / the prod),
    Prismium-accent fletching at the nock end (small t / the grip)."""
    for t in range(2, 12):
        x, y = base_point(t)
        x, y = x - 1, y - 1
        if (x, y) in occupied_pts:
            continue
        if t >= 10:
            color = S_HILITE
        elif t <= 3:
            color = ACCENT
        else:
            color = W_BASE
        set_px(px, x, y, color)


def draw_firework(px, occupied_pts):
    """A loaded firework rocket resting in the same groove a bolt would
    (see draw_bolt) - a short warm-coloured cylinder with a pointed nose
    toward the front and a fuse/fletching-equivalent flare at the nock
    end, deliberately in a different, warmer palette (see ROCKET_* above)
    so it reads as "a different kind of ammunition" from the bolt at a
    glance, the way vanilla's own firework-loaded crossbow icon also
    looks nothing like its arrow-loaded icon."""
    for t in range(3, 11):
        x, y = base_point(t)
        x, y = x - 1, y - 1
        if (x, y) in occupied_pts:
            continue
        if t >= 9:
            color = R_HILITE
        elif t <= 4:
            color = R_FUSE
        else:
            color = R_PAPER
        set_px(px, x, y, color)


def make_frame(pull, ammo):
    """ammo is None, 'bolt', or 'firework'."""
    img = new_img()
    px = img.load()
    stock_pts = draw_stock(px)
    tip_a, tip_b, prod_pts = draw_prod(px, stock_pts)
    occupied = stock_pts | prod_pts
    draw_string(px, pull, tip_a, tip_b, occupied)
    if ammo == "bolt":
        draw_bolt(px, occupied)
    elif ammo == "firework":
        draw_firework(px, occupied)
    return img


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)

    frames = {
        "prismium_crossbow_standby": make_frame(pull=0, ammo=None),
        "prismium_crossbow_pulling_0": make_frame(pull=1, ammo=None),
        "prismium_crossbow_pulling_1": make_frame(pull=3, ammo=None),
        "prismium_crossbow_pulling_2": make_frame(pull=5, ammo=None),
        "prismium_crossbow_arrow": make_frame(pull=5, ammo="bolt"),
        "prismium_crossbow_firework": make_frame(pull=5, ammo="firework"),
    }
    for name, img in frames.items():
        out_path = out_dir / f"{name}.png"
        img.save(out_path)
        print(f"wrote {out_path}")


if __name__ == "__main__":
    main()
