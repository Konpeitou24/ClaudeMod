package com.claudemod.block;

import com.claudemod.blockentity.PrismiumCableBlockEntity;
import com.claudemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Block for Prismium Cable (session 10, connected-model rework session 22).
 * See {@link PrismiumCableBlockEntity} for the relay logic.
 *
 * <p>Session 10 shipped every cable rendering identically (a static
 * centered post) regardless of what it was next to - documented then as a
 * deliberate simplification to avoid multipart blockstate + neighbor-aware
 * shape work in the first pass. Session 22 (see PROGRESS.md §5 item 4's
 * "(b) cable connection appearance") implements that deferred piece: six
 * {@link BooleanProperty} values (reusing vanilla's
 * {@link BlockStateProperties#NORTH}/{@code EAST}/{@code SOUTH}/{@code WEST}/
 * {@code UP}/{@code DOWN}, the same properties vanilla's chorus plant uses
 * for its own six-way connections) track, per side, whether that neighbor
 * is something this cable would actually interact with energy-wise. A
 * small central "core" model is always rendered, plus one "arm" model
 * (rotated per direction via the block model's own x/y rotation, the same
 * trick vanilla's observer block uses for its six facings) for every side
 * that is connected - see {@code blockstates/prismium_cable.json}. The arm
 * model deliberately omits the face that touches the core (it would be a
 * textured quad exactly coplanar with the core's own face on that side,
 * i.e. guaranteed z-fighting) - see
 * {@code models/block/prismium_cable_arm.json}.
 *
 * <p>The connection test ({@link #connectsTo}) intentionally mirrors, but
 * is not identical to, {@link com.claudemod.energy.EnergyPushHelper}: it
 * only checks whether the neighboring block entity exposes
 * {@code ForgeCapabilities.ENERGY} on the facing side at all
 * ({@code isPresent()}), not whether it currently {@code canReceive()}.
 * That's deliberate - a cable should visually connect to e.g. Prismium
 * Generator (whose storage sets {@code maxReceive} to 0, see
 * {@code PrismiumGeneratorBlockEntity}) even though the generator will
 * never accept a push, because the generator still legitimately pushes
 * energy INTO the cable on its own tick. Using {@code canReceive()} here
 * would have made the generator side of every cable look permanently
 * disconnected, which would have been visually misleading.
 *
 * <p><b>Unverified (session 22):</b> the x/y rotation values used for the
 * up/down/east/west/south arm variants are modeled on the well-known
 * vanilla observer blockstate pattern (default model faces north; east =
 * y:90, south = y:180, west = y:270, down = x:90, up = x:270) rather than
 * on anything rendered and inspected in-game in this sandbox - the usual
 * caveat that applies to every piece of 3D geometry in this mod so far
 * (see PROGRESS.md §4). If a direction's arm turns out to be rotated
 * backwards in actual play, swapping that one direction's x/y value here
 * (and nowhere else - the shape/collision code below is independent of the
 * model file and already direction-correct) is the fix.
 */
public class PrismiumCableBlock extends BaseEntityBlock {

    /** The always-present centered post, unchanged from session 10 -
     * matches {@code models/block/prismium_cable_arm.json}'s implicit
     * "core" reference via the base model in the blockstate multipart. */
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);

    private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);
    static {
        ARM_SHAPES.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4));
        ARM_SHAPES.put(Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16));
        ARM_SHAPES.put(Direction.WEST, Block.box(0, 4, 4, 4, 12, 12));
        ARM_SHAPES.put(Direction.EAST, Block.box(12, 4, 4, 16, 12, 12));
        ARM_SHAPES.put(Direction.UP, Block.box(4, 12, 4, 12, 16, 12));
        ARM_SHAPES.put(Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    /** All 64 combinations of the six connection booleans, precomputed
     * once rather than unioned fresh on every {@link #getShape} call -
     * this block's shape is queried very frequently (collision, culling,
     * selection) so caching the union avoids repeating up to six
     * {@link Shapes#or} calls per query. */
    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[64];
    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CORE;
            for (int i = 0; i < DIRECTIONS.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, ARM_SHAPES.get(DIRECTIONS[i]));
                }
            }
            SHAPE_CACHE[mask] = shape;
        }
    }

    private static BooleanProperty propertyFor(Direction direction) {
        switch (direction) {
            case NORTH: return BlockStateProperties.NORTH;
            case SOUTH: return BlockStateProperties.SOUTH;
            case EAST: return BlockStateProperties.EAST;
            case WEST: return BlockStateProperties.WEST;
            case UP: return BlockStateProperties.UP;
            default: return BlockStateProperties.DOWN;
        }
    }

    public PrismiumCableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.WEST, false)
                .setValue(BlockStateProperties.UP, false)
                .setValue(BlockStateProperties.DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.SOUTH,
                BlockStateProperties.EAST, BlockStateProperties.WEST,
                BlockStateProperties.UP, BlockStateProperties.DOWN);
    }

    /**
     * Whether a cable at {@code pos} should render/connect a "wire" toward
     * {@code direction} - true iff the block entity on that side exposes
     * the energy capability on the face pointing back at this cable. See
     * class javadoc for why this is deliberately looser than
     * {@code canReceive()}.
     */
    private static boolean connectsTo(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
        if (neighbor == null) {
            return false;
        }
        return neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).isPresent();
    }

    private static BlockState withConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction direction : DIRECTIONS) {
            state = state.setValue(propertyFor(direction), connectsTo(level, pos, direction));
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), connectsTo(level, pos, direction));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (state.getValue(propertyFor(DIRECTIONS[i]))) {
                mask |= (1 << i);
            }
        }
        return SHAPE_CACHE[mask];
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrismiumCableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PRISMIUM_CABLE.get(), PrismiumCableBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumCableBlockEntity cable)) {
            return InteractionResult.PASS;
        }
        if (player.getItemInHand(hand).isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_cable.status",
                            cable.getEnergyStorage().getEnergyStored(),
                            cable.getEnergyStorage().getMaxEnergyStored()), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
