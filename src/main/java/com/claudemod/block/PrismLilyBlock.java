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
 * Block for Prism Lily (session 40): the mod's third surface decoration,
 * and its first plant exclusive to the Prism Realm dimension rather than
 * shared with the overworld (see {@link PrismiumBloomBlock}, session 17,
 * and {@link PrismiumSpikeBlock}, session 18, for the two shared
 * predecessors this reuses the exact same pattern from - plain Block,
 * cross-quad model, no BlockEntity, no BushBlock/bonemeal).
 *
 * Session 39 (PROGRESS.md section 3AM-2) gave Prism Realm its own biome
 * with distinct sky/fog/water/foliage colors but left the biome's
 * "features" worldgen step empty out of caution about unverified
 * feature IDs, and flagged (section 5, item 9) that the dimension still
 * had no vegetation of its own - Bloom/Spike both generate in the
 * overworld too via the shared "#minecraft:is_overworld" biome tag, so
 * they read as "a rare overworld crystal" rather than "native Prism
 * Realm flora". This block is registered by a biome_modifier that
 * targets ONLY "claudemod:prism_realm" (see
 * data/claudemod/forge/biome_modifier/add_prism_lily.json), not the
 * is_overworld tag, so it is the dimension's first exclusive plant -
 * see PROGRESS.md for the full rationale.
 *
 * canSurvive mirrors PrismiumBloomBlock/PrismiumSpikeBlock exactly
 * (session 18's sturdy-top-block check), and the hitbox reuses their
 * same approximate vanilla-flower box rather than being pixel-measured
 * against this block's own (different, wider) cross texture - a known,
 * intentional simplification carried over from the two earlier plants.
 */
public class PrismLilyBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public PrismLilyBlock(Properties properties) {
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
