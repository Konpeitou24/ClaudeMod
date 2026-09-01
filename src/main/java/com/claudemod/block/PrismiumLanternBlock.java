package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * Shared block class for Prismium Lantern and Pale Prismium Lantern.
 *
 * <p>PROGRESS.md TODO/section-5 item, raised repeatedly by こんぺいとう氏:
 * "Prismium Lantern / Pale Prismium Lanternの形状が session 4からずっと
 * 単純な立方体(cube_all)のまま" - both lanterns had used a plain
 * {@code new Block(...)} registration with a {@code cube_all} model since
 * session 4/#79, unlike vanilla Lantern's proper hanging-cage shape. This
 * class replaces that with a real implementation: a {@code HANGING}
 * boolean state (standing on the floor vs. hung from the block above,
 * exactly like vanilla {@code LanternBlock}) plus {@code WATERLOGGED}
 * support, matching hitboxes, and placement logic that picks an
 * orientation based on which face the player targeted and what's actually
 * there to support it.
 *
 * <p><b>Shapes are vanilla-exact</b>, not eyeballed: fetched directly from
 * Mojang's own {@code assets/minecraft/models/block/template_lantern.json}
 * and {@code template_hanging_lantern.json} (verified via a public 1.20.1
 * asset mirror, see PROGRESS.md for the source), which define the
 * standing lantern's cage as box (5,0,5)-(11,7,11) and the hanging
 * lantern's as box (5,1,5)-(11,8,11) (shifted up one pixel for the short
 * chain-link nub connecting it to whatever it's hanging from). Reusing
 * these exact numbers for {@link #STANDING_SHAPE}/{@link #HANGING_SHAPE}
 * means the collision box matches the visual model pixel-for-pixel,
 * because the block/item *models* for this block
 * ({@code assets/claudemod/models/block/prismium_lantern.json} etc.) are
 * built the same way vanilla's own {@code block/lantern.json} is: parented
 * directly onto {@code minecraft:block/template_lantern}/
 * {@code template_hanging_lantern} with only the {@code lantern} texture
 * swapped out, so ClaudeMod gets vanilla's exact cage geometry for free
 * without hand-authoring element/face JSON. The texture itself
 * ({@code gen_prismium_lantern.py}/{@code gen_pale_prismium_lantern.py})
 * was rewritten in the same session to match that template's UV unwrap
 * (it used to be a flat repeating cube_all pattern, which would have
 * shown garbled/misaligned fragments once the shape changed).
 *
 * <p><b>Support/{@code canSurvive} is deliberately a simplified subset of
 * vanilla's</b>: real {@code LanternBlock} also special-cases hanging from
 * fences, walls, iron bars, chains, and trapdoors (several of which don't
 * expose the plain "sturdy face" vanilla blocks use). This class only
 * checks {@link BlockState#isFaceSturdy}: standing requires a sturdy top
 * face on the block below, hanging requires a sturdy bottom face on the
 * block above - i.e. normal floors/ceilings, not fence posts or chains.
 * That covers the overwhelming majority of real placements (including
 * hanging under a normal ceiling block, which is the headline ask) at a
 * fraction of the special-case surface area; the narrower fence/chain
 * mounting cases were judged not worth the risk of an unverified,
 * un-play-tested change trying to replicate vanilla's exact (and
 * undocumented from this sandbox) special-casing. See PROGRESS.md for
 * this being tracked as a known simplification, not an oversight.
 *
 * <p><b>UNVERIFIED</b> (see PROGRESS.md section 4 - no local game client
 * in this sandbox): compiles against the 1.20.1 Forge API as far as can
 * be checked without a build, but the actual in-game hanging/standing
 * placement behavior, hitboxes, and how the new UV-mapped texture
 * actually looks wrapped onto the 3D shape have not been visually
 * confirmed in a running client.
 */
public class PrismiumLanternBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Vanilla template_lantern.json's body element: box(5,0,5)-(11,7,11). */
    private static final VoxelShape STANDING_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 7.0D, 11.0D);
    /** Vanilla template_hanging_lantern.json's body element: box(5,1,5)-(11,8,11). */
    private static final VoxelShape HANGING_SHAPE = Block.box(5.0D, 1.0D, 5.0D, 11.0D, 8.0D, 11.0D);

    public PrismiumLanternBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HANGING, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING_SHAPE;
    }

    private static boolean canStandOn(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static boolean canHangFrom(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HANGING) ? canHangFrom(level, pos) : canStandOn(level, pos);
    }

    // NOTE (2026-09-01 build-fix): this class used to also override a
    // canPlace(BlockPlaceContext) method here, modeled on the mistaken
    // assumption that Block exposes a hook like that for "should placement
    // be allowed at all". It does not exist on 1.20.1's Block class ("method
    // does not override or implement a method from a supertype" - caught by
    // CI, see PROGRESS.md problem list) and was removed. No behavior is
    // lost: BlockItem's own placement logic already computes the state via
    // getStateForPlacement below and then checks that state's canSurvive()
    // before actually placing, so a position where neither canStandOn() nor
    // canHangFrom() is true still correctly fails to place without this
    // method existing.

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = context.getLevel().getFluidState(pos);
        boolean waterlogged = fluidState.getType() == Fluids.WATER;

        boolean canStand = canStandOn(level, pos);
        boolean canHang = canHangFrom(level, pos);
        Direction clickedFace = context.getClickedFace();

        boolean hanging;
        if (clickedFace == Direction.DOWN && canHang) {
            // Clicked the underside of a block: hang from it if possible,
            // exactly like vanilla Lantern.
            hanging = true;
        } else if (clickedFace == Direction.UP && canStand) {
            hanging = false;
        } else if (canStand) {
            hanging = false;
        } else {
            hanging = canHang;
        }

        return this.defaultBlockState()
                .setValue(HANGING, hanging)
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        boolean hanging = state.getValue(HANGING);
        if ((hanging && direction == Direction.UP) || (!hanging && direction == Direction.DOWN)) {
            // The block this lantern was standing on / hanging from is
            // gone (or no longer sturdy on the relevant face) - same
            // "pop off, drop nothing extra, let the loot table handle
            // drops" convention vanilla's own attachable blocks use.
            return this.canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
