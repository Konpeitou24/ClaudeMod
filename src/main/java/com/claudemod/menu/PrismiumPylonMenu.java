package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumPylonBlockEntity;
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
 * Menu for Prismium Pylon's GUI (session 25), the mod's third block to
 * get one after Prismium Cell (session 23) and Prismium Generator
 * (session 24) - see {@link com.claudemod.menu.PrismiumGeneratorMenu} for
 * the pattern this class follows almost verbatim: zero
 * {@link net.minecraft.world.inventory.Slot}s, a pure status display,
 * manual shard charging is still done by right-clicking the block with a
 * Prismium Shard exactly as before (see
 * {@link com.claudemod.block.PrismiumPylonBlock#use}) rather than moved
 * into the GUI.
 *
 * Three {@code ContainerData} ints, same count as Generator but a
 * different third value: current energy, max energy, and an
 * active-flag (0/1) mirroring {@link PrismiumPylonBlockEntity#isActive()}
 * instead of Generator's burn-time counter - Pylon has no fuel gauge of
 * its own (it only ever spends FE it was given, it never burns anything),
 * so the third slot only needs to convey a boolean "is this pulse
 * currently radiating" state for the screen's status label/indicator,
 * not a continuously varying quantity. Like Generator (session 24,
 * {@code PrismiumGeneratorBlockEntity#CAPACITY} = 8,000) no
 * {@code ENERGY_SYNC_DIVISOR}-style short-safety trick is needed here
 * either: {@link PrismiumPylonBlockEntity#CAPACITY} is 20,000, still
 * comfortably under {@code Short.MAX_VALUE} (32,767) on its own.
 */
public class PrismiumPylonMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by
     * {@link ModMenuTypes#PRISMIUM_PYLON_MENU}'s factory. */
    public PrismiumPylonMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /** Server-side constructor, used directly by
     * {@link PrismiumPylonBlockEntity#createMenu}. */
    public PrismiumPylonMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.PRISMIUM_PYLON_MENU.get(), windowId);
        checkContainerDataCount(data, 3);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumPylonBlockEntity pylon) {
            return pylon.getContainerData();
        }
        return new SimpleContainerData(3);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    /** Whether the pylon's most recent pulse actually radiated - see
     * {@link PrismiumPylonBlockEntity#isActive()} for the exact
     * semantics this mirrors (updated once every
     * {@code PrismiumPylonBlockEntity#PULSE_INTERVAL} ticks, not every
     * tick). */
    public boolean isActive() {
        return data.get(2) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_PYLON.get());
    }
}
