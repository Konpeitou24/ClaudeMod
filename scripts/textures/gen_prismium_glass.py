"""Generate prismium_glass.png / pale_prismium_glass.png (scheduled
session, 2026-10-29).

This mod's first ordinary translucent full-cube block texture (see
ModBlocks.PRISMIUM_GLASS / PALE_PRISMIUM_GLASS for the full design
rationale). Hand-drawn 16x16 pixel art, flat/limited-palette shading in
line with every other texture in this mod (no gradients/blur, crisp
nearest-neighbor pixels), with an RGBA alpha channel so the block reads
as translucent once ClientModEvents registers RenderType.translucent()
for it.

Palette:
  - Prismium Glass: cyan/teal, matching PRISMIUM_BLOCK's own texture hue
    (sampled: dominant colors cluster around RGB (100-200, 245-255,
    227-247), i.e. a bright teal).
  - Pale Prismium Glass: icy pale blue/white, matching PALE_PRISMIUM_BLOCK's
    own texture hue (sampled: dominant colors cluster around RGB
    (159-205, 211-235, 238-250)).

Both textures share the same simple composition (same "pane" silhouette
every vanilla glass variant reuses regardless of color):
  - A 1px frame around the edge, more saturated/opaque than the fill, so
    tiled glass blocks still read as individual panes (mirrors vanilla's
    own glass.png, which also darkens its outer ring slightly).
  - A mostly-uniform translucent fill in the middle.
  - A single diagonal "crystal shard" highlight streak (brighter, more
    opaque) running corner-to-corner, echoing the shard/vein motif this
    mod's other Prismium crystal textures (geode cluster, stalactite)
    already use, so the glass doesn't read as generic off-the-shelf
    stained glass.
"""

from PIL import Image

SIZE = 16


def make_glass(fill_rgba, frame_rgba, shine_rgba, out_path):
    im = Image.new("RGBA", (SIZE, SIZE), fill_rgba)
    px = im.load()

    # 1px frame around the whole pane.
    for x in range(SIZE):
        for y in range(SIZE):
            if x == 0 or x == SIZE - 1 or y == 0 or y == SIZE - 1:
                px[x, y] = frame_rgba

    # Diagonal crystal-shard highlight: a 2px-wide streak from the
    # upper-left area to the lower-right area, staying clear of the frame.
    for x in range(2, SIZE - 2):
        y1 = x
        y2 = x + 1
        if 1 <= y1 <= SIZE - 2:
            px[x, y1] = shine_rgba
        if 1 <= y2 <= SIZE - 2:
            px[x, y2] = shine_rgba

    # Small bright sparkle accent near the top-left corner of the shine.
    for (sx, sy) in [(3, 2), (4, 2), (3, 3)]:
        px[sx, sy] = (
            min(255, shine_rgba[0] + 20),
            min(255, shine_rgba[1] + 10),
            min(255, shine_rgba[2] + 5),
            min(255, shine_rgba[3] + 40),
        )

    im.save(out_path)
    print("wrote", out_path, im.size, im.mode)


if __name__ == "__main__":
    # Prismium Glass: bright cyan/teal, alpha kept low-ish so it reads as
    # genuinely see-through rather than a solid tinted block.
    make_glass(
        fill_rgba=(70, 200, 190, 95),
        frame_rgba=(15, 120, 115, 190),
        shine_rgba=(190, 255, 245, 190),
        out_path="src/main/resources/assets/claudemod/textures/block/prismium_glass.png",
    )

    # Pale Prismium Glass: icy pale blue/near-white, slightly higher alpha
    # on the fill than the regular variant (frostier/thicker-looking glass)
    # but still clearly translucent.
    make_glass(
        fill_rgba=(190, 220, 240, 115),
        frame_rgba=(95, 150, 190, 200),
        shine_rgba=(245, 252, 255, 210),
        out_path="src/main/resources/assets/claudemod/textures/block/pale_prismium_glass.png",
    )
