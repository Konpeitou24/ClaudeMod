#!/usr/bin/env python3
"""Generate block/pale_prismium_block.png (session #77, scheduled):
ClaudeMod's first genuinely "pale blue-white" (青白い) decorative block,
answering the direct user request tracked in PROGRESS.md's continued
backlog ("ユーザー直接要望2件(青白いブロック、Prism Realm巨大山岳地帯+
ボス)") for several sessions.

Deliberately a NEW, distinct palette rather than a lighter tint of the
mod's existing teal-cyan PRISMIUM_* crystal palette (see gen_prismium.py)
- the request was specifically for something that reads as icy pale
blue-white, not "Prismium Block but brighter", so this gets its own
PALE_* constants below and its own icy-glass silhouette instead of
reusing make_prismium_block()'s diagonal gradient verbatim. Structurally
it still follows the same technique (flat diagonal color bands + a few
hand-placed facet outline diagonals + scattered hilite sparkles + solid
border) established there, so it reads as a sibling material in the same
mod rather than a completely foreign style.

16x16, limited flat palette, no smooth gradients/AA, matching this mod's
established pixel-art convention. Deterministic (fixed seed).

Run from repo root: python3 scripts/textures/gen_pale_prismium_block.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260829
SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# Pale, icy blue-white palette - distinct from PRISMIUM_BASE (#11BBB8
# teal). Named PALE_* to avoid clashing with gen_prismium.py's PRISMIUM_*
# constants if this module is ever imported alongside it.
PALE_OUTLINE = "#31536E"   # dark slate-blue outline/mortar
PALE_SHADOW = "#5C8CB0"    # shaded facet
PALE_BASE = "#9FD3EE"      # main pale blue body
PALE_MID = "#CDEBFA"       # lighter blue-white band
PALE_HILITE = "#F6FCFF"    # near-white sparkle/hilite
PALE_ACCENT = "#7EE6FF"    # faint cyan energy fleck (ties into mod's energy theme)


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def make_pale_prismium_block():
    rng = random.Random(SEED)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()

    outline = hexrgb(PALE_OUTLINE)
    shadow = hexrgb(PALE_SHADOW)
    base = hexrgb(PALE_BASE)
    mid = hexrgb(PALE_MID)
    hilite = hexrgb(PALE_HILITE)
    accent = hexrgb(PALE_ACCENT)

    # Base fill: diagonal band gradient (mid -> base -> shadow), same
    # banding technique as make_prismium_block() but weighted paler/
    # whiter overall so the block reads as icy rather than teal.
    for y in range(SIZE):
        for x in range(SIZE):
            d = x + y  # 0..30
            if d < 6:
                c = hilite
            elif d < 12:
                c = mid
            elif d < 20:
                c = base
            elif d < 26:
                c = mid
            else:
                c = shadow
            px[x, y] = (*c, 255)

    # Facet outlines: a few diagonal cut lines for a faceted-ice look.
    for i in range(-3, 19, 7):
        for t in range(SIZE):
            x, y = i + t, t
            if 0 <= x < SIZE:
                px[x, y] = (*outline, 255)

    # Sparkle highlights scattered on facets (icy glints).
    for _ in range(11):
        x, y = rng.randint(0, SIZE - 1), rng.randint(0, SIZE - 1)
        px[x, y] = (*hilite, 255)

    # A few faint cyan energy flecks, small enough to read as a subtle
    # tie-in to the mod's energy theme rather than a competing accent.
    for _ in range(4):
        x, y = rng.randint(1, SIZE - 2), rng.randint(1, SIZE - 2)
        px[x, y] = (*accent, 255)

    # Border outline for a crisp block silhouette.
    for x in range(SIZE):
        px[x, 0] = (*outline, 255)
        px[x, SIZE - 1] = (*outline, 255)
    for y in range(SIZE):
        px[0, y] = (*outline, 255)
        px[SIZE - 1, y] = (*outline, 255)

    return img


def save(img, rel_path):
    out_path = ASSETS / rel_path
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path)
    print(f"wrote {out_path}")


if __name__ == "__main__":
    save(make_pale_prismium_block(), "block/pale_prismium_block.png")
