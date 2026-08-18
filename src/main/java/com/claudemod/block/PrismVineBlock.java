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
 * Block for Prism Vine (session 44): the mod's third plant exclusive
 * to the Prism Realm dimension, joining {@link PrismLilyBlock}
 * (session 40) and {@link PrismBrambleBlock} (session 43). Reuses the
 * exact same pattern as its siblings - plain Block, cross-quad model,
 * no BlockEntity, no BushBlock/bonemeal, canSurvive checks for a
 * sturdy block directly below.
 *
 * See the texture script (scripts/textures/gen_prism_vine.py) and
 * PROGRESS.md section 5 item 9(c) for why this plant was deliberately
 * given a low, wide, ground-hugging silhouette (bounding box
 * concentrated in the bottom half of the canvas) rather than another
 * upward-growing shape like Lily (rounded, vertically centered) or
 * Bramble (tall, top-weighted) - the explicit ask was a third plant
 * with a different "growth direction" so all three exclusive plants
 * stay easy to tell apart by silhouette alone. The bounding-box
 * shape is shorter than Lily/Bramble's, so the collision/selection
 * shape is capped lower (height 9 instead of 13) to roughly match.
 *
 * Registered via a biome_modifier scoped to ONLY
 * "claudemod:prism_realm" (see
 * data/claudemod/forge/biome_modifier/add_prism_vine.json), the same
 * exclusivity mechanism Lily/Bramble established - not the
 * "#minecraft:is_overworld" tag Bloom/Spike use.
 */
public class PrismVineBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 9, 15);

    public PrismVineBlock(Properties properties) {
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
