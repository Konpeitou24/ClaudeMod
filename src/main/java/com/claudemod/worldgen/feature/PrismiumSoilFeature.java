package com.claudemod.worldgen.feature;

import com.claudemod.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Worldgen feature for Prismium Soil (scheduled session #45).
 *
 * Addresses PROGRESS.md section 5 item 10-a: Prism Realm has had its own
 * biome colors (session 39), boosted ore/crystal density (session 41),
 * and three exclusive plants (sessions 40/43/44), but its ground itself
 * was still literally overworld grass_block/dirt/stone the entire time,
 * because dimension/prism_realm.json reuses "minecraft:overworld" noise
 * settings verbatim.
 *
 * The "correct" long-term fix would be a dedicated noise_settings JSON
 * with a Prism-Realm-specific surface_rule, but replicating that file
 * exactly for 1.20.1 (density function references, aquifer config, the
 * ~150+ line surface_rule tree) could not be verified against this
 * exact game version from this sandbox (no local build/game client -
 * see PROGRESS.md for the research trail through misode/mcmeta) and one
 * malformed field risks silently breaking or crashing the dimension's
 * terrain generation entirely. That risk was judged too high for an
 * unverifiable change.
 *
 * Instead, this feature runs as an ordinary decoration-step worldgen
 * feature (the same mechanism already proven safe and working for Prism
 * Lily/Bramble/Vine via forge:add_features biome modifiers) and simply
 * re-paints the terrain the overworld generator already produced: for
 * every column in the chunk, if the topmost solid block is grass_block,
 * dirt, or coarse_dirt, it is replaced with claudemod:prismium_soil.
 * Registered via the biome_modifier at
 * data/claudemod/forge/biome_modifier/add_prismium_soil.json with
 * "step": "local_modifications", i.e. BEFORE "vegetal_decoration" (used
 * by the three plants), so by the time Prism Lily/Bramble/Vine try to
 * place, the ground beneath them is already Prismium Soil - no risk of
 * a plant popping off from a canSurvive() check firing against soil that
 * gets swapped out from under it later. (Moot in practice anyway: all
 * three plants' canSurvive() only checks isFaceSturdy() on the block
 * below, not a specific whitelist, so they would have survived either
 * placement order - see PrismLilyBlock.java - but ordering it this way
 * is still the more correct/robust choice.)
 *
 * Uses no placement modifiers beyond a plain biome filter (this feature
 * is not "count on X per chunk" - it deterministically visits every one
 * of the chunk's 256 columns itself), so the position passed into
 * place() is expected to be the chunk's unmodified starting corner, as
 * given to the first entry of a placement chain by the decoration
 * system. FeaturePlaceContext.origin() is used directly as that corner
 * and NOT re-aligned to a 16-multiple, on the assumption the decoration
 * system already hands us a chunk-aligned position when no repositioning
 * placement modifier (in_square, count, etc.) is present.
 *
 * UNVERIFIED: like every other worldgen change in this mod (see
 * PROGRESS.md section 4), this compiles against the 1.20.1 Forge API as
 * best as can be checked without a local build, but has not been run in
 * an actual game client. In particular the assumption above about
 * context.origin() being exactly the chunk corner when the placement
 * chain has no repositioning modifier is based on general knowledge of
 * NoiseBasedChunkGenerator's decoration pipeline, not a source-verified
 * read of the 1.20.1 code in this sandbox - if the assumption is wrong,
 * the likely failure mode is columns being missed/duplicated across
 * chunk boundaries (cosmetic patchiness), not a crash, since every block
 * read/write here is bounds-checked by the level itself.
 */
public class PrismiumSoilFeature extends Feature<NoneFeatureConfiguration> {

    public PrismiumSoilFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockState soil = ModBlocks.PRISMIUM_SOIL.get().defaultBlockState();

        int chunkOriginX = origin.getX();
        int chunkOriginZ = origin.getZ();
        boolean changedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkOriginX + dx;
                int z = chunkOriginZ + dz;
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos pos = new BlockPos(x, topY, z);
                BlockState current = level.getBlockState(pos);
                if (current.is(Blocks.GRASS_BLOCK) || current.is(Blocks.DIRT) || current.is(Blocks.COARSE_DIRT)) {
                    level.setBlock(pos, soil, 2);
                    changedAny = true;
                }
            }
        }

        return changedAny;
    }
}
