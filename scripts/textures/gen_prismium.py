#!/usr/bin/env python3
"""Generate ClaudeMod's first texture set: the Prismium resource line.

Produces 16x16 pixel-art textures in Minecraft's style (flat limited palette,
clear silhouette, light dithered shading, no smooth gradients/AA) for:
  - block/prismium_ore.png
  - block/deepslate_prismium_ore.png
  - block/prismium_block.png
  - item/prismium_shard.png

All output is deterministic (fixed RNG seed) so re-running regenerates the
same art. Run from repo root: python3 scripts/textures/gen_prismium.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260816
SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette -----------------------------------------------------------
# Stone base (mimics vanilla stone.png tonal range)
STONE = ["#8C8C8C", "#828282", "#797979", "#8F8F8F", "#767676"]
# Deepslate base
DEEPSLATE = ["#4C4C50", "#454548", "#3E3E41", "#505054", "#393A3C"]

# Prismium crystal palette: teal/cyan primary with a violet energy fleck,
# kept distinct from vanilla diamond (blue-white) and emerald (green).
PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_SHADOW = "#1E7A78"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"
PRISMIUM_ACCENT = "#C97BFF"  # tiny violet energy fleck, ties into future energy system
PRISMIUM_ACCENT_DARK = "#7A3FA6"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def speckled_base(colors, rng):
    img = new_img()
    px = img.load()
    palette = [hexrgb(c) for c in colors]
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*rng.choice(palette), 255)
    return img


def scatter_crystals(img, rng, count, cluster=False):
    """Draw small 1-3px crystal flecks of the prismium palette onto img."""
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)

    placed = 0
    attempts = 0
    while placed < count and attempts < count * 20:
        attempts += 1
        cx = rng.randint(1, SIZE - 2)
        cy = rng.randint(1, SIZE - 2)
        shape = rng.choice(["speck", "shard"])
        if shape == "speck":
            px[cx, cy] = (*mid, 255)
            if rng.random() < 0.6:
                px[cx, cy] = (*hilite, 255)
        else:
            # tiny 3-pixel diagonal shard: shadow, base, highlight
            pts = [(cx - 1, cy + 1, shadow), (cx, cy, base), (cx + 1, cy - 1, hilite)]
            for (x, y, c) in pts:
                if 0 <= x < SIZE and 0 <= y < SIZE:
                    px[x, y] = (*c, 255)
            for (x, y) in [(cx - 2, cy + 2), (cx + 2, cy - 2)]:
                if 0 <= x < SIZE and 0 <= y < SIZE and rng.random() < 0.5:
                    px[x, y] = (*outline, 255)
        placed += 1
    return img


def make_ore(base_colors, seed_offset):
    rng = random.Random(SEED + seed_offset)
    img = speckled_base(base_colors, rng)
    scatter_crystals(img, rng, count=7)
    return img


def make_prismium_block():
    rng = random.Random(SEED + 99)
    img = new_img()
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    accent = hexrgb(PRISMIUM_ACCENT)
    accent_dark = hexrgb(PRISMIUM_ACCENT_DARK)

    # Base fill: soft diagonal gradient band (base -> mid -> hilite) to read
    # as a faceted crystal block, built from flat bands (no smooth AA).
    for y in range(SIZE):
        for x in range(SIZE):
            d = (x + y)  # 0..30
            if d < 8:
                c = base
            elif d < 14:
                c = mid
            elif d < 20:
                c = mid
            elif d < 26:
                c = base
            else:
                c = shadow
            px[x, y] = (*c, 255)

    # Facet outlines: a few diagonal cut lines for a crystalline look.
    for i in range(-2, 18, 6):
        for t in range(SIZE):
            x, y = i + t, t
            if 0 <= x < SIZE:
                px[x, y] = (*outline, 255)

    # Sparkle highlights scattered on facets.
    for _ in range(10):
        x, y = rng.randint(0, SIZE - 1), rng.randint(0, SIZE - 1)
        px[x, y] = (*hilite, 255)

    # A few violet energy flecks (ties block into the future energy system).
    for _ in range(5):
        x, y = rng.randint(1, SIZE - 2), rng.randint(1, SIZE - 2)
        px[x, y] = (*accent, 255)
        if rng.random() < 0.5 and x + 1 < SIZE:
            px[x + 1, y] = (*accent_dark, 255)

    # Border outline for a crisp block silhouette.
    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)

    return img


def make_shard_item():
    img = new_img()
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    accent = hexrgb(PRISMIUM_ACCENT)

    # Hand-authored elongated hex-crystal silhouette (tall shard shape),
    # similar composition to vanilla amethyst_shard but a distinct outline
    # and palette so it doesn't read as a re-skin.
    shard_shadow = set()
    shard_base = set()
    shard_mid = set()
    shard_hi = set()
    outline_px = set()

    # Rows are (y, x_start, x_end) spans defining a narrow hexagonal shard,
    # pointed at top and bottom, widest in the middle.
    rows = {
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

    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            # left half shadow, center mid, right sliver highlight
            width = x1 - x0
            rel = (x - x0) / max(width, 1)
            if rel < 0.35:
                shard_shadow.add((x, y))
            elif rel < 0.75:
                shard_mid.add((x, y))
            else:
                shard_hi.add((x, y))

    # outline = 1px border around the union of all shard pixels
    all_pts = shard_shadow | shard_mid | shard_hi
    for (x, y) in all_pts:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_pts and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_px.add((nx, ny))

    for (x, y) in outline_px:
        px[x, y] = (*outline, 255)
    for (x, y) in shard_shadow:
        px[x, y] = (*shadow, 255)
    for (x, y) in shard_mid:
        px[x, y] = (*base, 255)
    for (x, y) in shard_hi:
        px[x, y] = (*mid, 255)

    # A couple of bright highlight pixels + one violet energy spark.
    for (x, y) in [(9, 6), (8, 8)]:
        if (x, y) in all_pts:
            px[x, y] = (*hilite, 255)
    if (9, 9) in all_pts:
        px[9, 9] = (*accent, 255)

    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_ore(STONE, seed_offset=1), "block/prismium_ore.png")
    save(make_ore(DEEPSLATE, seed_offset=2), "block/deepslate_prismium_ore.png")
    save(make_prismium_block(), "block/prismium_block.png")
    save(make_shard_item(), "item/prismium_shard.png")
