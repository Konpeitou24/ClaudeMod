package com.claudemod.block;

import com.claudemod.blockentity.PrismiumRestorerBlockEntity;
import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Block for Prismium Restorer (session 20). See
 * {@link PrismiumRestorerBlockEntity} for the energy-storage side; this
 * class is the block shell plus the player-facing interaction, following
 * the exact same {@code BaseEntityBlock} + no-GUI/action-bar-status shape
 * every other Prismium Energy machine already uses (Cell/Generator/Cable/
 * Pylon, sessions 8-10 and 19).
 *
 * <p>Right-click with an empty hand: opens Prismium Restorer's GUI
 * (session 26, see {@link com.claudemod.menu.PrismiumRestorerMenu}),
 * which reports current/max FE - previously (sessions 20-25) this was an
 * action-bar status message only, same
 * {@code NetworkHooks.openScreen} replacement pattern already applied to
 * {@link PrismiumCellBlock} (session 23), {@link PrismiumGeneratorBlock}
 * (session 24) and {@link PrismiumPylonBlock} (session 25).
 * <p>Right-click holding a Prismium Shard: manually add
 * {@link PrismiumRestorerBlockEntity#SHARD_CHARGE_AMOUNT} FE, same shape
 * as {@link PrismiumCellBlock} / {@link PrismiumPylonBlock}.
 * <p>Right-click holding any other damaged, damageable item: spend FE to
 * restore some of its durability (this block's actual purpose - the
 * second FE consumer after the Pylon). Unlike the empty-hand case this
 * stays an action-bar message, not folded into the GUI - repairing is a
 * one-shot action tied to whatever item is currently in the player's
 * hand, not an ongoing status the GUI's {@code ContainerData} tracks.
 */
public class PrismiumRestorerBlock extends BaseEntityBlock {

    public PrismiumRestorerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrismiumRestorerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumRestorerBlockEntity restorer)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        Item prismiumShard = ModItems.PRISMIUM_SHARD.get();
        PrismiumEnergyStorage storage = restorer.getEnergyStorage();

        if (held.is(prismiumShard)) {
            // 2026-08-31 direct-chat feedback (PROGRESS.md TODO6): direct
            // hand-charging let a player skip Prismium Generator entirely
            // ("Generatorが死にアイテム化している"), so this consumer no
            // longer accepts a shard by hand at all - FE now only arrives
            // through the Generator -> Cable network (see
            // EnergyPushHelper#pushThroughNetwork), restoring the intended
            // Generator/Cable/consumer role split. The shard itself is
            // intentionally left unconsumed (unlike the old branch this
            // replaces) since no energy actually changes hands here.
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_restorer.no_direct_charge"), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            // Session 26: open Restorer's GUI instead of printing a status
            // message - see class doc. The old action-bar status message
            // (current/max FE) is removed rather than kept alongside the
            // GUI, same call as Cell/Generator/Pylon: the GUI is a
            // strictly more capable replacement for "check current status".
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, restorer, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        if (held.isDamageableItem() && held.getDamageValue() > 0) {
            int damage = held.getDamageValue();
            int wanted = Math.min(damage, PrismiumRestorerBlockEntity.MAX_DURABILITY_PER_USE);
            int affordable = storage.getEnergyStored() / PrismiumRestorerBlockEntity.FE_PER_DURABILITY;
            int repaired = Math.min(wanted, affordable);

            if (repaired <= 0) {
                player.displayClientMessage(
                        Component.translatable("message.claudemod.prismium_restorer.no_charge"), true);
                return InteractionResult.CONSUME;
            }

            int cost = repaired * PrismiumRestorerBlockEntity.FE_PER_DURABILITY;
            held.setDamageValue(damage - repaired);
            storage.setEnergy(storage.getEnergyStored() - cost);
            restorer.setChanged();
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_restorer.repaired",
                            repaired, cost, storage.getEnergyStored(), storage.getMaxEnergyStored()), true);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(
                Component.translatable("message.claudemod.prismium_restorer.not_damaged"), true);
        return InteractionResult.CONSUME;
    }
}
