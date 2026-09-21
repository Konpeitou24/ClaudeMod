package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Prismium Wall Lamp (scheduled session, 2026-09-21): the mod's first
 * block built on {@code BlockStateProperties.HORIZONTAL_FACING}, one of
 * the two "next new silhouette" options flagged in HANDOFF.md since the
 * v0.51.0/v0.52.0 sessions ("壁掛け版の新形状(HorizontalDirectionalBlock
 * +FACING、このMOD未使用のAPI)"). Every existing light-emitting block in
 * this mod so far (PrismiumLanternBlock's HANGING/standing states,
 * Bloom/Spike/GeodeCluster/Stalactite) is either placed on a floor/
 * ceiling or is a walk-through decoration; this is the first one meant
 * to be mounted flush against the *side* of a block, like a torch that
 * only works on walls.
 *
 * <p><b>API confirmed before use</b> (per PROGRESS.md's standing "no
 * unconfirmed @Override" rule, after the v0.37.0 build-failure incident):
 * {@code BlockStateProperties.HORIZONTAL_FACING} was verified as a real
 * static {@code DirectionProperty} field via
 * {@code mappings.dev/1.20.1/.../BlockStateProperties.html}'s field
 * summary before being used here. {@code Rotation#rotate(Direction)} and
 * {@code Mirror#getRotation(Direction)} were both confirmed on
 * {@code mappings.dev}'s {@code Rotation.html} / {@code Mirror.html}
 * method-summary tables before being called below. The {@code
 * canSurvive}/{@code getStateForPlacement}/{@code updateShape}/{@code
 * createBlockStateDefinition}/{@code getShape} override signatures are
 * copied verbatim from {@link PrismiumLanternBlock}, which already
 * compiles successfully in this codebase (see that class's own javadoc
 * for its build-failure history), rather than guessed from scratch.
 *
 * <p><b>Placement geometry</b>: {@code FACING} stores the direction the
 * lamp points <i>away</i> from its supporting wall (matching vanilla's
 * own furnace/dispenser convention verified against
 * {@code minecraft-assets}' {@code furnace.json} blockstate, which
 * authors its model for {@code facing=north} with no rotation and then
 * applies {@code y=90/180/270} for east/south/west — the same mapping
 * used by {@code assets/claudemod/blockstates/prismium_wall_lamp.json}).
 * The support block is looked up at {@code pos.relative(facing.getOpposite())}
 * and must have a sturdy face on that side ({@link BlockState#isFaceSturdy}),
 * exactly like {@link PrismiumLanternBlock}'s standing/hanging checks.
 * Placement is restricted to a horizontal clicked face only (clicking a
 * block's top or bottom does not place this lamp), the same restriction
 * vanilla's own wall torches/wall signs enforce.
 *
 * <p>Collision/outline shape is a thin (2px) plaque flush against the
 * wall, covering the full 16x16 face — <b>not</b> copied from any vanilla
 * template (unlike {@link PrismiumLanternBlock}'s exact
 * {@code template_lantern} reuse), since no vanilla block has this exact
 * "flat full-face plaque" shape; the four box coordinates below were
 * derived directly from the documented {@code Block.box(minX, minY, minZ,
 * maxX, maxY, maxZ)} contract (already used the same way by
 * {@code PrismiumLanternBlock#STANDING_SHAPE}/{@code HANGING_SHAPE}), not
 * from an external asset.
 *
 * <p><b>UNVERIFIED</b> (see PROGRESS.md section 4 — no local game client
 * in this sandbox): compiles against the 1.20.1 Forge API as far as can
 * be checked without a build, but actual in-game wall placement on all
 * four horizontal directions, the hitbox feel, and how the flat texture
 * looks mounted on the 3D shape have not been visually confirmed in a
 * running client.
 */
public class PrismiumWallLampBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // Thin (2px) plaque hugging the wall on the side opposite FACING,
        // covering the whole 16x16 face. See class doc for the geometry
        // derivation (facing = outward direction; wall/support sits on
        // facing.getOpposite() side of this block's position).
        SHAPES.put(Direction.NORTH, Block.box(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.SOUTH, Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D));
        SHAPES.put(Direction.WEST, Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));
        SHAPES.put(Direction.EAST, Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 16.0D));
    }

    public PrismiumWallLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    private static boolean hasSupport(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos supportPos = pos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSupport(level, pos, state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis() == Direction.Axis.Y) {
            // Wall-mounted only: clicking a floor/ceiling face can't
            // support this lamp, same restriction as vanilla wall
            // torches/wall signs.
            return null;
        }
        BlockState state = this.defaultBlockState().setValue(FACING, face);
        return hasSupport(context.getLevel(), context.getClickedPos(), face) ? state : null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getOpposite() && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return this.rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
}
