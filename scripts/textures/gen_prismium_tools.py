#!/usr/bin/env python3
"""Generate ClaudeMod's Prismium tool set textures (session 2).

Produces 16x16 pixel-art item textures, styled like vanilla Minecraft tools
(diagonal head-at-top-right / handle-at-bottom-left silhouette for pickaxe,
axe, shovel, hoe; vertical blade for the sword), using the same Prismium
crystal palette established in gen_prismium.py so the tool set reads as
part of the same material family.

All output is deterministic (fixed RNG seed). Run from repo root:
  python3 scripts/textures/gen_prismium_tools.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette (matches gen_prismium.py) ---------------------------------
PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_SHADOW = "#1E7A78"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"
PRISMIUM_ACCENT = "#C97BFF"

# Handle palette: warm dark wood, kept distinct from the crystal head.
HANDLE_OUTLINE = "#231208"
HANDLE_SHADOW = "#4A2A14"
HANDLE_BASE = "#6B3E1E"
HANDLE_HILITE = "#8F5A2E"

# Sword hilt/guard palette: muted steel-grey to frame the crystal blade.
HILT_OUTLINE = "#1B1B22"
HILT_BASE = "#4A4A57"
HILT_HILITE = "#6E6E80"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
SHADOW = hexrgb(PRISMIUM_SHADOW)
BASE = hexrgb(PRISMIUM_BASE)
MID = hexrgb(PRISMIUM_MID)
HILITE = hexrgb(PRISMIUM_HILITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)

H_OUTLINE = hexrgb(HANDLE_OUTLINE)
H_SHADOW = hexrgb(HANDLE_SHADOW)
H_BASE = hexrgb(HANDLE_BASE)
H_HILITE = hexrgb(HANDLE_HILITE)

G_OUTLINE = hexrgb(HILT_OUTLINE)
G_BASE = hexrgb(HILT_BASE)
G_HILITE = hexrgb(HILT_HILITE)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def set_px(px, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        px[x, y] = (*color, 255)


def draw_outline(px, pts):
    """Draw a 1px outline around the union of pts (only on empty neighbours)."""
    ptset = set(pts)
    for (x, y) in ptset:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, -1), (-1, 1), (1, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in ptset and 0 <= nx < SIZE and 0 <= ny < SIZE:
                if px[nx, ny][3] == 0:
                    px[nx, ny] = (*OUTLINE, 255)


def draw_handle(px, start, end):
    """Draw a 2px-thick diagonal wood handle from start (bottom-left) to
    end (top-right, exclusive - the head takes over from there)."""
    x0, y0 = start
    x1, y1 = end
    steps = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        # main handle pixel + one below-left companion pixel for 2px width
        color = H_HILITE if i % 4 == 0 else H_BASE
        set_px(px, x, y, color)
        set_px(px, x - 1, y + 1, H_SHADOW)
    # dark outline flanking the handle
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        set_px(px, x + 1, y, H_OUTLINE) if (x + 1, y) not in [] else None


def crystal_fill(px, pts, shade_fn):
    for (x, y) in pts:
        px[x, y] = (*shade_fn(x, y), 255)


def make_pickaxe():
    img = new_img()
    px = img.load()
    # Handle: bottom-left tip to mid-board.
    draw_handle(px, (1, 14), (8, 7))
    # Head: pickaxe head, angled like a shallow "V" opening to the right,
    # made of two crystal prongs meeting near (10, 5).
    head = set()
    # upper prong
    for i, (x, y) in enumerate([(9, 6), (10, 5), (11, 4), (12, 3), (13, 2), (14, 1), (14, 0)]):
        head.add((x, y))
    # lower prong
    for i, (x, y) in enumerate([(9, 6), (10, 6), (11, 5), (12, 5), (13, 4), (14, 4), (15, 3)]):
        head.add((x, y))
    # connective mass near the socket
    for (x, y) in [(8, 6), (9, 5), (10, 4)]:
        head.add((x, y))

    def shade(x, y):
        d = x + y
        if d <= 8:
            return SHADOW
        if d <= 12:
            return BASE
        if d <= 15:
            return MID
        return HILITE

    crystal_fill(px, head, shade)
    draw_outline(px, head)
    set_px(px, 13, 1, HILITE)
    set_px(px, 14, 3, ACCENT)
    return img


def make_axe():
    img = new_img()
    px = img.load()
    draw_handle(px, (1, 14), (8, 7))
    # Head: a broad crystal wedge blade on the upper-right side of the handle.
    head = set()
    rows = {
        0: (11, 15),
        1: (10, 15),
        2: (9, 14),
        3: (9, 13),
        4: (9, 12),
        5: (9, 11),
        6: (9, 10),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            head.add((x, y))

    def shade(x, y):
        rel = x - 9
        if rel <= 1:
            return SHADOW
        if rel <= 3:
            return BASE
        if rel <= 5:
            return MID
        return HILITE

    crystal_fill(px, head, shade)
    draw_outline(px, head)
    set_px(px, 14, 0, HILITE)
    set_px(px, 12, 1, ACCENT)
    return img


def make_shovel():
    img = new_img()
    px = img.load()
    draw_handle(px, (1, 14), (10, 5))
    # Head: small rounded crystal blade at the very top.
    head = set()
    rows = {
        0: (10, 13),
        1: (9, 14),
        2: (9, 13),
        3: (10, 13),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            head.add((x, y))

    def shade(x, y):
        rel = x - 9
        if rel <= 1:
            return SHADOW
        if rel <= 3:
            return BASE
        return HILITE

    crystal_fill(px, head, shade)
    draw_outline(px, head)
    set_px(px, 11, 0, HILITE)
    set_px(px, 12, 2, ACCENT)
    return img


def make_hoe():
    img = new_img()
    px = img.load()
    draw_handle(px, (1, 14), (10, 3))
    # Head: flat crystal blade perpendicular to the handle, near the top,
    # touching the handle's end so there's no visible gap.
    head = set()
    rows = {
        0: (10, 15),
        1: (9, 15),
        2: (9, 13),
        3: (9, 11),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            head.add((x, y))

    def shade(x, y):
        rel = x - 9
        if rel <= 2:
            return SHADOW
        if rel <= 4:
            return BASE
        return MID

    crystal_fill(px, head, shade)
    draw_outline(px, head)
    set_px(px, 14, 0, HILITE)
    set_px(px, 10, 1, ACCENT)
    return img


def make_sword():
    img = new_img()
    px = img.load()
    # Vertical blade down the middle-right, crystal shard shape.
    blade = set()
    rows = {
        1: (10, 10),
        2: (9, 10),
        3: (9, 10),
        4: (9, 10),
        5: (9, 10),
        6: (9, 10),
        7: (9, 10),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            blade.add((x, y))

    def shade(x, y):
        return MID if x == 10 else BASE

    crystal_fill(px, blade, shade)
    draw_outline(px, blade)
    set_px(px, 10, 1, HILITE)
    set_px(px, 9, 4, ACCENT)

    # Guard (horizontal steel bar).
    for x in range(7, 12):
        set_px(px, x, 8, G_BASE)
    set_px(px, 7, 8, G_OUTLINE)
    set_px(px, 11, 8, G_OUTLINE)
    for x in range(7, 12):
        set_px(px, x, 7, G_OUTLINE) if px[x, 7][3] == 0 else None

    # Hilt (grip) + pommel, diagonal like other tool handles but short.
    grip = [(8, 9), (9, 9), (8, 10), (9, 10), (8, 11), (9, 11)]
    for (x, y) in grip:
        set_px(px, x, y, H_BASE)
    set_px(px, 8, 9, G_HILITE)
    for (x, y) in [(7, 9), (10, 9), (7, 12), (10, 12)]:
        set_px(px, x, y, G_OUTLINE)
    set_px(px, 8, 12, H_OUTLINE)
    set_px(px, 9, 12, H_OUTLINE)

    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_pickaxe(), "item/prismium_pickaxe.png")
    save(make_axe(), "item/prismium_axe.png")
    save(make_shovel(), "item/prismium_shovel.png")
    save(make_hoe(), "item/prismium_hoe.png")
    save(make_sword(), "item/prismium_sword.png")
