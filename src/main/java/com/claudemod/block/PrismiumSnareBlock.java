package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Block for Prismium Snare (session 64): the mod's first genuine
 * "gimmick/trap" block, filling the long-standing gap in the roadmap's
 * item 6 ("新ブロック/ギミック: 装飾ブロック、罠、ダンジョン用ギミックブロック")
 * that had only ever been touched by purely decorative flora
 * ({@link PrismiumBloomBlock}, {@link PrismiumSpikeBlock},
 * {@link PrismLilyBlock}, {@link PrismVineBlock}, {@link PrismBrambleBlock}
 * - none of which have any gameplay effect on contact).
 *
 * <p>Visually and structurally it deliberately looks like one more piece
 * of alien flora at a glance (same cross-quad, no-collision, instabreak
 * silhouette family as the plants above) so that stumbling into one in
 * the Prism Realm reads as "I wasn't looking closely enough," not as an
 * obviously telegraphed trap block. Camouflage is the whole point of a
 * snare.
 *
 * <p><b>Trigger behaviour</b>: tracked via the {@link #TRIGGERED} boolean
 * state (same mechanism as {@code WATERLOGGED} on
 * {@link PrismBrambleBlock}/{@link PrismLilyBlock}, just repurposed).
 * While armed ({@code triggered=false}), any {@link LivingEntity} whose
 * hitbox intersects the block (via {@link #entityInside}, the same vanilla
 * hook {@code SweetBerryBushBlock}/{@code PowderSnowBlock} use for
 * walk-through contact effects - a long-stable, unchanged-for-years API,
 * chosen deliberately over inventing something new) causes the snare to
 * fire exactly once: it swaps to {@code triggered=true} (a visually
 * "spent/wilted" state, permanently harmless until broken and replaced),
 * plays a trap-click sound, spawns a small particle burst, and applies
 * Slowness + Poison to the entity.
 *
 * <p><b>Deliberately avoids constructing a {@code DamageSource}</b>, same
 * reasoning {@code PrismiumWardstoneBlockEntity}'s class doc lays out at
 * length: this codebase has never constructed one itself (only ever read
 * one off an existing event in {@code PrismiumSwordHandler}), so directly
 * dealing damage here would be new, unverified API surface in a session
 * with no way to compile-check locally. Poison achieves the same "this
 * was dangerous to blunder into" feel (it *does* deal damage over time)
 * while only touching the {@code addEffect}/{@code MobEffectInstance}
 * combo this mod has already proven compiles correctly (Pylon session 19,
 * Wardstone session 21, Pulse Charm session 63).
 *
 * <p>Obtainable two ways: rare Prism Realm worldgen (see
 * {@code data/claudemod/worldgen/placed_feature/prismium_snare_placed.json},
 * count 1 vs Bramble's 2 - deliberately rarer since it is meant to be an
 * occasional "gotcha," not a common sight), and a crafting recipe (Prism
 * Bramble + Prismium Shard + 2 String) so players can deliberately plant
 * their own snares as a defensive gimmick, not just find them.
 *
 * <p>Session 64 also makes it waterloggable from day one (unlike Bramble/
 * Lily/Spike's original ship states, which needed a retroactive fix per
 * PROGRESS.md §4-29/session 48) - copies {@link PrismBrambleBlock}'s
 * exact {@code SimpleWaterloggedBlock} implementation.
 *
 * <p><b>Unverified</b> (see PROGRESS.md): whether the Slowness/Poison
 * amplifiers and durations feel fair rather than punishing, whether
 * {@code entityInside} actually fires reliably given {@code noCollission()}
 * (believed correct by analogy with vanilla's SweetBerryBush/PowderSnow,
 * which use the identical properties/hook combo, but never exercised by
 * this codebase before), whether the "triggered" wilted texture reads
 * clearly as "already sprung, safe now" at a glance, and whether the
 * worldgen rarity feels right. All zero real-game playtesting, same
 * caveat as every other feature in this mod so far.
 */
public class PrismiumSnareBlock extends Block implements net.minecraft.world.level.block.SimpleWaterloggedBlock {

    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 11, 13);

    /** Slowness IV for 5 seconds - strong enough to feel like a real snare
     * without permanently stranding the victim. */
    private static final int SLOWNESS_DURATION_TICKS = 100;
    private static final int SLOWNESS_AMPLIFIER = 3;
    /** Poison I for 4 seconds - chip damage (never lethal on its own,
     * vanilla Poison never reduces an entity below 1 HP) that sells the
     * "you just triggered something" moment without a DamageSource. */
    private static final int POISON_DURATION_TICKS = 80;
    private static final int POISON_AMPLIFIER = 0;

    public PrismiumSnareBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(TRIGGERED, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == net.minecraft.world.level.material.Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER,
                    net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? net.minecraft.world.level.material.Fluids.WATER.getSource(false)
                : super.getFluidState(state);
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

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || state.getValue(TRIGGERED)) {
            return;
        }
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        level.setBlock(pos, state.setValue(TRIGGERED, true), 3);

        living.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER));
        living.addEffect(new MobEffectInstance(
                MobEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER));

        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.7F, 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D,
                    12, 0.3D, 0.2D, 0.3D, 0.05D);
        }
    }
}
