#!/usr/bin/env python3
"""Generate the item icon for Prismium Grappling Hook (session 7).

The mod's first accessory-style item (see PrismiumGrapplingHookItem /
PROGRESS.md). Visual language: reuses the tool set's diagonal "handle at
bottom-left, business end at top-right" composition (gen_prismium_tools.py)
for family consistency, but swaps the crystal blade for a coiled rope
(new warm-tan palette, distinct from the tools' dark wood handle so rope
reads as rope and not another tool haft) ending in a three-prong steel
grapnel hook (reuses the armor/tool set's neutral steel palette). A single
Prismium accent gem sits at the rope-to-hook joint, echoing the small
accent flecks used on every other item in the family.

Self-review note: first draft gave the hook four prongs in a symmetric
X, which at 16x16 read as a blurry asterisk with no clear "hook" silhouette.
Cut it down to three prongs, all curving the same general direction (like
a real grapnel), which reads far more legibly at small size - same lesson
as the Prismium Core rewrite (PROGRESS.md session 3): when a shape reads
as noise, simplify toward a clearer silhouette rather than add more detail.

Deterministic (no RNG needed - every pixel is placed explicitly). Run from
repo root: python3 scripts/textures/gen_prismium_grappling_hook.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette -------------------------------------------------------------
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"

# Steel hook: neutral grey, matches the tool set's sword-hilt palette
# (gen_prismium_tools.py HILT_*) for cross-item consistency.
STEEL_OUTLINE = "#1B1B22"
STEEL_SHADOW = "#3A3A46"
STEEL_BASE = "#5B5B6B"
STEEL_HILITE = "#8A8A9C"

# Rope: warm tan, deliberately lighter/less saturated than the tools'
# dark-wood handle palette so it reads as fibrous cord, not a haft.
ROPE_OUTLINE = "#2E2210"
ROPE_SHADOW = "#7A5A2E"
ROPE_BASE = "#B3854A"
ROPE_HILITE = "#DDB374"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_HILITE = hexrgb(PRISMIUM_ACCENT_HILITE)
S_OUTLINE = hexrgb(STEEL_OUTLINE)
S_SHADOW = hexrgb(STEEL_SHADOW)
S_BASE = hexrgb(STEEL_BASE)
S_HILITE = hexrgb(STEEL_HILITE)
R_OUTLINE = hexrgb(ROPE_OUTLINE)
R_SHADOW = hexrgb(ROPE_SHADOW)
R_BASE = hexrgb(ROPE_BASE)
R_HILITE = hexrgb(ROPE_HILITE)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def set_px(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = (*color, 255)


def draw_outline(px, pts, outline_color):
    ptset = set(pts)
    for (x, y) in ptset:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, -1), (-1, 1), (1, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in ptset and 0 <= nx < SIZE and 0 <= ny < SIZE:
                if px[nx, ny][3] == 0:
                    px[nx, ny] = (*outline_color, 255)


def make_grappling_hook():
    img = new_img()
    px = img.load()

    # --- Rope: diagonal coiled cord from bottom-left up to the hook.
    rope_path = [
        (1, 14), (2, 14), (2, 13), (3, 13), (3, 12), (4, 12),
        (4, 11), (5, 11), (5, 10), (6, 10), (6, 9), (7, 9),
        (7, 8), (8, 8), (8, 7), (9, 7),
    ]
    rope_pts = set()
    for i, (x, y) in enumerate(rope_path):
        rope_pts.add((x, y))
        rope_pts.add((x, y - 1) if i % 2 == 0 else (x + 1, y))

    for i, (x, y) in enumerate(rope_path):
        set_px(px, x, y, R_HILITE if i % 3 == 0 else R_BASE)
        ny = y - 1 if i % 2 == 0 else y
        nx = x if i % 2 == 0 else x + 1
        set_px(px, nx, ny, R_SHADOW)
    draw_outline(px, rope_pts, R_OUTLINE)

    # --- Hook: a rounded steel ring (shepherd's-hook eye) with a punched
    # hollow center, rather than a multi-prong claw.
    #
    # Self-review history (kept for the next session): the first draft used
    # three tightly-packed curved prongs radiating from a point, and the
    # second draft used a single curled barb with a 2px gap meant to stay
    # open as the hook's "mouth". Both, once filled and outlined, collapsed
    # into an unreadable dark blob at 16x16 - there just isn't enough room
    # in a ~6x6px corner for a thin open curve to survive outlining. A
    # closed ring with an explicit rectangular hole punched out of the
    # middle is much more robust: the hole is guaranteed to stay hollow
    # (it's carved out of a solid fill, not a gap between two separately
    # drawn strokes that outlining can accidentally bridge), so the
    # silhouette reads clearly as "ring/hook eye" even this small. Same
    # underlying lesson as the Prismium armor legging gap bug in
    # PROGRESS.md session 3: don't rely on two strokes staying apart,
    # carve negative space out of solid fill instead.
    full = {(x, y) for x in range(9, 15) for y in range(0, 6)}
    rounded_corners = {(9, 0), (14, 0), (14, 5)}  # keep (9,5): connects to rope
    hole = {(x, y) for x in range(11, 13) for y in range(2, 4)}
    hook_pts = full - rounded_corners - hole

    def shade(x, y):
        d = x + (SIZE - y)
        if d >= 26:
            return S_HILITE
        if d >= 22:
            return S_BASE
        return S_SHADOW

    for (x, y) in hook_pts:
        set_px(px, x, y, shade(x, y))
    draw_outline(px, hook_pts, S_OUTLINE)

    # --- Prismium accent gem at the rope-to-hook joint.
    set_px(px, 9, 6, ACCENT)
    set_px(px, 9, 5, ACCENT_HILITE)

    return img

def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)
    img = make_grappling_hook()
    out_path = out_dir / "prismium_grappling_hook.png"
    img.save(out_path)
    print(f"wrote {out_path}")


if __name__ == "__main__":
    main()
