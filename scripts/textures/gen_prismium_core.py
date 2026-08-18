#!/usr/bin/env python3
"""Generate the texture for Prismium Core (session 3's new exclusive block).

Reuses the Prismium palette and the same diagonal-band gradient technique as
make_prismium_block() in gen_prismium.py, so the two blocks read as a clear
family, but adds a bright radiant core cluster in the middle (echoes the
block's lightLevel 10) to distinguish it as the "condensed" upgrade. Kept
the violet energy flecks sparse (matching prismium_block's restraint)
after an initial denser draft looked noisy on self-review - see
PROGRESS.md. Deterministic (fixed seed). Run from repo root:
python3 scripts/textures/gen_prismium_core.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260817
SIZE = 16

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_CORE_WHITE = "#EFFFFC"
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_DARK = "#720070"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def new_img():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def make_prismium_core():
    rng = random.Random(SEED)
    img = new_img()
    px = img.load()
    outline = hexrgb(PRISMIUM_OUTLINE)
    shadow = hexrgb(PRISMIUM_SHADOW)
    base = hexrgb(PRISMIUM_BASE)
    mid = hexrgb(PRISMIUM_MID)
    hilite = hexrgb(PRISMIUM_HILITE)
    core_white = hexrgb(PRISMIUM_CORE_WHITE)
    accent = hexrgb(PRISMIUM_ACCENT)
    accent_dark = hexrgb(PRISMIUM_ACCENT_DARK)

    # Same diagonal-band gradient as make_prismium_block(), for family
    # consistency - base -> mid -> hilite -> base -> shadow.
    for y in range(SIZE):
        for x in range(SIZE):
            d = x + y
            if d < 8:
                c = base
            elif d < 14:
                c = mid
            elif d < 20:
                c = mid
            elif d < 26:
                c = base
            else:
                c = shadow
            px[x, y] = (*c, 255)

    # Facet cut lines, same cadence as prismium_block.
    for i in range(-2, 18, 6):
        for t in range(SIZE):
            x, y = i + t, t
            if 0 <= x < SIZE:
                px[x, y] = (*outline, 255)

    # Bright radiant core cluster in the middle, reading as the block's own
    # light source (lightLevel 10 in-game) - the main visual differentiator
    # from the plain storage block.
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
        px[x, y] = (*shadow, 255)
    for (x, y) in core_pts:
        px[x, y] = (*hilite, 255)
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        px[x, y] = (*core_white, 255)

    # A few sparkle highlights on the facets outside the core, same amount
    # as prismium_block.
    for _ in range(6):
        x, y = rng.randint(0, SIZE - 1), rng.randint(0, SIZE - 1)
        if (x, y) not in core_set and (x, y) not in ring:
            px[x, y] = (*hilite, 255)

    # Sparse violet energy flecks, kept restrained (same density as
    # prismium_block, not the denser scatter from the first draft).
    placed = 0
    attempts = 0
    while placed < 5 and attempts < 60:
        attempts += 1
        x, y = rng.randint(1, SIZE - 2), rng.randint(1, SIZE - 2)
        if (x, y) in core_set or (x, y) in ring:
            continue
        px[x, y] = (*accent, 255)
        if rng.random() < 0.5 and x + 1 < SIZE and (x + 1, y) not in core_set:
            px[x + 1, y] = (*accent_dark, 255)
        placed += 1

    # Crisp border outline, same treatment as prismium_block.
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


def use_user_submitted_core_texture():
    """Copy the user's hand-drawn Prismium Core art in as the real asset.

    Session (interactive follow-up, same day as the Prismium Block art
    adoption in gen_prismium.py): the user pointed out that Prismium Core
    had not caught up with the newly-adopted Prismium Block look (Core was
    still the programmatic make_prismium_core() diagonal-band pattern,
    just recolored to the new palette, not literal hand-drawn art) and
    hand-made a Core-specific 16x16 texture to fix that: same diagonal
    teal/cyan gradient and four corner magenta gem clusters as the block
    art, plus a bright white/hilite ring in the center to read as the
    block's own light source (matching what make_prismium_core() was
    trying to achieve programmatically with its core_pts cluster).
    Converts to RGBA (source PNG is plain RGB) but does not otherwise
    touch a single pixel.

    prismium_core_slab / prismium_core_stairs / prismium_core_wall all
    reference this same "claudemod:block/prismium_core" texture in their
    models, so they pick up this art automatically - no separate files
    needed for those ("Prismium Core-related building blocks").
    """
    src = REPO_ROOT / "scripts/textures/reference/user_submitted_prismium_core_2026-08-18.png"
    img = Image.open(src).convert("RGBA")
    assert img.size == (SIZE, SIZE), f"expected {SIZE}x{SIZE}, got {img.size}"
    out = ASSETS / "block/prismium_core.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"wrote {out.relative_to(REPO_ROOT)} (from user-submitted art, not make_prismium_core())")


if __name__ == "__main__":
    use_user_submitted_core_texture()
