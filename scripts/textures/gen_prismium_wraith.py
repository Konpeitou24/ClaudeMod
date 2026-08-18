#!/usr/bin/env python3
"""Generate the entity texture for Prismium Wraith (session 12), the mod's
first mob.

Layout: the classic 64x64 humanoid ("Steve-style") skin UV map, which is
what vanilla's ZombieModel (reused directly by PrismiumWraithRenderer, see
PrismiumWraithEntity's javadoc for why) expects. We paint both the
"mirrored" right-arm/right-leg cubes (used by the base Zombie geometry,
which mirrors the left side off of the right UVs) AND the separate
left-arm/left-leg regions at the standard 64x64 Player-skin offsets
(32,48) and (16,48). This is a deliberate belt-and-braces move: session 12
could not find a 100%-confirmed answer for whether 1.20.1's ZombieModel
still mirrors (old format) or has been updated to read distinct left-side
UVs, so painting both costs nothing (unused regions are simply never
sampled) and guarantees no blank/missing limb regardless of which is true.
Second layer ("hat"/"jacket" overlay) regions are intentionally left fully
transparent - same reasoning as PrismiumWraithEntity's javadoc, this is a
cosmetic-only unknown that can't crash anything either way.

Design: reuses the mod's two already-validated palettes wholesale (see
PROGRESS.md's "reuse verified palette on the Nth application" recurring
lesson) rather than inventing new colors for the mod's first mob:
- FRAME_* grays from gen_prismium_armor.py for the "weathered stone/rock"
  body, casting the Wraith as an animated guardian made of stone rather
  than flesh.
- PRISMIUM_* teals/cyans (ore/block/armor family) for glowing cracks,
  eyes and a chest accent, tying it visually to the ore it guards.

Self-review: after generating, this script also writes a 4x/8x upscaled
preview sheet (checkerboard background) to build/preview_prismium_wraith.png
for Read-based visual inspection, per the mod's texture workflow rules.
Deterministic (fixed seed, though most placement here is hand-authored
rather than random). Run from repo root:
    python3 scripts/textures/gen_prismium_wraith.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260817
W, H = 64, 64

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Stone/rock "flesh" palette, reused verbatim from gen_prismium_armor.py.
FRAME_OUTLINE = "#1B1B22"
FRAME_SHADOW = "#33333D"
FRAME_BASE = "#4A4A57"
FRAME_HILITE = "#6E6E80"

# Prismium crystal palette, reused verbatim from gen_prismium_core.py /
# gen_prismium_armor.py.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
PRISMIUM_ACCENT = "#FF7CFC"
PRISMIUM_ACCENT_DARK = "#720070"
EYE_WHITE = "#EFFFFC"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


FO = hexrgb(FRAME_OUTLINE)
FS = hexrgb(FRAME_SHADOW)
FB = hexrgb(FRAME_BASE)
FH = hexrgb(FRAME_HILITE)
PO = hexrgb(PRISMIUM_OUTLINE)
PS = hexrgb(PRISMIUM_SHADOW)
PB = hexrgb(PRISMIUM_BASE)
PM = hexrgb(PRISMIUM_MID)
PH = hexrgb(PRISMIUM_HILITE)
PA = hexrgb(PRISMIUM_ACCENT)
PAD = hexrgb(PRISMIUM_ACCENT_DARK)
EW = hexrgb(EYE_WHITE)
TRANSPARENT = (0, 0, 0, 0)


def new_img():
    return Image.new("RGBA", (W, H), TRANSPARENT)


def fill_rect(px, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = color


def set_px(px, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = color


def stone_gradient(px, x0, y0, w, h, rng, seed_offset):
    """Fills a rect with a mostly-FRAME_BASE fill, a darker top edge and a
    hilite bottom-right speckle, plus sparse 1px darker flecks for texture
    (same 'sparse flecks' restraint used across prior Prismium textures)."""
    local_rng = random.Random(SEED + seed_offset)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if y == y0:
                px[x, y] = FS
            elif x == x0 or y == y0 + h - 1:
                px[x, y] = FS if local_rng.random() < 0.35 else FB
            else:
                px[x, y] = FB
    # sparse hilite/darker flecks
    n = max(1, (w * h) // 10)
    for _ in range(n):
        x = x0 + local_rng.randrange(w)
        y = y0 + local_rng.randrange(h)
        px[x, y] = FH if local_rng.random() < 0.5 else FO


def crack_face(px, x0, y0, w, h, pattern):
    """Draws a glowing Prismium crack using a list of (dx, dy, color)
    offsets relative to the face's top-left corner."""
    for dx, dy, color in pattern:
        set_px(px, x0 + dx, y0 + dy, color)


def make_wraith_texture():
    rng = random.Random(SEED)
    img = new_img()
    px = img.load()

    # ---- HEAD (u=0, v=0, 8x8x8) ----
    stone_gradient(px, 8, 0, 8, 8, rng, 1)   # top
    stone_gradient(px, 16, 0, 8, 8, rng, 2)  # bottom
    stone_gradient(px, 0, 8, 8, 8, rng, 3)   # right side
    stone_gradient(px, 16, 8, 8, 8, rng, 4)  # left side
    stone_gradient(px, 24, 8, 8, 8, rng, 5)  # back
    stone_gradient(px, 8, 8, 8, 8, rng, 6)   # front (face)

    # Face details on the front head quad (8,8)-(16,16): glowing cyan eyes,
    # a crack running down from the brow, dark "mouth" slit.
    face_x, face_y = 8, 8
    # eyes (2px each, symmetric)
    for dx, dy in [(1, 3), (2, 3), (5, 3), (6, 3)]:
        set_px(px, face_x + dx, face_y + dy, EW)
    for dx, dy in [(1, 4), (2, 4), (5, 4), (6, 4)]:
        set_px(px, face_x + dx, face_y + dy, PB)
    # brow crack
    crack_face(px, face_x, face_y, 8, 8, [
        (3, 0, PM), (4, 1, PM), (3, 2, PB), (4, 2, PB),
    ])
    # mouth/jaw crack, faint glow
    for dx in range(2, 6):
        set_px(px, face_x + dx, face_y + 6, PS)
    set_px(px, face_x + 3, face_y + 7, PB)
    set_px(px, face_x + 4, face_y + 7, PB)

    # ---- BODY (u=16, v=16, 8x12x4) ----
    stone_gradient(px, 20, 16, 8, 4, rng, 10)  # top
    stone_gradient(px, 28, 16, 8, 4, rng, 11)  # bottom
    stone_gradient(px, 16, 20, 4, 12, rng, 12)  # right side
    stone_gradient(px, 28, 20, 4, 12, rng, 13)  # left side
    stone_gradient(px, 32, 20, 8, 12, rng, 14)  # back
    stone_gradient(px, 20, 20, 8, 12, rng, 15)  # front (chest)

    chest_x, chest_y = 20, 20
    # central crack running down the chest, forking near the bottom, plus
    # an embedded violet "core" shard high on the chest (echoes Prismium
    # Core's accent color - this is a fragment of the thing it guards).
    crack_face(px, chest_x, chest_y, 8, 12, [
        (3, 1, PS), (4, 1, PS),
        (3, 2, PB), (4, 2, PB),
        (3, 3, PM), (4, 3, PB),
        (3, 4, PB), (4, 4, PS),
        (2, 5, PS), (3, 5, PB), (4, 5, PB), (5, 5, PS),
        (2, 6, PB), (5, 6, PB),
        (1, 7, PS), (2, 7, PB), (5, 7, PB), (6, 7, PS),
        (1, 8, PB), (6, 8, PB),
        (1, 9, PS), (6, 9, PS),
    ])
    # violet core shard (diamond shape) near the collar
    for dx, dy, c in [
        (3, 1, PAD), (4, 1, PAD),
        (3, 0, PA), (4, 0, PA),
    ]:
        set_px(px, chest_x + dx, chest_y + dy, c)
    set_px(px, chest_x + 3, chest_y + 0, PH)

    # ---- RIGHT ARM (u=40, v=16, 4x12x4) - also used as the mirrored
    # left arm by ZombieModel if it does not read distinct left UVs.
    stone_gradient(px, 44, 16, 4, 4, rng, 20)  # top
    stone_gradient(px, 48, 16, 4, 4, rng, 21)  # bottom
    stone_gradient(px, 40, 20, 4, 12, rng, 22)  # right
    stone_gradient(px, 48, 20, 4, 12, rng, 23)  # left
    stone_gradient(px, 52, 20, 4, 12, rng, 24)  # back
    stone_gradient(px, 44, 20, 4, 12, rng, 25)  # front
    arm_x, arm_y = 44, 20
    crack_face(px, arm_x, arm_y, 4, 12, [
        (1, 2, PS), (2, 3, PB), (1, 5, PB), (2, 7, PS), (1, 9, PB),
    ])

    # ---- RIGHT LEG (u=0, v=16, 4x12x4) ----
    stone_gradient(px, 4, 16, 4, 4, rng, 30)   # top
    stone_gradient(px, 8, 16, 4, 4, rng, 31)   # bottom
    stone_gradient(px, 0, 20, 4, 12, rng, 32)  # right
    stone_gradient(px, 8, 20, 4, 12, rng, 33)  # left
    stone_gradient(px, 12, 20, 4, 12, rng, 34)  # back
    stone_gradient(px, 4, 20, 4, 12, rng, 35)   # front
    leg_x, leg_y = 4, 20
    crack_face(px, leg_x, leg_y, 4, 12, [
        (2, 1, PS), (1, 3, PB), (2, 6, PB), (1, 9, PS),
    ])

    # ---- Belt-and-braces: separate left arm/leg regions (64x64 Player
    # skin layout offsets). Mirror the right arm/leg front+side art so the
    # model looks correct even if this is read instead of the above.
    # Left Leg (u=16, v=48)
    stone_gradient(px, 20, 48, 4, 4, rng, 40)   # top
    stone_gradient(px, 24, 48, 4, 4, rng, 41)   # bottom
    stone_gradient(px, 16, 52, 4, 12, rng, 42)  # right
    stone_gradient(px, 24, 52, 4, 12, rng, 43)  # left
    stone_gradient(px, 28, 52, 4, 12, rng, 44)  # back
    stone_gradient(px, 20, 52, 4, 12, rng, 45)  # front
    crack_face(px, 20, 52, 4, 12, [
        (2, 1, PS), (1, 3, PB), (2, 6, PB), (1, 9, PS),
    ])
    # Left Arm (u=32, v=48)
    stone_gradient(px, 36, 48, 4, 4, rng, 50)   # top
    stone_gradient(px, 40, 48, 4, 4, rng, 51)   # bottom
    stone_gradient(px, 32, 52, 4, 12, rng, 52)  # right
    stone_gradient(px, 40, 52, 4, 12, rng, 53)  # left
    stone_gradient(px, 44, 52, 4, 12, rng, 54)  # back
    stone_gradient(px, 36, 52, 4, 12, rng, 55)  # front
    crack_face(px, 36, 52, 4, 12, [
        (1, 2, PS), (2, 3, PB), (1, 5, PB), (2, 7, PS), (1, 9, PB),
    ])

    return img


def make_preview(img, scales=(1, 4, 8)):
    """Composites upscaled copies over a checkerboard so alpha issues are
    visible on Read-based self-review, matching prior sessions' workflow."""
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
                cx, cy = x // (tile), y // (tile)
                bpx[x, y] = checker_light if (cx + cy) % 2 == 0 else checker_dark
        scaled = img.resize((W * s, H * s), Image.NEAREST)
        board.alpha_composite(scaled)
        preview.alpha_composite(board, (x_off, 0))
        x_off += W * s + 8

    return preview


def main():
    ASSETS.joinpath("entity").mkdir(parents=True, exist_ok=True)
    BUILD_DIR.mkdir(parents=True, exist_ok=True)

    img = make_wraith_texture()
    out_path = ASSETS / "entity" / "prismium_wraith.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_wraith.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    # Machine-check alpha values (must be fully opaque or fully transparent,
    # no partial-transparency bleeding - same check prior texture sessions
    # ran).
    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
