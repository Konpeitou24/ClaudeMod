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
 * Block for Prismium Spike (session 18): the mod's second surface
 * decoration for the Prism Realm / overworld (see {@link PrismiumBloomBlock},
 * session 17, for the first). Deliberately reuses that block's exact
 * design pattern (plain Block, cross-quad model, no BlockEntity, no
 * BushBlock/bonemeal) rather than introducing new API surface - session
 * 17/18's established "the smallest slice that still reads as alien
 * flora" philosophy, and PROGRESS.md's general preference for reusing an
 * already-reviewed-safe pattern over inventing a new one in a single
 * unverified session.
 *
 * Visually distinct from Prismium Bloom: a tall, narrow crystal shard
 * silhouette (vs. Bloom's wide flower-head diamond), taller VoxelShape
 * (3,0,3 -> 13,15,13, nearly full height) to loosely match the taller
 * texture, and a cooler/more saturated cyan-leaning palette (vs. Bloom's
 * warmer violet-leaning one) so the two read as different plants rather
 * than palette-swapped duplicates at a glance. See
 * scripts/textures/gen_prismium_spike.py for the texture rationale.
 *
 * Unlike Bloom's original (session 17) shipped state, canSurvive() is
 * included from day one here (session 17's own known-issue writeup,
 * PROGRESS.md §4-29, flagged that omitting it let Bloom generate floating
 * over cliffs/water; session 18 fixed that retroactively for Bloom in
 * the same commit as this file - see PrismiumBloomBlock). Both blocks now
 * require a sturdy-topped block directly below to be considered a valid
 * worldgen placement, enforced via the "minecraft:would_survive" block
 * predicate filter in their respective placed_feature JSON files.
 */
public class PrismiumSpikeBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 15, 13);

    public PrismiumSpikeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP);
    }
}
