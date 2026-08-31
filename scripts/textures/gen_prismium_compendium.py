#!/usr/bin/env python3
"""Item icon for Prismium Compendium (scheduled session, GitHub issue #7
follow-up): the mod's first in-game guide/manual item. Issue #7 ("MODに
ついて、ゲーム内で知ることができない") was already partially addressed by
per-block usage tooltips (session 38, EnergyStorageBlockItem), whose own
class doc explicitly flagged "a full in-game guide book/manual... is a
much bigger feature than fits in one session" as the still-missing piece.
This item is that follow-up: a WrittenBookItem (vanilla class used
directly, not subclassed - see PrismiumCompendiumFactory's doc for why)
pre-filled with several pages explaining the mod's major systems.

Visual language: deliberately drawn as a *recognizable book* first
(same fanned-pages-plus-angled-cover silhouette vanilla's own book/
written_book icons use, so it reads instantly as "a book" at hoverbar
size) with this mod's own identity layered on top rather than replacing
it - a dark teal-black cover (PRISMIUM_OUTLINE/PRISMIUM_SHADOW, not
vanilla's red/brown) and a small glowing Prismium gem (PRISMIUM_BASE/
PRISMIUM_HILITE, the same accent every other Prismium item in this mod
uses) embossed on the front cover, with the page block kept close to
vanilla's own warm off-white so the "pages" part of the silhouette isn't
lost. Not based on any copied vanilla asset file (none was available in
this sandbox to sample) - built from scratch at 16x16 in Minecraft's
flat-shaded pixel-art style, deliberately reusing this mod's own
established palette rather than inventing a new one so it sits
comfortably next to every other Prismium item in the creative tab.

Run from repo root: python3 scripts/textures/gen_prismium_compendium.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same palette every other Prismium item/block script uses (see
# gen_prismium.py's palette comment for provenance).
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_HILITE = "#CAFDF9"

PAGE_LIGHT = "#F1E6C8"
PAGE_MID = "#E0D2AC"
PAGE_SHADOW = "#B8A876"
COVER_DARK = "#0B1A1B"
COVER_MID = "#123334"
SPINE_HILITE = "#1E4A4C"


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in range(0, 6, 2))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_compendium_item():
    img = new_img()
    px = img.load()

    def set(x, y, hexcolor, alpha=255):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            r, g, b = hexrgb(hexcolor)
            px[x, y] = (r, g, b, alpha)

    page_light = PAGE_LIGHT
    page_mid = PAGE_MID
    page_shadow = PAGE_SHADOW

    # Fanned page block (bottom-left, mirrors vanilla book's angled stack
    # of pages) - rows 4..13, a slanted parallelogram.
    for y in range(3, 13):
        x_start = 2 + max(0, (y - 3) // 3)
        x_end = x_start + 8
        for x in range(x_start, x_end):
            shade = page_light if (x + y) % 3 else page_mid
            set(x, y, shade)
        # bottom edge shadow line of the page block
    for x in range(2, 11):
        set(x, 12, page_shadow)
    for x in range(3, 12):
        set(x, 13, page_shadow)

    # Cover: angled band across the top-right of the page block, like a
    # book lying open/tilted with its front cover raised.
    cover_pts = []
    for y in range(2, 12):
        x_start = 5 + max(0, (y - 2) // 3)
        width = 8
        for x in range(x_start, x_start + width):
            cover_pts.append((x, y))

    for (x, y) in cover_pts:
        set(x, y, COVER_MID)
    # Darker outline along the cover's top/right edge for silhouette.
    for y in range(2, 12):
        x_start = 5 + max(0, (y - 2) // 3)
        set(x_start, y, COVER_DARK)
        set(x_start + 7, y, COVER_DARK)
    for x in range(5, 13):
        set(x, 2, COVER_DARK)
    # Spine highlight line, one pixel in from the dark left edge.
    for y in range(3, 11):
        x_start = 5 + max(0, (y - 2) // 3)
        set(x_start + 1, y, SPINE_HILITE)

    # Prismium gem embossed on the cover's center, same diamond shape
    # gen_prismium.py's shard/ore scripts use for their accent facets.
    gem_cx, gem_cy = 9, 7
    gem_pts = [
        (gem_cx, gem_cy - 2),
        (gem_cx - 1, gem_cy - 1), (gem_cx, gem_cy - 1), (gem_cx + 1, gem_cy - 1),
        (gem_cx - 1, gem_cy), (gem_cx, gem_cy), (gem_cx + 1, gem_cy),
        (gem_cx, gem_cy + 1),
    ]
    for (x, y) in gem_pts:
        set(x, y, PRISMIUM_BASE)
    set(gem_cx, gem_cy - 2, PRISMIUM_OUTLINE)
    set(gem_cx - 1, gem_cy - 1, PRISMIUM_OUTLINE)
    set(gem_cx, gem_cy + 1, PRISMIUM_SHADOW)
    set(gem_cx + 1, gem_cy, PRISMIUM_SHADOW)
    # Small hilite fleck so the gem reads as glowing, not flat.
    set(gem_cx, gem_cy - 1, PRISMIUM_HILITE)

    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_compendium_item(), "item/prismium_compendium.png")
