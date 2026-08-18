#!/usr/bin/env python3
"""Generate the item icon for Prismium Featherstone (session 31, reworked
session 34), the mod's first passive/always-on accessory - unlike every
other Prismium item so far (tools/armor equipped in their vanilla slot, or
"hold and activate" items like the Grappling Hook/Locator/Rift
Shard/Guardian Charm), this one works simply by sitting anywhere in the
player's inventory (see PrismiumFeatherstoneHandler's LivingFallEvent
listener - no equip slot, no right-click action, nothing to consciously
use).

Visual language: a smooth pale stone (pebble) with a feather laid
diagonally across it and a small teal Prismium gem (reusing
PRISMIUM_BASE/PRISMIUM_HILITE verbatim from gen_prismium.py, keeping the
mod's core crystal accent identifiable) glowing where the feather tip
touches the stone - "a stone light enough to float", echoing the fall
-damage-softening gameplay effect. Kept visually distinct from the
Guardian Charm's gold pendant silhouette (a wearable necklace) since this
item is explicitly *not* worn/held for its effect to apply.

Session 34 rework: the session 31/32 self-review note (below) flagged
that the feather still read as "a crystal shard" rather than a feather at
a glance - the PROGRESS.md session 31 handoff (section 4-46) named this
explicitly as a candidate for revisiting. Root cause on closer look: the
old design put its darker "rachis" column on the *outer* edge of the
band, which is exactly what the mod's shard-family items
(scripts/gen_prismium.py's make_shard_item, Prismium Shard/Core) do for
their faceted highlight - so the silhouette language was accidentally
reused from the wrong item family. This rework moves the quill line to
the *center* of the band (a real feather's rachis runs down the middle,
not the edge) and cuts small notches into *both* outer edges at
intervals to break the smooth shard-like taper into a segmented,
comb-like barb silhouette - the two changes together should be enough to
read as "feather" instead of "narrow crystal" even at small sizes.
Un-verified in-game like everything else in this sandbox (see
PROGRESS.md) - judged only by the 4x/8x/16x preview sheet.

Self-review note (session 31, still relevant): an early draft placed the
gem fully inside the feather's silhouette rather than the stone's, which
read as "a feather with a chunk out of it" rather than "a gem embedded in
the stone" once previewed at 4x - moved the gem down one row so it sits
unambiguously on stone pixels only, with the feather tip merely touching
its edge. The session 34 rework kept the stone/gem geometry unchanged and
only replaced the feather itself, so this note still applies.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_featherstone.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette ------------------------------------------------------------
OUTLINE = "#2B3336"

STONE_SHADOW = "#8A9296"
STONE_BASE = "#B7C0C4"
STONE_HILITE = "#E6ECEE"

FEATHER_SHADOW = "#AEDFE0"
FEATHER_BASE = "#E8FAFA"
FEATHER_HILITE = "#FFFFFF"

# Prismium crystal accent, reused verbatim from gen_prismium.py so this
# item still reads as part of the Prismium family at a glance.
GEM_RING = "#1E7A78"
GEM_CORE = "#3FBDB8"
GEM_GLINT = "#B9FFF3"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


C_OUTLINE = hexrgb(OUTLINE)
S_SHADOW = hexrgb(STONE_SHADOW)
S_BASE = hexrgb(STONE_BASE)
S_HILITE = hexrgb(STONE_HILITE)
F_SHADOW = hexrgb(FEATHER_SHADOW)
F_BASE = hexrgb(FEATHER_BASE)
F_HILITE = hexrgb(FEATHER_HILITE)
G_RING = hexrgb(GEM_RING)
G_CORE = hexrgb(GEM_CORE)
G_GLINT = hexrgb(GEM_GLINT)

# Pebble silhouette: a squat oval sitting in the lower half of the icon.
STONE_ROWS = {
    9: (6, 9),
    10: (5, 10),
    11: (4, 11),
    12: (4, 11),
    13: (5, 10),
    14: (6, 9),
}

# Feather silhouette (session 34 rework): a diagonal plume from the
# upper-right tip down to where it touches the stone, defined per-row as
# (quill_x, half_width) so the rachis (quill) is always the *center*
# column of the row rather than an edge column - see the module
# docstring for why that matters. half_width=0 means a single-pixel tip.
FEATHER_SPEC = {
    0: (12, 0),
    1: (12, 1),
    2: (11, 1),
    3: (11, 2),
    4: (10, 2),
    5: (10, 3),
    6: (9, 2),
    7: (9, 3),
    8: (8, 2),
}
FEATHER_ROWS = {y: (q - hw, q + hw) for y, (q, hw) in FEATHER_SPEC.items()}
QUILL_COL = {y: q for y, (q, hw) in FEATHER_SPEC.items()}

# Barb notches: the leading (left/lower-x) edge is left smooth - a real
# feather's leading edge is fairly clean - while the FEATHER_SPEC
# half-widths above already zigzag the trailing (right/higher-x) edge
# row-to-row (13,12,13,12,13,11,12,10 across rows 1-8) for a serrated,
# comb-like silhouette instead of a single smooth taper. This dict adds a
# few extra single-pixel nicks along that same trailing edge to reinforce
# the barb read at a glance.
FEATHER_NOTCHES = {
    (13, 1), (13, 3), (13, 5),
}

# Gem: small diamond embedded in the stone, just below where the
# feather's tip touches it (see self-review note in the docstring).
GEM_CORE_PTS = {(7, 11), (8, 11)}
GEM_RING_PTS = {(7, 10), (8, 10), (6, 11), (9, 11), (7, 12), (8, 12)}
GEM_GLINT_PT = (7, 10)
# GEM_CORE_PTS overlaps GEM_RING_PTS at (7,11)/(8,11) on purpose - core
# drawn after ring below, so it simply wins there.
GEM_RING_PTS -= GEM_CORE_PTS


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_icon():
    img = new_img()
    px = img.load()

    stone_pts = set()
    for y, (x0, x1) in STONE_ROWS.items():
        for x in range(x0, x1 + 1):
            stone_pts.add((x, y))

    feather_pts = set()
    for y, (x0, x1) in FEATHER_ROWS.items():
        for x in range(x0, x1 + 1):
            feather_pts.add((x, y))

    feather_pts -= FEATHER_NOTCHES

    all_solid = stone_pts | feather_pts

    # 1px outline around the combined silhouette.
    outline_pts = set()
    for (x, y) in all_solid:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in all_solid and 0 <= nx < SIZE and 0 <= ny < SIZE:
                outline_pts.add((nx, ny))
    for (x, y) in outline_pts:
        px[x, y] = (*C_OUTLINE, 255)

    # Stone fill: left-shadow / right-highlight gradient.
    for y, (x0, x1) in STONE_ROWS.items():
        width = x1 - x0
        for x in range(x0, x1 + 1):
            rel = (x - x0) / max(width, 1)
            if rel < 0.3:
                color = S_SHADOW
            elif rel < 0.75:
                color = S_BASE
            else:
                color = S_HILITE
            px[x, y] = (*color, 255)

    # Feather fill: the quill (center column) is drawn in the shadow
    # tone as a distinct vein running down the middle of the plume; the
    # barbs on either side use the base tone, with the outermost pixel
    # of each side in the highlight tone so each barb segment still
    # catches a bit of light at its tip (echoes the old edge-highlight
    # without making the whole edge read as one smooth faceted line).
    for y, (x0, x1) in FEATHER_ROWS.items():
        quill = QUILL_COL[y]
        for x in range(x0, x1 + 1):
            if (x, y) in FEATHER_NOTCHES:
                continue
            if x == quill:
                color = F_SHADOW
            elif x in (x0, x1):
                color = F_HILITE
            else:
                color = F_BASE
            px[x, y] = (*color, 255)

    # Gem punched into the stone last.
    for (x, y) in GEM_RING_PTS:
        if (x, y) in stone_pts:
            px[x, y] = (*G_RING, 255)
    for (x, y) in GEM_CORE_PTS:
        if (x, y) in stone_pts:
            px[x, y] = (*G_CORE, 255)
    if GEM_GLINT_PT in stone_pts:
        px[GEM_GLINT_PT] = (*G_GLINT, 255)

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

    img = make_icon()
    out_path = out_dir / "prismium_featherstone.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_featherstone.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
