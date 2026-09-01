package com.claudemod.worldgen.feature;

import com.claudemod.registry.ModBlocks;
import com.claudemod.worldgen.noise.FractalNoise;
import com.claudemod.worldgen.noise.PerlinNoise;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Worldgen feature that adds gentle height variation to the Prism Realm
 * seafloor (PROGRESS.md TODO/section-5 item: "Prism Realmの海底が完全に
 * 平らで不自然").
 *
 * Like {@link PrismiumStoneTransitionFeature}, this exists because the
 * Prism Realm dimension uses a {@code minecraft:flat} chunk generator
 * (data/claudemod/dimension/prism_realm.json): the layers list places
 * claudemod:prismium_soil as a perfectly flat single-block plane at
 * y=SEAFLOOR_Y, with water filling everything above it up to the surface.
 * There is no density function/surface_rule to hook into for a flat
 * generator, so - the same technique as the stone/deepstone boundary
 * feature - this repaints the plane after the fact using coherent noise:
 * for every column in the chunk, a 2D {@link FractalNoise} sample decides
 * a small vertical offset (roughly -2..+2 blocks) for that column, then
 * either piles extra prismium_soil up into the water column (raising the
 * floor into a mound) or carves soil/stone away into water (lowering the
 * floor into a shallow depression, exposing prismium_stone underneath -
 * which reads as natural seafloor material variety, not just a height
 * change).
 *
 * Registered via the biome modifier at
 * data/claudemod/forge/biome_modifier/add_prismium_seafloor.json with
 * "step": "raw_generation" - the same step as the stone/deepstone
 * transition feature (their Y ranges don't overlap: that one operates
 * within +-6 blocks of y=0, this one within +-3 blocks of y=SEAFLOOR_Y=40,
 * see the dimension JSON's layer heights) and, importantly, BEFORE the
 * "underground_ores" step. Prismium ore's placed-feature height range
 * caps at y=40 (absolute) already, so ore placement naturally only ever
 * targets actual stone/deepstone blocks; running this feature first just
 * means a handful of columns that get carved down to water lose a little
 * of their topmost ore-eligible stone, which is a minor, acceptable and
 * realistic side effect (no ore ends up floating in open water).
 *
 * UNVERIFIED: like every other worldgen change in this mod (see
 * PROGRESS.md section 4), this compiles against the 1.20.1 Forge API as
 * best as can be checked without a local build/game client, but has not
 * been run in an actual game client. The datapack validation step in CI
 * (world generation + region-file ore scan) does exercise this feature's
 * code path indirectly, but cannot visually confirm the terrain looks
 * right.
 */
public class PrismiumSeafloorFeature extends Feature<NoneFeatureConfiguration> {

    /** The Y level of the originally-flat prismium_soil layer (see dimension/prism_realm.json). */
    private static final int SEAFLOOR_Y = 40;

    /** Maximum blocks the floor can rise or sink from SEAFLOOR_Y. */
    private static final int AMPLITUDE = 2;

    /** How far above/below SEAFLOOR_Y this feature re-evaluates blocks. */
    private static final int BAND_RADIUS = AMPLITUDE + 1;

    /** Noise-space scale; smaller = broader, gentler mounds/depressions. */
    private static final double NOISE_FREQUENCY = 0.045;

    private static final int NOISE_OCTAVES = 2;
    private static final double NOISE_PERSISTENCE = 0.5;
    private static final double NOISE_LACUNARITY = 2.0;

    /**
     * Fixed salt XORed into the world seed so this feature's noise field
     * doesn't line up with the stone/deepstone transition's noise field
     * (which uses a different salt) or any other noise source that might
     * reuse the same seed elsewhere in the mod.
     */
    private static final long NOISE_SEED_SALT = 0x5345415F464C4F52L;

    public PrismiumSeafloorFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        FractalNoise noise = new FractalNoise(
                new PerlinNoise(level.getSeed() ^ NOISE_SEED_SALT),
                NOISE_OCTAVES, NOISE_PERSISTENCE, NOISE_LACUNARITY);

        BlockState soil = ModBlocks.PRISMIUM_SOIL.get().defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        int chunkOriginX = origin.getX();
        int chunkOriginZ = origin.getZ();
        boolean changedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkOriginX + dx;
                int z = chunkOriginZ + dz;

                double sample = noise.sample(x * NOISE_FREQUENCY, z * NOISE_FREQUENCY);
                int offset = (int) Math.round(sample * AMPLITUDE);
                if (offset == 0) {
                    continue;
                }

                int targetTop = SEAFLOOR_Y + offset;

                if (offset > 0) {
                    // Raise the floor: pile soil up into what was water.
                    for (int y = SEAFLOOR_Y + 1; y <= targetTop; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState current = level.getBlockState(pos);
                        if (current.is(Blocks.WATER) || current.isAir()) {
                            level.setBlock(pos, soil, 2);
                            changedAny = true;
                        }
                    }
                } else {
                    // Lower the floor: carve soil/stone away into water,
                    // exposing whatever was underneath as the new floor.
                    for (int y = targetTop + 1; y <= SEAFLOOR_Y; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState current = level.getBlockState(pos);
                        boolean isSoil = current.is(ModBlocks.PRISMIUM_SOIL.get());
                        boolean isStone = current.is(ModBlocks.PRISMIUM_STONE.get());
                        if (isSoil || isStone) {
                            level.setBlock(pos, water, 2);
                            changedAny = true;
                        }
                    }
                }
            }
        }

        return changedAny;
    }
}
