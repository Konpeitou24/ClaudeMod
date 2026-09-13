#!/usr/bin/env python3
"""Generate the texture for Prismium Geode Cluster (scheduled session,
2026-09-13's third "block/cross" surface-decoration crystal prop, after
gen_prismium_bloom.py (session 17) and gen_prismium_spike.py (session 18)
- see PrismiumGeodeClusterBlock / ModBlocks.PRISMIUM_GEODE_CLUSTER).

Same 16x16 canvas and shared Prismium shading ramp as the rest of the
family, but a deliberately different silhouette so it reads as a third,
distinct decoration rather than a re-arranged duplicate of Bloom/Spike:
  - Bloom: wide diamond "flower head" on a short stem.
  - Spike: three tall, narrow, pointed shards, almost no base.
  - Geode Cluster (this script): a wide, jagged broken-rock base (evoking
    a cracked-open geode) spanning nearly the full width, with FIVE short
    stubby crystal points of varying height poking out of it at
    different x-positions - short and wide instead of tall and narrow,
    so the overall silhouette reads as "a chunk of rock full of small
    crystals" rather than "a single plant."

Deterministic (fixed seed - no actual randomness used, but kept for
consistency with the rest of the family's texture scripts). Run from repo
root: python3 scripts/textures/gen_prismium_geode_cluster.py
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

# Same rocky base palette as gen_prismium_spike.py, so the two blocks'
# "grows out of broken rock" bases read as the same material.
ROOT_DARK = "#1B2226"
ROOT_MID = "#2C3A3F"
ROOT_HI = "#3D4E54"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_geode_cluster():
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

    # Jagged broken-rock base spanning nearly the full width (x=1..14),
    # with an uneven top edge (per-column height) so it reads as broken
    # rock rather than a flat platform like Spike's thin straight band.
    rock_top = {
        1: 13, 2: 12, 3: 13, 4: 12, 5: 13, 6: 12, 7: 13,
        8: 12, 9: 13, 10: 12, 11: 13, 12: 12, 13: 13, 14: 12,
    }
    rock_colors = [root_dark, root_mid, root_hi]
    for x, top_y in rock_top.items():
        for y in range(top_y, 16):
            c = rock_colors[(x + y) % 3]
            put(x, y, c)

    # Five short, stubby crystal points of varying height/width poking up
    # out of the rock at different x-positions, using the same
    # triangular-taper shading technique as gen_prismium_spike.py's
    # shards but much shorter (max height 6px vs Spike's up to 15px) and
    # spread across the whole width instead of clustered at the center.
    # Defined as (center_x, base_y, tip_y, half_width_at_base).
    points = [
        (2, 12, 8, 1),
        (5, 12, 6, 2),
        (8, 11, 4, 2),
        (11, 12, 7, 1),
        (13, 12, 9, 1),
    ]

    for cx, base_y, tip_y, half_w in points:
        height = base_y - tip_y
        for y in range(tip_y, base_y):
            t = (y - tip_y) / max(height, 1)  # 0 at tip, 1 at base
            w = max(0, round(half_w * t))
            for x in range(cx - w, cx + w + 1):
                dx = abs(x - cx)
                tip_bias = 1.0 - t
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
    img = make_prismium_geode_cluster()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "prismium_geode_cluster.png"
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
    preview_path = REPO_ROOT / "prismium_geode_cluster_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
