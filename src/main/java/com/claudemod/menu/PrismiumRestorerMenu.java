package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumRestorerBlockEntity;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for Prismium Restorer's GUI (session 26), the mod's fourth block
 * to get one after Prismium Cell (session 23), Prismium Generator
 * (session 24) and Prismium Pylon (session 25) - continuing the
 * "同じMenu/Screenパターンを消費ブロック3種(Pylon・Restorer・Wardstone)へ
 * さらに展開する" plan recorded in PROGRESS.md's session 24/25 handoffs.
 *
 * Structurally closest to {@link PrismiumCellMenu}, not
 * {@link PrismiumPylonMenu}: Restorer has no {@code BlockEntityTicker}
 * and no ticking "active/idle" boolean to expose (see
 * {@link PrismiumRestorerBlockEntity}'s class doc - all of its behaviour
 * is a synchronous response to a right-click), so its
 * {@link ContainerData} only carries 2 ints (current/max energy), same
 * count as Cell, unlike Pylon/Generator's 3. Repairing an item and
 * charging with a Prismium Shard both remain direct right-click actions
 * on the block (see {@link com.claudemod.block.PrismiumRestorerBlock#use}),
 * unchanged from session 20 - this GUI is a pure status display, exactly
 * like Cell's, with zero {@link net.minecraft.world.inventory.Slot}s.
 *
 * Like Pylon (20,000) and Generator (8,000), {@link PrismiumRestorerBlockEntity#CAPACITY}
 * (30,000) is safely under {@code Short.MAX_VALUE} (32,767) so - unlike
 * Cell (100,000) - no {@code ENERGY_SYNC_DIVISOR}-style scaling is needed;
 * {@link #getEnergy()}/{@link #getMaxEnergy()} return the synced ints
 * unscaled.
 */
public class PrismiumRestorerMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by
     * {@link ModMenuTypes#PRISMIUM_RESTORER_MENU}'s factory. */
    public PrismiumRestorerMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /** Server-side constructor, used directly by
     * {@link PrismiumRestorerBlockEntity#createMenu}. */
    public PrismiumRestorerMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.PRISMIUM_RESTORER_MENU.get(), windowId);
        checkContainerDataCount(data, 2);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumRestorerBlockEntity restorer) {
            return restorer.getContainerData();
        }
        return new SimpleContainerData(2);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_RESTORER.get());
    }
}
