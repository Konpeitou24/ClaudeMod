package com.claudemod.worldgen.feature;

import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModEntities;
import com.claudemod.entity.PrismiumColossusEntity;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Worldgen feature: a small, rare "ruin" room built on Prism Realm dry
 * land (see {@link PrismiumLandFeature}), with a {@link
 * PrismiumColossusEntity} (the mod's first boss, see that class's
 * javadoc) waiting inside - PROGRESS.md TODO9's "各バイオーム固有のボス
 * を伴うダンジョン" roadmap item, opening with the land/mountain-adjacent
 * version this mod has (direct request, 2026-10-17 live chat: "次はダン
 * ジョンを陸地に稀に作ってボスを出すとこまで").
 *
 * <p><b>Deliberately NOT a vanilla jigsaw/NBT structure</b>. This mod has
 * never generated a multi-block structure before, and the vanilla
 * {@code Structure}/{@code StructurePiece}/jigsaw pipeline (Codecs,
 * {@code StructureType} registration, template pools, piece processors)
 * is a large, mostly-unexplored API surface this sandbox cannot verify
 * end-to-end without a local build. Instead this reuses the exact
 * mechanism every other ClaudeMod worldgen addition already uses
 * successfully: a plain {@link Feature} that procedurally {@code
 * setBlock}s a room directly, the same technique {@link
 * PrismiumLandFeature} already proved out for "shape the Prism Realm's
 * flat generator by hand". The only genuinely new API surface here is
 * spawning a mob from inside a {@code Feature} (this mod has only ever
 * spawned mobs via spawn eggs/conversion before) - see {@link
 * #spawnBoss} for how that was verified before use.
 *
 * <p><b>Placement</b>: registered via
 * data/claudemod/worldgen/placed_feature/prismium_dungeon_placed.json
 * with a {@code minecraft:rarity_filter} (1-in-250 chunks) +
 * {@code minecraft:in_square} (random position within the chosen chunk)
 * + {@code minecraft:biome} placement modifier chain - the same pattern
 * {@code prismium_ore_placed.json} already uses successfully, just
 * swapping the ore feature's {@code count} decorator for
 * {@code rarity_filter} (this needs "rare, at most one per very many
 * chunks", not "several per chunk"). Wired to the
 * {@code claudemod:prism_realm} biome only (same as the land/Wisp/
 * Familiar biome modifiers) at step {@code vegetal_decoration} - the
 * same step {@code add_prismium_soil.json} already uses successfully,
 * and late enough that {@link PrismiumLandFeature} (step
 * {@code raw_generation}, much earlier) has already finished raising
 * this chunk's land before this feature runs, so the heightmap queries
 * below see the land's final shape.
 *
 * <p><b>Land/flatness check, done entirely inside {@link #place}</b>
 * (not by the placement-modifier chain, which has no "is this land"
 * concept): queries {@code Heightmap.Types.WORLD_SURFACE_WG} - the same
 * heightmap type {@link PrismiumSoilFeature} already uses successfully -
 * at the room's center and all four footprint corners. A column counts
 * as land only if its surface sits at or above {@link #SEA_LEVEL_Y}
 * (63, the same sea-level constant {@link PrismiumLandFeature} uses;
 * redefined locally here rather than imported, matching this mod's
 * existing per-feature-class convention of each feature owning its own
 * constants - see {@link PrismiumSeafloorFeature}/{@link
 * PrismiumStoneTransitionFeature} for the same pattern): since {@link
 * PrismiumLandFeature} only ever raises land to/above sea level and
 * leaves ocean columns' water-filled tops below it, this single Y
 * comparison cleanly distinguishes "dry land" from "still open water"
 * without needing to inspect block types at all (both land and seafloor
 * use {@code claudemod:prismium_soil} as their topmost block, so a
 * block-type check alone would not have worked here). If the center or
 * any corner fails the land/flatness check, {@link #place} aborts
 * (returns {@code false}, placing nothing) rather than half-building a
 * room into open water or a steep slope.
 *
 * <p><b>Room shape</b>: a 9x9 footprint (interior 7x7), 1-block-thick
 * walls/floor/ceiling of {@code claudemod:prismium_deepstone_bricks}
 * (with a scattered {@code cracked_prismium_deepstone_bricks} chance for
 * a weathered-ruin look - plain {@code BlockState} swaps, no new block
 * types), 5 blocks of interior clearance (comfortably taller than the
 * boss's 2.7-tall hitbox), a 3-wide/2-tall doorway carved into the south
 * wall, and a {@code claudemod:prismium_geode_cluster} light source in
 * each of the four interior corners (the exact "future Prism Realm
 * dungeon interior dressing" use case that block's own javadoc already
 * anticipated when it was added). The interior space inside the
 * footprint is explicitly cleared to air before anything else is placed,
 * so pre-existing terrain bumps inside the room's footprint can't leave
 * floating dirt/stone clipped through the walls.
 *
 * <p><b>Unverified</b>: like every other worldgen/entity change in this
 * mod (see PROGRESS.md section 3), this compiles against the 1.20.1
 * Forge API as best as can be checked without a local build/game client,
 * but has not been run in an actual game client. In particular: the
 * actual in-game rarity/findability of a 1-in-250-chunk feature that
 * also requires landing on dry land, whether the room ever generates
 * partially embedded in a hill on a borderline-flat site, and whether
 * the boss spawns cleanly on the floor (rather than, say, inside a wall
 * if the flatness/footprint math has an off-by-one) are all unverified
 * and flagged in PROGRESS.md as needing real playtesting.
 */
public class PrismiumDungeonFeature extends Feature<NoneFeatureConfiguration> {

    /** Sea level in the Prism Realm's flat generator - see {@link PrismiumLandFeature}. */
    private static final int SEA_LEVEL_Y = 63;

    /** Footprint half-size: full room is (2*ROOM_HALF_SIZE + 1) blocks on a side (9x9). */
    private static final int ROOM_HALF_SIZE = 4;

    /** How many blocks of open interior airspace, floor to ceiling. */
    private static final int INTERIOR_HEIGHT = 5;

    /** Max allowed Y difference between the room's center and any footprint corner before aborting. */
    private static final int FLATNESS_TOLERANCE = 3;

    /** Chance (out of 10) that a given wall/floor/ceiling block uses the cracked variant instead. */
    private static final int CRACKED_CHANCE_OUT_OF_TEN = 2;

    public PrismiumDungeonFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int cx = origin.getX();
        int cz = origin.getZ();

        int centerGroundY = surfaceY(level, cx, cz);
        if (centerGroundY < SEA_LEVEL_Y) {
            return false;
        }
        if (!cornersAreFlatLand(level, cx, cz, centerGroundY)) {
            return false;
        }

        int floorY = centerGroundY + 1;
        int ceilingY = floorY + INTERIOR_HEIGHT + 1;

        BlockState bricks = ModBlocks.PRISMIUM_DEEPSTONE_BRICKS.get().defaultBlockState();
        BlockState crackedBricks = ModBlocks.CRACKED_PRISMIUM_DEEPSTONE_BRICKS.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int dx = -ROOM_HALF_SIZE; dx <= ROOM_HALF_SIZE; dx++) {
            for (int dz = -ROOM_HALF_SIZE; dz <= ROOM_HALF_SIZE; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                boolean perimeter = dx == -ROOM_HALF_SIZE || dx == ROOM_HALF_SIZE
                        || dz == -ROOM_HALF_SIZE || dz == ROOM_HALF_SIZE;
                boolean doorwayColumn = dz == ROOM_HALF_SIZE && dx >= -1 && dx <= 1;

                level.setBlock(new BlockPos(x, floorY, z), ruinBlock(random, bricks, crackedBricks), 2);
                level.setBlock(new BlockPos(x, ceilingY, z), ruinBlock(random, bricks, crackedBricks), 2);

                for (int y = floorY + 1; y <= ceilingY - 1; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!perimeter) {
                        level.setBlock(pos, air, 2);
                    } else if (doorwayColumn && y <= floorY + 2) {
                        level.setBlock(pos, air, 2);
                    } else {
                        level.setBlock(pos, ruinBlock(random, bricks, crackedBricks), 2);
                    }
                }
            }
        }

        placeLight(level, cx - 2, floorY + 1, cz - 2);
        placeLight(level, cx + 2, floorY + 1, cz - 2);
        placeLight(level, cx - 2, floorY + 1, cz + 2);
        placeLight(level, cx + 2, floorY + 1, cz + 2);

        spawnBoss(level, cx + 0.5D, floorY + 1, cz + 0.5D);

        return true;
    }

    /** Topmost non-air block's Y, same technique {@link PrismiumSoilFeature} already uses. */
    private static int surfaceY(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    private static boolean cornersAreFlatLand(WorldGenLevel level, int cx, int cz, int centerGroundY) {
        int[][] corners = {
                {cx - ROOM_HALF_SIZE, cz - ROOM_HALF_SIZE},
                {cx + ROOM_HALF_SIZE, cz - ROOM_HALF_SIZE},
                {cx - ROOM_HALF_SIZE, cz + ROOM_HALF_SIZE},
                {cx + ROOM_HALF_SIZE, cz + ROOM_HALF_SIZE},
        };
        for (int[] corner : corners) {
            int cornerY = surfaceY(level, corner[0], corner[1]);
            if (cornerY < SEA_LEVEL_Y) {
                return false;
            }
            if (Math.abs(cornerY - centerGroundY) > FLATNESS_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static BlockState ruinBlock(RandomSource random, BlockState bricks, BlockState crackedBricks) {
        return random.nextInt(10) < CRACKED_CHANCE_OUT_OF_TEN ? crackedBricks : bricks;
    }

    private static void placeLight(WorldGenLevel level, int x, int y, int z) {
        level.setBlock(new BlockPos(x, y, z), ModBlocks.PRISMIUM_GEODE_CLUSTER.get().defaultBlockState(), 2);
    }

    /**
     * Spawns the boss standing on the room's floor. {@code WorldGenLevel}
     * (the level type every {@code Feature#place} receives) extends
     * {@code ServerLevelAccessor}, which declares {@code
     * addFreshEntityWithPassengers(Entity)} (a {@code default void}
     * method) directly - confirmed against two independent mapping
     * sources before use (mappings.dev and the nekoyue ForgeJavaDocs-NG
     * mirror both agree on this exact signature), per this mod's
     * standing "verify an unfamiliar API against more than one source"
     * rule (PROGRESS.md section 1, the 2026-09-28 mappings.dev
     * hallucination lesson) - this is the first time any ClaudeMod
     * worldgen code has spawned an entity directly, so it was treated as
     * new, unverified API surface rather than assumed. {@code
     * Entity#absMoveTo(double, double, double, float, float)} (also
     * confirmed via the same mirror) positions the entity before it is
     * added; yaw/pitch are both left at 0 since the boss is stationary
     * until it notices a player.
     */
    private static void spawnBoss(WorldGenLevel level, double x, double y, double z) {
        // Deliberately does NOT call Mob#finalizeSpawn - that method's
        // exact signature/overload set was not independently verified
        // (unlike everything else in this method), and this mob has
        // nothing to gain from it: AbstractPrismiumMonster's
        // populateDefaultEquipmentSlots is a no-op (see that class's
        // javadoc - no vanilla gear to assign) and this boss's stats
        // come from its registered AttributeSupplier (ModEntityEvents),
        // not from finalizeSpawn's local-difficulty scaling. Skipping an
        // unverified call entirely is safer than guessing its signature.
        PrismiumColossusEntity colossus =
                new PrismiumColossusEntity(ModEntities.PRISMIUM_COLOSSUS.get(), level.getLevel());
        colossus.absMoveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntityWithPassengers(colossus);
    }
}
