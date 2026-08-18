#!/usr/bin/env python3
"""Generate ClaudeMod's Prismium tool set textures (session 2, redesigned
session 13 and again session 41 in response to GitHub issue #2: "each
tool's silhouette doesn't match what it is, and they all look too similar
to tell apart at a glance").

Session 13 gave each head a different internal shape (fork / wedge / blob
/ crossbar) but kept every head as a similarly-sized, similarly-coloured
teal triangular mass on the same diagonal handle. Comparing a rendered
preview at hotbar scale (session 41) confirmed the user's complaint still
held: pickaxe and axe read as near-identical "teal blobs on a stick", and
the shovel's small rounded head looked like a loose gem rather than a
spade blade. This revision keeps the crystal palette (so the set still
reads as one material family) but pushes the *overall bounding shapes*
further apart, since that's what actually differentiates icons at small
size - fine internal detail (a notch, a crossbar) does not survive
shrinking, but gross silhouette (tall vs wide, thin vs blocky, forked vs
solid) does:
  - Pickaxe: two separate, narrow diagonal prongs forming a wide, shallow
             fork - an "open" silhouette with a visible gap in the middle.
  - Axe:     a single wide, flat-topped rectangular block flush against
             the handle - a "solid, blocky" silhouette, deliberately
             short and wide instead of tall and pointed so it can't be
             mistaken for one half of the pickaxe's fork.
  - Shovel:  a slim, elongated paddle (tall and narrow) instead of the
             old rounded blob - reads as a spade blade, not a gem.
  - Hoe:     unchanged (flat crossbar near the handle tip - already
             distinct from the other three).
  - Sword:   unchanged (vertical blade + crossguard + hilt - already
             distinct thanks to the guard/hilt, which none of the other
             tools have).

Produces 16x16 pixel-art item textures. All output is deterministic (no
randomness). Run from repo root:
  python3 scripts/textures/gen_prismium_tools.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# ---- palette (matches gen_prismium.py) ---------------------------------
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_ACCENT = "#FF7CFC"

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


def draw_thin_diagonal(px, start, end, base_color, hilite_color, outline_color, hilite_every=4):
    """Draw a thin (1px core) diagonal line - used for the shovel/hoe shafts
    so they read as slimmer than the 2px pickaxe/axe handle. Returns the
    ordered list of (x, y) points so callers can recolor the tip."""
    x0, y0 = start
    x1, y1 = end
    steps = max(abs(x1 - x0), abs(y1 - y0))
    pts = []
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        pts.append((x, y))
    ptset = set(pts)
    for i, (x, y) in enumerate(pts):
        color = hilite_color if i % hilite_every == 0 else base_color
        set_px(px, x, y, color)
    for (x, y) in pts:
        for (dx, dy) in [(1, 0), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in ptset and 0 <= nx < SIZE and 0 <= ny < SIZE and px[nx, ny][3] == 0:
                px[nx, ny] = (*outline_color, 255)
    return pts


def crystal_fill(px, pts, shade_fn):
    for (x, y) in pts:
        px[x, y] = (*shade_fn(x, y), 255)


def make_pickaxe():
    img = new_img()
    px = img.load()
    # Handle: bottom-left tip up to the socket where the two prongs meet.
    draw_handle(px, (1, 14), (7, 8))
    # Head (session 41): two narrow, separate diagonal prongs spread wide
    # from a shared socket - a shallow, OPEN fork. Deliberately kept
    # narrower (2-3px per prong) and shorter/wider (peaks at y=2, not a
    # tall point) than the old design so the overall bounding shape reads
    # as "wide and forked" rather than "tall wedge" - the opposite of the
    # axe's silhouette below.
    head = set()
    right_rows = {
        2: (12, 13),
        3: (11, 13),
        4: (10, 12),
        5: (9, 11),
        6: (8, 10),
        7: (8, 9),
    }
    left_rows = {
        2: (2, 3),
        3: (2, 4),
        4: (3, 5),
        5: (4, 6),
        6: (5, 7),
        7: (6, 7),
    }
    for rows in (right_rows, left_rows):
        for y, (x0, x1) in rows.items():
            for x in range(x0, x1 + 1):
                head.add((x, y))
    # socket connecting both prongs to the handle
    for (x, y) in [(7, 7), (8, 7), (7, 8)]:
        head.add((x, y))

    def shade(x, y):
        d = abs(x - 8)
        if d <= 1:
            return SHADOW
        if d <= 3:
            return BASE
        if d <= 5:
            return MID
        return HILITE

    crystal_fill(px, head, shade)
    draw_outline(px, head)
    set_px(px, 12, 2, HILITE)
    set_px(px, 3, 2, HILITE)
    set_px(px, 7, 7, ACCENT)
    return img


def make_axe():
    img = new_img()
    px = img.load()
    draw_handle(px, (1, 14), (8, 9))
    # Head (session 41): a single wide, flat-topped rectangular block,
    # flush against the handle on its inner edge - short and blocky
    # rather than tall and pointed, so its bounding shape can't be
    # mistaken for one lobe of the pickaxe's fork above. The flat top
    # edge (row y=2) is a full-width hilite line standing in for a
    # cutting edge.
    head = set()
    rows = {
        2: (9, 13),
        3: (8, 13),
        4: (8, 13),
        5: (8, 12),
        6: (8, 11),
        7: (8, 10),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            head.add((x, y))

    def shade(x, y):
        rel = x - 8
        if rel <= 1:
            return SHADOW
        if rel <= 3:
            return BASE
        if rel <= 5:
            return MID
        return HILITE

    crystal_fill(px, head, shade)
    draw_outline(px, head)
    for x in range(9, 13):
        set_px(px, x, 2, HILITE)
    set_px(px, 11, 4, ACCENT)
    return img


def make_shovel():
    img = new_img()
    px = img.load()
    # Session 13 redesign: the shovel is now a single slim continuous blade
    # running the whole length of the tool - no separate triangular head
    # blob like the old design (which looked almost identical to the hoe).
    # Session 41 redesign: the session-13b spade blade was a short, rounded
    # blob (5 rows tall) that read as a loose gem rather than a spade at
    # hotbar scale. This version keeps the same slim shaft but stretches
    # the blade into a taller, narrower paddle (7 rows) with a pointed tip
    # - an elongated shape that can't be mistaken for a round gem.
    draw_thin_diagonal(px, (1, 14), (9, 8), H_BASE, H_HILITE, H_OUTLINE)
    blade = set()
    # Flat-topped, narrow rectangle (not a pointed diamond) - narrower than
    # the axe's block and taller than it is wide, so it reads as a slim
    # paddle continuing the thin shaft rather than a gem stuck on a stick.
    rows = {
        1: (9, 12),
        2: (9, 12),
        3: (9, 12),
        4: (9, 12),
        5: (9, 12),
        6: (9, 11),
        7: (9, 10),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            blade.add((x, y))
    # joint pixel so the blade visibly meets the handle tip
    set_px(px, 9, 8, H_HILITE)

    def shade(x, y):
        if y <= 2:
            return HILITE
        if y <= 5:
            return MID
        return BASE

    crystal_fill(px, blade, shade)
    draw_outline(px, blade)
    set_px(px, 10, 1, HILITE)
    set_px(px, 10, 4, ACCENT)
    return img


def make_hoe():
    img = new_img()
    px = img.load()
    # Session-13 redesign: long slim handle (thinner than pickaxe/axe) with
    # a flat crossbar blade sticking out near the tip - a rectangle, not a
    # triangular wedge, so it can't be confused with the axe or old shovel.
    # Session-13b fix: shifted 1px down / 1px left versus the original
    # (which reached x=15 and y=0 with raw fill colour, no room for an
    # outline on that side - looked clipped at the canvas edge).
    draw_thin_diagonal(px, (1, 14), (10, 3), H_BASE, H_HILITE, H_OUTLINE)
    head = set()
    for y in (1, 2):
        for x in range(8, 14):
            head.add((x, y))
    # small wood joint pixel so the crossbar visibly meets the handle tip
    # instead of floating above it with a gap.
    set_px(px, 10, 2, H_HILITE)

    def shade(x, y):
        rel = 13 - x
        if rel <= 1:
            return HILITE
        if rel <= 3:
            return MID
        return BASE

    crystal_fill(px, head, shade)
    draw_outline(px, head)
    set_px(px, 8, 1, ACCENT)
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
