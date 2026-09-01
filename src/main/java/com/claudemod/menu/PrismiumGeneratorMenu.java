package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumGeneratorBlockEntity;
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
 * Menu for Prismium Generator's GUI (session 24), the mod's second block
 * to get one after Prismium Cell (session 23, see
 * {@link com.claudemod.menu.PrismiumCellMenu} for the pattern this class
 * originally followed almost verbatim: zero {@link net.minecraft.world.inventory.Slot}s,
 * a pure status display, fuel added only by right-clicking the block with
 * a Prismium Shard - see {@link com.claudemod.block.PrismiumGeneratorBlock#use}).
 *
 * <b>Session 58 update:</b> a single real {@code Slot} (backed by
 * {@code SlotItemHandler}) was added for a fuel-input item. The
 * right-click-to-fuel path described above still works unchanged; the
 * slot is a second, additive way to fuel the same block.
 *
 * <b>Session (TODO6 followup) update:</b> the single fuel slot from
 * session 58, combined with this menu never adding any player-inventory
 * {@code Slot}s, made bulk fuel-loading impractical - with no player
 * slots registered here, the player's own inventory/hotbar was not even
 * clickable while this GUI was open (see {@code AbstractContainerScreen}:
 * it only draws/hit-tests the {@code Slot}s a menu actually registers),
 * so shift-click had nothing to do and manual placement was the only
 * option. This session adds {@link PrismiumGeneratorBlockEntity#FUEL_SLOT_COUNT}
 * (4) fuel slots plus the standard 27+9 player inventory/hotbar grid,
 * following the exact pattern {@link PrismiumPulverizerMenu} established
 * for the mod's first player-inventory-bearing menu (session 67) -
 * see that class's doc for the shift-click index-range reasoning this
 * class's {@link #quickMoveStack} now mirrors.
 *
 * The interesting difference from Cell, and the reason Generator was
 * picked as the second GUI target (see PROGRESS.md session 23 handoff,
 * "(a) Generatorは燃焼ゲージという2つ目の同期すべき値がある"): this menu
 * syncs {@code ContainerData} ints - current energy, max energy, and
 * burn time, plus (session, GitHub issue #15 follow-up comment - see
 * {@link #getLastGenerated()}/{@link #getLastPushed()}) two more for this
 * tick's actual generated/pushed FE, five in total, versus Cell's two -
 * giving
 * {@link com.claudemod.client.screen.PrismiumGeneratorScreen} a second,
 * differently-shaped gauge (a vertical "flame" bar) to draw alongside the
 * horizontal energy bar already established by Cell's screen.
 *
 * Unlike Cell, no {@code ENERGY_SYNC_DIVISOR} is needed for the energy
 * values here: {@link PrismiumGeneratorBlockEntity#CAPACITY} is only
 * 16,000, comfortably inside {@code Short.MAX_VALUE} (32,767) on its own.
 * Burn time, however, is *not* capped by anything else in the block
 * entity (see {@link PrismiumGeneratorBlockEntity#BURN_TIME_SYNC_CAP}'s
 * doc for why it still needed its own short-safety clamp) - a smaller,
 * different-shaped instance of the same short-truncation risk Cell's
 * energy value had in session 23, confirmed again while implementing
 * this menu.
 */
public class PrismiumGeneratorMenu extends AbstractContainerMenu {

    private static final int FUEL_SLOT_COUNT = PrismiumGeneratorBlockEntity.FUEL_SLOT_COUNT;
    private static final int INVENTORY_START = FUEL_SLOT_COUNT; // 4
    private static final int INVENTORY_END = INVENTORY_START + 27; // exclusive, 31
    private static final int HOTBAR_END = INVENTORY_END + 9; // exclusive, 40

    // Fuel slot socket coordinates: a single horizontal row, centered,
    // sitting in its own band between the FE-amount text and the player
    // inventory grid (see PrismiumGeneratorScreen's matching sockets
    // baked into gen_prismium_generator_gui.py at these same
    // coordinates). Deliberately its own row rather than tucked next to
    // the flame gauge/status text - that top area's status/burn/rate
    // text lines can run wide enough (see PrismiumGeneratorScreen's
    // status/burn/rate text at x=30) to have risked overlapping a
    // top-right slot cluster; a dedicated row below the energy bar has
    // no such risk and reads more clearly as "this is where fuel goes"
    // besides.
    private static final int[][] FUEL_SLOT_POS = {
            {52, 102}, {70, 102}, {88, 102}, {106, 102},
    };

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by
     * {@link ModMenuTypes#PRISMIUM_GENERATOR_MENU}'s factory. */
    public PrismiumGeneratorMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos),
                resolveFuelInventory(inv, pos));
    }

    /** Server-side constructor, used directly by
     * {@link PrismiumGeneratorBlockEntity#createMenu}. */
    public PrismiumGeneratorMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        this(windowId, inv, data, access, new ItemStackHandler(FUEL_SLOT_COUNT));
    }

    /** Full server-side constructor including the real fuel-slot handler
     * (session 58, expanded to {@link PrismiumGeneratorBlockEntity#FUEL_SLOT_COUNT}
     * slots plus a player inventory grid in the TODO6 followup session -
     * see class doc). {@link PrismiumGeneratorBlockEntity#createMenu}
     * calls this overload directly with its actual handler; the shorter
     * overload above exists only so callers that don't care about the
     * fuel slots (there are none left in this codebase, but keeping the
     * narrower signature avoids a needless call-site update) still
     * compile against a harmless throwaway handler. */
    public PrismiumGeneratorMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access,
                                  ItemStackHandler fuelInventory) {
        super(ModMenuTypes.PRISMIUM_GENERATOR_MENU.get(), windowId);
        checkContainerDataCount(data, 5);
        this.data = data;
        this.access = access;
        addDataSlots(data);

        for (int i = 0; i < FUEL_SLOT_COUNT; i++) {
            this.addSlot(new SlotItemHandler(fuelInventory, i, FUEL_SLOT_POS[i][0], FUEL_SLOT_POS[i][1]));
        }

        // Player main inventory (3x9) and hotbar - standard vanilla
        // furnace-family geometry, matching PrismiumGeneratorScreen's
        // taller (TODO6 followup) panel. See PrismiumPulverizerMenu's doc
        // for why this mod treats adding these as more than cosmetic (the
        // SWAP hotbar-number-key case in AbstractContainerMenu#doClick
        // indexes into the last 9 slots assuming a hotbar is present).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 128 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 190));
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
     * (Note: {@code resolveFuelInventory} below does *not* have this bug
     * and is intentionally left alone - {@code Slot}/{@code SlotItemHandler}
     * sync writes item stacks with a real {@code set(ItemStack)}, not a
     * no-op, so reusing the client's mirrored block entity's inventory
     * there is harmless and even lets slot contents already known to the
     * client render one tick sooner.) */
    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        return new SimpleContainerData(5);
    }

    /** Client-side resolution of the block entity's real fuel-inventory
     * handler, mirroring {@link #resolveData}'s pattern exactly (including
     * the same harmless-dummy fallback for the split-tick window where the
     * client menu factory can run before the server's block entity data
     * has arrived - see that method). */
    private static ItemStackHandler resolveFuelInventory(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumGeneratorBlockEntity generator) {
            return generator.getFuelInventory();
        }
        return new ItemStackHandler(FUEL_SLOT_COUNT);
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    /** Raw synced burn time in ticks, already clamped to
     * {@link PrismiumGeneratorBlockEntity#BURN_TIME_SYNC_CAP} server-side -
     * see that constant's doc. */
    public int getBurnTime() {
        return data.get(2);
    }

    /** Fraction (0..1) of a single shard's burn duration
     * ({@link PrismiumGeneratorBlockEntity#BURN_TIME_PER_SHARD}) currently
     * queued, clamped at 1. Deliberately different semantics from
     * vanilla's furnace flame icon: furnace tracks *the currently burning
     * item's own duration counting down to empty and resets to full when
     * the next item starts*, but this block entity only tracks one
     * cumulative burnTime counter (see
     * {@link PrismiumGeneratorBlockEntity#addFuel()} - stacks like furnace
     * fuel but never resets a "per item" baseline). So this gauge instead
     * reads as "how many shards' worth of fuel (0 to 1+) are currently
     * queued", saturating at a full bar once a full shard or more remains
     * rather than ever visually resetting mid-burn. This is a simpler,
     * honestly-scoped alternative to reproducing furnace's per-item
     * tracking, not a bug - documented here so a future session doesn't
     * "fix" it into matching furnace behavior without weighing the
     * tradeoff first. */
    public float getBurnFraction() {
        return Math.min(1f, getBurnTime() / (float) PrismiumGeneratorBlockEntity.BURN_TIME_PER_SHARD);
    }

    public int getBurnSeconds() {
        return getBurnTime() / 20;
    }

    /** FE this generator actually added to its own buffer on the most
     * recently-ticked server tick (0 most ticks it isn't burning, or
     * once the buffer is already full). See
     * {@link PrismiumGeneratorBlockEntity#lastGenerated}'s doc - part of
     * this session's response to the GitHub issue #15 follow-up comment
     * about the buffer reading 0 once a hungry consumer is attached. */
    public int getLastGenerated() {
        return data.get(3);
    }

    /** FE this generator actually pushed out to the network on the most
     * recently-ticked server tick (0 if nothing moved). See
     * {@link PrismiumGeneratorBlockEntity#lastPushed}'s doc. */
    public int getLastPushed() {
        return data.get(4);
    }

    /** GitHub issue #8 ("発電できない" - opening the Generator's GUI
     * shows no sign that it is actually generating): mirrors
     * {@link PrismiumGeneratorBlockEntity#serverTick}'s own condition for
     * whether *this* tick would add FE (queued burn time, and buffer not
     * already full) so {@link com.claudemod.client.screen.PrismiumGeneratorScreen}
     * can show an explicit status label instead of leaving the player to
     * infer activity from the flame gauge and energy bar alone - the same
     * explicit-status treatment {@link PrismiumPylonMenu#isActive()} and
     * {@link PrismiumWardstoneMenu} already give Pylon/Wardstone. */
    public boolean isGenerating() {
        return getBurnTime() > 0 && getEnergy() < getMaxEnergy();
    }

    /**
     * Standard three-band shift-click routing (fuel slots <-> player
     * inventory <-> hotbar), following {@link PrismiumPulverizerMenu#quickMoveStack}'s
     * shape (itself modeled on vanilla {@code FurnaceMenu#quickMoveStack})
     * almost exactly, adapted for a *range* of interchangeable fuel slots
     * instead of one fixed input slot: shift-clicking a Prismium Shard in
     * the player's own inventory routes it into the first fuel slot with
     * room ({@link #moveItemStackTo} already searches the whole
     * [0, FUEL_SLOT_COUNT) range for one), and shift-clicking any fuel
     * slot sends its contents back to the player's inventory/hotbar. The
     * mod's first working implementation of this method for this menu -
     * every earlier version just returned {@code ItemStack.EMPTY} (see
     * class doc history above).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            newStack = stackInSlot.copy();

            if (index < INVENTORY_START) {
                if (!this.moveItemStackTo(stackInSlot, INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (PrismiumGeneratorBlockEntity.isValidFuel(stackInSlot)) {
                if (!this.moveItemStackTo(stackInSlot, 0, INVENTORY_START, false)) {
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
        return stillValid(access, player, ModBlocks.PRISMIUM_GENERATOR.get());
    }
}
