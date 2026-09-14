package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Block for Prismium Stalactite Crystal (scheduled session, 2026-09-14):
 * the mod's fourth "surface decoration" style crystal prop, joining
 * Prismium Bloom (session 17), Prismium Spike (session 18) and Prismium
 * Geode Cluster (session, 2026-09-13). Follows the exact same
 * proven-safe pattern those three established (plain {@link Block}
 * subclass, {@code block/cross} model, no BlockEntity, hand-authored
 * VoxelShape, a single {@code canSurvive()} check against one
 * neighboring face via {@code isFaceSturdy}) - see
 * {@link PrismiumGeodeClusterBlock}'s class doc for the full rationale
 * on why this family deliberately avoids vanilla's
 * {@code AmethystClusterBlock} (FACING state, per-face placement logic,
 * multi-directional model/blockstate wiring - all unverified API surface
 * for this mod).
 *
 * <p>Unlike its three siblings (all floor-standing, {@code canSurvive}
 * requiring a sturdy block below), this is the family's first
 * ceiling-mounted variant: it requires a sturdy block <em>above</em> it
 * (checked via {@code isFaceSturdy(..., Direction.DOWN)}, the mirror
 * image of the existing {@code Direction.UP} check used by Bloom/Spike/
 * Geode Cluster) and its {@link #SHAPE} sits flush against the top of
 * the block volume, hanging downward, rather than flush against the
 * bottom. This is the same {@code isFaceSturdy} method already exercised
 * by the other three blocks, just with the opposite {@link Direction}
 * argument, so no new Block/LevelReader API is introduced here.
 *
 * <p>Player-craftable (see
 * {@code data/claudemod/recipes/prismium_stalactite.json}, which uses
 * vanilla Pointed Dripstone as a thematic ingredient) rather than
 * worldgen-placed, keeping this session's scope small (no
 * biome_modifier/configured_feature/placed_feature risk).
 */
public class PrismiumStalactiteBlock extends Block {

    // Flush against the ceiling (y=16) and hanging down to y=6, the
    // mirror image of PrismiumGeodeClusterBlock's box(2,0,2,14,10,14)
    // (which sits flush against the floor, y=0, and rises to y=10).
    private static final VoxelShape SHAPE = Block.box(2, 6, 2, 14, 16, 14);

    public PrismiumStalactiteBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        return aboveState.isFaceSturdy(level, above, Direction.DOWN);
    }
}
