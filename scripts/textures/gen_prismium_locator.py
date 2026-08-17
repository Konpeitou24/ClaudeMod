#!/usr/bin/env python3
"""Generate the item icon for Prismium Locator (session 16).

The mod's second accessory-style item (after the session 7 grappling
hook) and first detection item - see PrismiumLocatorItem. Visual
language: a round steel compass-like casing (same neutral steel palette
as the grappling hook's hook ring, gen_prismium_grappling_hook.py
STEEL_*, for family consistency) with a dark inner dial face and a single
diagonal Prismium crystal "needle" pointing toward the upper-right,
echoing a compass needle without literally reusing vanilla's compass
texture. A small bright accent tip marks the needle head, the same
"accent gem" trick used on the grappling hook and tool set.

Self-review note: first draft used a perfect mathematical circle mask
for the casing, which at 16x16 produced a slightly lumpy silhouette with
a few stray single-pixel corner nubs. Manually trimmed the four
diagonal-most corner pixels of the outer ring so the silhouette reads as
a clean rounded disc rather than a jagged circle approximation - same
"simplify toward a clear silhouette" lesson as the grappling hook and
Prismium Core (PROGRESS.md session 3 / session 7).

Deterministic (no RNG). Run from repo root:
python3 scripts/textures/gen_prismium_locator.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette ---------------------------------------------------------
PRISMIUM_ACCENT = "#C97BFF"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"

# Steel casing: same neutral grey family as the grappling hook's hook
# ring and the tool set's hilt, for cross-item consistency.
STEEL_OUTLINE = "#1B1B22"
STEEL_SHADOW = "#3A3A46"
STEEL_BASE = "#5B5B6B"
STEEL_HILITE = "#8A8A9C"

# Dial face: near-black, so the crystal needle reads clearly against it.
DIAL_FACE = "#15141C"
DIAL_FACE_HILITE = "#232230"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_HILITE = hexrgb(PRISMIUM_ACCENT_HILITE)
S_OUTLINE = hexrgb(STEEL_OUTLINE)
S_SHADOW = hexrgb(STEEL_SHADOW)
S_BASE = hexrgb(STEEL_BASE)
S_HILITE = hexrgb(STEEL_HILITE)
FACE = hexrgb(DIAL_FACE)
FACE_HILITE = hexrgb(DIAL_FACE_HILITE)


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


def disc(cx, cy, r):
    """Round disc of block coordinates, trimmed of the jaggiest corner
    pixels so it reads as a clean circle rather than a diamond/square
    at this size (see module docstring self-review note)."""
    pts = set()
    for x in range(SIZE):
        for y in range(SIZE):
            ddx = x - cx + 0.5
            ddy = y - cy + 0.5
            if ddx * ddx + ddy * ddy <= r * r:
                pts.add((x, y))
    return pts


def make_locator():
    img = new_img()
    px = img.load()

    cx, cy = 7.5, 7.5

    outer = disc(cx, cy, 7.2)
    # Trim the 4 most-diagonal corner pixels of the bounding box so the
    # silhouette doesn't show stray square nubs (self-review note above).
    for (x, y) in [(1, 1), (1, 14), (14, 1), (14, 14),
                   (2, 2), (2, 13), (13, 2), (13, 13)]:
        outer.discard((x, y))

    inner = disc(cx, cy, 5.4)

    ring = outer - inner

    # Shade the steel ring: light source from upper-left.
    for (x, y) in ring:
        d = (x - cx) + (cy - y)
        if d >= 5:
            color = S_HILITE
        elif d >= -2:
            color = S_BASE
        else:
            color = S_SHADOW
        set_px(px, x, y, color)
    draw_outline(px, outer, S_OUTLINE)

    # Dial face fill (dark), with a subtle lighter crescent upper-left.
    for (x, y) in inner:
        d = (x - cx) + (cy - y)
        color = FACE_HILITE if d >= 4 else FACE
        set_px(px, x, y, color)

    # Crystal needle: a thin diagonal shard from lower-left of the dial
    # toward upper-right, like a compass needle pointing to a find.
    needle = [
        (5, 10), (6, 9), (6, 8), (7, 8), (7, 7), (8, 7),
        (8, 6), (9, 6), (9, 5),
    ]
    for i, (x, y) in enumerate(needle):
        if (x, y) not in inner:
            continue
        set_px(px, x, y, ACCENT_HILITE if i >= len(needle) - 3 else ACCENT)

    # Bright tip + small back counterweight dot, like a real needle.
    set_px(px, 10, 4, ACCENT_HILITE)
    set_px(px, 5, 11, S_SHADOW)

    # Center pivot pin.
    set_px(px, 7, 8, S_HILITE)

    return img


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)
    img = make_locator()
    out_path = out_dir / "prismium_locator.png"
    img.save(out_path)
    print(f"wrote {out_path}")


if __name__ == "__main__":
    main()
