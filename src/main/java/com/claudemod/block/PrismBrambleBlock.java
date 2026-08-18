package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
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
 *
 * Session 48: made waterloggable, same fix and same rationale as
 * {@link PrismLilyBlock} - see that class's javadoc for the full
 * worldgen-heightmap bug this addresses (this block's placed_feature
 * had the identical WORLD_SURFACE_WG bug and has never generated
 * since Prism Realm became a flat waterworld in session 47).
 */
public class PrismBrambleBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public PrismBrambleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
