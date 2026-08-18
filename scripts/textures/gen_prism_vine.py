#!/usr/bin/env python3
"""Generate the texture for Prism Vine (session 44): the mod's third
plant exclusive to the Prism Realm dimension, joining Prism Lily
(session 40) and Prism Bramble (session 43) - see PrismVineBlock /
ModBlocks.PRISM_VINE / PROGRESS.md section 5 item 9(c), which
explicitly asked for a third plant with a different "growth direction"
than Lily/Bramble (both of which read as upward-growing: Lily a
rounded flower centered vertically, Bramble a tall three-pronged fan
concentrated in the top half).

This plant is a low, ground-hugging tangle that snakes horizontally
across the *bottom* half of the canvas instead - three asymmetric
leaf/tendril clusters (left/center/right, uneven heights, unlike
Bramble's straighter three-prong fan) that merge into a single wavy
mass near the floor, with only one small tendril reaching up past the
mid-line. Silhouette occupies rows 7-15 (bottom-weighted) versus
Lily's rows 0-10 (full-height, vertically centered) and Bramble's rows
1-9 (top-weighted) - three plants, three different bounding-box
"growth directions" at a glance, which was the explicit ask.

Shading: Lily/Bramble both shade by "erosion depth" (how many rings a
pixel sits inside the silhouette from the nearest transparent
neighbour), which works because their spans are wide enough to have a
real interior. This plant's spans are almost all 1-2px wide, so an
erosion pass leaves ~90% of the pixels at depth 1 (the single darkest
"outline" band) - tried first, and on self-review it read as a nearly
solid dark blob with a few colored flecks, not a legible vine. Fixed
by switching to directional (top-lit) banding instead: each filled
pixel is classified by whether the cells directly above/below it (in
the silhouette) are open air - a pixel open on both sides is a
free-hanging strand and gets the brightest band, open above only gets
a light band, open below only (its "underside") gets a darker band,
and only pixels enclosed both above and below get the darkest band.
This reads correctly for thin winding shapes the way erosion depth
does for wide filled shapes. An ASCII debug dump (--debug) was used to
check both the silhouette and the resulting band distribution before
ever touching PIL - see PROGRESS.md session 44 for the before/after
band counts that prompted the switch.

No separate elevated "stem" segment like Lily/Bramble have below their
canopy - a creeping vine has no stalk holding it up off the ground, so
the tangle itself already touches the bottom row.

16x16 texture for a vanilla "block/cross" model, same two-crossed-quads
canvas budget as Lily/Bloom/Spike/Bramble.

Deterministic (no randomness). Run from repo root:
python3 scripts/textures/gen_prism_vine.py [--debug]
"""
import sys
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same Prism Realm violet family as Lily/Bramble (session 39 biome
# effects: sky_color #2B1A4D / fog_color #3A2360), reused so all three
# exclusive plants read as the same dimension's flora.
VINE_OUTLINE = "#170F29"
VINE_SHADOW = "#2B1A4D"
VINE_BASE = "#3A2360"
VINE_MID = "#6A3FA0"
VINE_HILITE = "#B98CE8"

# Mod-wide magenta accent, used here as small "berry" clusters along
# the vine - a third distinct accent placement (Lily: single pistil
# center, Bramble: two frond-tip spore flecks, Vine: berries nestled
# in the tangle) so all three plants stay visually distinguishable by
# where the magenta appears, not just by silhouette.
ACCENT = "#FF7CFC"

# Hand-authored silhouette: one row = (y, [(x_start, x_end), ...]) with
# inclusive x ranges. Bottom-weighted (rows 7-15) and asymmetric -
# three tendril clusters merging into a wavy ground-hugging mass, with
# only one tendril (top-right, row 7) reaching above the mid-line.
ROWS = [
    (7,  [(11, 11)]),
    (8,  [(4, 4), (10, 12)]),
    (9,  [(3, 5), (10, 11)]),
    (10, [(2, 4), (9, 10), (13, 13)]),
    (11, [(1, 3), (6, 8), (9, 9), (12, 14)]),
    (12, [(1, 2), (4, 6), (7, 9), (11, 13)]),
    (13, [(2, 3), (5, 6), (9, 11), (13, 14)]),
    (14, [(3, 4), (7, 8), (10, 12)]),
    (15, [(4, 5), (8, 9)]),
]

# Berry fleck positions (left cluster and right cluster, none in the
# center to avoid crowding the busiest part of the tangle).
BERRY_FLECKS = [(2, 11), (13, 12)]


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def build_mask():
    mask = [[False] * SIZE for _ in range(SIZE)]
    for y, spans in ROWS:
        for x0, x1 in spans:
            for x in range(x0, x1 + 1):
                mask[y][x] = True
    return mask


def is_open(mask, x, y):
    """True if (x, y) is outside the canvas or not part of the
    silhouette - i.e. "open air" from the filled pixel's perspective."""
    if y < 0 or y >= SIZE:
        return True
    return not mask[y][x]


def band_grid(mask):
    """Directional (top-lit) banding: classify each filled pixel by
    whether it's open to "air" directly above and/or below it.
    open above AND below -> HILITE (free-hanging strand, lit from
    both sides); open above only -> MID (top surface catches light);
    open below only -> BASE (underside, gently shaded); enclosed on
    both sides -> SHADOW (true interior, darkest normal band)."""
    bands = {}
    for y in range(SIZE):
        for x in range(SIZE):
            if not mask[y][x]:
                continue
            up = is_open(mask, x, y - 1)
            down = is_open(mask, x, y + 1)
            if up and down:
                bands[(x, y)] = "HILITE"
            elif up:
                bands[(x, y)] = "MID"
            elif down:
                bands[(x, y)] = "BASE"
            else:
                bands[(x, y)] = "SHADOW"
    return bands


def ascii_dump(mask, bands=None):
    if bands is None:
        return "\n".join(
            "".join("#" if mask[y][x] else "." for x in range(SIZE))
            for y in range(SIZE)
        )
    sym = {"HILITE": "H", "MID": "M", "BASE": "B", "SHADOW": "S"}
    return "\n".join(
        "".join(sym[bands[(x, y)]] if mask[y][x] else "." for x in range(SIZE))
        for y in range(SIZE)
    )


def make_prism_vine():
    img = new_img()
    px = img.load()

    band_color = {
        "HILITE": hexrgb(VINE_HILITE),
        "MID": hexrgb(VINE_MID),
        "BASE": hexrgb(VINE_BASE),
        "SHADOW": hexrgb(VINE_SHADOW),
    }
    accent = hexrgb(ACCENT)

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    mask = build_mask()
    bands = band_grid(mask)

    for (x, y), b in bands.items():
        put(x, y, band_color[b])

    # Berry flecks (small magenta accent nestled in the tangle),
    # applied last so they win over the banded color underneath.
    for x, y in BERRY_FLECKS:
        put(x, y, accent)

    return img, mask, bands


def save_preview(img, name, scale):
    checker = Image.new("RGBA", (SIZE * scale, SIZE * scale), (0, 0, 0, 0))
    cpx = checker.load()
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            light = ((x // scale) + (y // scale)) % 2 == 0
            cpx[x, y] = (200, 200, 200, 255) if light else (150, 150, 150, 255)
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    checker.alpha_composite(big)
    out_path = REPO_ROOT / name
    checker.save(out_path)
    print(f"wrote preview {out_path}")


def main():
    debug = "--debug" in sys.argv
    img, mask, bands = make_prism_vine()

    if debug:
        print(ascii_dump(mask, bands))
        from collections import Counter
        print(Counter(bands.values()))
        return

    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "prism_vine.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    # Verify RGBA sanity: fully opaque or fully transparent only.
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            a = px[x, y][3]
            assert a in (0, 255), f"bad alpha at {x},{y}: {a}"
    print("alpha check OK (0/255 only)")

    save_preview(img, "prism_vine_preview_24x.png", 24)
    save_preview(img, "prism_vine_preview_4x.png", 4)


if __name__ == "__main__":
    main()
