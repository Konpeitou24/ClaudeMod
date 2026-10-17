#!/usr/bin/env python3
"""Generate the entity texture for Prismium Colossus (scheduled session,
2026-10-17), ClaudeMod's eighth mob and first boss-tier monster - see
PrismiumColossusEntity's javadoc for the design writeup.

Layout: the same 64x64 "Steve-style" humanoid skin UV map used by
gen_prismium_wraith.py (reused because PrismiumColossusRenderer, like
PrismiumWraithRenderer before it, bakes a plain HumanoidModel from the
vanilla ModelLayers.ZOMBIE layer rather than a bespoke model class - see
that renderer's javadoc). Same belt-and-braces approach as the Wraith
texture: both the mirrored right-arm/right-leg cubes AND the separate
left-arm/left-leg 64x64 player-skin regions are painted identically, so
the result is correct regardless of whether this model reads mirrored or
distinct left-side UVs.

Design: deliberately heavier/darker than every prior Prismium mob texture
to read as a boss at a glance - a near-black "corrupted stone" body (the
existing FRAME_* grays from gen_prismium_wraith.py, darkened further) laced
with the mod's violet PRISMIUM_ACCENT family (reused from
gen_prismium_rift_shard.py / gen_prismium.py) as the dominant crack color
instead of Wraith's cyan, plus a wide glowing "crown" of jagged crystal
shards across the top of the head and a thick band of accent cracks across
the chest, to visually separate this boss from the regular Wraith/
Sentinel/Familiar roster even though they share the same base body
geometry.

Self-review: writes a 4x/8x upscaled preview sheet to
build/preview_prismium_colossus.png for Read-based visual inspection
(same workflow every prior mob texture in this mod follows), and prints
the set of distinct alpha values present (must be only {0, 255}).
Deterministic (fixed seed). Run from repo root:
    python3 scripts/textures/gen_prismium_colossus.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20261017
W, H = 64, 64

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Darkened "corrupted stone" body palette - a near-black variant of the
# FRAME_* grays gen_prismium_wraith.py uses for its "weathered stone"
# look, so this boss reads as a heavier, more ominous relative of the
# Wraith rather than an unrelated creature.
FRAME_OUTLINE = "#0A0A0E"
FRAME_SHADOW = "#1C1C24"
FRAME_BASE = "#2E2E39"
FRAME_HILITE = "#4A4A5C"

# Violet "corrupted crystal" palette - the mod's existing PRISMIUM_ACCENT
# family (gen_prismium.py / gen_prismium_rift_shard.py), used here as the
# dominant crack/glow color (rather than a minor accent on a mostly-cyan
# body, as every prior mob/item uses it) to set this boss visually apart.
ACCENT_OUTLINE = "#2E0030"
ACCENT_SHADOW = "#720070"
ACCENT_BASE = "#C34BF2"
ACCENT_MID = "#E79BFF"
ACCENT_HILITE = "#FCE6FF"
EYE_WHITE = "#FFF3FF"

# A sliver of the mod's cyan PRISMIUM_BASE family for a few "still loyal
# to the ore" veins low on the body, so the palette doesn't read as a
# totally disconnected new family.
PRISMIUM_BASE = "#11BBB8"


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


FO = hexrgb(FRAME_OUTLINE)
FS = hexrgb(FRAME_SHADOW)
FB = hexrgb(FRAME_BASE)
FH = hexrgb(FRAME_HILITE)
AO = hexrgb(ACCENT_OUTLINE)
AS_ = hexrgb(ACCENT_SHADOW)
AB = hexrgb(ACCENT_BASE)
AM = hexrgb(ACCENT_MID)
AH = hexrgb(ACCENT_HILITE)
EW = hexrgb(EYE_WHITE)
PB = hexrgb(PRISMIUM_BASE)
TRANSPARENT = (0, 0, 0, 0)


def new_img():
    return Image.new("RGBA", (W, H), TRANSPARENT)


def set_px(px, x, y, color):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = color


def stone_gradient(px, x0, y0, w, h, rng, seed_offset):
    """Same technique as gen_prismium_wraith.py's stone_gradient: mostly
    FRAME_BASE fill, darker top/edges, sparse hilite/darker flecks."""
    local_rng = random.Random(SEED + seed_offset)
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if y == y0:
                px[x, y] = FS
            elif x == x0 or y == y0 + h - 1:
                px[x, y] = FS if local_rng.random() < 0.35 else FB
            else:
                px[x, y] = FB
    n = max(1, (w * h) // 9)
    for _ in range(n):
        x = x0 + local_rng.randrange(w)
        y = y0 + local_rng.randrange(h)
        px[x, y] = FH if local_rng.random() < 0.5 else FO


def crack_face(px, x0, y0, pattern):
    for dx, dy, color in pattern:
        set_px(px, x0 + dx, y0 + dy, color)


def make_colossus_texture():
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

    # Jagged violet "crown" of crystal shards across the top-of-head quad,
    # reading as broken crystal growths rather than hair.
    crown_x, crown_y = 8, 0
    crack_face(px, crown_x, crown_y, [
        (0, 2, AB), (1, 1, AB), (1, 2, AM), (2, 0, AB), (2, 1, AM),
        (3, 2, AS_), (4, 2, AS_), (5, 0, AB), (5, 1, AM), (6, 1, AB),
        (6, 2, AM), (7, 2, AB),
    ])

    # Face details on the front head quad: wide glowing violet eyes (bigger
    # than Wraith's cyan ones, to read as more intense), a crack running
    # down from the brow, and a jagged "fanged" mouth line.
    face_x, face_y = 8, 8
    for dx, dy in [(1, 3), (2, 3), (5, 3), (6, 3)]:
        set_px(px, face_x + dx, face_y + dy, EW)
    for dx, dy in [(1, 4), (2, 4), (5, 4), (6, 4)]:
        set_px(px, face_x + dx, face_y + dy, AB)
    crack_face(px, face_x, face_y, [
        (3, 0, AM), (4, 1, AM), (3, 2, AB), (4, 2, AB),
    ])
    for dx in range(1, 7):
        set_px(px, face_x + dx, face_y + 6, AS_)
    for dx in [2, 4]:
        set_px(px, face_x + dx, face_y + 7, AB)
    set_px(px, face_x + 3, face_y + 7, FO)

    # ---- BODY (u=16, v=16, 8x12x4) ----
    stone_gradient(px, 20, 16, 8, 4, rng, 10)  # top
    stone_gradient(px, 28, 16, 8, 4, rng, 11)  # bottom
    stone_gradient(px, 16, 20, 4, 12, rng, 12)  # right side
    stone_gradient(px, 28, 20, 4, 12, rng, 13)  # left side
    stone_gradient(px, 32, 20, 8, 12, rng, 14)  # back
    stone_gradient(px, 20, 20, 8, 12, rng, 15)  # front (chest)

    chest_x, chest_y = 20, 20
    # Wide violet crack network dominating the chest (thicker/denser than
    # Wraith's single thin crack, to read as "heavier damage holding back
    # more power"), plus a bright core gem low on the chest.
    crack_face(px, chest_x, chest_y, [
        (3, 0, AM), (4, 0, AM),
        (2, 1, AB), (3, 1, AB), (4, 1, AB), (5, 1, AB),
        (2, 2, AS_), (5, 2, AS_),
        (1, 3, AB), (2, 3, AM), (5, 3, AM), (6, 3, AB),
        (1, 4, AS_), (6, 4, AS_),
        (2, 5, AB), (3, 5, AM), (4, 5, AM), (5, 5, AB),
        (2, 6, AS_), (5, 6, AS_),
        (1, 7, AB), (3, 7, AM), (4, 7, AM), (6, 7, AB),
        (1, 8, AS_), (6, 8, AS_),
        (3, 9, PB), (4, 9, PB),
    ])
    # bright core gem (diamond) near the bottom of the chest crack network
    for dx, dy, c in [
        (3, 10, AS_), (4, 10, AS_),
        (3, 11, AH), (4, 11, AH),
    ]:
        set_px(px, chest_x + dx, chest_y + dy, c)

    # ---- RIGHT ARM (u=40, v=16, 4x12x4) ----
    stone_gradient(px, 44, 16, 4, 4, rng, 20)  # top
    stone_gradient(px, 48, 16, 4, 4, rng, 21)  # bottom
    stone_gradient(px, 40, 20, 4, 12, rng, 22)  # right
    stone_gradient(px, 48, 20, 4, 12, rng, 23)  # left
    stone_gradient(px, 52, 20, 4, 12, rng, 24)  # back
    stone_gradient(px, 44, 20, 4, 12, rng, 25)  # front
    arm_x, arm_y = 44, 20
    crack_face(px, arm_x, arm_y, [
        (1, 1, AM), (2, 2, AB), (1, 4, AS_), (2, 5, AB),
        (1, 7, AB), (2, 8, AM), (1, 10, AS_),
    ])

    # ---- RIGHT LEG (u=0, v=16, 4x12x4) ----
    stone_gradient(px, 4, 16, 4, 4, rng, 30)   # top
    stone_gradient(px, 8, 16, 4, 4, rng, 31)   # bottom
    stone_gradient(px, 0, 20, 4, 12, rng, 32)  # right
    stone_gradient(px, 8, 20, 4, 12, rng, 33)  # left
    stone_gradient(px, 12, 20, 4, 12, rng, 34)  # back
    stone_gradient(px, 4, 20, 4, 12, rng, 35)   # front
    leg_x, leg_y = 4, 20
    crack_face(px, leg_x, leg_y, [
        (2, 0, AB), (1, 2, AM), (2, 5, AB), (1, 8, AS_), (2, 10, AB),
    ])

    # ---- Belt-and-braces: separate left arm/leg regions (64x64 player
    # skin layout offsets), mirroring the right side's art.
    # Left Leg (u=16, v=48)
    stone_gradient(px, 20, 48, 4, 4, rng, 40)   # top
    stone_gradient(px, 24, 48, 4, 4, rng, 41)   # bottom
    stone_gradient(px, 16, 52, 4, 12, rng, 42)  # right
    stone_gradient(px, 24, 52, 4, 12, rng, 43)  # left
    stone_gradient(px, 28, 52, 4, 12, rng, 44)  # back
    stone_gradient(px, 20, 52, 4, 12, rng, 45)  # front
    crack_face(px, 20, 52, [
        (2, 0, AB), (1, 2, AM), (2, 5, AB), (1, 8, AS_), (2, 10, AB),
    ])
    # Left Arm (u=32, v=48)
    stone_gradient(px, 36, 48, 4, 4, rng, 50)   # top
    stone_gradient(px, 40, 48, 4, 4, rng, 51)   # bottom
    stone_gradient(px, 32, 52, 4, 12, rng, 52)  # right
    stone_gradient(px, 40, 52, 4, 12, rng, 53)  # left
    stone_gradient(px, 44, 52, 4, 12, rng, 54)  # back
    stone_gradient(px, 36, 52, 4, 12, rng, 55)  # front
    crack_face(px, 36, 52, [
        (1, 1, AM), (2, 2, AB), (1, 4, AS_), (2, 5, AB),
        (1, 7, AB), (2, 8, AM), (1, 10, AS_),
    ])

    return img


def make_preview(img, scales=(1, 4, 8)):
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

    img = make_colossus_texture()
    out_path = ASSETS / "entity" / "prismium_colossus.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_colossus.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
