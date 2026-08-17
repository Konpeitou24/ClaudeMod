#!/usr/bin/env python3
"""Generate the item icon for Prismium Rift Shard (session 14), the mod's
first item associated with the Prism Realm dimension (see
PrismiumRiftShardItem / PROGRESS.md).

Composition: reuses the exact same hand-authored hex-crystal silhouette
rows as the base Prismium Shard (gen_prismium.py's make_shard_item) so the
Rift Shard immediately reads as "part of the Prismium family, but a
special variant" rather than an unrelated new shape - the same "reuse a
verified silhouette, change only what needs to differ" approach used for
the tool set redesign (PROGRESS.md session 13, gen_prismium_tools.py).
What's different: instead of a uniform teal crystal fill, a small dark
"tear" is punched into the middle of the shard - a near-black void core
ringed by the mod's existing violet energy-accent color (PRISMIUM_ACCENT,
already used as tiny flecks on the plain shard/block) - meant to read as
"a crystal with a hole torn into another place", distinct at a glance from
the plain shard's solid fill.

Self-review: generates a 4x/8x/16x upscaled checkerboard preview sheet to
build/preview_prismium_rift_shard.png for Read-based visual inspection,
per the mod's texture workflow rules, and prints the set of distinct alpha
values present (must be only {0, 255} - no partial-transparency bleeding).

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_rift_shard.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette --------------------------------------------------------------
# Crystal frame: identical teal family used by every other Prismium item
# (gen_prismium.py / gen_prismium_core.py) for cross-item consistency.
PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_SHADOW = "#1E7A78"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"

# Rift tear: near-black void core ringed by the mod's existing violet
# energy-accent color (PRISMIUM_ACCENT / PRISMIUM_ACCENT_DARK, reused
# verbatim from gen_prismium.py / gen_prismium_grappling_hook.py).
VOID_CORE = "#0D0518"
RIFT_ACCENT = "#C97BFF"
RIFT_ACCENT_DARK = "#7A3FA6"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
SHADOW = hexrgb(PRISMIUM_SHADOW)
BASE = hexrgb(PRISMIUM_BASE)
MID = hexrgb(PRISMIUM_MID)
HILITE = hexrgb(PRISMIUM_HILITE)
VOID = hexrgb(VOID_CORE)
ACCENT = hexrgb(RIFT_ACCENT)
ACCENT_DARK = hexrgb(RIFT_ACCENT_DARK)

# Same hex-crystal shard row spans as gen_prismium.py's make_shard_item,
# reused verbatim for silhouette-family consistency.
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

# The "tear" punched into the shard's middle - a small ring of pixels
# around (7-8, 7-8), the shard's widest row. Kept well inside the
# silhouette (never touches the outline) so the void reads as a hole IN
# the crystal, not damage to its edge.
RIFT_CORE_PTS = {(7, 7), (8, 7), (7, 8), (8, 8)}
RIFT_RING_PTS = {(6, 7), (9, 7), (6, 8), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)}


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_rift_shard():
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
    # other item in this mod: outline occupies the ring of empty pixels
    # directly adjacent to a solid fill, never overlapping it).
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

    # Punch the rift tear into the crystal fill last, so it always wins
    # over whatever base/mid/hilite color would otherwise sit there.
    for (x, y) in RIFT_RING_PTS:
        if (x, y) in all_pts:
            px[x, y] = (*ACCENT, 255)
    for (x, y) in RIFT_CORE_PTS:
        if (x, y) in all_pts:
            px[x, y] = (*VOID, 255)
    # A couple of accent-dark pixels at the ring's outer edge for a subtle
    # glow gradient rather than a flat ring of one color.
    for (x, y) in [(6, 7), (9, 8)]:
        if (x, y) in all_pts:
            px[x, y] = (*ACCENT_DARK, 255)

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

    img = make_rift_shard()
    out_path = out_dir / "prismium_rift_shard.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_rift_shard.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
