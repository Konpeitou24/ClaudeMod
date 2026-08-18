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
 * Block for Prism Bramble (session 43): the mod's second plant
 * exclusive to the Prism Realm dimension, joining {@link PrismLilyBlock}
 * (session 40). Reuses the exact same pattern as Lily/{@link
 * PrismiumBloomBlock}/{@link PrismiumSpikeBlock} - plain Block,
 * cross-quad model, no BlockEntity, no BushBlock/bonemeal, canSurvive
 * checks for a sturdy block directly below (see PROGRESS.md section
 * 5 item 9(c) for why a second exclusive plant was wanted, and the
 * texture script for why this one was deliberately given a different
 * bounding silhouette than Lily's rounded flower - a thorny,
 * three-pronged fan rather than a flower cup).
 *
 * Registered via a biome_modifier scoped to ONLY
 * "claudemod:prism_realm" (see
 * data/claudemod/forge/biome_modifier/add_prism_bramble.json), the
 * same exclusivity mechanism Lily established - not the
 * "#minecraft:is_overworld" tag Bloom/Spike use.
 */
public class PrismBrambleBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public PrismBrambleBlock(Properties properties) {
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
