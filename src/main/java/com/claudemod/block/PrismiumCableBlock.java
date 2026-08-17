package com.claudemod.block;

import com.claudemod.blockentity.PrismiumCableBlockEntity;
import com.claudemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Block for Prismium Cable (session 10). See
 * {@link PrismiumCableBlockEntity} for the relay logic; this class adds
 * the block shell and the mod's first non-full-cube hitbox/model (a
 * centered post, see {@link #SHAPE} and
 * {@code models/block/prismium_cable.json}) - every earlier block in the
 * mod used the default full-cube shape. Deliberately does NOT attempt a
 * fence/pane-style connected-texture model (per-direction boolean
 * blockstate properties + multipart JSON + neighbor-aware shape) in this
 * first pass; every cable renders identically regardless of what it's
 * next to. That's a known, intentional simplification - see PROGRESS.md.
 *
 * <p>Right-click with an empty hand: report the cable's current buffered
 * charge via an action-bar message, same "no GUI, use chat feedback"
 * convention as Prismium Cell/Generator - mostly useful for confirming
 * energy really is passing through a given segment.
 */
public class PrismiumCableBlock extends BaseEntityBlock {

    /** A 8x8x8 post centered in the block (4..12 on every axis), rather
     * than the full 0..16 cube every earlier block in the mod used - see
     * class javadoc. */
    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

    public PrismiumCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
