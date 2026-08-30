package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumCompressorBlockEntity;
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
 * Menu for Prismium Compressor's GUI (session 70, scheduled), the
 * mod's eighth GUI. Structurally a direct copy of {@link
 * PrismiumSmelterMenu} (see that class's doc for the full rationale
 * behind the slot layout/full player-inventory grid/quickMoveStack
 * shape) - same input/output slot coordinates, same 27+9 player
 * inventory bands, same three-band shift-click routing. The only
 * functional difference from Smelter's menu is which item {@link
 * PrismiumCompressorBlockEntity#isValidInput} accepts into the input
 * slot (Prismium Ingot here, vs. Prismium Shard there).
 */
public class PrismiumCompressorMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int INVENTORY_START = 2;
    private static final int INVENTORY_END = 29; // exclusive
    private static final int HOTBAR_END = 38; // exclusive

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by {@link
     * ModMenuTypes#PRISMIUM_COMPRESSOR_MENU}'s factory. */
    public PrismiumCompressorMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos),
                resolveInventory(inv, pos));
    }

    /** Server-side constructor, used directly by {@link
     * PrismiumCompressorBlockEntity#createMenu}. */
    public PrismiumCompressorMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access,
                                   ItemStackHandler inventory) {
        super(ModMenuTypes.PRISMIUM_COMPRESSOR_MENU.get(), windowId);
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

    /** Client-side {@link ContainerData} for the GUI-open packet's menu
     * factory. <b>Deliberately always a fresh {@link SimpleContainerData}
     * (session #84 bugfix)</b> - a previous version of this method tried
     * to be clever and look up the client's own (locally mirrored) block
     * entity at {@code pos} and reuse its real {@code ContainerData}
     * instance directly, on the theory that this would let the GUI show
     * correct values immediately instead of waiting a tick for the first
     * sync packet. This was wrong and caused every GUI in this mod to
     * appear to "freeze" (energy/progress bars never move, even though
     * the block is actually processing/charging server-side): the real
     * {@code ContainerData}'s {@code set(index, value)} is a deliberate
     * no-op (see that class's own doc - {@code get()} always reads the
     * live authoritative fields directly, which is correct and required
     * for the *server* instance that {@code broadcastChanges()} reads
     * from every tick, but is fatal for the *client* instance: the only
     * way the client ever learns about a changed value is via {@link
     * net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket},
     * which calls {@code AbstractContainerMenu#setData} ->
     * {@code DataSlot#set} -> this very {@code ContainerData#set} - a
     * no-op there means the incoming synced value is silently discarded
     * every time, and {@code get()} keeps returning whatever the client's
     * mirrored block entity's fields happened to start at (typically 0,
     * since this mod never overrides {@code getUpdatePacket()}/{@code
     * onDataPacket()} for NBT-based mirroring either). Forge's own 1.20.1
     * "Menus" doc is explicit that the client menu constructor "should
     * always supply" a fresh {@code SimpleContainerData} - a real,
     * independent {@code int[]}-backed instance whose {@code set()}
     * actually stores the value {@code get()} later returns - which is
     * exactly what makes the normal per-tick sync packets work at all.
     * (Note: {@code resolveInventory} below does *not* have this bug and
     * is intentionally left alone - {@code Slot}/{@code SlotItemHandler}
     * sync writes item stacks with a real {@code set(ItemStack)}, not a
     * no-op, so reusing the client's mirrored block entity's inventory
     * there is harmless and even lets slot contents already known to the
     * client render one tick sooner.) */
    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        return new SimpleContainerData(4);
    }

    private static ItemStackHandler resolveInventory(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumCompressorBlockEntity compressor) {
            return compressor.getInventory();
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
        return Math.min(1f, getProgress() / (float) PrismiumCompressorBlockEntity.PROCESS_TIME_TICKS);
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
            } else if (PrismiumCompressorBlockEntity.isValidInput(stackInSlot)) {
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
        return stillValid(access, player, ModBlocks.PRISMIUM_COMPRESSOR.get());
    }
}
