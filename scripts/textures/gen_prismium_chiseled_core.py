#!/usr/bin/env python3
"""Generate the block texture for Chiseled Prismium Core (session 37).

Mirrors Chiseled Prismium Block (session 34, see
gen_prismium_chiseled_block.py) as a purely decorative masonry variant of
an existing resource block - no new block class, no new mechanics, just a
second look so builders have a detail texture for Prismium Core the same
way they already have one for Prismium Block. Reuses the framed-panel
"carved stone" construction (1px outline, inset shadow ring, raised
bevel) verbatim from the Block version so the two chiseled textures read
as the same masonry *technique* applied to two different materials.

The center motif is where this diverges deliberately: instead of Chiseled
Block's flat violet diamond rune, this uses the same "radiant core
cluster" cross shape (hilite ring + core-white center) that
gen_prismium_core.py uses on the plain Prismium Core texture, echoing
Core's lightLevel(10) and its "condensed, glowing" identity - the same
plain-vs-chiseled contrast established for Block (scattered flecks vs.
symmetric rune) is reused here but with Core's own signature motif
instead of Block's, so someone who knows both plain textures immediately
recognizes which chiseled variant is which family.

Deterministic (no RNG - every pixel is placed explicitly). Run from repo
root: python3 scripts/textures/gen_prismium_chiseled_core.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16
REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# ---- palette (reused verbatim from gen_prismium.py / gen_prismium_core.py)
PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_SHADOW = "#1E7A78"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#C97BFF"
PRISMIUM_ACCENT_DARK = "#7A3FA6"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


OUTLINE = hexrgb(PRISMIUM_OUTLINE)
SHADOW = hexrgb(PRISMIUM_SHADOW)
BASE = hexrgb(PRISMIUM_BASE)
MID = hexrgb(PRISMIUM_MID)
HILITE = hexrgb(PRISMIUM_HILITE)
CORE_WHITE = hexrgb(PRISMIUM_CORE_WHITE)
ACCENT = hexrgb(PRISMIUM_ACCENT)
ACCENT_DARK = hexrgb(PRISMIUM_ACCENT_DARK)


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_chiseled_core():
    img = new_img()
    px = img.load()

    # Flat mid-tone field fill, identical framing technique to Chiseled
    # Prismium Block so the two share an obvious "cut from the same
    # workshop" masonry language.
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*BASE, 255)

    # Outer masonry border.
    for x in range(SIZE):
        px[x, 0] = (*OUTLINE, 255)
        px[x, SIZE - 1] = (*OUTLINE, 255)
    for y in range(SIZE):
        px[0, y] = (*OUTLINE, 255)
        px[SIZE - 1, y] = (*OUTLINE, 255)

    # Inner recessed-panel ring, 3px in.
    for i in range(3, SIZE - 3):
        px[i, 3] = (*SHADOW, 255)
        px[i, SIZE - 4] = (*SHADOW, 255)
        px[3, i] = (*SHADOW, 255)
        px[SIZE - 4, i] = (*SHADOW, 255)
    for (x, y) in [(3, 3), (SIZE - 4, 3), (3, SIZE - 4), (SIZE - 4, SIZE - 4)]:
        px[x, y] = (*SHADOW, 255)

    # Raised bevel band between the two rings.
    for y in range(1, 3):
        for x in range(1, SIZE - 1):
            px[x, y] = (*MID, 255)
    for y in range(SIZE - 3, SIZE - 1):
        for x in range(1, SIZE - 1):
            px[x, y] = (*MID, 255)
    for x in range(1, 3):
        for y in range(3, SIZE - 3):
            px[x, y] = (*MID, 255)
    for x in range(SIZE - 3, SIZE - 1):
        for y in range(3, SIZE - 3):
            px[x, y] = (*MID, 255)

    # Centered radiant core cluster (same shape/coordinates as
    # gen_prismium_core.py's core_pts, reused verbatim so the motif reads
    # as literally "the same light source", just set into a chiseled
    # frame instead of a rough diagonal-gradient facet field).
    core_pts = [
        (7, 6), (8, 6),
        (6, 7), (7, 7), (8, 7), (9, 7),
        (6, 8), (7, 8), (8, 8), (9, 8),
        (7, 9), (8, 9),
    ]
    core_set = set(core_pts)
    ring = set()
    for (x, y) in core_set:
        for (dx, dy) in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if (nx, ny) not in core_set and 0 <= nx < SIZE and 0 <= ny < SIZE:
                ring.add((nx, ny))
    for (x, y) in ring:
        px[x, y] = (*SHADOW, 255)
    for (x, y) in core_pts:
        px[x, y] = (*HILITE, 255)
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        px[x, y] = (*CORE_WHITE, 255)

    # Two small violet accent studs at the panel's outer corners (inside
    # the bevel band), echoing Core's energy-fleck accent color without
    # competing with the central motif the way Chiseled Block's full
    # diamond rune would - kept deliberately minimal since the core
    # cluster is already the focal point.
    for (x, y) in [(2, 2), (SIZE - 3, SIZE - 3)]:
        px[x, y] = (*ACCENT, 255)

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
    out_dir = ASSETS / "block"
    out_dir.mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_chiseled_core()
    out_path = out_dir / "chiseled_prismium_core.png"
    img.save(out_path)
    print(f"wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_chiseled_prismium_core.png"
    preview.save(preview_path)
    print(f"wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
