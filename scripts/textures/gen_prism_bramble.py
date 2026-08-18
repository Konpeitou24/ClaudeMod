#!/usr/bin/env python3
"""Generate the texture for Prism Bramble (session 43): the mod's second
plant exclusive to the Prism Realm dimension, joining Prism Lily
(session 40) - see PrismBrambleBlock / ModBlocks.PRISM_BRAMBLE / PROGRESS.md
section 5 item 9(c) ("Prism Realm-only plants, ideally with a
silhouette different from Lily's").

Prism Lily reads as a symmetric 3-lobe flower (rounded petal cup).
This plant is instead an asymmetric fan of three separate frond
clusters (left/center/right) fanning out from a shared base just above
the stem, with the center frond noticeably taller than the two side
fronds - a "bounding shape" contrast with Lily's rounded symmetry, so
the two Prism Realm-exclusive plants stay easy to tell apart even at
hotbar scale (the same lesson session 41's tool-silhouette rework
drew: overall silhouette contrast matters more than internal detail at
16x16).

Technique: same two-part approach Prism Lily settled on after its
first, discarded parametric-sweep attempt read as a noisy blob -
(1) hand-author the silhouette as explicit per-row pixel spans (three
clusters instead of Lily's three lobes), not a smooth curve or
algorithmic line-draw; (2) shade with the same rim/erosion band pass
so each frond cluster gets its own highlighted spine rather than one
shared radial gradient. An ASCII debug dump (--debug) was used while
hand-placing the row spans, checked directly in the terminal, before
ever touching PIL - this caught two earlier span layouts that
Bresenham-line-based attempts produced (overlapping fronds merging
into a solid blob) well before spending a Read-tool review cycle on
them; see PROGRESS.md session 43 for the iteration history.

16x16 texture for a vanilla "block/cross" model, same two-crossed-quads
canvas budget as Lily/Bloom/Spike.

Deterministic (no randomness). Run from repo root:
python3 scripts/textures/gen_prism_bramble.py [--debug]
"""
import sys
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Same Prism Realm violet family as Prism Lily (session 39 biome
# effects: sky_color #2B1A4D / fog_color #3A2360), reused so both
# exclusive plants read as the same dimension's flora.
FROND_OUTLINE = "#170F29"
FROND_SHADOW = "#2B1A4D"
FROND_BASE = "#3A2360"
FROND_MID = "#6A3FA0"
FROND_HILITE = "#B98CE8"

# Mod-wide magenta accent, used far more sparingly than Lily's flower
# center (a bramble has no pistil) - just two "spore" flecks on the side
# frond tips, so the two plants don't share the same accent motif.
ACCENT = "#FF7CFC"

STEM_DARK = "#241B33"
STEM_MID = "#3B2C52"

# Hand-authored silhouette: one row = (y, [(x_start, x_end), ...]) with
# inclusive x ranges. Three frond clusters (left, center, right)
# fanning out from a shared base at y=8-9, center frond tapering to a
# single-pixel tip at y=1 (tall spine), side fronds ending lower
# (y=3) and wider (irregular per-row width) for a thornier, more angular read.
ROWS = [
    (1, [(8, 9)]),
    (2, [(8, 8)]),
    (3, [(1, 2), (7, 8), (11, 12)]),
    (4, [(1, 3), (7, 9), (11, 12)]),
    (5, [(2, 3), (7, 8), (11, 12)]),
    (6, [(2, 4), (6, 8), (10, 11)]),
    (7, [(3, 5), (7, 8), (10, 12)]),
    (8, [(4, 6), (7, 8), (9, 11)]),
    (9, [(6, 9)]),
]

# Spore fleck positions (left frond tip, right frond tip).
SPORE_FLECKS = [(1, 3), (12, 3)]

# Hand-forced hilite pixels: the frond spans are only 1-2px wide almost
# everywhere, so the rim/erosion band pass alone rarely reaches band 3
# ("mid") or band 4 ("hilite") - most of the plant would stay
# outline/shadow-dark. A few explicit hilite pixels at the frond tips
# and along the widest part of each cluster add sparkle/contrast
# without changing the silhouette, echoing how Lily's pistil accent
# was also hand-placed rather than depth-derived.
HILITE_FLECKS = [(8, 1), (2, 4), (11, 4)]


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
    """For each filled pixel, how many erosion passes until it would be
    removed (1 = touches a transparent/edge pixel, higher = deeper
    inside the silhouette). Same technique as Prism Lily."""
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


def make_prism_bramble():
    img = new_img()
    px = img.load()

    outline = hexrgb(FROND_OUTLINE)
    shadow = hexrgb(FROND_SHADOW)
    base = hexrgb(FROND_BASE)
    mid = hexrgb(FROND_MID)
    hilite = hexrgb(FROND_HILITE)
    accent = hexrgb(ACCENT)
    stem_dark = hexrgb(STEM_DARK)
    stem_mid = hexrgb(STEM_MID)

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
    # as Lily/Bloom.
    for y in range(10, 16):
        put(7, y, stem_mid if y % 2 == 0 else stem_dark)
        put(8, y, stem_dark if y % 2 == 0 else stem_mid)

    # Spore flecks on the two side frond tips only (not center) - a
    # restrained accent so it doesn't compete with Lily's flower
    # center as "the mod's magenta highlight motif".
    for x, y in SPORE_FLECKS:
        put(x, y, accent)

    # Hand-forced hilite sparkle (see HILITE_FLECKS comment above).
    # Only applied where the mask is actually filled, so this can
    # never punch a hole outside the authored silhouette.
    for x, y in HILITE_FLECKS:
        if mask[y][x]:
            put(x, y, hilite)

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
    img, mask = make_prism_bramble()

    if debug:
        print(ascii_dump(mask))
        return

    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "prism_bramble.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    # Verify RGBA sanity: fully opaque or fully transparent only.
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            a = px[x, y][3]
            assert a in (0, 255), f"bad alpha at {x},{y}: {a}"
    print("alpha check OK (0/255 only)")

    save_preview(img, "prism_bramble_preview_24x.png", 24)
    save_preview(img, "prism_bramble_preview_4x.png", 4)


if __name__ == "__main__":
    main()
