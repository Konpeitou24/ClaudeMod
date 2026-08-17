package com.claudemod.block;

import com.claudemod.blockentity.PrismiumPylonBlockEntity;
import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.registry.ModBlockEntities;
import com.claudemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Block for Prismium Pylon (session 19). See {@link PrismiumPylonBlockEntity}
 * for the drain/effect logic; this class is the block shell, following the
 * exact same {@code BaseEntityBlock} + reused {@code BlockStateProperties.LIT}
 * pattern already confirmed working by {@link PrismiumGeneratorBlock}
 * (sessions 9) - deliberately not inventing a new blockstate property for
 * what is functionally the same "idle vs active" swap.
 *
 * <p>Right-click with an empty hand: opens Prismium Pylon's GUI (session
 * 25, see {@link com.claudemod.menu.PrismiumPylonMenu}), which reports
 * current/max FE and whether the pylon is actively radiating - previously
 * (sessions 19-24) this was an action-bar status message only, same as
 * every un-GUI'd Prismium Energy block still is.
 * Right-click holding a Prismium Shard: manually add
 * {@link PrismiumPylonBlockEntity#SHARD_CHARGE_AMOUNT} FE, same shape as
 * {@link PrismiumCellBlock}'s manual charge - lets a player use a Pylon
 * standalone before building out a full Generator/Cable network.
 */
public class PrismiumPylonBlock extends BaseEntityBlock {

    public PrismiumPylonBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrismiumPylonBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PRISMIUM_PYLON.get(), PrismiumPylonBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumPylonBlockEntity pylon)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        Item prismiumShard = ModItems.PRISMIUM_SHARD.get();

        if (held.is(prismiumShard)) {
            PrismiumEnergyStorage storage = pylon.getEnergyStorage();
            int accepted = storage.receiveEnergy(PrismiumPylonBlockEntity.SHARD_CHARGE_AMOUNT, true);
            if (accepted <= 0) {
                player.displayClientMessage(
                        Component.translatable("message.claudemod.prismium_pylon.full"), true);
                return InteractionResult.CONSUME;
            }
            storage.receiveEnergy(accepted, false);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            pylon.setChanged();
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_pylon.charged",
                            storage.getEnergyStored(), storage.getMaxEnergyStored()), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            // Session 25: open Pylon's GUI instead of printing a status
            // message - same NetworkHooks.openScreen pattern established
            // by PrismiumCellBlock#use (session 23) and
            // PrismiumGeneratorBlock#use (session 24). The old action-bar
            // status message (current/max FE plus active/idle) is removed
            // rather than kept alongside the GUI, same call as those two:
            // the GUI is a strictly more capable replacement for "check
            // current status".
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, pylon, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
