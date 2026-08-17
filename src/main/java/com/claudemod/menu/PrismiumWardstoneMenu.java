package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumWardstoneBlockEntity;
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
 * Menu for Prismium Wardstone's GUI (session 27), the mod's fifth GUI and
 * the last of the three original consumer blocks (Pylon session 19,
 * Restorer session 20, Wardstone session 21) to get one - see
 * PROGRESS.md session 24/25/26 handoff, option (a): "同じMenu/Screen
 * パターンを消費ブロック3種(Pylon・Restorer・Wardstone)へさらに展開する".
 * With this class, all three consumer blocks (and both storage/generation
 * blocks, Cell and Generator) now have a GUI.
 *
 * Structurally identical to {@link com.claudemod.menu.PrismiumPylonMenu}
 * (not {@link com.claudemod.menu.PrismiumRestorerMenu}'s simpler 2-int
 * shape): Wardstone, like Pylon, has a ticking {@code active}/idle
 * boolean driven by its {@code LIT} blockstate (see
 * {@link PrismiumWardstoneBlockEntity#isActive()} - "is this pulse
 * currently warding") that Restorer has no equivalent of. Same three
 * {@code ContainerData} ints as Pylon: current energy, max energy, and
 * the active-flag (0/1). {@link PrismiumWardstoneBlockEntity#CAPACITY}
 * is 20,000, identical to Pylon's and Wardstone's own value, comfortably
 * under {@code Short.MAX_VALUE} (32,767) on its own - no sync-divisor
 * trick needed here either.
 */
public class PrismiumWardstoneMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by
     * {@link ModMenuTypes#PRISMIUM_WARDSTONE_MENU}'s factory. */
    public PrismiumWardstoneMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /** Server-side constructor, used directly by
     * {@link PrismiumWardstoneBlockEntity#createMenu}. */
    public PrismiumWardstoneMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.PRISMIUM_WARDSTONE_MENU.get(), windowId);
        checkContainerDataCount(data, 3);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumWardstoneBlockEntity wardstone) {
            return wardstone.getContainerData();
        }
        return new SimpleContainerData(3);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    /** Whether the wardstone's most recent pulse actually warded - see
     * {@link PrismiumWardstoneBlockEntity#isActive()} for the exact
     * semantics this mirrors (updated once every
     * {@code PrismiumWardstoneBlockEntity#PULSE_INTERVAL} ticks, not
     * every tick). */
    public boolean isActive() {
        return data.get(2) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_WARDSTONE.get());
    }
}
