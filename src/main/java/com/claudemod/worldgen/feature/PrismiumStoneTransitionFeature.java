package com.claudemod.worldgen.feature;

import com.claudemod.registry.ModBlocks;
import com.claudemod.worldgen.noise.FractalNoise;
import com.claudemod.worldgen.noise.PerlinNoise;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Worldgen feature that scatters the Prismium Stone / Prismium Deepstone
 * boundary in the Prism Realm dimension.
 *
 * Addresses the follow-up comment on GitHub Issue #23: "0付近にはプリズミ
 * ウムの深層岩と、プリズミウムの石の生成がくっきり分かれているように思え
 * ます。ノイズを自作して、まばらに切り替わるようにしてください" (around
 * y=0 the Deepstone/Stone split looks like a hard line; make our own
 * noise so it switches over more scattered).
 *
 * The Prism Realm dimension uses a `minecraft:flat` chunk generator
 * (data/claudemod/dimension/prism_realm.json), so unlike a normal noise-
 * based dimension there is no density function / surface_rule to hook
 * into for this - the layers list places Deepstone up to y=0 and Stone
 * from y=1 up as a perfectly flat plane. This feature re-paints that
 * plane after the fact: for every column in the chunk it walks a
 * vertical band centered on the y=0 boundary and, for each block in that
 * band, combines a straight-line gradient (which side of the boundary it
 * would "normally" be on) with a {@link FractalNoise} sample to decide
 * whether that specific block should be Deepstone or Stone. The result
 * is a wavy, patchy transition instead of a hard cutoff, while blocks
 * far from the boundary are left untouched.
 *
 * The noise itself ({@link PerlinNoise}, combined into
 * {@link FractalNoise}) is written as a general-purpose, reusable
 * utility in com.claudemod.worldgen.noise - it has no knowledge of this
 * feature, this dimension, or these blocks - so it is ready to reuse for
 * other worldgen scattering needs later, such as blending across a
 * biome edge.
 *
 * Registered via the biome modifier at
 * data/claudemod/forge/biome_modifier/add_prismium_stone_transition.json
 * with "step": "raw_generation", i.e. before ore placement
 * (UNDERGROUND_ORES) and before Prismium Soil (LOCAL_MODIFICATIONS), so
 * later features still see the expected Deepstone/Stone family of
 * blocks at their usual heights - this feature only swaps which of the
 * two a given position is, it never removes/changes anything else.
 *
 * UNVERIFIED: like every other worldgen change in this mod (see
 * PROGRESS.md), this compiles against the 1.20.1 Forge API as best as
 * can be checked without a local build, but has not been run in an
 * actual game client.
 */
public class PrismiumStoneTransitionFeature extends Feature<NoneFeatureConfiguration> {

    /** How many blocks above and below the y=0 boundary get re-evaluated. */
    private static final int BAND_RADIUS = 6;

    /** Noise-space scale; smaller = larger, blobbier patches. */
    private static final double NOISE_FREQUENCY = 0.08;

    /** How strongly the noise can push a block across the boundary. */
    private static final double NOISE_WEIGHT = 0.75;

    /** Octaves stacked for the noise field (see FractalNoise). */
    private static final int NOISE_OCTAVES = 3;
    private static final double NOISE_PERSISTENCE = 0.5;
    private static final double NOISE_LACUNARITY = 2.0;

    /**
     * Fixed salt XORed into the world seed so this feature's noise field
     * doesn't line up with any other noise source that might reuse the
     * same seed elsewhere in the mod.
     */
    private static final long NOISE_SEED_SALT = 0x505249534D5F5354L;

    public PrismiumStoneTransitionFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        FractalNoise noise = new FractalNoise(
                new PerlinNoise(level.getSeed() ^ NOISE_SEED_SALT),
                NOISE_OCTAVES, NOISE_PERSISTENCE, NOISE_LACUNARITY);

        BlockState deepstone = ModBlocks.PRISMIUM_DEEPSTONE.get().defaultBlockState();
        BlockState stone = ModBlocks.PRISMIUM_STONE.get().defaultBlockState();

        int chunkOriginX = origin.getX();
        int chunkOriginZ = origin.getZ();
        boolean changedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkOriginX + dx;
                int z = chunkOriginZ + dz;

                for (int y = -BAND_RADIUS; y <= BAND_RADIUS; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState current = level.getBlockState(pos);
                    boolean isDeepstone = current.is(ModBlocks.PRISMIUM_DEEPSTONE.get());
                    boolean isStone = current.is(ModBlocks.PRISMIUM_STONE.get());
                    if (!isDeepstone && !isStone) {
                        // Something else already occupies this position
                        // (e.g. bedrock, or a future feature that ran
                        // earlier) - leave it alone.
                        continue;
                    }

                    double gradient = (double) y / BAND_RADIUS;
                    double sample = noise.sample(x * NOISE_FREQUENCY, y * NOISE_FREQUENCY, z * NOISE_FREQUENCY);
                    boolean shouldBeDeepstone = gradient + sample * NOISE_WEIGHT < 0;

                    BlockState target = shouldBeDeepstone ? deepstone : stone;
                    if (current != target) {
                        level.setBlock(pos, target, 2);
                        changedAny = true;
                    }
                }
            }
        }

        return changedAny;
    }
}
