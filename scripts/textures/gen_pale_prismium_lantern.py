#!/usr/bin/env python3
"""Generate block/pale_prismium_lantern.png.

PROGRESS.md TODO/section-5 item ("Prismium Lantern / Pale Prismium
Lanternの形状が...単純な立方体(cube_all)のまま"): mirrors
gen_prismium_lantern.py's 2026-09-01 rework - the Pale Prismium Lantern
block now also parents onto vanilla's `minecraft:block/template_lantern`
/ `minecraft:block/template_hanging_lantern` (see
com.claudemod.block.PrismiumLanternBlock, reused by both lanterns), which
needs a texture laid out for that template's specific UV unwrap (body
sides, top/bottom grille, top-cage lip, vertical handle/chain column)
instead of the old flat cube_all repeating pattern.

Keeps this script self-contained (duplicating the drawing helpers rather
than importing gen_prismium_lantern.py) to match this repo's existing
per-script convention, and keeps the PALE_* icy palette + cooler
blue-black cage color from the original session #79 version - only the
UV layout changed, not the color language.

Run from repo root: python3 scripts/textures/gen_pale_prismium_lantern.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PALE_OUTLINE = "#31536E"
PALE_SHADOW = "#5C8CB0"
PALE_BASE = "#9FD3EE"
PALE_MID = "#CDEBFA"
PALE_HILITE = "#F6FCFF"
PALE_ACCENT = "#7EE6FF"
PALE_CORE_WHITE = "#FFFFFF"

# Cage frame: cool blue-black, distinct from PALE_OUTLINE so the cage
# still reads as a separate "metal" layer over the icy glow rather than
# blending into the border.
CAGE_DARK = "#1B2A38"
CAGE_MID = "#2E4356"
CAGE_HILITE = "#3F5C73"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def glow_color(d, palette):
    outline, shadow, base, mid, hilite, core_white = palette
    if d < 1.0:
        return core_white
    elif d < 2.0:
        return hilite
    elif d < 3.2:
        return mid
    elif d < 4.2:
        return base
    else:
        return shadow


def draw_cage_window(px, x0, y0, w, h, palette, accent, put_accent):
    outline, shadow, base, mid, hilite, core_white = palette
    cage_dark = hexrgb(CAGE_DARK)
    cage_mid = hexrgb(CAGE_MID)
    cx, cy = x0 + (w - 1) / 2.0, y0 + (h - 1) / 2.0
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            on_border = xx in (x0, x0 + w - 1) or yy in (y0, y0 + h - 1)
            if on_border:
                px[xx, yy] = (*cage_dark, 255)
            else:
                d = max(abs(xx - cx), abs(yy - cy))
                px[xx, yy] = (*hexrgb(glow_color(d, palette)), 255)
    px[x0, y0] = (*cage_mid, 255)
    if put_accent and w >= 4 and h >= 5:
        px[x0 + 2, y0 + 2] = (*hexrgb(accent), 255)


def draw_hoop_strip(px, x0, y0, w, h):
    cage_dark = hexrgb(CAGE_DARK)
    cage_mid = hexrgb(CAGE_MID)
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            c = cage_mid if (xx + yy) % 2 == 0 else cage_dark
            px[xx, yy] = (*c, 255)


def draw_handle_column(px, x0, y0, w, h):
    cage_dark = hexrgb(CAGE_DARK)
    cage_mid = hexrgb(CAGE_MID)
    cage_hi = hexrgb(CAGE_HILITE)
    for yy in range(y0, y0 + h):
        link_phase = yy % 3
        for xx in range(x0, x0 + w):
            edge = xx in (x0, x0 + w - 1)
            if link_phase == 0:
                c = cage_dark
            elif edge:
                c = cage_mid
            else:
                c = cage_hi
            px[xx, yy] = (*c, 255)


def make_lantern_texture(outline, shadow, base, mid, hilite, core_white, accent):
    palette = (outline, shadow, base, mid, hilite, core_white)
    img = new_img()
    px = img.load()

    cage_dark = hexrgb(CAGE_DARK)
    for yy in range(SIZE):
        for xx in range(SIZE):
            px[xx, yy] = (*cage_dark, 255)

    draw_cage_window(px, 0, 2, 6, 7, palette, accent, put_accent=True)
    draw_cage_window(px, 0, 9, 6, 6, palette, accent, put_accent=True)
    draw_hoop_strip(px, 1, 0, 4, 2)
    draw_hoop_strip(px, 1, 10, 4, 4)
    draw_handle_column(px, 11, 0, 3, 13)

    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    img = make_lantern_texture(
        PALE_OUTLINE, PALE_SHADOW, PALE_BASE, PALE_MID,
        PALE_HILITE, PALE_CORE_WHITE, PALE_ACCENT)
    save(img, "block/pale_prismium_lantern.png")
