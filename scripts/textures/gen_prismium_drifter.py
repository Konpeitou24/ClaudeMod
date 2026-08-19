#!/usr/bin/env python3
"""Generate the entity texture for Prismium Drifter, ClaudeMod's fourth
mob and first non-combat/environmental entity (see PrismiumDrifterEntity's
javadoc for the design writeup).

Layout: unlike every prior mob texture script in this mod (all humanoid
64x64 skins reusing ZombieModel/SkeletonModel UV layouts, which this repo
has exercised three times and is confident about), this is the mod's
first non-humanoid mob texture, wrapping vanilla's own SquidModel. This
sandbox could not confirm SquidModel's exact UV region layout or vanilla
squid.png's exact pixel dimensions against real game source (network
access from the build sandbox is restricted to github.com's HTML pages,
which don't expose raw file contents) - 64x32 is a well-established,
high-confidence assumption (it's the standard "pre-HD-rework" mob texture
canvas size referenced throughout the Minecraft texturing community, e.g.
old/vanilla mob skins like squid, cod, chicken), verified as best as
possible this session via public API documentation (Yarn mappings
javadoc for the equivalent SquidEntityModel class, which confirms a
single ModelPart root + 8 separate tentacle ModelParts - consistent with
a squid needing a compact 64x32 sheet) rather than pure guesswork, but it
remains genuinely unverified against a real client. Mitigated by going
even further than the humanoid scripts' "belt-and-braces" approach: every
mob script so far has painted each *known* UV region generously to
tolerate imprecise region boundaries, but still assumed the boundaries
were roughly right. This script instead paints the *entire* canvas with
one continuous, seamless vertical gradient (no hard region edges, no
per-limb patterning) specifically so that regardless of where SquidModel
actually slices this image into body/tentacle UV rects, every possible
slice still looks like an intentional, coherent part of the same
creature - the worst case if the real UV layout differs from assumption
is "slightly different crop of the same gradient", not "visible seams or
wrong body part colors". If a future session can verify the real layout
(e.g. once local builds become possible), this can be revisited to add
actual per-part shading detail like the other mob scripts have.

Design: a bioluminescent "drifter" - dark violet/indigo body (same family
as PRISMIUM_WRAITH_SPAWN_EGG's 0x2b1033 base, tying it visually to the
mod's existing dark "casing" palette) fading into the mod's
already-validated PRISMIUM_* teal glow toward the tentacles, with
scattered soft cyan bioluminescent dot clusters distributed evenly across
the whole canvas (not tied to any specific body part, for the same
UV-uncertainty reason above) so it reads as "glowing spots on a dark
aquatic creature" from any angle/crop.

Self-review: writes an upscaled checkerboard preview to
build/preview_prismium_drifter.png for Read-based visual inspection, and
prints the set of distinct alpha values present (must be only 0/255, no
partial-transparency bleeding), matching the mod's texture workflow
rules. Run from repo root:
    python3 scripts/textures/gen_prismium_drifter.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260819
W, H = 64, 32

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Dark violet "casing" palette, reused from the Wraith spawn egg base
# (0x2b1033) so this new mob still reads as part of the Prismium family
# at a glance, even though it is a completely different body plan.
BASE_OUTLINE = "#160819"
BASE_DARK = "#2B1033"
BASE_MID = "#4A1F52"

# Prismium crystal palette, reused verbatim from every prior mob/block
# script (gen_prismium_wraith.py / gen_prismium_sentinel.py / etc.) -
# PROGRESS.md's recurring "reuse verified palette on the Nth application"
# lesson.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


BO = hexrgb(BASE_OUTLINE)
BD = hexrgb(BASE_DARK)
BM = hexrgb(BASE_MID)
PO = hexrgb(PRISMIUM_OUTLINE)
PS = hexrgb(PRISMIUM_SHADOW)
PB = hexrgb(PRISMIUM_BASE)
PM = hexrgb(PRISMIUM_MID)
PH = hexrgb(PRISMIUM_HILITE)

# Gradient stops, sampled top (y=0, reads as "mantle/top of body" on most
# plausible UV layouts) to bottom (y=H-1, reads as "tentacle tips"):
# dark violet outline -> dark violet -> mid violet -> teal shadow -> teal
# base -> teal mid -> teal hilite. Squid tentacles hang below the body in
# every plausible UV unwrap, so light/glowing colors belong at the bottom.
GRADIENT_STOPS = [BO, BO, BD, BD, BD, BM, BM, PS, PS, PB, PB, PB, PM, PM, PH]


def lerp(c0, c1, t):
    return tuple(round(c0[i] + (c1[i] - c0[i]) * t) for i in range(4))


def gradient_color(t, rng):
    """t in [0, 1] maps into GRADIENT_STOPS, plus a little per-pixel
    dither so the gradient doesn't look like a flat CG ramp."""
    n = len(GRADIENT_STOPS)
    pos = t * (n - 1)
    i0 = min(int(pos), n - 2)
    frac = pos - i0
    color = lerp(GRADIENT_STOPS[i0], GRADIENT_STOPS[i0 + 1], frac)
    if rng.random() < 0.12:
        # subtle dither: nudge toward the neighboring stop for a bit of
        # painterly noise instead of a perfectly smooth ramp.
        color = lerp(color, GRADIENT_STOPS[min(i0 + 1, n - 1)], 0.35)
    return color


def make_drifter_texture():
    rng = random.Random(SEED)
    img = Image.new("RGBA", (W, H), BO)
    px = img.load()

    for y in range(H):
        t = y / (H - 1)
        for x in range(W):
            px[x, y] = gradient_color(t, rng)

    # Scattered bioluminescent dot clusters, distributed across the whole
    # canvas (not aligned to any assumed body-part rect) - small 1-2px
    # cyan/hilite pinpricks with a soft outline pixel, echoing the "glow
    # spot" motif without depending on knowing exactly which UV region is
    # "the mantle" vs "a tentacle".
    n_clusters = 14
    for _ in range(n_clusters):
        cx = rng.randrange(2, W - 2)
        cy = rng.randrange(1, H - 1)
        # skip clusters too close to the very top edge (outline row) to
        # avoid muddying the silhouette edge.
        if cy == 0:
            continue
        px[cx, cy] = PH
        # soft outline ring using the darker prismium shadow tone,
        # skipped probabilistically so it reads as an organic glow
        # rather than a hard-edged sticker.
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = cx + dx, cy + dy
            if 0 <= nx < W and 0 <= ny < H and rng.random() < 0.55:
                px[nx, ny] = PM if rng.random() < 0.5 else PS

    return img


def make_preview(img, scales=(1, 4, 8)):
    """Same preview compositor as gen_prismium_sentinel.py (checkerboard
    background so alpha issues are visible on Read-based self-review)."""
    tile = 4
    checker_light = (200, 200, 200, 255)
    checker_dark = (150, 150, 150, 255)

    total_w = sum(s * W for s in scales) + 8 * (len(scales) - 1)
    total_h = max(s * H for s in scales)
    preview = Image.new("RGBA", (total_w, total_h), (30, 30, 30, 255))

    x_off = 0
    for s in scales:
        board = Image.new("RGBA", (W * s, H * s))
        bpx = board.load()
        for y in range(H * s):
            for x in range(W * s):
                cx, cy = x // tile, y // tile
                bpx[x, y] = checker_light if (cx + cy) % 2 == 0 else checker_dark
        scaled = img.resize((W * s, H * s), Image.NEAREST)
        board.alpha_composite(scaled)
        preview.alpha_composite(board, (x_off, 0))
        x_off += W * s + 8

    return preview


def main():
    ASSETS.joinpath("entity").mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_drifter_texture()
    out_path = ASSETS / "entity" / "prismium_drifter.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_drifter.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
