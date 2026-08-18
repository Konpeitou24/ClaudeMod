#!/usr/bin/env python3
"""Generate the texture for Prismium Portal (session 52's new gateway block).

Reuses the established Prismium palette (see gen_prismium_core.py) but,
unlike every solid block texture so far, this one is semi-transparent
(varying alpha) since the block is meant to read as an energy field, not
solid stone - the block's client-side render type is set to translucent
in ClientModEvents so alpha actually shows through in-game rather than
being treated as a hard cutout.

Design: diagonal magenta/teal streaks (evoking vanilla's own nether_portal
swirl, but in this mod's own colors instead of copying that texture) over
a dark violet base, with alpha lowest at the texture's edges and highest
along the streaks themselves, so the block reads as "hazy at the edges,
bright energy in the middle" even as a single static frame (no .mcmeta
animation - deliberately kept simple/static for this first version, see
PROGRESS.md).

Deterministic (fixed seed). Run from repo root:
python3 scripts/textures/gen_prismium_portal.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260819
SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PORTAL_DARK = "#180A33"
PORTAL_BASE = "#3A1F73"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_DARK = "#720070"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def make_prismium_portal():
    random.seed(SEED)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    for y in range(SIZE):
        for x in range(SIZE):
            # Base: dark violet field, alpha lower near the four edges so
            # the block doesn't read as a hard-edged opaque cube even
            # with a plain cube_all model.
            edge_dist = min(x, SIZE - 1 - x, y, SIZE - 1 - y)
            base_alpha = 150 + min(edge_dist, 3) * 25  # 150..225

            # Two families of diagonal streaks (perpendicular to each
            # other) so the pattern doesn't look like a single flat
            # gradient from any angle.
            diag_a = (x + y) % 8
            diag_b = (x - y) % 8

            if diag_a in (0, 1):
                color = hexrgb(PRISMIUM_ACCENT)
                alpha = min(255, base_alpha + 55)
            elif diag_b in (0,):
                color = hexrgb(PRISMIUM_MID)
                alpha = min(255, base_alpha + 35)
            elif diag_a in (4,):
                color = hexrgb(PRISMIUM_BASE)
                alpha = base_alpha
            else:
                color = hexrgb(PORTAL_BASE)
                alpha = base_alpha

            # Sparse bright flecks (highest-contrast accent) for a
            # "sparkling energy" feel, matching the sparse-flecks
            # restraint already established by gen_prismium_core.py.
            if random.random() < 0.05:
                color = hexrgb(PRISMIUM_HILITE)
                alpha = 255

            px[x, y] = (color[0], color[1], color[2], alpha)

    # Very dark violet vignette corners (2px) to frame the swirl and hide
    # the seam where this tiles against the Prismium Core frame blocks.
    dark = hexrgb(PORTAL_DARK)
    for x, y in [(0, 0), (1, 0), (0, 1), (SIZE - 1, 0), (SIZE - 2, 0), (SIZE - 1, 1),
                 (0, SIZE - 1), (1, SIZE - 1), (0, SIZE - 2), (SIZE - 1, SIZE - 1),
                 (SIZE - 2, SIZE - 1), (SIZE - 1, SIZE - 2)]:
        r, g, b, a = px[x, y]
        px[x, y] = (dark[0], dark[1], dark[2], max(a, 200))

    return img


def main():
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    img = make_prismium_portal()
    img.save(out_dir / "prismium_portal.png")
    print(f"Wrote {out_dir / 'prismium_portal.png'}")

    # Upscaled preview (checkerboard background so alpha is visible) for
    # self-review, written next to this script's output rather than into
    # assets/.
    preview_dir = REPO_ROOT / "build"
    preview_dir.mkdir(parents=True, exist_ok=True)
    scale = 16
    checker = Image.new("RGBA", (SIZE * scale, SIZE * scale), (255, 255, 255, 255))
    cpx = checker.load()
    for y in range(SIZE * scale):
        for x in range(SIZE * scale):
            if ((x // (scale // 2)) + (y // (scale // 2))) % 2 == 0:
                cpx[x, y] = (200, 200, 200, 255)
    upscaled = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    checker.alpha_composite(upscaled)
    checker.save(preview_dir / "preview_prismium_portal.png")
    print(f"Wrote preview: {preview_dir / 'preview_prismium_portal.png'}")


if __name__ == "__main__":
    main()
