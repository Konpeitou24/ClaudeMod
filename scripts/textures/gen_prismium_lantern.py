#!/usr/bin/env python3
"""Generate the texture for Prismium Lantern.

PROGRESS.md TODO/section-5 item ("Prismium Lantern / Pale Prismium
Lanternの形状が...単純な立方体(cube_all)のまま"): the block itself was
rebuilt (2026-09-01) into a proper hanging-lantern shape by parenting its
models onto vanilla's own `minecraft:block/template_lantern` /
`minecraft:block/template_hanging_lantern` (see
com.claudemod.block.PrismiumLanternBlock and the model JSONs under
assets/claudemod/models/block/). Those templates use a specific UV
"unwrap" layout (single 16x16 texture, several differently-shaped regions
for the cage body, the top grille, the small hoop lip, and the vertical
handle/chain strip - see the template JSON comments in
PrismiumLanternBlock's class doc for the exact rectangles), which is a
different layout than the old cube_all texture this script used to
generate (one flat repeating face pattern). This version draws directly
into each of those UV rectangles so the new 3D shape actually looks right
instead of showing stretched/misaligned fragments of a cube-only design.

Reuses the family's existing "dark metal cage over Prismium glow" visual
language (same palette as gen_prismium_core.py/gen_prismium_block.py)
rather than inventing a new look, just recomposed to fit the lantern
unwrap. Deterministic (fixed seed). Run from repo root:
python3 scripts/textures/gen_prismium_lantern.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#FF7CFC"

# Dark cage frame color: distinct from PRISMIUM_OUTLINE (a very dark teal
# used as a thin silhouette border elsewhere in the family) - closer to
# neutral dark slate so the cage reads as "metal", not "crystal edge".
CAGE_DARK = "#26302E"
CAGE_MID = "#3B4A47"
CAGE_HILITE = "#57706B"


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
    """Fills a w x h rectangle at (x0, y0) with a 1px dark-cage border and
    a Prismium glow interior - the "window into the lantern's light"
    look shared by the body-side and top/bottom-grille UV regions."""
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
    # A lighter highlight tick on the top-left border corner so the frame
    # doesn't read as flat black, matching the rest of the family.
    px[x0, y0] = (*cage_mid, 255)
    if put_accent and w >= 4 and h >= 5:
        px[x0 + 2, y0 + 2] = (*hexrgb(accent), 255)


def draw_hoop_strip(px, x0, y0, w, h):
    """Fills a small strip with an alternating dark/mid metal pattern -
    used for the lantern's short top-cage lip regions, which are too
    small to show any glow and read best as plain metal."""
    cage_dark = hexrgb(CAGE_DARK)
    cage_mid = hexrgb(CAGE_MID)
    for yy in range(y0, y0 + h):
        for xx in range(x0, x0 + w):
            c = cage_mid if (xx + yy) % 2 == 0 else cage_dark
            px[xx, yy] = (*c, 255)


def draw_handle_column(px, x0, y0, w, h):
    """Fills the vertical handle/chain strip sampled by both the standing
    (short) and hanging (tall) model variants: a repeating link pattern
    so it looks correct regardless of how much of the column height each
    variant actually samples."""
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

    # Safe background fill first (nothing in the template UV layout should
    # ever sample outside the regions drawn below, but this keeps stray
    # pixels from ever showing up as transparent/magenta if a future model
    # tweak samples slightly outside what's currently used).
    cage_dark = hexrgb(CAGE_DARK)
    for yy in range(SIZE):
        for xx in range(SIZE):
            px[xx, yy] = (*cage_dark, 255)

    # Body sides: uv (0,2)-(6,9), sampled on all four vertical faces.
    draw_cage_window(px, 0, 2, 6, 7, palette, accent, put_accent=True)
    # Body top/bottom grille: uv (0,9)-(6,15).
    draw_cage_window(px, 0, 9, 6, 6, palette, accent, put_accent=True)

    # Top-cage lip, sides: uv (1,0)-(5,2).
    draw_hoop_strip(px, 1, 0, 4, 2)
    # Top-cage lip, top/bottom: uv (1,10)-(5,14).
    draw_hoop_strip(px, 1, 10, 4, 4)

    # Vertical handle/chain column: covers every UV row either model
    # variant samples from x=11-14 (standing: y1-3 & y10-12, hanging:
    # y1-5 & y6-12), so fill the whole y=0-13 span once.
    draw_handle_column(px, 11, 0, 3, 13)

    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    img = make_lantern_texture(
        PRISMIUM_OUTLINE, PRISMIUM_SHADOW, PRISMIUM_BASE, PRISMIUM_MID,
        PRISMIUM_HILITE, PRISMIUM_CORE_WHITE, PRISMIUM_ACCENT)
    save(img, "block/prismium_lantern.png")
