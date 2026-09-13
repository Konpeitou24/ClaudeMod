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
 * Block for Prismium Geode Cluster (scheduled session, 2026-09-13): the
 * mod's third "surface decoration" style crystal prop, joining Prismium
 * Bloom (session 17) and Prismium Spike (session 18). Reuses the exact
 * same proven-safe pattern those two established (plain {@link Block}
 * subclass, {@code block/cross} model, no BlockEntity, no
 * BushBlock/bonemeal, hand-authored VoxelShape, {@code canSurvive()}
 * requiring a sturdy block below) rather than reaching for vanilla's
 * {@code AmethystClusterBlock} - that class needs a FACING blockstate
 * property, per-direction placement logic (matching the clicked face,
 * not just "always up"), and specific model/blockstate rotation wiring
 * that no session of this mod has exercised before, so it would be new,
 * unverified API surface for a single low-priority decorative block.
 * PROGRESS.md's standing guidance is to reuse an already-reviewed-safe
 * pattern instead of introducing new mechanics in a single unverified
 * session; this class follows that.
 *
 * <p>Unlike Bloom/Spike (which are natural Prism Realm/overworld surface
 * decorations placed only by worldgen, see their {@code placed_feature}
 * JSON files), this block is deliberately player-craftable (see
 * {@code data/claudemod/recipes/prismium_geode_cluster.json}) - it is
 * meant as a decorative accent players can place intentionally while
 * building, foreshadowing future Prism Realm dungeon interior dressing
 * (see PROGRESS.md TODO9's planned per-biome dungeon/boss work) rather
 * than something you only ever find growing wild. No worldgen placement
 * files exist for it at all, which keeps this session's scope small (no
 * biome_modifier/configured_feature risk to get wrong, unlike Bloom/
 * Spike/Lily/Bramble/Vine).
 *
 * <p>Visually distinct from Bloom (wide flower-head diamond) and Spike
 * (three tall narrow shards): a short, wide cluster of several stubby
 * crystal points of varying height emerging from a rocky base, evoking a
 * cracked-open geode rather than a single shard or a flower. See
 * scripts/textures/gen_prismium_geode_cluster.py for the art rationale.
 * VoxelShape is short and nearly full-width (2,0,2 -&gt; 14,10,14) to
 * match that squat "cluster hugging the ground" silhouette, distinct
 * from Spike's tall narrow box.
 */
public class PrismiumGeodeClusterBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public PrismiumGeodeClusterBlock(Properties properties) {
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
