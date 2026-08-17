package com.claudemod.block;

import com.claudemod.blockentity.PrismiumCellBlockEntity;
import com.claudemod.energy.PrismiumEnergyStorage;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Block for Prismium Cell (session 8). See {@link PrismiumCellBlockEntity}
 * for the capability/energy-storage side; this class only adds the block
 * shell (renders as a normal cube-model block backed by a block entity,
 * per the confirmed 1.20.1 BaseEntityBlock pattern - PROGRESS.md session
 * 8) and the player-facing manual charge/status interaction described
 * there.
 *
 * Right-click with an empty hand: open Prismium Cell's GUI (session 23,
 * see {@link PrismiumCellBlockEntity#createMenu}) - before session 23
 * this only printed an action-bar status message, since no GUI existed
 * yet; the GUI is a strictly more capable replacement for that same
 * "check current charge" use case, so the old message branch was removed
 * rather than kept alongside it.
 * Right-click holding a Prismium Shard: consume one shard and add a fixed
 * chunk of energy (SHARD_CHARGE_AMOUNT), unless already full. This is a
 * deliberately simple "manual generator" stand-in until a real automatic
 * generator exists (see PROGRESS.md for the roadmap), and lets the whole
 * capability round-trip (receive -> persist -> read back) be exercised
 * without needing a second machine to pair the cell with. Left as a
 * direct right-click action rather than folded into the GUI, since a GUI
 * button to consume a held item is more machinery than this needs.
 */
public class PrismiumCellBlock extends BaseEntityBlock {

    /** FE added per Prismium Shard consumed via the manual charge
     * interaction. Chosen so filling an empty cell from scratch (100_000 /
     * 4_000 = 25 shards) is a deliberate but not absurd resource sink;
     * unbalanced/untested like every other number introduced this
     * session. */
    public static final int SHARD_CHARGE_AMOUNT = 4000;

    public PrismiumCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrismiumCellBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PrismiumCellBlockEntity cell)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        Item prismiumShard = ModItems.PRISMIUM_SHARD.get();

        if (held.is(prismiumShard)) {
            PrismiumEnergyStorage storage = cell.getEnergyStorage();
            int accepted = storage.receiveEnergy(SHARD_CHARGE_AMOUNT, true);
            if (accepted <= 0) {
                player.displayClientMessage(
                        Component.translatable("message.claudemod.prismium_cell.full"), true);
                return InteractionResult.CONSUME;
            }
            storage.receiveEnergy(accepted, false);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            cell.setChanged();
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_cell.charged",
                            storage.getEnergyStored(), storage.getMaxEnergyStored()), true);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty()) {
            // NetworkHooks.openScreen is the correct 1.20.1 API for this
            // (confirmed against the version-pinned docs.minecraftforge.net/
            // en/1.20.1/gui/menus/ page specifically while implementing
            // this, not the generic "1.20.x"/"latest" docs branches - those
            // instead document ServerPlayer#openMenu, which only exists
            // from 1.20.2 onward and does NOT compile here). The
            // FriendlyByteBuf-writing overload is required because this
            // menu type was registered via IForgeMenuType.create (see
            // ModMenuTypes) and needs the BlockPos on the client side.
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, cell, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
