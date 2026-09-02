"""Generate the Prismium Wisp entity texture (64x32, SquidModel UV layout).

ClaudeMod's sixth mob, first flying-ambient creature (see
PrismiumWispEntity.java's javadoc). Rather than author a brand new
SquidModel UV layout by guesswork, this recolors the mod's own existing
prismium_drifter.png (already proven to line up correctly with
SquidModel's UV rects via Drifter's successful CI builds) via an HSV hue
remap: Drifter's bright cyan highlight band becomes a warm gold glow,
and its dark violet base is nudged slightly warmer, turning an
"underwater crystal squid" look into a "airborne golden light spirit"
look while keeping every shading/UV boundary pixel-identical to the
proven-correct source.

Run from the repo root: `python3 scripts/textures/gen_prismium_wisp.py`
"""

import colorsys
import os

from PIL import Image

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC_PATH = os.path.join(
    REPO_ROOT, "src/main/resources/assets/claudemod/textures/entity/prismium_drifter.png"
)
OUT_PATH = os.path.join(
    REPO_ROOT, "src/main/resources/assets/claudemod/textures/entity/prismium_wisp.png"
)

# Cyan/teal family (Drifter's bright highlight, hue ~150-245deg) -> warm gold (~40-54deg).
CYAN_HUE_MIN, CYAN_HUE_MAX = 150, 245
GOLD_HUE_MIN, GOLD_HUE_MAX = 40, 54
# Violet/dark family (shadow/base, hue ~245-330deg) -> same hue family, nudged
# slightly warmer (toward magenta/red) for a deeper twilight base.
VIOLET_HUE_MIN, VIOLET_HUE_MAX = 245, 330
VIOLET_HUE_SHIFT = -10


def remap_pixel(r, g, b, a):
    if a == 0:
        return (0, 0, 0, 0)
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    deg = h * 360.0
    if CYAN_HUE_MIN <= deg <= CYAN_HUE_MAX:
        t = (deg - CYAN_HUE_MIN) / (CYAN_HUE_MAX - CYAN_HUE_MIN)
        new_deg = GOLD_HUE_MIN + t * (GOLD_HUE_MAX - GOLD_HUE_MIN)
        new_h = new_deg / 360.0
        new_s = min(1.0, s * 0.85)
        new_v = min(1.0, v * 1.05)
        nr, ng, nb = colorsys.hsv_to_rgb(new_h, new_s, new_v)
        return (round(nr * 255), round(ng * 255), round(nb * 255), a)
    if VIOLET_HUE_MIN < deg <= VIOLET_HUE_MAX:
        new_h = ((deg + VIOLET_HUE_SHIFT) % 360) / 360.0
        nr, ng, nb = colorsys.hsv_to_rgb(new_h, s, v)
        return (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return (r, g, b, a)


def main():
    src = Image.open(SRC_PATH).convert("RGBA")
    out = Image.new("RGBA", src.size)
    src_px = src.load()
    out_px = out.load()
    for y in range(src.height):
        for x in range(src.width):
            out_px[x, y] = remap_pixel(*src_px[x, y])
    out.save(OUT_PATH)
    print(f"wrote {OUT_PATH} ({out.size[0]}x{out.size[1]})")


if __name__ == "__main__":
    main()
