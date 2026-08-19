#!/usr/bin/env python3
"""Generate the item icon for Prismium Pulse Charm (scheduled session #63)
- see PrismiumPulseCharmItem for the right-click "reveal nearby hostiles"
behavior this icon represents.

Visual language: reuses the exact disc-casing/dark-dial-face silhouette
technique from gen_prismium_locator.py (session 16) - same helper
functions (disc(), draw_outline()) copied verbatim for consistency and
because that circle-approximation-with-trimmed-corners trick was already
proven to read cleanly at 16x16 (see that script's own self-review
note) - but the casing is recolored to a dark teal-black "echo shard/
sculk" metal (distinct from the Locator's neutral steel) to visually tie
this item to its crafting material (Echo Shard, see the recipe json),
and the compass needle is replaced with concentric pulse rings radiating
from a bright center dot - a small "sonar ping" motif that reads as
"detection", matching the item's danger-sensing role without literally
reusing the Locator's compass-needle language (these two detection items
should look like siblings, not twins).

Self-review note: an early version used three ring bands of equal
brightness, which at 16x16 read as a flat bullseye rather than an
outward pulse. Made the outermost ring dimmer than the inner one (light
fading as it travels outward) so the rings read as an expanding pulse
rather than a static target symbol.

Deterministic (no RNG). Run from repo root:
python3 scripts/textures/gen_prismium_pulse_charm.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette -----------------------------------------------------------
# Prismium family accent, reused verbatim from every other item script
# (gen_prismium_locator.py, gen_prismium_grappling_hook.py, etc.) so this
# charm still reads as part of the same item family at a glance.
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"
PRISMIUM_ACCENT_DIM = "#B34FB0"

# Casing: a dark teal-black "echo shard/sculk" metal, distinct from the
# Locator's neutral steel casing (see module docstring) - ties the icon
# to its Echo Shard crafting material.
CASING_OUTLINE = "#07141A"
CASING_SHADOW = "#123339"
CASING_BASE = "#1F565E"
CASING_HILITE = "#3D8A90"

# Dial face: near-black, same role as the Locator's dial face.
DIAL_FACE = "#0C0E14"
DIAL_FACE_HILITE = "#1A1E2C"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_HILITE = hexrgb(PRISMIUM_ACCENT_HILITE)
ACCENT_DIM = hexrgb(PRISMIUM_ACCENT_DIM)
C_OUTLINE = hexrgb(CASING_OUTLINE)
C_SHADOW = hexrgb(CASING_SHADOW)
C_BASE = hexrgb(CASING_BASE)
C_HILITE = hexrgb(CASING_HILITE)
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
    pixels - copied verbatim from gen_prismium_locator.py (see that
    script's own self-review note for why)."""
    pts = set()
    for x in range(SIZE):
        for y in range(SIZE):
            ddx = x - cx + 0.5
            ddy = y - cy + 0.5
            if ddx * ddx + ddy * ddy <= r * r:
                pts.add((x, y))
    return pts


def make_pulse_charm():
    img = new_img()
    px = img.load()

    cx, cy = 7.5, 7.5

    outer = disc(cx, cy, 7.2)
    for (x, y) in [(1, 1), (1, 14), (14, 1), (14, 14),
                   (2, 2), (2, 13), (13, 2), (13, 13)]:
        outer.discard((x, y))

    inner = disc(cx, cy, 5.4)
    ring = outer - inner

    # Shade the casing ring: light source from upper-left (same
    # convention as every other item script in this mod).
    for (x, y) in ring:
        d = (x - cx) + (cy - y)
        if d >= 5:
            color = C_HILITE
        elif d >= -2:
            color = C_BASE
        else:
            color = C_SHADOW
        set_px(px, x, y, color)
    draw_outline(px, outer, C_OUTLINE)

    # Dial face fill (dark), subtle lighter crescent upper-left.
    for (x, y) in inner:
        d = (x - cx) + (cy - y)
        color = FACE_HILITE if d >= 4 else FACE
        set_px(px, x, y, color)

    # Concentric pulse rings radiating from center - a "sonar ping"
    # motif standing in for the item's danger-sensing pulse. Outermost
    # ring deliberately dimmer than the inner one, so it reads as light
    # fading as it travels outward rather than a flat bullseye target
    # (see module docstring self-review note).
    for (x, y) in inner:
        dist = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
        if dist <= 1.1:
            set_px(px, x, y, ACCENT_HILITE)
        elif 2.1 <= dist <= 2.9:
            set_px(px, x, y, ACCENT)
        elif 3.9 <= dist <= 4.5:
            set_px(px, x, y, ACCENT_DIM)

    return img


def make_preview(img, scales=(4, 8, 16)):
    tile = 2
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * SIZE for s in scales) + 8 * (len(scales) - 1)
    total_h = max(s * SIZE for s in scales)
    preview = Image.new("RGBA", (total_w, total_h), (30, 30, 30, 255))

    x_off = 0
    for s in scales:
        board = Image.new("RGBA", (SIZE * s, SIZE * s))
        bpx = board.load()
        for y in range(SIZE * s):
            for x in range(SIZE * s):
                cx2, cy2 = x // tile, y // tile
                bpx[x, y] = checker_light if (cx2 + cy2) % 2 == 0 else checker_dark
        scaled = img.resize((SIZE * s, SIZE * s), Image.NEAREST)
        board.alpha_composite(scaled)
        preview.alpha_composite(board, (x_off, 0))
        x_off += SIZE * s + 8

    return preview


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_pulse_charm()
    out_path = out_dir / "prismium_pulse_charm.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_pulse_charm.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
