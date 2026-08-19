#!/usr/bin/env python3
"""Generate the two textures for Prismium Snare (session 64): the mod's
first genuine gimmick/trap block - see PrismiumSnareBlock / ModBlocks.
PRISMIUM_SNARE / PROGRESS.md session 64 for the full design rationale.

Unlike every earlier Prism Realm-exclusive plant (Lily's flower cup,
Bramble's three-frond fan, Vine's hanging tangle, Spike's tall shard -
each a distinct *organic* silhouette), this block needed a silhouette
that reads as camouflage first and "trap" only in hindsight: a coiled
loop/noose ring with a few outward thorn barbs and a single hidden
magenta "trigger bud" pixel nested inside the loop. The ring shape is
deliberately unlike any existing plant in this mod (no other block uses
a closed-loop silhouette), so once a player learns to recognize it,
it stands out - but at a glance among Bramble/Lily/Vine clutter it
should still read as "just more alien flora."

Two output states, sharing the exact same ring silhouette (mask) so the
16x16 model swap on trigger doesn't shift the block's footprint:
  - prismium_snare.png            (armed):    violet "family" palette,
    identical hue language to Bramble/Lily/Vine, magenta trigger bud lit.
  - prismium_snare_triggered.png  (triggered): desaturated grey/brown
    "spent" palette, trigger bud dimmed to a dull grey - reads as
    "already sprung, safe now" without changing the recognizable ring
    shape.

Technique: same two-part approach as Bramble/Lily - (1) hand-authored
per-row pixel spans for the ring + thorns, (2) rim/erosion depth shading
bands. Deterministic (no randomness). Run from repo root:
python3 scripts/textures/gen_prismium_snare.py [--debug]
"""
import sys
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Armed: same Prism Realm violet family as Bramble/Lily/Vine so this
# blends in with them as "just more flora" at a glance.
ARMED_OUTLINE = "#170F29"
ARMED_SHADOW = "#2B1A4D"
ARMED_BASE = "#3A2360"
ARMED_MID = "#6A3FA0"
ARMED_HILITE = "#B98CE8"
ARMED_ACCENT = "#FF7CFC"

# Triggered: desaturated grey/brown "spent" palette - same value
# structure (outline/shadow/base/mid/hilite) so the shading pass code
# is reused verbatim, just re-keyed to duller hues signalling "already
# sprung, no longer dangerous."
SPENT_OUTLINE = "#171310"
SPENT_SHADOW = "#332B24"
SPENT_BASE = "#4A3F35"
SPENT_MID = "#6E6055"
SPENT_HILITE = "#948575"
SPENT_ACCENT = "#665E56"

STEM_DARK = "#241B33"
STEM_MID = "#3B2C52"
STEM_DARK_SPENT = "#241D17"
STEM_MID_SPENT = "#3B3226"

# Hand-authored ring silhouette: a closed coiled loop (the "snare")
# plus three thorn barbs jutting outward from the ring (left @ y=2,
# right @ y=5, lower-left @ y=8). Inclusive x ranges per row.
ROWS = [
    (1, [(7, 8)]),
    (2, [(2, 2), (5, 6), (9, 10)]),
    (3, [(4, 4), (11, 11)]),
    (4, [(3, 3), (12, 12)]),
    (5, [(3, 3), (12, 12), (13, 13)]),
    (6, [(3, 3), (12, 12)]),
    (7, [(4, 4), (11, 11)]),
    (8, [(2, 2), (5, 6), (9, 9)]),
    (9, [(7, 8)]),
]

# The hidden trigger bud, nested inside the ring's empty interior -
# the one pixel that differs in colour between armed/triggered beyond
# the overall palette swap (it's meaningfully brighter than its
# surroundings when armed, and flush/dull when spent).
TRIGGER_BUD = (7, 6)

# Thorn barb tips get a hilite fleck so they read as sharp points
# rather than blending into the ring's shading bands.
HILITE_FLECKS = [(2, 2), (13, 5), (2, 8), (7, 1)]

# The ring itself is only 1px wide almost everywhere, so (like Bramble
# before it) the rim/erosion depth pass alone can't reach past band 1
# (outline) - the whole loop would render as a flat near-black wire
# with no shading variety. Unlike Bramble (which only added hilite
# flecks), here the loop is the entire silhouette rather than one
# element among several fronds, so flat-outline-only reads too much
# like a plain black hoop rather than a shaded crystalline coil.
# These hand-placed base/mid flecks break up the ring into a subtle
# two-tone twist (alternating outline/base) reminiscent of braided
# wire, without touching the silhouette mask itself.
BASE_FLECKS = [(9, 2), (12, 4), (4, 7), (6, 8)]
MID_FLECKS = [(10, 2), (5, 8)]


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


def rim_depth(mask):
    """Same erosion-pass depth technique as Bramble/Lily: depth 1 =
    touches a transparent/edge pixel, higher = deeper inside."""
    depth = [[0] * SIZE for _ in range(SIZE)]
    remaining = {(x, y) for y in range(SIZE) for x in range(SIZE) if mask[y][x]}
    d = 0
    while remaining:
        d += 1
        boundary = set()
        for (x, y) in remaining:
            is_edge = False
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if not (0 <= nx < SIZE and 0 <= ny < SIZE) or (nx, ny) not in remaining:
                    is_edge = True
                    break
            if is_edge:
                boundary.add((x, y))
        for (x, y) in boundary:
            depth[y][x] = d
        remaining -= boundary
    return depth


def ascii_dump(mask):
    return "\n".join(
        "".join("#" if mask[y][x] else "." for x in range(SIZE))
        for y in range(SIZE)
    )


def make_image(outline, shadow, base, mid, hilite, accent, stem_dark, stem_mid, bud_lit):
    img = new_img()
    px = img.load()

    def put(x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            px[x, y] = (*c, 255)

    mask = build_mask()
    depth = rim_depth(mask)

    band_by_depth = {1: outline, 2: shadow, 3: base, 4: mid}
    for y in range(SIZE):
        for x in range(SIZE):
            if not mask[y][x]:
                continue
            d = depth[y][x]
            c = band_by_depth.get(d, hilite)
            put(x, y, c)

    # Stem: 2px wide, y=10..15, same alternating-tone taper convention
    # as Bramble/Lily/Vine.
    for y in range(10, 16):
        put(7, y, stem_mid if y % 2 == 0 else stem_dark)
        put(8, y, stem_dark if y % 2 == 0 else stem_mid)

    for x, y in BASE_FLECKS:
        if mask[y][x]:
            put(x, y, base)

    for x, y in MID_FLECKS:
        if mask[y][x]:
            put(x, y, mid)

    for x, y in HILITE_FLECKS:
        if mask[y][x]:
            put(x, y, hilite)

    # Trigger bud: placed in the ring's transparent interior (not on
    # the mask), so it is written unconditionally after the mask pass.
    bx, by = TRIGGER_BUD
    put(bx, by, accent if bud_lit else accent)

    return img, mask


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

    armed, mask = make_image(
        hexrgb(ARMED_OUTLINE), hexrgb(ARMED_SHADOW), hexrgb(ARMED_BASE),
        hexrgb(ARMED_MID), hexrgb(ARMED_HILITE), hexrgb(ARMED_ACCENT),
        hexrgb(STEM_DARK), hexrgb(STEM_MID), bud_lit=True,
    )
    triggered, _ = make_image(
        hexrgb(SPENT_OUTLINE), hexrgb(SPENT_SHADOW), hexrgb(SPENT_BASE),
        hexrgb(SPENT_MID), hexrgb(SPENT_HILITE), hexrgb(SPENT_ACCENT),
        hexrgb(STEM_DARK_SPENT), hexrgb(STEM_MID_SPENT), bud_lit=False,
    )

    if debug:
        print("armed/triggered share this silhouette:")
        print(ascii_dump(mask))
        return

    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    armed.save(out_dir / "prismium_snare.png")
    triggered.save(out_dir / "prismium_snare_triggered.png")
    print(f"wrote {out_dir / 'prismium_snare.png'}")
    print(f"wrote {out_dir / 'prismium_snare_triggered.png'}")

    save_preview(armed, "_preview_prismium_snare_4x.png", 4)
    save_preview(armed, "_preview_prismium_snare_8x.png", 8)
    save_preview(triggered, "_preview_prismium_snare_triggered_4x.png", 4)
    save_preview(triggered, "_preview_prismium_snare_triggered_8x.png", 8)


if __name__ == "__main__":
    main()
