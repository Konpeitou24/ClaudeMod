package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Block for Prismium Bloom (session 17): a decorative, non-collidable
 * "cross" plant, the mod's first dedicated surface decoration for the
 * Prism Realm dimension (see PROGRESS.md section 5 discussion point
 * "専用地表ブロック" - this was the natural next step flagged after Prism
 * Realm/Prismium Rift Shard shipped in sessions 14-15).
 *
 * Follows the same "override getShape() for a non-full-cube hitbox"
 * pattern {@link PrismiumCableBlock} introduced in session 10 (the mod's
 * first non-full-cube block), but simpler: no BlockEntity, no ticker, no
 * BaseEntityBlock - just a plain Block with a smaller VoxelShape, since a
 * cross-quad plant needs no block entity of its own. The shape values
 * (3,0,3 -> 13,13,13) approximate vanilla flowers' own hitbox rather than
 * being pixel-measured against this mod's actual model - a known,
 * intentional simplification (see PROGRESS.md).
 *
 * Deliberately does NOT extend {@code BushBlock} / implement
 * {@code BonemealableBlock}: no "must be planted on dirt/grass" survival
 * check and no bonemeal growth. It behaves like a purely decorative prop
 * placed directly by worldgen (see
 * data/claudemod/worldgen/{configured_feature,placed_feature}/prismium_bloom*.json)
 * rather than a farmable plant - deliberately the smallest slice that
 * still reads as "alien flora" in the world. A future session could grow
 * this into a real BushBlock if survival/bonemeal behaviour turns out to
 * matter once someone can actually play-test the dimension.
 */
public class PrismiumBloomBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public PrismiumBloomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
