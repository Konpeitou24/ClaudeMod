#!/usr/bin/env python3
"""Generate the item icon for Prismium Colossus Core (scheduled session,
2026-10-17), the guaranteed drop from the mod's first boss-tier mob (see
PrismiumColossusEntity / the mob's loot table).

Composition: reuses the same hand-authored hex-crystal silhouette rows as
the base Prismium Shard (gen_prismium.py's make_shard_item / already
reused once by gen_prismium_rift_shard.py) so this reads as "part of the
Prismium family, but a rarer variant" at a glance, consistent with the
established "reuse a verified silhouette" approach. Unlike the plain Shard
(solid teal) or the Rift Shard (teal with a small void tear), this one is
a solid violet crystal (the mod's PRISMIUM_ACCENT family, matching
gen_prismium_colossus.py's boss texture) with a bright white-hot core,
reading as "a crystallized fragment of the boss's own power" rather than a
simple ore byproduct.

Self-review: writes a 4x/8x/16x upscaled checkerboard preview sheet to
build/preview_prismium_colossus_core.png, and prints the set of distinct
alpha values present (must be only {0, 255}).

Deterministic (no RNG). Run from repo root:
    python3 scripts/textures/gen_prismium_colossus_core.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Violet "corrupted crystal" palette, reused verbatim from
# gen_prismium_colossus.py for family consistency between the boss and
# its drop.
ACCENT_OUTLINE = "#2E0030"
ACCENT_SHADOW = "#720070"
ACCENT_BASE = "#C34BF2"
ACCENT_MID = "#E79BFF"
ACCENT_HILITE = "#FCE6FF"
CORE_WHITE = "#FFFFFF"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(ACCENT_OUTLINE)
SHADOW = hexrgb(ACCENT_SHADOW)
BASE = hexrgb(ACCENT_BASE)
MID = hexrgb(ACCENT_MID)
HILITE = hexrgb(ACCENT_HILITE)
CORE = hexrgb(CORE_WHITE)

# Same hex-crystal shard row spans as gen_prismium.py's make_shard_item /
# gen_prismium_rift_shard.py, reused verbatim for silhouette-family
# consistency.
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

# A bright white-hot core punched into the shard's widest row, ringed by
# the mid-tone accent - the inverse of the Rift Shard's dark void core,
# reading as "radiating power" rather than "a hole torn into the shard".
CORE_PTS = {(7, 7), (8, 7), (7, 8), (8, 8)}
RING_PTS = {(6, 7), (9, 7), (6, 8), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_colossus_core():
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
        px[x, y] = (*MID, 255)

    # Punch the bright core in last so it always wins.
    for (x, y) in RING_PTS:
        if (x, y) in all_pts:
            px[x, y] = (*HILITE, 255)
    for (x, y) in CORE_PTS:
        if (x, y) in all_pts:
            px[x, y] = (*CORE, 255)

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

    img = make_colossus_core()
    out_path = out_dir / "prismium_colossus_core.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_colossus_core.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
