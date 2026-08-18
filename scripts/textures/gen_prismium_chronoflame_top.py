#!/usr/bin/env python3
"""Generate a dedicated TOP-face texture for Prismium Chronoflame
(session 54, scheduled) - see PROGRESS.md section 5 (self-critique
recorded in session 49's review notes for gen_prismium_chronoflame.py):
at 1x scale the existing single cube_all texture's two short "hands"
mostly disappear into the glow core, so the block reads as "a glowing
shrine" but not clearly as "a clock".

Fix approach: split the block into a cube_column model (top/bottom use
this new dedicated "top" texture, sides keep the existing
prismium_chronoflame.png untouched) so the one face players actually
look down at while using the altar can afford a much more literal,
larger clock face without giving up the existing stone-housing side
look. Reuses the same Prismium palette constants and worked-stone grey
samples as gen_prismium_chronoflame.py verbatim (no new colors
invented).

Self-review: writes a checkerboard-composited 1x/4x/8x preview to
build/preview_prismium_chronoflame_top.png for Read-based visual
inspection, per the mod's texture workflow rules. Deterministic (no
RNG). Run from repo root:
    python3 scripts/textures/gen_prismium_chronoflame_top.py
"""
import math
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Same Prismium crystal family constants as gen_prismium_chronoflame.py.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#FF7CFC"

# Same worked-stone greys as gen_prismium_chronoflame.py / gen_prismium_stone.py.
STONE_SHADES = [
    (118, 118, 118, 255),
    (121, 121, 121, 255),
    (130, 130, 130, 255),
    (140, 140, 140, 255),
    (143, 143, 143, 255),
]
STONE_DARK_EDGE = (96, 96, 96, 255)


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def make_chronoflame_top_texture():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    core_white = hexrgb(PRISMIUM_CORE_WHITE)
    accent = hexrgb(PRISMIUM_ACCENT)

    # 1. Worked-stone surround fills the tile first, same deterministic
    # pick-by-position as the side texture so the housing material still
    # reads as the same masonry.
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = STONE_SHADES[(x * 3 + y * 5) % len(STONE_SHADES)]
    for (x, y) in [(1, 1), (14, 2), (2, 14), (13, 13)]:
        px[x, y] = STONE_DARK_EDGE

    cx, cy = 7.5, 7.5

    # 2. A larger circular dial rim (radius ~7) cut into the stone, using
    # the outline/shadow colors as a dark bezel so the round "clock face"
    # silhouette itself is unmistakable even before any hands are drawn.
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, y - cy
            d = math.sqrt(dx * dx + dy * dy)
            if d < 7.2:
                if d >= 6.4:
                    px[x, y] = (*outline, 255)
                elif d >= 5.6:
                    px[x, y] = (*shadow, 255)
                elif d >= 3.2:
                    px[x, y] = (*base, 255)
                elif d >= 1.6:
                    px[x, y] = (*mid, 255)
                else:
                    px[x, y] = (*core_white, 255)

    # 3. Twelve tick marks around the dial rim, computed on a real circle
    # (unlike the side texture's hand-listed points) so they land evenly
    # spaced now that the dial fills much more of the tile.
    for i in range(12):
        angle = math.radians(i * 30 - 90)  # 12 o'clock first, clockwise
        tx = cx + 6.0 * math.cos(angle)
        ty = cy + 6.0 * math.sin(angle)
        xi, yi = round(tx), round(ty)
        if 0 <= xi < SIZE and 0 <= yi < SIZE:
            # The 12/3/6/9 marks get a slightly brighter hilite pixel so
            # the four cardinal ticks stand out from the other eight.
            px[xi, yi] = (*hilite, 255) if i % 3 == 0 else (*accent, 255)

    # 4. Hour + minute hands, this time long and unambiguous (the whole
    # point of this texture): minute hand toward 12, hour hand toward 4,
    # both drawn as short line segments via integer steps from center.
    def draw_hand(length, angle_deg, color, width_extra=None):
        angle = math.radians(angle_deg - 90)
        dx, dy = math.cos(angle), math.sin(angle)
        for step in range(1, length + 1):
            xi = round(cx + dx * step)
            yi = round(cy + dy * step)
            if 0 <= xi < SIZE and 0 <= yi < SIZE:
                px[xi, yi] = (*color, 255)
            if width_extra and step <= length - 1:
                # thicken near the base only, tapering toward the tip
                perp_x, perp_y = -dy, dx
                xi2 = round(cx + dx * step + perp_x * 0.6)
                yi2 = round(cy + dy * step + perp_y * 0.6)
                if 0 <= xi2 < SIZE and 0 <= yi2 < SIZE:
                    px[xi2, yi2] = (*color, 255)

    # Hands are drawn in the dark OUTLINE color (not a bright accent) so
    # they read as solid dark silhouettes against the bright glow, the
    # same way real clock hands read dark against a lit face - a bright
    # hand on a bright core was the exact readability failure of the
    # original single-texture design this file is replacing.
    draw_hand(6, 0, outline, width_extra=True)     # minute hand -> 12
    draw_hand(4, 105, outline, width_extra=True)    # hour hand -> ~3:30

    # Center pin: a single dark pixel (not bright) so it reads as the
    # hands' pivot rather than merging back into the white glow.
    px[7, 7] = (*outline, 255)

    return img


def make_preview(img, scales=(1, 4, 8)):
    checker_a = (60, 60, 60, 255)
    checker_b = (90, 90, 90, 255)
    tiles = []
    for scale in scales:
        big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        bg = Image.new("RGBA", big.size, (0, 0, 0, 0))
        cb = 4
        for y in range(0, big.size[1], cb):
            for x in range(0, big.size[0], cb):
                color = checker_a if ((x // cb) + (y // cb)) % 2 == 0 else checker_b
                for yy in range(y, min(y + cb, big.size[1])):
                    for xx in range(x, min(x + cb, big.size[0])):
                        bg.putpixel((xx, yy), color)
        bg.alpha_composite(big)
        tiles.append(bg)

    pad = 8
    total_w = sum(t.width for t in tiles) + pad * (len(tiles) - 1)
    max_h = max(t.height for t in tiles)
    canvas = Image.new("RGBA", (total_w, max_h), (30, 30, 30, 255))
    x_off = 0
    for t in tiles:
        canvas.alpha_composite(t, (x_off, 0))
        x_off += t.width + pad
    return canvas


def main():
    img = make_chronoflame_top_texture()
    out_path = ASSETS / "block" / "prismium_chronoflame_top.png"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    preview_path = BUILD_DIR / "preview_prismium_chronoflame_top.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")


if __name__ == "__main__":
    main()
