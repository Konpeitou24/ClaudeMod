package com.claudemod.worldgen.feature;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Worldgen feature: a small, enemy-free "treasure vault" on Prism Realm dry
 * land - the other half of the two dungeon ideas PROGRESS.md's TODO9
 * follow-up list carried over from {@link PrismiumDungeonFeature} ("宝物庫
 * だけで敵がいないもの" - a vault that is only a treasure room, with no
 * boss/monster inside). Structurally this is {@link PrismiumDungeonFeature}'s
 * little sibling: same overall technique (a plain {@link Feature} that
 * procedurally {@code setBlock}s a room, the same grid-based deterministic
 * spacing approach, the same land/flatness precheck), scaled down to a
 * single-chest payoff instead of a boss encounter, so a player who finds one
 * gets a quick, low-risk reward rather than a fight. See that class's
 * javadoc first for the shared design rationale (deliberately not a vanilla
 * jigsaw/NBT structure, why the grid never reads a block, etc.) - this
 * javadoc only calls out what differs.
 *
 * <p><b>Placement grid</b>: uses its own {@link #isDesignatedChunk}, built
 * the exact same way as {@link PrismiumDungeonFeature#isDesignatedChunk}
 * (same {@link #mix} MurmurHash3 finalizer, same {@code Math.floorDiv}/
 * {@code Math.floorMod} cell math) but with a smaller {@value
 * #CELL_SIZE_CHUNKS}-chunk cell (roughly 1-in-100 chunks, noticeably more
 * common than the boss dungeon's 1-in-256, matching "treasure vault" being
 * the more frequent, lower-stakes find of the two) and a different additive
 * salt folded into the hash before mixing, so the two grids are independent
 * pseudorandom fields rather than the same pattern at a different scale.
 * Independent grids can still, in principle, both designate the exact same
 * chunk once in a very great while; rather than trying to prove that can
 * never happen (which would need reasoning about two unrelated hash
 * functions colliding), {@link #place} just asks {@link
 * PrismiumDungeonFeature#isDesignatedChunk} directly and backs off if the
 * dungeon also claims this chunk, so the two features can never both build
 * into the same spot regardless of how the grids line up.
 *
 * <p><b>Room shape</b>: a 5x5 footprint (interior 3x3, much smaller than the
 * dungeon's 9x9), 1-block-thick walls/floor/ceiling of {@code
 * claudemod:prismium_bricks} (with the same scattered-cracked-variant
 * weathering as the dungeon, but using the surface {@code prismium_bricks}/
 * {@code cracked_prismium_bricks} pair rather than the dungeon's deep-layer
 * {@code prismium_deepstone_bricks}, so the two room types read as visually
 * distinct ruins even before a player steps inside), 3 blocks of interior
 * clearance, a single 1-wide/2-tall doorway in the south wall, a {@code
 * claudemod:prismium_geode_cluster} light in the back-left interior corner,
 * and a single vanilla chest in the center running the {@link
 * #VAULT_LOOT_TABLE} loot table (see
 * data/claudemod/loot_tables/chests/prismium_vault.json) - no mob is ever
 * spawned here, by design.
 *
 * <p><b>Chest loot table API</b>: {@code RandomizableContainerBlockEntity}
 * (which {@code ChestBlockEntity} extends) declares {@code public void
 * setLootTable(ResourceLocation, long)} - confirmed against two independent
 * mapping sources (mappings.dev and the nekoyue ForgeJavaDocs-NG mirror both
 * agree on this exact signature) before use, per this mod's standing
 * "verify an unfamiliar API against more than one source" rule (PROGRESS.md
 * section 1). {@code ChestBlock.FACING} (a {@code DirectionProperty}) was
 * confirmed the same way. This is the first time any ClaudeMod worldgen code
 * has placed a loot-table container, so both were treated as new,
 * unverified API surface rather than assumed, matching how {@link
 * PrismiumDungeonFeature#spawnBoss} handled its own first-time API surface.
 *
 * <p><b>Unverified</b>: like {@link PrismiumDungeonFeature} and every other
 * worldgen/entity change in this mod (see PROGRESS.md section 3), this
 * compiles against the 1.20.1 Forge API as best as can be checked without a
 * local build/game client, but has not been run in an actual game client.
 * In particular: the actual in-game rarity/findability of this smaller grid,
 * whether the room ever generates partially embedded in a hill on a
 * borderline-flat site, and whether the chest's loot table is appropriately
 * balanced (both the base shard/ingot payout and the rare charm bonus pool)
 * are all unverified and flagged in PROGRESS.md as needing real playtesting.
 */
public class PrismiumVaultFeature extends Feature<NoneFeatureConfiguration> {

    /** Sea level in the Prism Realm's flat generator - see {@link PrismiumLandFeature}. */
    private static final int SEA_LEVEL_Y = 63;

    /** Footprint half-size: full room is (2*ROOM_HALF_SIZE + 1) blocks on a side (5x5). */
    private static final int ROOM_HALF_SIZE = 2;

    /** How many blocks of open interior airspace, floor to ceiling. */
    private static final int INTERIOR_HEIGHT = 3;

    /** Max allowed Y difference between the room's center and any footprint corner before aborting. */
    private static final int FLATNESS_TOLERANCE = 3;

    /** Chance (out of 10) that a given wall/floor/ceiling block uses the cracked variant instead. */
    private static final int CRACKED_CHANCE_OUT_OF_TEN = 2;

    /** Side length, in chunks, of one placement grid cell - see {@link #isDesignatedChunk}. */
    private static final int CELL_SIZE_CHUNKS = 10;

    /**
     * Chunks of margin kept clear on every edge of a cell, so that the
     * designated chunk of one cell can never land right next to the
     * designated chunk of a neighboring cell - see {@link
     * #isDesignatedChunk}. {@code 2 * CELL_MARGIN_CHUNKS + 1 == 5}, exactly
     * this room's footprint width, the same tight-fit margin sizing {@link
     * PrismiumDungeonFeature} uses.
     */
    private static final int CELL_MARGIN_CHUNKS = 2;

    /** Usable offset range once {@link #CELL_MARGIN_CHUNKS} is excluded from both edges of a cell. */
    private static final int CELL_INNER_RANGE_CHUNKS = CELL_SIZE_CHUNKS - 2 * CELL_MARGIN_CHUNKS;

    /**
     * Additive salt folded into the hash before mixing, purely so this
     * grid's pseudorandom pattern is independent of {@link
     * PrismiumDungeonFeature#isDesignatedChunk}'s (which uses no salt at
     * all) rather than the same pattern at a different cell size - see this
     * class's "Placement grid" javadoc above.
     */
    private static final long GRID_SALT = 0xBF58476D1CE4E5B9L;

    /** Resource location of this feature's chest loot table (see that JSON file's own comments). */
    private static final ResourceLocation VAULT_LOOT_TABLE =
            new ResourceLocation(ClaudeMod.MOD_ID, "chests/prismium_vault");

    public PrismiumVaultFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int cx = origin.getX();
        int cz = origin.getZ();
        int chunkX = cx >> 4;
        int chunkZ = cz >> 4;

        if (!isDesignatedChunk(chunkX, chunkZ)) {
            return false;
        }
        // Never build into the exact same chunk the boss dungeon's own
        // (independent) grid also happens to designate - see this class's
        // "Placement grid" javadoc above.
        if (PrismiumDungeonFeature.isDesignatedChunk(chunkX, chunkZ)) {
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

        BlockState bricks = ModBlocks.PRISMIUM_BRICKS.get().defaultBlockState();
        BlockState crackedBricks = ModBlocks.CRACKED_PRISMIUM_BRICKS.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int dx = -ROOM_HALF_SIZE; dx <= ROOM_HALF_SIZE; dx++) {
            for (int dz = -ROOM_HALF_SIZE; dz <= ROOM_HALF_SIZE; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                boolean perimeter = dx == -ROOM_HALF_SIZE || dx == ROOM_HALF_SIZE
                        || dz == -ROOM_HALF_SIZE || dz == ROOM_HALF_SIZE;
                boolean doorwayColumn = dz == ROOM_HALF_SIZE && dx == 0;

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

        placeLight(level, cx - 1, floorY + 1, cz - 1);
        placeChest(level, random, cx, floorY + 1, cz);

        return true;
    }

    /**
     * True only for the single chunk, inside the {@value #CELL_SIZE_CHUNKS}x
     * {@value #CELL_SIZE_CHUNKS}-chunk grid cell that {@code (chunkX, chunkZ)}
     * falls into, that is eligible to even attempt a vault - the same
     * deterministic-grid technique {@link
     * PrismiumDungeonFeature#isDesignatedChunk} uses (see that method's
     * javadoc for why this shape of check was chosen over a block-scan
     * approach), just with this class's own {@value #CELL_SIZE_CHUNKS}-chunk
     * cell size and {@link #GRID_SALT}.
     *
     * <p>Public for the exact same reason {@code
     * PrismiumDungeonFeature#isDesignatedChunk} is: so {@code
     * com.claudemod.gametest.ClaudeModGameTests#prismiumVaultPlacementGridIsDeterministicAndSpaced}
     * can call this production method directly as cheap, safe CI coverage
     * for worldgen logic that otherwise has none.
     */
    public static boolean isDesignatedChunk(int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX, CELL_SIZE_CHUNKS);
        int cellZ = Math.floorDiv(chunkZ, CELL_SIZE_CHUNKS);

        long hash = mix(mix((long) cellX ^ GRID_SALT) ^ (mix((long) cellZ) * 0x9E3779B97F4A7C15L));
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

    /** Topmost non-air block's Y, same technique {@link PrismiumDungeonFeature} already uses. */
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
     * Places the single reward chest and assigns it {@link
     * #VAULT_LOOT_TABLE} - see this class's "Chest loot table API" javadoc
     * above for how {@code setLootTable} was verified before use. Facing is
     * set towards the doorway (south) purely so the chest visually "greets"
     * a player walking in; a freestanding chest has no placement support
     * requirement to satisfy (unlike {@code PRISMIUM_LANTERN}'s
     * hanging/standing logic), so no {@code canSurvive} concern applies
     * here.
     */
    private static void placeChest(WorldGenLevel level, RandomSource random, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState chestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        level.setBlock(pos, chestState, 2);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RandomizableContainerBlockEntity randomizable) {
            randomizable.setLootTable(VAULT_LOOT_TABLE, random.nextLong());
        }
    }
}
