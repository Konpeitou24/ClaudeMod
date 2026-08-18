#!/usr/bin/env python3
"""Shared LabPBR specular-map ("_s.png") generation for ClaudeMod's block
textures (interactive session, same day as the Chiseled Prismium
Block/Core art adoptions).

Background: shaderpacks (Iris/Oculus on Forge) only render reflections
for a texture if a matching "<name>_s.png" specular map exists next to
the base color texture. Vanilla blocks like Diamond Block get shader
reflections either because some shaderpacks hardcode them, or because
the shader's own heuristics reward very bright/saturated vanilla
textures - our textures never get any of that, so Prismium blocks read
as completely matte in shaders even though the art itself looks like
polished crystal/metal. Confirmed against the LabPBR spec (WebSearch,
shaderlabs.org/wiki/LabPBR_Material_Standard and
shaders.properties/current/how-to/pbr_standards) rather than assumed
from memory, since this is a numeric/binary format where guessing wrong
channel order would silently produce garbage results in-shader.

LabPBR specular texture channel layout (v1.3, current):
  R = perceptual smoothness (0 rough - 255 mirror-smooth)
  G = 0-229 dielectric F0/reflectance (linear), 230-255 = fixed metals
      (230=Iron, 231=Gold, ... 255=albedo-tinted generic metal)
  B = 0-64 porosity, 65-255 subsurface scattering (both linear)
  A = emissive, 0 (no glow) - 254 (max glow); 255 is a special "ignore"
      value reserved for plain RGB images with no real alpha, so real
      data must stay in the 0-254 range even at "no glow" (use 0, not
      255, for "not emissive").

This module classifies each opaque pixel of an existing base-color PNG
by hue/saturation/value into one of a handful of "material" buckets
(crystal body, magenta gem, metal casing, dark mortar/outline, warm
accent, fallback) and assigns smoothness/F0/porosity accordingly, then
layers in emissive strength driven by the block's real lightLevel from
ModBlocks.java (see LIGHT_LEVELS in gen_specular_maps.py) so glow
brightness in-shader is grounded in actual game data rather than
guessed - only pixels bright enough to plausibly be the block's "light
source" motif (its hilite/core-white accents) receive emissive, not the
whole face.

Deliberately does NOT generate normal maps ("_n.png") this session:
LabPBR only requires smoothness+F0 to be "ready", normal maps encode
real bump/height data that would need to be hand-authored per texture
to look right (a flat/fake normal map adds file-count risk for zero
visual benefit over having no normal map at all, since shaders fall
back to a flat normal when the file is absent). Left as documented
future work in PROGRESS.md.
"""
import colorsys
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS = REPO_ROOT / "src/main/resources/assets/claudemod/textures"

# LabPBR green-channel codes for the fixed metal presets we use.
METAL_IRON = 230
METAL_GOLD = 231

# Non-metal (dielectric) F0 values, 0-229 range, linear.
F0_MATTE = 12
F0_CRYSTAL = 45
F0_GEM = 110
F0_HILITE_GLASS = 70


def _bucket(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)

    if v < 0.28:
        # Dark outline / mortar / recessed shadow lines.
        return dict(smoothness=55, g_channel=F0_MATTE, porosity=22)

    if s < 0.15:
        if v > 0.85:
            # Near-white hilite/core accents - polished glass highlight,
            # and the main candidate for emissive (see apply_emissive).
            return dict(smoothness=250, g_channel=F0_HILITE_GLASS, porosity=0)
        # Grey, not-too-bright - the machine family's steel casing.
        return dict(smoothness=185, g_channel=METAL_IRON, porosity=0)

    # Magenta/violet gem accents (PRISMIUM_ACCENT family): hue ~0.78-0.92.
    if 0.74 <= h <= 0.95 and s > 0.25:
        return dict(smoothness=240, g_channel=F0_GEM, porosity=0)

    # Amber/gold accents (Restorer's cross glyph): hue ~0.09-0.16.
    if 0.08 <= h <= 0.17 and s > 0.35 and v > 0.35:
        return dict(smoothness=210, g_channel=METAL_GOLD, porosity=0)

    # Blood-red accents (Wardstone rune): hue ~0.95-1.0 or 0.0-0.04.
    if (h >= 0.96 or h <= 0.04) and s > 0.35 and v > 0.25:
        return dict(smoothness=225, g_channel=F0_GEM, porosity=0)

    # Teal/cyan crystal body (PRISMIUM_BASE/MID/HILITE family):
    # hue ~0.40-0.58 covers this mod's teal-cyan range.
    if 0.38 <= h <= 0.60 and s > 0.20:
        return dict(smoothness=205, g_channel=F0_CRYSTAL, porosity=0)

    # Fallback for anything unclassified (kept deliberately conservative
    # - a matte-ish dielectric rather than guessing metal or gem).
    return dict(smoothness=140, g_channel=F0_MATTE + 15, porosity=5)


def generate_specular_map(color_img, light_level):
    """Build a LabPBR specular map from a base-color RGBA image.

    light_level: the block's real in-game light level (0-15, from
    ModBlocks.java's .lightLevel(...) - 0 for blocks/states that don't
    glow at all, in which case the output alpha channel is all 0).
    """
    img = color_img.convert("RGBA")
    w, h = img.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    src_px = img.load()
    dst_px = out.load()

    glow_scale = light_level / 15.0

    for y in range(h):
        for x in range(w):
            r, g, b, a = src_px[x, y]
            if a == 0:
                dst_px[x, y] = (0, 0, 0, 0)
                continue

            mat = _bucket(r, g, b)

            emissive = 0
            if glow_scale > 0:
                h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
                # Only the near-white/desaturated "hilite" bucket counts as
                # the block's literal light-source motif - bright but
                # *saturated* pixels (magenta gems, amber glyphs, red
                # runes) are colored decoration, not the glow itself, even
                # though they can have an equally high V in HSV. Without
                # this saturation gate every corner gem lit up too (caught
                # during self-review: a false-color emissive-channel
                # preview showed the magenta corner clusters glowing on
                # prismium_core.png, which looked wrong once actually
                # visualized).
                if s < 0.15 and v > 0.75:
                    brightness_factor = min(1.0, (v - 0.75) / 0.25)
                    emissive = int(round(254 * glow_scale * brightness_factor))
                    emissive = max(0, min(254, emissive))

            dst_px[x, y] = (mat["smoothness"], mat["g_channel"], mat["porosity"], emissive)

    return out


def save_specular_map(color_png_path, light_level):
    color_img = Image.open(color_png_path)
    spec_img = generate_specular_map(color_img, light_level)
    out_path = color_png_path.with_name(color_png_path.stem + "_s.png")
    spec_img.save(out_path)
    return out_path
