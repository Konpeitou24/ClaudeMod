#!/usr/bin/env python3
"""Generate the texture for Prismium Spike (session 18's second surface
decoration for the Prism Realm / overworld - see PrismiumSpikeBlock /
ModBlocks.PRISMIUM_SPIKE / PROGRESS.md).

Sibling to gen_prismium_bloom.py (session 17): same 16x16 "block/cross"
canvas, same flat-banded shading technique, but a deliberately different
silhouette and palette so the two decorations read as distinct plants at
a glance rather than palette-swapped duplicates:
  - Bloom: wide diamond "flower head" on a short stem, warm violet accent.
  - Spike: a tall, narrow, pointed crystal shard cluster (three shards of
    slightly different heights) with almost no stem, cool cyan-leaning
    accent instead of violet, and a sharper pointed silhouette (built from
    a distance-to-centerline taper instead of Bloom's Manhattan-distance
    diamond) to read as "jagged crystal" rather than "flower".

Deterministic (fixed seed - no actual randomness used, but kept for
consistency with the rest of the family's texture scripts). Run from repo
root: python3 scripts/textures/gen_prismium_spike.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same outline/shadow/base/mid/hilite ramp as the rest of the Prismium
# family (gen_prismium.py onward) for cross-texture consistency.
PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_SHADOW = "#1E7A78"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"

# Spike-only accent: cool cyan/blue instead of Bloom's warm violet, so the
# two decorations don't share the same accent color at a glance.
SPIKE_ACCENT = "#7BD9FF"
SPIKE_ACCENT_DARK = "#3F8FA6"

# Root/base: a dark desaturated teal-grey (rock-like), distinct from both
# the crystal ramp and the pure-black outline, echoing the stem treatment
# in gen_prismium_bloom.py but reading as "broken rock base" rather than
# "stalk" since these shards jut straight out of the ground.
ROOT_DARK = "#1B2226"
ROOT_MID = "#2C3A3F"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_spike():
    img = new_img()
    px = img.load()

    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    accent = hexrgb(SPIKE_ACCENT)
    accent_dark = hexrgb(SPIKE_ACCENT_DARK)
    root_dark = hexrgb(ROOT_DARK)
    root_mid = hexrgb(ROOT_MID)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    # Rocky base: a thin 2px band at the very bottom, so the shards read
    # as jutting out of broken ground rather than floating on nothing.
    for x in range(3, 13):
        put(x, 15, root_dark if x % 2 == 0 else root_mid)

    # Three crystal shards of different heights/widths/x-offsets, each
    # built as a simple triangular taper (wide at the base, pointed at the
    # tip) using the shared shading ramp. Defined as
    # (center_x, base_y, tip_y, half_width_at_base).
    shards = [
        (5, 15, 3, 2),   # left shard, medium height
        (9, 15, 1, 2),   # right shard, tallest (near-full height)
        (7, 15, 7, 3),   # center shard, shortest and widest - anchors the
                          # cluster visually so it doesn't look like two
                          # separate plants side by side
    ]

    for cx, base_y, tip_y, half_w in shards:
        height = base_y - tip_y
        for y in range(tip_y, base_y):
            # Linear taper: half-width shrinks from half_w at the base to
            # ~0 at the tip.
            t = (y - tip_y) / max(height, 1)  # 0 at tip, 1 at base
            w = max(0, round(half_w * t))
            for x in range(cx - w, cx + w + 1):
                # Shade by how close to the shard's centerline (gives each
                # shard its own little highlight spine) and by how close
                # to the tip (tips read brighter/sharper, like real
                # crystal facets catching light).
                dx = abs(x - cx)
                tip_bias = 1.0 - t  # 1 at tip, 0 at base
                score = dx - tip_bias * 1.5
                if score > 1.6:
                    c = outline
                elif score > 0.9:
                    c = shadow
                elif score > 0.1:
                    c = base
                elif score > -0.6:
                    c = mid
                else:
                    c = hilite
                put(x, y, c)

    # A few cyan accent flecks near the shard tips, sparse like Bloom's
    # violet flecks (session 4 lantern lesson: dense flecks read as noise
    # at 16x16). Only tints pixels that are already part of a shard.
    for (fx, fy, c) in [(9, 2, accent), (5, 4, accent_dark), (7, 8, accent)]:
        if px[fx, fy][3] != 0:
            put(fx, fy, c)

    return img


def main():
    img = make_prismium_spike()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "prismium_spike.png"
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
    preview_path = REPO_ROOT / "prismium_spike_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")


if __name__ == "__main__":
    main()
