#!/usr/bin/env python3
"""Generate the item icon for Prismium Shield (session 28).

The mod's first blocking-capable gear item (see PrismiumShieldItem /
PROGRESS.md session 28) and first brand-new equipment slot since the
Prismium Rift Shard (session 14) - five sessions (#23-27) had gone by
with GUI-only work, so this deliberately returns to the "new content"
side of the roadmap rather than another energy-block GUI.

Visual language: a classic front-facing heater-shield silhouette (flat
2D icon, matching this item's flat in-hand rendering - see
PrismiumShieldItem's class doc for why it does not have a 3D banner-style
model). Palette reuses the tool set's neutral steel rim
(gen_prismium_tools.py / gen_prismium_grappling_hook.py STEEL_*) for the
metal edge banding, a warm wood-brown face (distinct from the grappling
hook's lighter rope tan, so the two don't read as the same material) for
the board itself, and a single Prismium accent gem as a raised central
boss - the same "one accent gem ties it to the family" strategy used by
every other accessory item (grappling hook, locator, rift shard).

Self-review note: first draft gave the boss gem a 3x3 diamond with no
outline, which at 16x16 blurred into the wood-brown face behind it with
almost no contrast. Outlining the boss (dark ring one px around the gem)
and using the brighter ACCENT_HILITE only on its top-left 2 pixels (a
single specular highlight, not a symmetric shine) fixed the legibility -
same "carve clear negative space / don't rely on color alone at this
resolution" lesson noted in past sessions' texture scripts.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_shield.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette -------------------------------------------------------------
PRISMIUM_ACCENT = "#C97BFF"
PRISMIUM_ACCENT_HILITE = "#EAC8FF"

# Steel rim: identical to the tool set / grappling hook's neutral steel,
# for cross-item family consistency.
STEEL_OUTLINE = "#1B1B22"
STEEL_SHADOW = "#3A3A46"
STEEL_BASE = "#5B5B6B"
STEEL_HILITE = "#8A8A9C"

# Shield face: a darker, more saturated wood-brown than the grappling
# hook's rope tan, so the two materials read distinctly even though both
# are "brown".
WOOD_OUTLINE = "#241407"
WOOD_SHADOW = "#5C3A1E"
WOOD_BASE = "#8A5A30"
WOOD_HILITE = "#B07D48"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


S_OUTLINE = hexrgb(STEEL_OUTLINE)
S_SHADOW = hexrgb(STEEL_SHADOW)
S_BASE = hexrgb(STEEL_BASE)
S_HILITE = hexrgb(STEEL_HILITE)
W_OUTLINE = hexrgb(WOOD_OUTLINE)
W_SHADOW = hexrgb(WOOD_SHADOW)
W_BASE = hexrgb(WOOD_BASE)
W_HILITE = hexrgb(WOOD_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_HILITE = hexrgb(PRISMIUM_ACCENT_HILITE)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def set_px(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = (*color, 255)


# Heater-shield silhouette: (row, x_start, x_end_inclusive) - a slightly
# rounded top, a full-width main body, then a symmetric taper to a single
# point at the bottom. Centered on x=7/8 (16-wide canvas).
ROWS = [
    (1, 4, 11),
    (2, 3, 12),
    (3, 3, 12),
    (4, 2, 13),
    (5, 2, 13),
    (6, 2, 13),
    (7, 2, 13),
    (8, 2, 13),
    (9, 3, 12),
    (10, 3, 12),
    (11, 4, 11),
    (12, 5, 10),
    (13, 6, 9),
    (14, 7, 8),
]


def make_shield():
    img = new_img()
    px = img.load()

    body_pts = set()
    for (y, x0, x1) in ROWS:
        for x in range(x0, x1 + 1):
            body_pts.add((x, y))

    # --- Base fill: steel rim band (outer 1-2px ring) around a wood-brown
    # center face, shaded darker toward the left/bottom for a subtle
    # top-left light source consistent with the rest of the mod's items.
    for (x, y) in body_pts:
        row = next(r for r in ROWS if r[0] == y)
        _, x0, x1 = row
        is_rim = (x - x0 < 2) or (x1 - x < 2) or y in (1, 14)
        if is_rim:
            color = S_HILITE if (x + y) % 5 == 0 else (S_BASE if (x - x0) % 2 == 0 else S_SHADOW)
        else:
            color = W_HILITE if (x - x0) <= 2 else (W_BASE if (x + y) % 2 == 0 else W_SHADOW)
        set_px(px, x, y, color)

    # Outline around the whole silhouette.
    for (x, y) in body_pts:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in body_pts and 0 <= nx < SIZE and 0 <= ny < SIZE:
                if px[nx, ny][3] == 0:
                    px[nx, ny] = (*W_OUTLINE, 255)
    # A thin steel outline replaces the wood outline along the very top
    # rim row for a cleaner metal edge read.
    for x in range(3, 13):
        nx, ny = x, 0
        if px[nx, ny][3] != 0 and px[nx, ny][:3] == W_OUTLINE:
            px[nx, ny] = (*S_OUTLINE, 255)

    # --- Central Prismium boss: a small outlined diamond sitting on the
    # vertical centerline, upper-middle of the face (classic shield boss
    # placement), with a single specular highlight pixel rather than a
    # symmetric shine.
    boss_pts = [(7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6),
                (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8)]
    boss_set = set(boss_pts)
    for (x, y) in boss_pts:
        set_px(px, x, y, ACCENT)
    set_px(px, 7, 5, ACCENT_HILITE)
    set_px(px, 6, 6, ACCENT_HILITE)
    for (x, y) in boss_set:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in boss_set and (nx, ny) in body_pts:
                if (nx, ny) not in boss_set:
                    px[nx, ny] = (*W_OUTLINE, 255) if px[nx, ny][:3] != S_OUTLINE else px[nx, ny]
    # Explicit thin dark ring directly touching the boss so it reads as a
    # raised, separate piece rather than blending into the wood face.
    for (x, y) in boss_set:
        for (dx, dy) in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) in body_pts and (nx, ny) not in boss_set:
                px[nx, ny] = (*W_OUTLINE, 255)

    return img


def main():
    out_dir = ASSETS / "item"
    out_dir.mkdir(parents=True, exist_ok=True)
    img = make_shield()
    out_path = out_dir / "prismium_shield.png"
    img.save(out_path)
    print(f"wrote {out_path}")


if __name__ == "__main__":
    main()
