#!/usr/bin/env python3
"""Generate the texture for Prismium Cable (session 10 - see
ModBlocks.PRISMIUM_CABLE / PrismiumCableBlockEntity / PROGRESS.md).

Fourth distinct visual language for the family, after the diagonal-gradient
crystal faces (prismium_block/prismium_core), the dark cage lattice
(prismium_lantern), and the "battery casing" (prismium_cell/
prismium_generator, which intentionally share one palette - see
gen_prismium_cell.py's docstring). This block is the mod's first
non-full-cube shape (an 8x8x8 centered post, see PrismiumCableBlock/
models/block/prismium_cable.json), so the same texture appears on all six
faces of a small cube rather than a flat 16x16 face of a full block - the
design needed to read clearly at that reduced apparent size too.

Reused the Cell/Generator "machine family" CASING_DARK/CASING_MID slate
gray (verified in session 8 to contrast well against PRISMIUM_OUTLINE, see
that script's docstring and PROGRESS.md's "reuse a validated palette for a
second block in the same family" note from session 9) for the outer ring,
so a cable reads as visually related to the two machines it's meant to
connect. The center gets a small glowing "conduit" cross-section (concentric
squares like the Cell's glass window, but smaller and with a thin plus-sign
accent instead of gauge bars) to suggest energy passing through, distinct
enough from Cell's "window" and Generator's "ember grate" to still be
identifiable as its own block at a glance.

Self-review done on generation (16x/4x upscaled preview, standard process
for this mod): ring contrast against the glow and against
PRISMIUM_OUTLINE both read clearly at small size; alpha channel confirmed
uniform 255 (fully opaque, no partial-transparency edge artifacts) since,
unlike the armor layer textures, this one has no intentional transparent
regions to worry about. No redraw was needed.

Run from repo root: python3 scripts/textures/gen_prismium_cable.py
"""
from pathlib import Path

from PIL import Image

SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#0B3D3C"
PRISMIUM_BASE = "#3FBDB8"
PRISMIUM_MID = "#66D9D2"
PRISMIUM_HILITE = "#B9FFF3"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#C97BFF"

# Same validated "machine family" tones as prismium_cell.png /
# prismium_generator*.png (see that script's docstring for why these
# specific values were chosen over the lantern's near-black cage tones).
CASING_DARK = "#4A5A58"
CASING_MID = "#7A8C89"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_cable():
    img = new_img()
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    core_white = hexrgb(PRISMIUM_CORE_WHITE)
    accent = hexrgb(PRISMIUM_ACCENT)
    casing_dark = hexrgb(CASING_DARK)
    casing_mid = hexrgb(CASING_MID)

    # 1) Base fill: metal ring casing, same starting point as the Cell.
    for y in range(SIZE):
        for x in range(SIZE):
            px[x, y] = (*casing_dark, 255)

    # 2) "Sunward" highlight on top row/left column, one pixel deep this
    # time - the cable's ring is meant to read as thinner/lighter than
    # the Cell's thick battery casing, since this is a conduit segment,
    # not a storage container.
    for x in range(SIZE):
        px[x, 0] = (*casing_mid, 255)
    for y in range(SIZE):
        px[0, y] = (*casing_mid, 255)

    # 3) Glowing conduit core: concentric squares, x/y 5..10 inclusive
    # (6x6), smaller than the Cell's 8x8 window since this texture will
    # be seen on a smaller (8x8x8, not full-cube) element in-world and a
    # thicker casing ring reads better at that reduced apparent size.
    cx, cy = 7.5, 7.5
    for y in range(5, 11):
        for x in range(5, 11):
            d = max(abs(x - cx), abs(y - cy))
            if d < 1.0:
                c = core_white
            elif d < 2.0:
                c = hilite
            elif d < 2.5:
                c = mid
            else:
                c = base
            px[x, y] = (*c, 255)

    # 4) Thin plus-sign accent crossing the core, suggesting current
    # flowing straight through the segment (distinct from the Cell's
    # horizontal gauge bars and the Generator's horizontal ember slits -
    # this one reads as "pass-through", matching what the block actually
    # does).
    for x in range(4, 12):
        px[x, 7] = (*accent, 255)
    for y in range(4, 12):
        px[7, y] = (*accent, 255)
    # keep the very center a bright core pixel rather than accent, so the
    # plus-sign doesn't muddy the brightest point of the glow
    px[7, 7] = (*core_white, 255)

    # 5) Rivets at the four inner corners where the ring meets the core,
    # matching the Cell's visual grammar for "ring meets window".
    for (x, y) in [(4, 4), (11, 4), (4, 11), (11, 11)]:
        px[x, y] = (*casing_mid, 255)

    # 6) Crisp 1px outline border, matching the rest of the family.
    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)

    return img


def save(img, rel_path):
    out = ASSETS / rel_path
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    save(make_prismium_cable(), "block/prismium_cable.png")
