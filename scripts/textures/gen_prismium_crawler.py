#!/usr/bin/env python3
"""Generate the entity texture for Prismium Crawler, ClaudeMod's fifth
mob and first purely ambient *land* creature (see PrismiumCrawlerEntity's
javadoc for the design writeup).

Layout: reuses vanilla's SilverfishModel geometry (confirmed generic,
<T extends Entity>, via mappings.dev's 1.20.1 mojmap javadoc this
session - see PROGRESS.md). This sandbox has no way to confirm
SilverfishModel's exact UV region layout or vanilla silverfish.png's
exact pixel dimensions against real game source (network access from
this build sandbox is restricted to github.com's HTML pages only), so
64x32 is used as a well-established, high-confidence assumption (the
standard small-mob texture canvas size, same one gen_prismium_drifter.py
used for SquidModel) rather than guesswork.

Mitigation (same "belt-and-braces" technique gen_prismium_drifter.py
pioneered, reused verbatim per PROGRESS.md's "reuse verified technique on
the Nth application" lesson): rather than assuming where SilverfishModel's
UV rects for its ~7 segmented body parts fall, this script paints the
*entire* canvas with one continuous, seamless vertical gradient plus
scattered glow-dot clusters distributed across the whole image. Whatever
crop SilverfishModel actually takes for any given segment, it will still
look like an intentional slice of the same crystalline creature - worst
case is "slightly different crop of the same gradient", never a visible
seam or wrong-part color.

Design: unlike every prior Prismium mob (all built on the teal
PRISMIUM_* palette), this one gets its own accent so the mod's five mobs
don't all look identical at a glance: dark indigo "casing" base (same
family as the other mobs' 0x2b1033-derived dark violet, for family
cohesion) fading into a magenta/pink crystal glow - the same magenta
already established by Prismium Core/Chiseled Prismium Core's gem color
(see RELEASE_NOTES.md v0.3.0) as a valid second "Prismium accent" next to
the teal one, now extended to a mob for the first time.

Self-review: writes an upscaled checkerboard preview to
build/preview_prismium_crawler.png for Read-based visual inspection, and
prints the set of distinct alpha values present (must be only 0/255, no
partial-transparency bleeding), matching the mod's texture workflow
rules. Run from repo root:
    python3 scripts/textures/gen_prismium_crawler.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260830
W, H = 64, 32

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Dark indigo "casing" palette - same family as the other mobs' dark
# violet base, kept close (not identical) so the Crawler still reads as
# part of the Prismium mob family.
BASE_OUTLINE = "#0E0A1C"
BASE_DARK = "#241246"
BASE_MID = "#3D1E5E"

# Magenta/pink crystal glow palette (first mob to use this accent instead
# of the teal PRISMIUM_* one - see module docstring).
GLOW_OUTLINE = "#5C1259"
GLOW_SHADOW = "#9A1F8F"
GLOW_BASE = "#D93FC9"
GLOW_MID = "#FF7BE8"
GLOW_HILITE = "#FFD6F7"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


BO = hexrgb(BASE_OUTLINE)
BD = hexrgb(BASE_DARK)
BM = hexrgb(BASE_MID)
GO = hexrgb(GLOW_OUTLINE)
GS = hexrgb(GLOW_SHADOW)
GB = hexrgb(GLOW_BASE)
GM = hexrgb(GLOW_MID)
GH = hexrgb(GLOW_HILITE)

# Gradient stops, top (y=0) to bottom (y=H-1): dark indigo outline -> dark
# indigo -> mid indigo -> magenta shadow -> magenta base -> magenta mid ->
# pink hilite. Same top-dark/bottom-bright shape as the Drifter script
# (glow concentrated toward the "underside"/legs region on most plausible
# UV unwraps for a small segmented creature).
GRADIENT_STOPS = [BO, BO, BD, BD, BD, BM, BM, GS, GS, GB, GB, GB, GM, GM, GH]


def lerp(c0, c1, t):
    return tuple(round(c0[i] + (c1[i] - c0[i]) * t) for i in range(4))


def gradient_color(t, rng):
    n = len(GRADIENT_STOPS)
    pos = t * (n - 1)
    i0 = min(int(pos), n - 2)
    frac = pos - i0
    color = lerp(GRADIENT_STOPS[i0], GRADIENT_STOPS[i0 + 1], frac)
    if rng.random() < 0.12:
        color = lerp(color, GRADIENT_STOPS[min(i0 + 1, n - 1)], 0.35)
    return color


def make_crawler_texture():
    rng = random.Random(SEED)
    img = Image.new("RGBA", (W, H), BO)
    px = img.load()

    for y in range(H):
        t = y / (H - 1)
        for x in range(W):
            px[x, y] = gradient_color(t, rng)

    # Scattered crystalline glow-dot clusters, distributed across the
    # whole canvas (not tied to any assumed body-part rect), same
    # technique as gen_prismium_drifter.py's bioluminescent spots.
    n_clusters = 16
    for _ in range(n_clusters):
        cx = rng.randrange(2, W - 2)
        cy = rng.randrange(1, H - 1)
        if cy == 0:
            continue
        px[cx, cy] = GH
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = cx + dx, cy + dy
            if 0 <= nx < W and 0 <= ny < H and rng.random() < 0.55:
                px[nx, ny] = GM if rng.random() < 0.5 else GS

    return img


def make_preview(img, scales=(1, 4, 8)):
    """Same preview compositor as gen_prismium_drifter.py (checkerboard
    background so alpha issues are visible on Read-based self-review)."""
    tile = 4
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * W for s in scales) + 8 * (len(scales) - 1)
    total_h = max(s * H for s in scales)
    preview = Image.new("RGBA", (total_w, total_h), (30, 30, 30, 255))

    x_off = 0
    for s in scales:
        board = Image.new("RGBA", (W * s, H * s))
        bpx = board.load()
        for y in range(H * s):
            for x in range(W * s):
                cx, cy = x // tile, y // tile
                bpx[x, y] = checker_light if (cx + cy) % 2 == 0 else checker_dark
        scaled = img.resize((W * s, H * s), Image.NEAREST)
        board.alpha_composite(scaled)
        preview.alpha_composite(board, (x_off, 0))
        x_off += W * s + 8

    return preview


def main():
    ASSETS.joinpath("entity").mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_crawler_texture()
    out_path = ASSETS / "entity" / "prismium_crawler.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_crawler.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
