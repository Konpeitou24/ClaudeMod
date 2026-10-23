#!/usr/bin/env python3
"""Generate the entity texture for the Prismium Mountain Colossus (scheduled
session, 2026-10-23), the overworld-mountain-biome palette variant of
PrismiumColossusEntity added to close out PROGRESS.md TODO53's final
remaining piece ("各バイオームに固有のボス"). Reuses the exact same Java
entity class (PrismiumColossusEntity) and the exact same HumanoidModel/
ModelLayers.ZOMBIE-baked renderer as the original Prism Realm boss - only a
second EntityType registration (ModEntities.PRISMIUM_MOUNTAIN_COLOSSUS) and
this distinct texture differ, mirroring the "same class, second registration,
different material/texture" pattern v0.83.0 already proved out for
PrismiumDungeonFeature/PrismiumVaultFeature (see ModFeatures javadoc).

Layout: identical 64x64 "Steve-style" humanoid skin UV map as
gen_prismium_colossus.py (same belt-and-braces mirrored-arm/leg painting,
for the same reason - correct regardless of whether the baked model reads
mirrored or distinct left-side UVs).

Design: where the original Colossus reads as "near-black corrupted stone
laced with violet void-crystal", this mountain variant reads as "weathered
granite/mossy stone laced with earthy moss-green crystal" - the same
weathered vanilla stone-brick palette (mossy_stone_bricks/
cracked_stone_bricks/stone_bricks) the mountain dungeon/vault rooms
themselves already use (v0.83.0), so the boss now visually matches the room
it stands in instead of looking like a displaced Prism Realm creature. A
sliver of the original's cyan PRISMIUM_BASE accent is kept (same as the
original keeping a "still loyal to the ore" cyan vein) so the two variants
still read as kin, not unrelated monsters.

Self-review: writes a 4x/8x upscaled preview sheet to
build/preview_prismium_mountain_colossus.png for Read-based visual
inspection (same workflow every prior mob texture in this mod follows),
and prints the set of distinct alpha values present (must be only
{0, 255}). Deterministic (fixed seed, distinct from the original Colossus
script's seed so the "random" fleck placement differs rather than
silently reproducing the exact same noise). Run from repo root:
    python3 scripts/textures/gen_prismium_mountain_colossus.py
"""
import random
from pathlib import Path

from PIL import Image

SEED = 20261023
W, H = 64, 64

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"
BUILD_DIR = REPO_ROOT / "build"

# Weathered granite/mossy-stone body palette - lighter and warmer/greyer
# than the original Colossus's near-black "corrupted stone" family, chosen
# to sit visually alongside mossy_stone_bricks/stone_bricks/
# cracked_stone_bricks (the mountain dungeon/vault's own material palette,
# see PrismiumDungeonFeature/PrismiumVaultFeature v0.83.0 javadoc).
FRAME_OUTLINE = "#23221E"
FRAME_SHADOW = "#454339"
FRAME_BASE = "#6E6C5E"
FRAME_HILITE = "#908D7B"

# Earthy moss-green "living stone" crystal palette, replacing the original's
# violet PRISMIUM_ACCENT family as the dominant crack/glow color - a mossy,
# overgrown-ruin read rather than a void-corrupted one.
ACCENT_OUTLINE = "#132008"
ACCENT_SHADOW = "#2C4A1A"
ACCENT_BASE = "#6FA83F"
ACCENT_MID = "#A8D873"
ACCENT_HILITE = "#E3FFC2"
EYE_WHITE = "#F3FFE3"

# A sliver of the mod's cyan PRISMIUM_BASE family for a few "still loyal
# to the ore" veins low on the body, same role as in the original texture,
# so the two variants still read as kin.
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
    """Same technique as gen_prismium_colossus.py's stone_gradient: mostly
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

    # Jagged moss-green "crown" of crystal shards across the top-of-head
    # quad (same layout as the original's violet crown, recolored).
    crown_x, crown_y = 8, 0
    crack_face(px, crown_x, crown_y, [
        (0, 2, AB), (1, 1, AB), (1, 2, AM), (2, 0, AB), (2, 1, AM),
        (3, 2, AS_), (4, 2, AS_), (5, 0, AB), (5, 1, AM), (6, 1, AB),
        (6, 2, AM), (7, 2, AB),
    ])

    # Face details: glowing moss-green eyes, a crack running down from the
    # brow, and a jagged "fanged" mouth line (same layout as the original).
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
    # Wide moss-green crack network dominating the chest, plus a bright
    # core gem low on the chest (same layout as the original, recolored).
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
    # bright core gem near the bottom of the chest crack network
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
    out_path = ASSETS / "entity" / "prismium_mountain_colossus.png"
    img.save(out_path)
    print(f"Wrote {out_path}")

    preview = make_preview(img)
    preview_path = BUILD_DIR / "preview_prismium_mountain_colossus.png"
    preview.save(preview_path)
    print(f"Wrote {preview_path}")

    alphas = set(img.getdata(3))
    print(f"Distinct alpha values present: {sorted(alphas)}")


if __name__ == "__main__":
    main()
