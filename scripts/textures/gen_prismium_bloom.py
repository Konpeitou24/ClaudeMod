#!/usr/bin/env python3
"""Generate the texture for Prismium Bloom (session 17's first surface
decoration for the Prism Realm dimension - see PrismiumBloomBlock /
ModBlocks.PRISMIUM_BLOOM / PROGRESS.md).

Unlike every earlier block in the mod (full cube faces), this one is a
16x16 texture for a vanilla "block/cross" model: two crossed quads, so
most of the canvas must stay fully transparent and only the drawn pixels
matter. Visual language: a short dark stem (distinct from the teal/cyan
crystal palette so it doesn't get confused with the "glow" itself) topped
with a cluster of Prismium crystal "petals" using the same flat-banded
shading (outline/shadow/base/mid/hilite) as every other texture in the
family, plus a couple of violet accent flecks tying it back to the energy
system theme (same accent color used since gen_prismium.py in session 1).

Deterministic (fixed seed). Run from repo root:
python3 scripts/textures/gen_prismium_bloom.py
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
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_DARK = "#720070"

# Stem: a dark, desaturated violet - reads as "stalk", stays visually
# distinct from both the crystal petals above and the pure-black outline
# used elsewhere, so the silhouette doesn't merge into a single blob.
STEM_DARK = "#241B33"
STEM_MID = "#3B2C52"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_bloom():
    img = new_img()
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    accent = hexrgb(PRISMIUM_ACCENT)
    accent_dark = hexrgb(PRISMIUM_ACCENT_DARK)
    stem_dark = hexrgb(STEM_DARK)
    stem_mid = hexrgb(STEM_MID)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    # Stem: 2px wide, y=11..15, slightly tapered by alternating the two
    # stem tones so it doesn't read as a flat solid rectangle.
    for y in range(11, 16):
        put(7, y, stem_mid if y % 2 == 0 else stem_dark)
        put(8, y, stem_dark if y % 2 == 0 else stem_mid)

    # Bloom head: a diamond cluster of crystal "petals" centered around
    # (7.5, 7), built from concentric Chebyshev-ish rings like the rest of
    # the family's flat-banded shading, but clipped to a diamond
    # (Manhattan distance) silhouette so it reads as a flower head instead
    # of a square/circle.
    cx, cy = 7.5, 7.0
    for y in range(0, 12):
        for x in range(0, 16):
            dx = abs(x - cx)
            dy = abs(y - cy)
            manhattan = dx + dy
            if manhattan > 6.5:
                continue
            if manhattan > 5.2:
                c = outline
            elif manhattan > 4.0:
                c = shadow
            elif manhattan > 2.7:
                c = base
            elif manhattan > 1.4:
                c = mid
            else:
                c = hilite
            put(x, y, c)

    # A handful of violet accent flecks near the petal tips - deliberately
    # sparse (4 pixels) after the session 4 lantern's lesson (PROGRESS.md:
    # a denser first pass read as visual noise at small scale).
    for (fx, fy, c) in [(4, 4, accent), (11, 4, accent_dark), (7, 1, accent), (8, 9, accent_dark)]:
        if px[fx, fy][3] != 0:  # only tint pixels that are already part of the bloom
            put(fx, fy, c)

    return img


def main():
    img = make_prismium_bloom()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "prismium_bloom.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    # 24x scaled preview on a checkerboard background so transparency is
    # visible during self-review (required step, see PROGRESS.md).
    scale = 24
    checker = Image.new("RGBA", (SIZE * scale, SIZE * scale), (0, 0, 0, 0))
    cpx = checker.load()
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            light = ((x // scale) + (y // scale)) % 2 == 0
            cpx[x, y] = (200, 200, 200, 255) if light else (150, 150, 150, 255)
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    checker.alpha_composite(big)
    preview_path = REPO_ROOT / "prismium_bloom_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
