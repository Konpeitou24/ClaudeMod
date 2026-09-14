#!/usr/bin/env python3
"""Generate the texture for Prismium Stalactite Crystal (scheduled
session, 2026-09-14's fourth "block/cross" surface-decoration crystal
prop, after gen_prismium_bloom.py (session 17), gen_prismium_spike.py
(session 18) and gen_prismium_geode_cluster.py (scheduled session,
2026-09-13) - see PrismiumStalactiteBlock / ModBlocks.PRISMIUM_STALACTITE).

Same 16x16 canvas and shared Prismium shading ramp as the rest of the
family, but this one is a vertical mirror of Geode Cluster's silhouette:
the jagged broken-rock base sits at the TOP of the canvas (y=0..3) and
three crystal points hang DOWNWARD out of it (tips pointing toward the
bottom of the canvas, y=15), so it reads as "growing down from a rocky
ceiling" rather than "growing up from the floor" - matching
PrismiumStalactiteBlock's ceiling-mounted canSurvive() check.

Deterministic (fixed seed - no actual randomness used, but kept for
consistency with the rest of the family's texture scripts). Run from repo
root: python3 scripts/textures/gen_prismium_stalactite.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same outline/shadow/base/mid/hilite ramp as the rest of the Prismium
# family (gen_prismium.py onward) for cross-texture consistency.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"

# Same rocky base palette as gen_prismium_spike.py / gen_prismium_geode_
# cluster.py, so all three "grows out of broken rock" bases read as the
# same material.
ROOT_DARK = "#1B2226"
ROOT_MID = "#2C3A3F"
ROOT_HI = "#3D4E54"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_stalactite():
    img = new_img()
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    root_dark = hexrgb(ROOT_DARK)
    root_mid = hexrgb(ROOT_MID)
    root_hi = hexrgb(ROOT_HI)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    # Jagged broken-rock ceiling spanning nearly the full width (x=1..14)
    # at the TOP of the canvas, with an uneven bottom edge (per-column
    # depth) - the vertical mirror of Geode Cluster's rock_top dict.
    rock_bottom = {
        1: 3, 2: 4, 3: 3, 4: 4, 5: 3, 6: 4, 7: 3,
        8: 4, 9: 3, 10: 4, 11: 3, 12: 4, 13: 3, 14: 4,
    }
    rock_colors = [root_dark, root_mid, root_hi]
    for x, bottom_y in rock_bottom.items():
        for y in range(0, bottom_y):
            c = rock_colors[(x + y) % 3]
            put(x, y, c)

    # Three crystal points hanging down out of the rock, tips pointing
    # toward the bottom of the canvas. Defined as (center_x, base_y,
    # tip_y, half_width_at_base) where base_y is where it meets the rock
    # (small y, near the top) and tip_y is the pointed end (large y, near
    # the bottom) - the vertical mirror of Geode Cluster's "points" list.
    points = [
        (3, 4, 12, 2),
        (8, 3, 15, 2),
        (12, 4, 10, 1),
    ]

    for cx, base_y, tip_y, half_w in points:
        height = tip_y - base_y
        for y in range(base_y, tip_y):
            t = (y - base_y) / max(height, 1)  # 0 at base (top), 1 at tip (bottom)
            w = max(0, round(half_w * (1.0 - t)))
            for x in range(cx - w, cx + w + 1):
                dx = abs(x - cx)
                tip_bias = t
                score = dx - tip_bias * 1.3
                if score > 1.4:
                    c = outline
                elif score > 0.8:
                    c = shadow
                elif score > 0.1:
                    c = base
                elif score > -0.5:
                    c = mid
                else:
                    c = hilite
                put(x, y, c)

    return img


def main():
    img = make_prismium_stalactite()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "prismium_stalactite.png"
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
    preview_path = REPO_ROOT / "prismium_stalactite_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
