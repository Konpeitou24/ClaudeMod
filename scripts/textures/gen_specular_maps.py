#!/usr/bin/env python3
"""Generate LabPBR "_s.png" specular maps for every ClaudeMod block
texture, so shaderpacks (Iris/Oculus) render Prismium blocks with
reflections instead of flat matte surfaces (user report, interactive
session: Diamond Block reflects under shaders, Prismium blocks don't).

LIGHT_LEVELS below is copied by hand from each block's real
.lightLevel(...) call in ModBlocks.java (checked at the time of writing
- if a block's light level changes later, this dict needs updating to
match, or emissive strength in the generated specular maps will drift
out of sync with the actual game behavior). 0 means "never glows" (the
generated specular map's alpha channel is all 0 for that texture) -
this covers the *_unlit variant of the three toggleable blocks
(Generator/Pylon/Wardstone), which use a separate PNG for their LIT
state that has its own non-zero entry below.

Run from repo root: python3 scripts/textures/gen_specular_maps.py
"""
from pathlib import Path

from pbr_common import ASSETS, save_specular_map

LIGHT_LEVELS = {
    "prismium_ore.png": 3,
    "deepslate_prismium_ore.png": 3,
    "prismium_block.png": 6,
    "chiseled_prismium_block.png": 6,
    "prismium_core.png": 10,
    "chiseled_prismium_core.png": 10,
    "prismium_lantern.png": 15,
    "prismium_cell.png": 5,
    "prismium_generator.png": 0,       # unlit state
    "prismium_generator_lit.png": 8,
    "prismium_cable.png": 4,
    "prismium_bloom.png": 5,
    "prismium_spike.png": 7,
    "prism_lily.png": 3,
    "prism_bramble.png": 2,
    "prism_vine.png": 1,
    "prismium_pylon.png": 0,           # unlit state
    "prismium_pylon_lit.png": 9,
    "prismium_wardstone.png": 0,       # unlit state
    "prismium_wardstone_lit.png": 10,
    "prismium_restorer.png": 0,        # never glows (no LIT state at all)
    "prismium_soil.png": 0,            # ground block, never glows
    "prismium_snare.png": 0,           # camouflage plant - deliberately never glows (see class doc)
    "prismium_snare_triggered.png": 0,
    "prismium_geyser.png": 5,
    "prismium_pulverizer.png": 0,       # unlit state
    "prismium_pulverizer_lit.png": 8,
    "prismium_smelter.png": 0,          # unlit state
    "prismium_smelter_lit.png": 8,
    "prismium_compressor.png": 0,       # unlit state
    "prismium_compressor_lit.png": 8,
    "prismium_stone.png": 0,           # plain worldgen stone variant, never glows
    "prismium_deepstone.png": 0,       # plain worldgen deepslate variant, never glows
    "prismium_alloy_block.png": 6,
    "prismium_portal.png": 11,
    "prismium_chronoflame.png": 14,
    "prismium_chronoflame_top.png": 14,
    "pale_prismium_block.png": 8,
    "pale_prismium_lantern.png": 15,
}


def main():
    block_dir = ASSETS / "block"
    missing = []
    written = []
    for filename, light_level in LIGHT_LEVELS.items():
        path = block_dir / filename
        if not path.exists():
            missing.append(filename)
            continue
        out_path = save_specular_map(path, light_level)
        written.append(out_path.relative_to(Path.cwd()) if out_path.is_relative_to(Path.cwd()) else out_path.name)

    print(f"wrote {len(written)} specular maps")
    for w in written:
        print(f"  {w}")
    if missing:
        print(f"WARNING: {len(missing)} textures in LIGHT_LEVELS not found on disk: {missing}")

    # Sanity check: every block/*.png that ISN'T in LIGHT_LEVELS (and
    # isn't already a generated _s.png) should be flagged so nothing
    # silently gets skipped.
    all_pngs = {p.name for p in block_dir.glob("*.png") if not p.stem.endswith("_s")}
    unaccounted = all_pngs - set(LIGHT_LEVELS.keys())
    if unaccounted:
        print(f"NOTE: block textures with no specular map generated (not in LIGHT_LEVELS): {sorted(unaccounted)}")


if __name__ == "__main__":
    main()
