#!/usr/bin/env python3
"""Generate the entity texture for Prismium Sentinel, ClaudeMod's third
mob and first ranged attacker (see PrismiumSentinelEntity's javadoc for
the design writeup).

Layout: same 64x64 humanoid skin UV map as gen_prismium_wraith.py /
gen_prismium_deep_wraith.py (PrismiumSentinelRenderer wraps vanilla's own
SkeletonModel, which shares the classic biped UV layout, just with
thinner arm/leg cuboids sampling a subset of each region). Painting the
full region with a continuous gradient/pattern, exactly like the prior
two mob scripts, means the result looks correct regardless of exactly how
narrow SkeletonModel's limb boxes turn out to be - belt-and-braces, same
reasoning as gen_prismium_wraith.py.

Design: an ivory/bone "living statue" frame (distinct from Wraith's gray
stone and Deep Wraith's dark waterlogged-basalt) with the mod's
already-validated PRISMIUM_* teal accents reused verbatim (see
PROGRESS.md's recurring "reuse verified palette on the Nth application"
lesson), plus a new warm gold accent (glowing eyes/rune marks) instead of
Wraith's violet or Deep Wraith's green, so all three mobs read as a
related family while still being told apart at a glance.

Self-review: writes an upscaled checkerboard preview to
build/preview_prismium_sentinel.png for Read-based visual inspection, and
prints the set of distinct alpha values present (must be only 0/255, no
partial-transparency bleeding), matching the mod's texture workflow
rules. Run from repo root:
    python3 scripts/textures/gen_prismium_sentinel.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20260819
W, H = 64, 64

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Ivory/bone "living statue" palette - new for this mob (distinct from
# Wraith's FRAME_* gray-stone and Deep Wraith's dark blue-basalt).
FRAME_OUTLINE = "#2B2620"
FRAME_SHADOW = "#5C5346"
FRAME_BASE = "#B8AC94"
FRAME_HILITE = "#EDE4CC"

# Prismium crystal palette, reused verbatim from gen_prismium_wraith.py /
# gen_prismium_core.py / gen_prismium_armor.py.
PRISMIUM_OUTLINE = "#024D4B"
PRISMIUM_SHADOW = "#008282"
PRISMIUM_BASE = "#11BBB8"
PRISMIUM_MID = "#65F5E3"
PRISMIUM_HILITE = "#CAFDF9"
# New warm gold accent (marksman/sentinel authority vibe), replacing
# Wraith's violet PRISMIUM_ACCENT and Deep Wraith's green one.
PRISMIUM_ACCENT = "#FFD37C"
PRISMIUM_ACCENT_DARK = "#7A4B00"
EYE_WHITE = "#FFF6E0"


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


def set_px(px, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = color


def bone_gradient(px, x0, y0, w, h, rng, seed_offset):
    """Fills a rect with a mostly-FRAME_BASE fill, a darker top edge and
    sparse hilite/darker flecks - same treatment gen_prismium_wraith.py's
    stone_gradient uses, renamed to match this mob's bone/ivory theme."""
    local_rng = random.Random(SEED + seed_offset)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if y == y0:
                px[x, y] = FS
            elif x == x0 or y == y0 + h - 1:
                px[x, y] = FS if local_rng.random() < 0.35 else FB
            else:
                px[x, y] = FB
    n = max(1, (w * h) // 10)
    for _ in range(n):
        x = x0 + local_rng.randrange(w)
        y = y0 + local_rng.randrange(h)
        px[x, y] = FH if local_rng.random() < 0.5 else FO


def rune_face(px, x0, y0, pattern):
    """Draws a glowing Prismium rune mark using a list of (dx, dy, color)
    offsets relative to the face's top-left corner - same helper shape as
    gen_prismium_wraith.py's crack_face, renamed for this mob's rune
    motif (dots/lines rather than a jagged crack)."""
    for dx, dy, color in pattern:
        set_px(px, x0 + dx, y0 + dy, color)


def make_sentinel_texture():
    rng = random.Random(SEED)
    img = new_img()
    px = img.load()

    # ---- HEAD (u=0, v=0, 8x8x8) ----
    bone_gradient(px, 8, 0, 8, 8, rng, 1)    # top
    bone_gradient(px, 16, 0, 8, 8, rng, 2)   # bottom
    bone_gradient(px, 0, 8, 8, 8, rng, 3)    # right side
    bone_gradient(px, 16, 8, 8, 8, rng, 4)   # left side
    bone_gradient(px, 24, 8, 8, 8, rng, 5)   # back
    bone_gradient(px, 8, 8, 8, 8, rng, 6)    # front (face)

    # Face: hollow dark eye sockets with a glowing gold pinprick center
    # (distinct from Wraith's solid cyan eyes), a teal rune line across
    # the brow instead of a crack.
    face_x, face_y = 8, 8
    for dx, dy in [(1, 3), (2, 3), (5, 3), (6, 3)]:
        set_px(px, face_x + dx, face_y + dy, FO)
    for dx, dy in [(1, 4), (2, 4), (5, 4), (6, 4)]:
        set_px(px, face_x + dx, face_y + dy, FO)
    set_px(px, face_x + 2, face_y + 3, PA)
    set_px(px, face_x + 5, face_y + 3, PA)
    rune_face(px, face_x, face_y, [
        (2, 1, PB), (3, 1, PM), (4, 1, PM), (5, 1, PB),
    ])
    # thin dark jaw line (skeletal, not a wide mouth)
    for dx in range(2, 6):
        set_px(px, face_x + dx, face_y + 6, FS)

    # ---- BODY (u=16, v=16, 8x12x4) ----
    bone_gradient(px, 20, 16, 8, 4, rng, 10)   # top
    bone_gradient(px, 28, 16, 8, 4, rng, 11)   # bottom
    bone_gradient(px, 16, 20, 4, 12, rng, 12)  # right side
    bone_gradient(px, 28, 20, 4, 12, rng, 13)  # left side
    bone_gradient(px, 32, 20, 8, 12, rng, 14)  # back
    bone_gradient(px, 20, 20, 8, 12, rng, 15)  # front (chest)

    chest_x, chest_y = 20, 20
    # Central rune column: a gold diamond "core" near the collar (echoes
    # the mod's other mobs each having an embedded core motif) with a
    # teal glow line running down, ending in a small horizontal rune bar
    # instead of Wraith's forking crack.
    set_px(px, chest_x + 3, chest_y + 0, PAD)
    set_px(px, chest_x + 4, chest_y + 0, PAD)
    set_px(px, chest_x + 3, chest_y + 1, PA)
    set_px(px, chest_x + 4, chest_y + 1, PA)
    rune_face(px, chest_x, chest_y, [
        (3, 2, PS), (4, 2, PS),
        (3, 3, PB), (4, 3, PB),
        (3, 4, PM), (4, 4, PM),
        (3, 5, PB), (4, 5, PB),
    ])
    for dx in range(2, 6):
        set_px(px, chest_x + dx, chest_y + 7, PS)
    set_px(px, chest_x + 2, chest_y + 7, PB)
    set_px(px, chest_x + 5, chest_y + 7, PB)

    # ---- RIGHT ARM (u=40, v=16, 4x12x4) ----
    bone_gradient(px, 44, 16, 4, 4, rng, 20)   # top
    bone_gradient(px, 48, 16, 4, 4, rng, 21)   # bottom
    bone_gradient(px, 40, 20, 4, 12, rng, 22)  # right
    bone_gradient(px, 48, 20, 4, 12, rng, 23)  # left
    bone_gradient(px, 52, 20, 4, 12, rng, 24)  # back
    bone_gradient(px, 44, 20, 4, 12, rng, 25)  # front
    arm_x, arm_y = 44, 20
    rune_face(px, arm_x, arm_y, [
        (1, 3, PB), (2, 3, PB), (1, 7, PS), (2, 7, PS),
    ])

    # ---- RIGHT LEG (u=0, v=16, 4x12x4) ----
    bone_gradient(px, 4, 16, 4, 4, rng, 30)    # top
    bone_gradient(px, 8, 16, 4, 4, rng, 31)    # bottom
    bone_gradient(px, 0, 20, 4, 12, rng, 32)   # right
    bone_gradient(px, 8, 20, 4, 12, rng, 33)   # left
    bone_gradient(px, 12, 20, 4, 12, rng, 34)  # back
    bone_gradient(px, 4, 20, 4, 12, rng, 35)   # front
    leg_x, leg_y = 4, 20
    rune_face(px, leg_x, leg_y, [
        (1, 2, PB), (2, 2, PB), (1, 8, PS), (2, 8, PS),
    ])

    # ---- Belt-and-braces: separate left arm/leg regions (64x64 skin
    # layout offsets), mirroring the right side's art - see
    # gen_prismium_wraith.py for why both are painted.
    # Left Leg (u=16, v=48)
    bone_gradient(px, 20, 48, 4, 4, rng, 40)   # top
    bone_gradient(px, 24, 48, 4, 4, rng, 41)   # bottom
    bone_gradient(px, 16, 52, 4, 12, rng, 42)  # right
    bone_gradient(px, 24, 52, 4, 12, rng, 43)  # left
    bone_gradient(px, 28, 52, 4, 12, rng, 44)  # back
    bone_gradient(px, 20, 52, 4, 12, rng, 45)  # front
    rune_face(px, 20, 52, [
        (1, 2, PB), (2, 2, PB), (1, 8, PS), (2, 8, PS),
    ])
    # Left Arm (u=32, v=48)
    bone_gradient(px, 36, 48, 4, 4, rng, 50)   # top
    bone_gradient(px, 40, 48, 4, 4, rng, 51)   # bottom
    bone_gradient(px, 32, 52, 4, 12, rng, 52)  # right
    bone_gradient(px, 40, 52, 4, 12, rng, 53)  # left
    bone_gradient(px, 44, 52, 4, 12, rng, 54)  # back
    bone_gradient(px, 36, 52, 4, 12, rng, 55)  # front
    rune_face(px, 36, 52, [
        (1, 3, PB), (2, 3, PB), (1, 7, PS), (2, 7, PS),
    ])

    return img


def make_preview(img, scales=(1, 4, 8)):
    """Same preview compositor as gen_prismium_wraith.py (checkerboard
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

    img = make_sentinel_texture()
    out_path = ASSETS / "entity" / "prismium_sentinel.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_sentinel.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
