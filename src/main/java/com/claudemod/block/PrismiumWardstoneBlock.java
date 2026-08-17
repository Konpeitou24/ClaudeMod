package com.claudemod.block;

import com.claudemod.blockentity.PrismiumWardstoneBlockEntity;
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
 * Block for Prismium Wardstone (session 21). See
 * {@link PrismiumWardstoneBlockEntity} for the drain/effect logic; this
 * class is the block shell, copied structurally from
 * {@link PrismiumPylonBlock} (session 19) - same {@code BaseEntityBlock} +
 * reused {@code BlockStateProperties.LIT} pattern already confirmed
 * working by both {@link PrismiumGeneratorBlock} (session 9) and
 * {@link PrismiumPylonBlock} (session 19).
 *
 * <p>Right-click with an empty hand: opens Prismium Wardstone's GUI
 * (session 27, see {@link com.claudemod.menu.PrismiumWardstoneMenu}),
 * which reports current/max FE and whether the wardstone is actively
 * warding - previously (sessions 21-26) this was an action-bar status
 * message only, same {@code NetworkHooks.openScreen} pattern established
 * by {@link PrismiumCellBlock#use} (session 23), {@link
 * PrismiumGeneratorBlock#use} (session 24), {@link PrismiumPylonBlock#use}
 * (session 25) and {@code PrismiumRestorerBlock#use} (session 26). With
 * this change every energy block in the mod (Cell, Generator, Pylon,
 * Restorer, Wardstone) now opens a GUI instead of printing a status
 * message. Right-click holding a Prismium Shard: manually add
 * {@link PrismiumWardstoneBlockEntity#SHARD_CHARGE_AMOUNT} FE, same shape
 * as Cell/Generator/Pylon/Restorer's manual charge - lets a player use a
 * Wardstone standalone before building out a full Generator/Cable
 * network. This action stays outside the GUI, same call made for every
 * other machine's shard-charge interaction so far.
 */
public class PrismiumWardstoneBlock extends BaseEntityBlock {

    public PrismiumWardstoneBlock(Properties properties) {
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
        return new PrismiumWardstoneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PRISMIUM_WARDSTONE.get(), PrismiumWardstoneBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumWardstoneBlockEntity wardstone)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        Item prismiumShard = ModItems.PRISMIUM_SHARD.get();

        if (held.is(prismiumShard)) {
            PrismiumEnergyStorage storage = wardstone.getEnergyStorage();
            int accepted = storage.receiveEnergy(PrismiumWardstoneBlockEntity.SHARD_CHARGE_AMOUNT, true);
            if (accepted <= 0) {
                player.displayClientMessage(
                        Component.translatable("message.claudemod.prismium_wardstone.full"), true);
                return InteractionResult.CONSUME;
            }
            storage.receiveEnergy(accepted, false);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            wardstone.setChanged();
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_wardstone.charged",
                            storage.getEnergyStored(), storage.getMaxEnergyStored()), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            // Session 27: open Wardstone's GUI instead of printing a
            // status message - see class doc.
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, wardstone, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
