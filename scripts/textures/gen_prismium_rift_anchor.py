#!/usr/bin/env python3
"""Generate the item icon for Prismium Rift Anchor (session 48), the
mod's first Rift Shard-family derivative item (see PrismiumRiftAnchorItem
/ PROGRESS.md section 5 item 12(a)(i)).

Composition: reuses the exact same hex-crystal shard silhouette rows as
gen_prismium.py's make_shard_item / gen_prismium_rift_shard.py's
make_rift_shard (the same "reuse a verified silhouette, change only what
needs to differ" approach gen_prismium_rift_shard.py itself already
used against the base shard) so this immediately reads as "part of the
Rift Shard family" at a glance.

What's different from the Rift Shard: instead of a near-black "void tear"
ring (dark core, bright violet ring - reads as "a hole torn into
another place"), this uses the *inverted* emphasis - a bright warm-gold
core ringed by a darker amber/brown edge, meant to read as "a lit
beacon/waypoint marker" rather than a rift. Warm gold vs. the Rift
Shard's cool violet/magenta also makes the two instantly distinguishable
in an inventory grid, not just a palette-swapped duplicate.

Self-review: generates a 4x/8x/16x upscaled checkerboard preview sheet to
build/preview_prismium_rift_anchor.png for Read-based visual inspection,
per the mod's texture workflow rules, and prints the set of distinct
alpha values present (must be only {0, 255} - no partial-transparency
bleeding).

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_rift_anchor.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette --------------------------------------------------------------
# Crystal frame: identical teal family used by every other Prismium item
# (gen_prismium.py / gen_prismium_core.py / gen_prismium_rift_shard.py)
# for cross-item consistency.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"

# Anchor beacon: warm gold core ringed by a darker amber edge - the
# inverse brightness arrangement of the Rift Shard's void tear (dark
# core / bright ring), and a warm hue instead of that item's cool
# violet/magenta, so the two read as different family members rather
# than palette-identical duplicates.
BEACON_CORE = "#FFD97A"
BEACON_RING = "#B36A00"
BEACON_SPARK = "#FFF6D9"

# Same hex-crystal shard row spans as gen_prismium.py's make_shard_item /
# gen_prismium_rift_shard.py's make_rift_shard, reused verbatim for
# silhouette-family consistency.
ROWS = {
    2: (7, 8),
    3: (6, 9),
    4: (6, 9),
    5: (5, 10),
    6: (5, 10),
    7: (4, 11),
    8: (5, 10),
    9: (5, 10),
    10: (6, 9),
    11: (6, 9),
    12: (7, 8),
    13: (7, 8),
}

# Same footprint as gen_prismium_rift_shard.py's RIFT_CORE_PTS/
# RIFT_RING_PTS (kept well inside the silhouette, never touching the
# outline), but with the bright/dark roles swapped for the beacon look.
BEACON_CORE_PTS = {(7, 7), (8, 7), (7, 8), (8, 8)}
BEACON_RING_PTS = {(6, 7), (9, 7), (6, 8), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)}
# A couple of bright spark pixels at the ring's outer edge, mirroring the
# Rift Shard's accent-dark glow-gradient pixels but bright instead of
# dark, to read as light escaping the beacon rather than a void.
BEACON_SPARK_PTS = {(6, 6), (9, 9)}


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
SHADOW = hexrgb(PRISMIUM_SHADOW)
BASE = hexrgb(PRISMIUM_BASE)
MID = hexrgb(PRISMIUM_MID)
HILITE = hexrgb(PRISMIUM_HILITE)
CORE = hexrgb(BEACON_CORE)
RING = hexrgb(BEACON_RING)
SPARK = hexrgb(BEACON_SPARK)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_rift_anchor():
    img = new_img()
    px = img.load()

    all_pts = set()
    shadow_pts = set()
    mid_pts = set()
    hi_pts = set()

    for y, (x0, x1) in ROWS.items():
        for x in range(x0, x1 + 1):
            all_pts.add((x, y))
            width = x1 - x0
            rel = (x - x0) / max(width, 1)
            if rel < 0.35:
                shadow_pts.add((x, y))
            elif rel < 0.75:
                mid_pts.add((x, y))
            else:
                hi_pts.add((x, y))

    # 1px outline around the shard silhouette (same technique as every
    # other item in this mod).
    outline_pts = set()
    for (x, y) in all_pts:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_pts and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))

    for (x, y) in outline_pts:
        px[x, y] = (*OUTLINE, 255)
    for (x, y) in shadow_pts:
        px[x, y] = (*SHADOW, 255)
    for (x, y) in mid_pts:
        px[x, y] = (*BASE, 255)
    for (x, y) in hi_pts:
        px[x, y] = (*HILITE, 255)

    # Punch the beacon into the crystal fill last, so it always wins over
    # whatever base/mid/hilite color would otherwise sit there.
    for (x, y) in BEACON_RING_PTS:
        if (x, y) in all_pts:
            px[x, y] = (*RING, 255)
    for (x, y) in BEACON_CORE_PTS:
        if (x, y) in all_pts:
            px[x, y] = (*CORE, 255)
    for (x, y) in BEACON_SPARK_PTS:
        if (x, y) in all_pts:
            px[x, y] = (*SPARK, 255)

    return img


def make_preview(img, scales=(4, 8, 16)):
    tile = 2
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * SIZE for s in scales) + 8 * (len(scales) - 1)
    total_h = max(s * SIZE for s in scales)
    preview = Image.new("RGBA", (total_w, total_h), (30, 30, 30, 255))

    x_off = 0
    for s in scales:
        board = Image.new("RGBA", (SIZE * s, SIZE * s))
        bpx = board.load()
        for y in range(SIZE * s):
            for x in range(SIZE * s):
                cx, cy = x // tile, y // tile
                bpx[x, y] = checker_light if (cx + cy) % 2 == 0 else checker_dark
        scaled = img.resize((SIZE * s, SIZE * s), Image.NEAREST)
        board.alpha_composite(scaled)
        preview.alpha_composite(board, (x_off, 0))
        x_off += SIZE * s + 8

    return preview


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_rift_anchor()
    out_path = out_dir / "prismium_rift_anchor.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_rift_anchor.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
