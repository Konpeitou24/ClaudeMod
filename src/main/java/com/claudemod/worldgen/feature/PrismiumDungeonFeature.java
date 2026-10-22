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

import java.util.function.Supplier;

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
 * with a {@code minecraft:in_square} (random position within the
 * current chunk) + {@code minecraft:biome} placement modifier chain -
 * the same pattern {@code prismium_ore_placed.json} already uses
 * successfully, minus the {@code count}/{@code rarity_filter} decorator
 * those placements use, because this feature now does its own
 * frequency/spacing decision in {@link #place} (see {@link
 * #isDesignatedChunk} below) rather than delegating it to a modifier -
 * {@code place} is therefore invoked once for every chunk of every biome
 * this feature is wired to (cheaply rejecting all but the one
 * designated chunk per grid cell before doing anything else, see below),
 * not just a random 1-in-250 of them as the original v0.78.0 version
 * did. Wired to the {@code claudemod:prism_realm} biome (same as the
 * land/Wisp/Familiar biome modifiers, {@code
 * data/claudemod/forge/biome_modifier/add_prismium_dungeon.json}) at
 * step {@code vegetal_decoration} - the same step {@code
 * add_prismium_soil.json} already uses successfully, and late enough
 * that {@link PrismiumLandFeature} (step {@code raw_generation}, much
 * earlier) has already finished raising this chunk's land before this
 * feature runs, so the heightmap queries below see the land's final
 * shape. <b>Since v0.82.0</b>, also wired via a second biome modifier
 * ({@code add_prismium_dungeon_mountains.json}) to the vanilla {@code
 * #minecraft:is_mountain} biome tag (meadow/frozen_peaks/jagged_peaks/
 * stony_peaks/snowy_slopes/cherry_grove in 1.20.1, confirmed against the
 * actual vanilla tag file before use) - PROGRESS.md TODO9's final
 * "山岳等の他バイオームへの横展開" piece. This class's land/flatness check
 * never inspects block types (only {@code Heightmap.Types.WORLD_SURFACE_WG}
 * Y values against the same sea-level constant vanilla itself uses), and
 * nothing else in {@link #place} reads a Prism-Realm-specific block
 * either, so the exact same compiled class works unmodified in the
 * vanilla overworld biomes too. Steep peaks (frozen_peaks/jagged_peaks)
 * will mostly fail {@link #cornersAreFlatLand} and so rarely host a
 * room in practice; the flatter sub-biomes (meadow/snowy_slopes/
 * stony_peaks) are where it is expected to actually appear.
 *
 * <p><b>Since v0.83.0</b>, the mountain biome modifier points at a
 * <em>second</em> registration of this same class -
 * {@code com.claudemod.registry.ModFeatures#PRISMIUM_MOUNTAIN_DUNGEON} -
 * rather than reusing {@code PRISMIUM_DUNGEON} verbatim as v0.82.0 did, so
 * the overworld version of this room is built out of weathered vanilla
 * stone brick ({@code Blocks.MOSSY_STONE_BRICKS}/{@code
 * Blocks.CRACKED_STONE_BRICKS}, see {@link #wallStateSupplier}) instead of
 * the Prism Realm's crystalline {@code prismium_deepstone_bricks} - the
 * "各バイオームに固有の見た目" half of TODO53's remaining follow-up
 * (PROGRESS.md TODO9/TODO53). The boss itself is still the exact same
 * {@link PrismiumColossusEntity} in both registrations - a biome-specific
 * boss variant is deliberately left for a future session, see PROGRESS.md.
 *
 * <p><b>Minimum separation between dungeons (added after v0.79.0, see
 * PROGRESS.md TODO9 "ダンジョン生成の重複抑制")</b>: the original
 * v0.78.0 version placed a dungeon in roughly 1-in-250 chunks completely
 * independently of every other chunk, so two dungeons could in principle
 * (if rarely) have rolled the same chance in neighboring or even
 * touching chunks, overlapping or crowding each other. {@link
 * #isDesignatedChunk} fixes this by replacing that per-chunk coin flip
 * with a deterministic spatial grid: the world's chunk grid is divided
 * into {@value #CELL_SIZE_CHUNKS}x{@value #CELL_SIZE_CHUNKS}-chunk
 * cells, and exactly one chunk inside each cell - chosen by hashing the
 * cell's own coordinates, never anything seed- or time-dependent, so the
 * same cell always designates the same chunk - is ever eligible to
 * attempt a dungeon at all; every other chunk's call to {@link #place}
 * returns {@code false} immediately, before the land/flatness checks
 * even run. Because exactly one candidate chunk exists per cell, two
 * dungeons can never end up in the same cell, and because the candidate
 * is additionally kept at least {@value #CELL_MARGIN_CHUNKS} chunks away
 * from every edge of its cell (see {@link #isDesignatedChunk}'s
 * javadoc), the worst case between two dungeons in *adjacent* cells is
 * still {@code 2 * CELL_MARGIN_CHUNKS} chunks apart - comfortably clear
 * of this room's 9x9 footprint. A {@value #CELL_SIZE_CHUNKS}-chunk cell
 * was chosen to keep the overall frequency close to the original
 * design's "about 1-in-250 chunks" ({@value #CELL_SIZE_CHUNKS}^2 = 256),
 * rather than the land/flatness rejection rate further on top of that,
 * same as before. This check deliberately never reads a single block
 * (only does integer arithmetic on the chunk coordinates passed into
 * {@link #place}), so unlike a block-scan-based "is there already a
 * dungeon nearby" approach, it cannot touch a chunk outside the small
 * footprint {@link #place} already writes to, and so cannot risk the
 * kind of "WorldGenRegion asked to access an unloaded chunk" crash that
 * reading distant terrain during decoration could cause (a failure mode
 * that, like everything else in this class, this sandbox has no game
 * client to rule out by testing - staying within the footprint {@link
 * #cornersAreFlatLand} already proved safe sidesteps the question
 * entirely rather than gambling on an unverified decoration-radius
 * assumption).
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
 * actual in-game rarity/findability/spacing of this grid-based feature
 * that also requires landing on dry land, whether the room ever
 * generates partially embedded in a hill on a borderline-flat site, and
 * whether the boss spawns cleanly on the floor (rather than, say, inside
 * a wall if the flatness/footprint math has an off-by-one) are all
 * unverified and flagged in PROGRESS.md as needing real playtesting.
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

    /** Side length, in chunks, of one placement grid cell - see {@link #isDesignatedChunk}. */
    private static final int CELL_SIZE_CHUNKS = 16;

    /**
     * Chunks of margin kept clear on every edge of a cell, so that the
     * designated chunk of one cell can never land right next to the
     * designated chunk of a neighboring cell - see {@link
     * #isDesignatedChunk}.
     */
    private static final int CELL_MARGIN_CHUNKS = 4;

    /** Usable offset range once {@link #CELL_MARGIN_CHUNKS} is excluded from both edges of a cell. */
    private static final int CELL_INNER_RANGE_CHUNKS = CELL_SIZE_CHUNKS - 2 * CELL_MARGIN_CHUNKS;

    /**
     * Wall/floor/ceiling material and its weathered variant, as lazy {@code
     * BlockState} suppliers rather than fields resolved at construction time
     * (this class is instantiated by {@code ModFeatures} during {@code
     * DeferredRegister} setup, before the vanilla/mod block registries are
     * necessarily populated, so {@code .defaultBlockState()} must not be
     * called until {@link #place} actually runs - the same reason the
     * original version only ever called {@code ModBlocks.*.get()} inside
     * {@code place} and never in its constructor).
     *
     * <p><b>Added in v0.83.0</b> so {@link com.claudemod.registry.ModFeatures}
     * can register a second instance of this exact class - {@code
     * PRISMIUM_MOUNTAIN_DUNGEON} - that builds the same room shape and the
     * same boss encounter out of a different, vanilla-only material palette
     * (see {@link com.claudemod.registry.ModFeatures} for the two palettes),
     * giving the overworld mountain version of this dungeon ("the other
     * half" of TODO53's "各バイオームに固有の見た目" follow-up) a distinct,
     * weathered-stone-ruin look instead of reusing the Prism Realm's
     * crystalline deepstone brick look verbatim. Every other field/method on
     * this class (placement grid, flatness check, light source, boss) is
     * unchanged and shared by both registrations.
     */
    private final Supplier<BlockState> wallStateSupplier;
    private final Supplier<BlockState> crackedWallStateSupplier;

    /** Prism Realm-flavored constructor: the original v0.78.0 deepstone brick ruin look, unchanged. */
    public PrismiumDungeonFeature(Codec<NoneFeatureConfiguration> codec) {
        this(codec,
                () -> ModBlocks.PRISMIUM_DEEPSTONE_BRICKS.get().defaultBlockState(),
                () -> ModBlocks.CRACKED_PRISMIUM_DEEPSTONE_BRICKS.get().defaultBlockState());
    }

    /**
     * Material-parameterized constructor - see {@link #wallStateSupplier}'s
     * javadoc above for why this exists and why these are suppliers rather
     * than plain {@code BlockState} arguments.
     */
    public PrismiumDungeonFeature(Codec<NoneFeatureConfiguration> codec,
            Supplier<BlockState> wallStateSupplier, Supplier<BlockState> crackedWallStateSupplier) {
        super(codec);
        this.wallStateSupplier = wallStateSupplier;
        this.crackedWallStateSupplier = crackedWallStateSupplier;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int cx = origin.getX();
        int cz = origin.getZ();

        if (!isDesignatedChunk(cx >> 4, cz >> 4)) {
            return false;
        }

        int centerGroundY = surfaceY(level, cx, cz);
        if (centerGroundY < SEA_LEVEL_Y) {
            return false;
        }
        if (!cornersAreFlatLand(level, cx, cz, centerGroundY)) {
            return false;
        }

        int floorY = centerGroundY + 1;
        int ceilingY = floorY + INTERIOR_HEIGHT + 1;

        BlockState bricks = wallStateSupplier.get();
        BlockState crackedBricks = crackedWallStateSupplier.get();
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

    /**
     * True only for the single chunk, inside the {@value #CELL_SIZE_CHUNKS}x
     * {@value #CELL_SIZE_CHUNKS}-chunk grid cell that {@code (chunkX, chunkZ)}
     * falls into, that is eligible to even attempt a dungeon - see this
     * class's "Minimum separation between dungeons" javadoc above for why.
     *
     * <p>Deliberately uses only plain {@code long}/{@code int} arithmetic on
     * the chunk coordinates (no world seed, no {@code RandomSource}, no
     * Minecraft-specific hashing class) so that (a) the same cell always
     * designates the same chunk - a cell's designated chunk must be a fixed
     * property of its coordinates alone, not of generation order or which
     * chunk happens to ask first, or two neighboring chunks could each
     * believe themselves the designated one - and (b) this needed no new,
     * unfamiliar API to verify (this mod's standing rule, see PROGRESS.md
     * section 1): {@link #mix} is the public-domain MurmurHash3 finalizer
     * (pure bit math, not a library call), and {@code Math.floorDiv}/{@code
     * Math.floorMod} are plain {@code java.lang.Math} methods that correctly
     * handle negative chunk coordinates (unlike {@code /}/{@code %}, which
     * round toward zero and would make the cell boundaries asymmetric across
     * the origin).
     *
     * <p>Package-visibility would be the tighter choice, but this is {@code
     * public} instead specifically so {@code
     * com.claudemod.gametest.ClaudeModGameTests#prismiumDungeonPlacementGridIsDeterministicAndSpaced}
     * (a different package) can call this exact production method directly
     * - a pure function with no world/level dependency is cheap, safe CI
     * coverage this mod otherwise has no way to get for worldgen logic (see
     * that test's own javadoc for why).
     */
    public static boolean isDesignatedChunk(int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX, CELL_SIZE_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, CELL_SIZE_CHUNKS);

        long hash = mix(mix((long) cellX) ^ (mix((long) cellZ) * 0x9E3779B97F4A7C15L));
        int offsetX = CELL_MARGIN_CHUNKS + (int) Math.floorMod(hash, (long) CELL_INNER_RANGE_CHUNKS);
        int offsetZ = CELL_MARGIN_CHUNKS + (int) Math.floorMod(hash >>> 32, (long) CELL_INNER_RANGE_CHUNKS);

        int designatedChunkX = cellX * CELL_SIZE_CHUNKS + offsetX;
        int designatedChunkZ = cellZ * CELL_SIZE_CHUNKS + offsetZ;
        return chunkX == designatedChunkX && chunkZ == designatedChunkZ;
    }

    /** MurmurHash3's 64-bit finalizer (public-domain bit-mixing math, not a library/API call). */
    private static long mix(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
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
