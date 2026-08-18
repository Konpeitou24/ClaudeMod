#!/usr/bin/env python3
"""Generate the texture for Prism Lily (session 40): the mod's third
surface decoration and its first plant exclusive to the Prism Realm
dimension - see PrismLilyBlock / ModBlocks.PRISM_LILY / PROGRESS.md
section 5 item 9.

Unlike Prismium Bloom (session 17) and Prismium Spike (session 18),
which both use the mod's original teal/cyan Prismium crystal palette
(and therefore also spawn in the overworld, sharing the
"#minecraft:is_overworld" biome tag), this plant is meant to read as
*native Prism Realm flora* rather than another crystal outcrop. Its
palette is instead pulled from the Prism Realm biome's own sky/fog
colors (deep violet, session 39's data/claudemod/worldgen/biome/
prism_realm.json "effects" block) with the mod's existing magenta
accent color as the pistil highlight, so the plant visually belongs to
the dimension it exclusively spawns in.

16x16 texture for a vanilla "block/cross" model, same two-crossed-quads
canvas budget as Bloom/Spike: most of the canvas stays transparent.

Design approach (revised after a first attempt read as a ragged/noisy
blob on self-review - kept only as a lesson here, not as dead code):
rather than sweeping parametric petal shapes (which produced jagged,
uncontrolled edges), the silhouette is now a small set of hand-authored
per-row spans (three lobes: left/center/right, like vanilla's own
flower sprites), and shading is a rim/erosion pass - each filled pixel
is banded by how many pixels it sits inside the silhouette from the
nearest transparent neighbour - rather than distance from a fixed
center point (Bloom's technique). This makes shading hug the actual
petal shapes (each lobe gets its own highlighted spine) instead of one
shared radial gradient, and keeps edges clean since the silhouette
itself is authored explicitly instead of computed.

Deterministic (no randomness). Run from repo root:
python3 scripts/textures/gen_prism_lily.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Prism Realm sky/fog-derived violet family (session 39 biome effects:
# sky_color 2824781 = #2B1A4D, fog_color 3810144 = #3A2360). Darkest to
# lightest, same 5-band flat-shading convention as the rest of the mod.
PETAL_OUTLINE = "#170F29"
PETAL_SHADOW = "#2B1A4D"
PETAL_BASE = "#3A2360"
PETAL_MID = "#6A3FA0"
PETAL_HILITE = "#B98CE8"

# Existing mod-wide magenta accent (same hex used since session 1's
# gen_prismium.py, and again by Bloom's accent flecks) for the pistil.
ACCENT = "#FF7CFC"
ACCENT_DARK = "#720070"

# Stem: reuses Bloom's stem tones outright (session 17) so all three
# plants' stems still read as "the same kind of stalk" even though
# their heads use different palettes.
STEM_DARK = "#241B33"
STEM_MID = "#3B2C52"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


# Hand-authored silhouette: one row = (y, [(x_start, x_end), ...]) with
# inclusive x ranges. Three lobes (center petal tip pointing straight
# up, two side petals fanning out at ~45 degrees) merging into a small
# flower "cup" just above the stem - deliberately simple, blocky spans
# rather than a smooth curve, matching this mod's existing flat/
# faceted pixel-art style (see armor bevel/band technique, session 39).
ROWS = [
    (0, [(7, 8)]),
    (1, [(7, 8)]),
    (2, [(6, 9)]),
    (3, [(3, 4), (6, 9), (11, 12)]),
    (4, [(3, 5), (6, 9), (10, 12)]),
    (5, [(2, 5), (6, 9), (10, 13)]),
    (6, [(2, 5), (10, 13)]),
    (7, [(3, 6), (9, 12)]),
    (8, [(4, 6), (9, 11)]),
    (9, [(5, 10)]),
    (10, [(6, 9)]),
]


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
    inside the silhouette)."""
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


def make_prism_lily():
    img = new_img()
    px = img.load()

    outline = hexrgb(PETAL_OUTLINE)
    shadow = hexrgb(PETAL_SHADOW)
    base = hexrgb(PETAL_BASE)
    mid = hexrgb(PETAL_MID)
    hilite = hexrgb(PETAL_HILITE)
    accent = hexrgb(ACCENT)
    accent_dark = hexrgb(ACCENT_DARK)
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

    # Stem: 2px wide, y=11..15, same alternating-tone taper as Bloom.
    for y in range(11, 16):
        put(7, y, stem_mid if y % 2 == 0 else stem_dark)
        put(8, y, stem_dark if y % 2 == 0 else stem_mid)

    # Pistil: two accent pixels where the three lobes meet, just above
    # the stem - echoes Bloom's accent flecks but concentrated into one
    # clear "flower center".
    put(7, 9, accent)
    put(8, 9, accent_dark)

    return img


def main():
    img = make_prism_lily()
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "prism_lily.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    scale = 24
    checker = Image.new("RGBA", (SIZE * scale, SIZE * scale), (0, 0, 0, 0))
    cpx = checker.load()
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            light = ((x // scale) + (y // scale)) % 2 == 0
            cpx[x, y] = (200, 200, 200, 255) if light else (150, 150, 150, 255)
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    checker.alpha_composite(big)
    preview_path = REPO_ROOT / "prism_lily_preview.png"
    checker.save(preview_path)
    print(f"wrote preview {preview_path}")

    # Small-scale (4x) preview too, since in-world this reads mostly at
    # a glance - self-review needs to check it doesn't turn to noise.
    small_scale = 4
    checker4 = Image.new("RGBA", (SIZE * small_scale, SIZE * small_scale), (0, 0, 0, 0))
    cpx4 = checker4.load()
    for y in range(SIZE * small_scale):
        for x in range(SIZE * small_scale):
            light = ((x // small_scale) + (y // small_scale)) % 2 == 0
            cpx4[x, y] = (200, 200, 200, 255) if light else (150, 150, 150, 255)
    big4 = img.resize((SIZE * small_scale, SIZE * small_scale), Image.NEAREST)
    checker4.alpha_composite(big4)
    preview4_path = REPO_ROOT / "prism_lily_preview_4x.png"
    checker4.save(preview4_path)
    print(f"wrote preview {preview4_path}")


if __name__ == "__main__":
    main()
