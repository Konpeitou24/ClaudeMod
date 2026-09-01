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
 * Worldgen feature that adds the Prism Realm's first dry land
 * (PROGRESS.md TODO9, "Prism Realmにまず陸地(平原などの基本地形)を追加
 * する" - GitHub issue history #23/#24 established the dimension should
 * be a normal explorable overworld-alternative, not a lidless fish tank).
 *
 * Like {@link PrismiumSeafloorFeature} and
 * {@link PrismiumStoneTransitionFeature}, this exists because
 * data/claudemod/dimension/prism_realm.json uses a {@code minecraft:flat}
 * chunk generator: the layers list is a perfectly flat plane of
 * claudemod:prismium_soil at y=40 (see {@code PrismiumSeafloorFeature.
 * SEAFLOOR_Y}) with claudemod:prismium_stone/deepstone below and 22
 * layers of water above (y=41..62, i.e. sea level sits at y=63, the
 * first air block) filling the rest of the column up to the surface.
 * There is no density function/surface_rule to hook into for a flat
 * generator, so - the same technique used by the other two features
 * above - this repaints columns after the fact: a broad, low-frequency
 * {@link FractalNoise} field decides, per column, whether that column is
 * "land" (above a threshold) and, if so, how high above sea level it
 * rises (a second, higher-frequency/low-amplitude noise layer adds gentle
 * plains-style undulation on top of that so land isn't perfectly flat
 * either). Land columns get their entire water column (y=41..62) plus
 * however much air above sea level is needed replaced with
 * claudemod:prismium_stone, capped with a few blocks of
 * claudemod:prismium_soil "topsoil" near the surface - mirroring how
 * vanilla plains terrain is mostly stone with a thin dirt/grass cap,
 * rather than solid soil all the way down. Columns that stay below the
 * threshold are left completely untouched (still open water over the
 * seafloor), so existing seafloor terrain/plants/mobs are unaffected.
 *
 * Registered via the biome modifier at
 * data/claudemod/forge/biome_modifier/add_prismium_land.json with
 * "step": "raw_generation" - the same step as the seafloor/stone-
 * transition features. This feature only ever writes to y=41 and above,
 * comfortably clear of the underground_ores step's placed-feature height
 * cap (y=40 absolute, see PrismiumSeafloorFeature's class doc) and of
 * the stone/deepstone transition feature's band (+-6 blocks around
 * y=0), so ore placement and that boundary are both unaffected by this
 * feature running in the same step. It can very slightly overlap the
 * seafloor mound/depression feature's band (+-2 blocks around y=40, i.e.
 * up to y=42) on land columns right at the threshold - in that case this
 * feature's fill (which starts at y=41 unconditionally) simply wins for
 * that column, which is fine: a rare seafloor bump that happens to sit
 * exactly at a coastline is not a meaningful loss.
 *
 * UNVERIFIED: like every other worldgen change in this mod (see
 * PROGRESS.md section 3), this compiles against the 1.20.1 Forge API as
 * best as can be checked without a local build/game client, but has not
 * been run in an actual game client. In particular the resulting land/
 * ocean ratio, the shape/scale of the landmasses, and how natural the
 * plains undulation looks are all unverified and may need tuning
 * (LAND_THRESHOLD, LAND_NOISE_FREQUENCY, MAX_LAND_HEIGHT below) once
 * someone can actually fly around in-game.
 */
public class PrismiumLandFeature extends Feature<NoneFeatureConfiguration> {

    /** The Y level of the flat seafloor/soil layer (see dimension/prism_realm.json). */
    private static final int SEAFLOOR_Y = 40;

    /** First air block above the water layer, i.e. sea level. */
    private static final int SEA_LEVEL_Y = 63;

    /** Lowest Y this feature ever touches - just above the flat soil layer. */
    private static final int FILL_START_Y = SEAFLOOR_Y + 1;

    /** How many of the topmost land blocks are prismium_soil "topsoil" rather than prismium_stone. */
    private static final int TOPSOIL_DEPTH = 3;

    /**
     * Broad noise frequency deciding land/ocean placement. Small = large,
     * continent/island-scale shapes rather than a speckled/noisy coastline.
     */
    private static final double LAND_NOISE_FREQUENCY = 0.006;

    private static final int LAND_NOISE_OCTAVES = 3;
    private static final double LAND_NOISE_PERSISTENCE = 0.5;
    private static final double LAND_NOISE_LACUNARITY = 2.0;

    /**
     * Noise values (roughly [-1, 1]) above this are land; below are left
     * as open water. Controls the overall land/ocean ratio.
     */
    private static final double LAND_THRESHOLD = 0.28;

    /** How many blocks above sea level the tallest land can rise. */
    private static final int MAX_LAND_HEIGHT = 6;

    /** Finer, low-amplitude noise layered on top of land height for plains-style undulation. */
    private static final double DETAIL_NOISE_FREQUENCY = 0.09;
    private static final int DETAIL_NOISE_OCTAVES = 2;
    private static final double DETAIL_NOISE_PERSISTENCE = 0.5;
    private static final double DETAIL_NOISE_LACUNARITY = 2.0;
    private static final double DETAIL_AMPLITUDE = 1.5;

    /**
     * Fixed salts XORed into the world seed so this feature's noise
     * fields don't line up with the seafloor/stone-transition features'
     * fields (each of which uses its own distinct salt) or with each
     * other.
     */
    private static final long LAND_NOISE_SEED_SALT = 0x4C414E445F53484CL;
    private static final long DETAIL_NOISE_SEED_SALT = 0x504C41494E535F44L;

    public PrismiumLandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        FractalNoise landNoise = new FractalNoise(
                new PerlinNoise(level.getSeed() ^ LAND_NOISE_SEED_SALT),
                LAND_NOISE_OCTAVES, LAND_NOISE_PERSISTENCE, LAND_NOISE_LACUNARITY);
        FractalNoise detailNoise = new FractalNoise(
                new PerlinNoise(level.getSeed() ^ DETAIL_NOISE_SEED_SALT),
                DETAIL_NOISE_OCTAVES, DETAIL_NOISE_PERSISTENCE, DETAIL_NOISE_LACUNARITY);

        BlockState soil = ModBlocks.PRISMIUM_SOIL.get().defaultBlockState();
        BlockState stone = ModBlocks.PRISMIUM_STONE.get().defaultBlockState();

        int chunkOriginX = origin.getX();
        int chunkOriginZ = origin.getZ();
        boolean changedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkOriginX + dx;
                int z = chunkOriginZ + dz;

                double sample = landNoise.sample(x * LAND_NOISE_FREQUENCY, z * LAND_NOISE_FREQUENCY);
                if (sample <= LAND_THRESHOLD) {
                    continue;
                }

                double t = (sample - LAND_THRESHOLD) / (1.0 - LAND_THRESHOLD);
                t = Math.max(0.0, Math.min(1.0, t));

                double detail = detailNoise.sample(x * DETAIL_NOISE_FREQUENCY, z * DETAIL_NOISE_FREQUENCY);
                int heightAboveSea = (int) Math.round(t * MAX_LAND_HEIGHT + detail * DETAIL_AMPLITUDE);
                heightAboveSea = Math.max(0, heightAboveSea);

                int landTop = SEA_LEVEL_Y + heightAboveSea;
                int topsoilStart = Math.max(FILL_START_Y, landTop - TOPSOIL_DEPTH + 1);

                for (int y = FILL_START_Y; y <= landTop; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState target = y >= topsoilStart ? soil : stone;
                    level.setBlock(pos, target, 2);
                }
                changedAny = true;
            }
        }

        return changedAny;
    }
}
