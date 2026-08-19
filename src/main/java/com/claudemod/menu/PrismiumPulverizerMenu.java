package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumPulverizerBlockEntity;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Menu for Prismium Pulverizer's GUI (session 67), the mod's sixth GUI
 * and first to be a genuine 2-way item-processing screen rather than a
 * pure FE status display (Cell/Pylon/Restorer/Wardstone) or a single
 * auto-consumed fuel slot (Generator, session 58).
 *
 * <p>This is also the mod's <b>first menu with a full player inventory +
 * hotbar slot grid</b>. Every earlier menu in this mod (see {@link
 * PrismiumGeneratorMenu}'s doc) registers zero or one {@code Slot} and
 * always returns {@code ItemStack.EMPTY} from {@code quickMoveStack} -
 * workable for a single manually-fed fuel slot, but for a real 2-slot
 * input/output machine that arrangement would leave shift-click
 * completely inert and (worse - see {@code AbstractContainerMenu#doClick}'s
 * {@code SWAP} case, which indexes into {@code this.slots} assuming the
 * last 9 entries are the hotbar) risks an index-out-of-bounds if a player
 * ever pressed a number-key hotbar-swap while this GUI was open, since a
 * 1-slot menu has no such last-9 range. Adding the standard 27+9 player
 * slots (identical geometry to vanilla's furnace: 8,84 for the main grid,
 * 8,142 for the hotbar) removes that latent risk as a side effect of
 * doing shift-click properly, not merely as a cosmetic nicety.
 *
 * <p>Slot layout: index 0 = input (backed by {@link
 * PrismiumPulverizerBlockEntity#getInventory()} slot 0), index 1 = output
 * (same handler, slot 1 - {@code SlotItemHandler#mayPlace} automatically
 * returns false for it since the handler's own {@code isItemValid}
 * already rejects slot 1, see that block entity's doc, so no custom
 * {@code Slot} subclass is needed here), indices 2-28 = player main
 * inventory, indices 29-37 = hotbar - the same three-band index
 * convention vanilla's own {@code FurnaceMenu} uses, which {@link
 * #quickMoveStack} below relies on directly.
 */
public class PrismiumPulverizerMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int INVENTORY_START = 2;
    private static final int INVENTORY_END = 29; // exclusive
    private static final int HOTBAR_END = 38; // exclusive

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by {@link
     * ModMenuTypes#PRISMIUM_PULVERIZER_MENU}'s factory. */
    public PrismiumPulverizerMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos),
                resolveInventory(inv, pos));
    }

    /** Server-side constructor, used directly by {@link
     * PrismiumPulverizerBlockEntity#createMenu}. */
    public PrismiumPulverizerMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access,
                                   ItemStackHandler inventory) {
        super(ModMenuTypes.PRISMIUM_PULVERIZER_MENU.get(), windowId);
        checkContainerDataCount(data, 4);
        this.data = data;
        this.access = access;
        addDataSlots(data);

        // Machine slots (see class doc for the coordinate choice -
        // matches PrismiumPulverizerScreen's INPUT_SLOT_X/Y and
        // OUTPUT_SLOT_X/Y exactly).
        this.addSlot(new SlotItemHandler(inventory, INPUT_SLOT, 56, 20));
        this.addSlot(new SlotItemHandler(inventory, OUTPUT_SLOT, 116, 20));

        // Player main inventory (3x9), standard vanilla furnace geometry.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 66 + row * 18));
            }
        }
        // Hotbar.
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 124));
        }
    }

    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumPulverizerBlockEntity pulverizer) {
            return pulverizer.getContainerData();
        }
        return new SimpleContainerData(4);
    }

    private static ItemStackHandler resolveInventory(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumPulverizerBlockEntity pulverizer) {
            return pulverizer.getInventory();
        }
        return new ItemStackHandler(2);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    /** Ticks accumulated on the current operation, 0-{@link
     * PrismiumPulverizerBlockEntity#PROCESS_TIME_TICKS}. Read directly
     * against that constant rather than syncing a second "max" value,
     * same shortcut {@link PrismiumGeneratorMenu#getBurnFraction} takes
     * with {@code BURN_TIME_PER_SHARD} - both constants live on a common
     * (non-client-only) class, so referencing them directly from
     * {@code PrismiumPulverizerScreen} needs no network sync. */
    public int getProgress() {
        return data.get(2);
    }

    public float getProgressFraction() {
        return Math.min(1f, getProgress() / (float) PrismiumPulverizerBlockEntity.PROCESS_TIME_TICKS);
    }

    public boolean isActive() {
        return data.get(3) != 0;
    }

    /**
     * Standard three-band shift-click routing (machine slots <->
     * player inventory <-> hotbar), following vanilla {@code
     * FurnaceMenu#quickMoveStack}'s shape exactly - the mod's first
     * working implementation of this method (every earlier menu just
     * returns {@code ItemStack.EMPTY}, see class doc). Shift-clicking an
     * item in the player's own inventory routes it into the input slot
     * only if {@link PrismiumPulverizerBlockEntity#isValidInput} accepts
     * it; shift-clicking either machine slot sends its contents back to
     * the player's inventory/hotbar.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            newStack = stackInSlot.copy();

            if (index == INPUT_SLOT || index == OUTPUT_SLOT) {
                if (!this.moveItemStackTo(stackInSlot, INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (PrismiumPulverizerBlockEntity.isValidInput(stackInSlot)) {
                if (!this.moveItemStackTo(stackInSlot, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < INVENTORY_END) {
                if (!this.moveItemStackTo(stackInSlot, INVENTORY_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < HOTBAR_END) {
                if (!this.moveItemStackTo(stackInSlot, INVENTORY_START, INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stackInSlot);
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_PULVERIZER.get());
    }
}
