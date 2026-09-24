"""Generate the Prismium Familiar entity texture (64x32, SquidModel UV layout).

ClaudeMod's seventh mob, first tamable companion (see
PrismiumFamiliarEntity.java's javadoc). Rather than author a brand new
SquidModel UV layout by guesswork, this recolors the mod's own existing
prismium_wisp.png (already proven to line up correctly with SquidModel's
UV rects via Wisp's successful CI builds) via an HSV hue remap: Wisp's
warm gold highlight band becomes a soft rose/pink glow, and its violet
base is nudged toward a gentler lavender, turning a "drifting golden
light spirit" look into a "friendly rose-lavender companion spirit"
look while keeping every shading/UV boundary pixel-identical to the
proven-correct source.

Run from the repo root: `python3 scripts/textures/gen_prismium_familiar.py`
"""

import colorsys
import os

from PIL import Image

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC_PATH = os.path.join(
    REPO_ROOT, "src/main/resources/assets/claudemod/textures/entity/prismium_wisp.png"
)
OUT_PATH = os.path.join(
    REPO_ROOT, "src/main/resources/assets/claudemod/textures/entity/prismium_familiar.png"
)

# Gold family (Wisp's bright highlight, hue ~40-54deg) -> soft rose/pink (~330-345deg).
GOLD_HUE_MIN, GOLD_HUE_MAX = 40, 54
ROSE_HUE_MIN, ROSE_HUE_MAX = 330, 345
# Dark/base family (Wisp's shifted-violet base, hue ~235-320deg after its own
# remap) -> nudged further toward a gentler lavender (slightly higher hue).
BASE_HUE_MIN, BASE_HUE_MAX = 235, 320
BASE_HUE_SHIFT = 15


def remap_pixel(r, g, b, a):
    if a == 0:
        return (0, 0, 0, 0)
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    deg = h * 360.0
    if GOLD_HUE_MIN <= deg <= GOLD_HUE_MAX:
        t = (deg - GOLD_HUE_MIN) / (GOLD_HUE_MAX - GOLD_HUE_MIN)
        new_deg = ROSE_HUE_MIN + t * (ROSE_HUE_MAX - ROSE_HUE_MIN)
        new_h = new_deg / 360.0
        new_s = min(1.0, s * 0.9)
        new_v = min(1.0, v * 1.05)
        nr, ng, nb = colorsys.hsv_to_rgb(new_h, new_s, new_v)
        return (round(nr * 255), round(ng * 255), round(nb * 255), a)
    if BASE_HUE_MIN <= deg <= BASE_HUE_MAX:
        new_h = ((deg + BASE_HUE_SHIFT) % 360) / 360.0
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
