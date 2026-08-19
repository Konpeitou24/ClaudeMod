package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumSmelterBlockEntity;
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
 * Menu for Prismium Smelter's GUI (session 68), the mod's seventh GUI.
 * Structurally a direct copy of {@link PrismiumPulverizerMenu} (session
 * 67, see that class's doc for the full rationale behind the slot
 * layout/full player-inventory grid/quickMoveStack shape) - same input/
 * output slot coordinates, same 27+9 player inventory bands, same
 * three-band shift-click routing. The only functional difference from
 * Pulverizer's menu is which item {@link PrismiumSmelterBlockEntity#isValidInput}
 * accepts into the input slot (Prismium Shard here, vs. raw ore there).
 */
public class PrismiumSmelterMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int INVENTORY_START = 2;
    private static final int INVENTORY_END = 29; // exclusive
    private static final int HOTBAR_END = 38; // exclusive

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by {@link
     * ModMenuTypes#PRISMIUM_SMELTER_MENU}'s factory. */
    public PrismiumSmelterMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos),
                resolveInventory(inv, pos));
    }

    /** Server-side constructor, used directly by {@link
     * PrismiumSmelterBlockEntity#createMenu}. */
    public PrismiumSmelterMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access,
                                ItemStackHandler inventory) {
        super(ModMenuTypes.PRISMIUM_SMELTER_MENU.get(), windowId);
        checkContainerDataCount(data, 4);
        this.data = data;
        this.access = access;
        addDataSlots(data);

        this.addSlot(new SlotItemHandler(inventory, INPUT_SLOT, 56, 20));
        this.addSlot(new SlotItemHandler(inventory, OUTPUT_SLOT, 116, 20));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 66 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 124));
        }
    }

    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumSmelterBlockEntity smelter) {
            return smelter.getContainerData();
        }
        return new SimpleContainerData(4);
    }

    private static ItemStackHandler resolveInventory(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumSmelterBlockEntity smelter) {
            return smelter.getInventory();
        }
        return new ItemStackHandler(2);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    public int getProgress() {
        return data.get(2);
    }

    public float getProgressFraction() {
        return Math.min(1f, getProgress() / (float) PrismiumSmelterBlockEntity.PROCESS_TIME_TICKS);
    }

    public boolean isActive() {
        return data.get(3) != 0;
    }

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
            } else if (PrismiumSmelterBlockEntity.isValidInput(stackInSlot)) {
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
        return stillValid(access, player, ModBlocks.PRISMIUM_SMELTER.get());
    }
}
