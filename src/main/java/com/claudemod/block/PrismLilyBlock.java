package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.context.BlockPlaceContext;
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
 *
 * Session 48: made waterloggable (implements {@link SimpleWaterloggedBlock},
 * standard vanilla WATERLOGGED pattern - see e.g. StairBlock/FenceBlock).
 * This fixes a worldgen bug found while auditing PROGRESS.md's carried-over
 * "check whether the Prism plants still spawn in the new flat waterworld
 * terrain" item: Prism Realm (session 47) became a flat dimension whose
 * only dry surface is submerged under ~68 blocks of water, and this
 * block's placed_feature previously used the WORLD_SURFACE_WG heightmap,
 * which (per Minecraft's heightmap semantics) treats water as "surface"
 * and returns the position just above the water column - i.e. floating in
 * open air with a water block below, which is not isFaceSturdy() and so
 * always failed the would_survive placement filter. Zero Lily has ever
 * generated since the flat-world switch. Fixed by switching the
 * placed_feature to OCEAN_FLOOR_WG (lands on the seafloor/soil instead)
 * and adding waterlogging so the block renders correctly submerged
 * instead of leaving a dry air-pocket at its own position. Not yet
 * verified in-game (no local build available in this sandbox - see
 * PROGRESS.md).
 */
public class PrismLilyBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);

    public PrismLilyBlock(Properties properties) {
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
