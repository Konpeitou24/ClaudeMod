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
 * <b>Session 58 update:</b> a single real {@code Slot} (backed by
 * {@code SlotItemHandler}) was added for a fuel-input item, the mod's
 * first Slot-bearing menu - see the fuel-slot constructor below and
 * {@code PrismiumGeneratorBlockEntity#fuelInventory}'s doc for why. The
 * right-click-to-fuel path described above still works unchanged; the
 * slot is a second, additive way to fuel the same block. Every *other*
 * GUI in the mod (Cell, Wardstone, Pylon) is still a zero-Slot pure
 * status display as originally described.
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
 * 8,000, comfortably inside {@code Short.MAX_VALUE} (32,767) on its own.
 * Burn time, however, is *not* capped by anything else in the block
 * entity (see {@link PrismiumGeneratorBlockEntity#BURN_TIME_SYNC_CAP}'s
 * doc for why it still needed its own short-safety clamp) - a smaller,
 * different-shaped instance of the same short-truncation risk Cell's
 * energy value had in session 23, confirmed again while implementing
 * this menu.
 */
public class PrismiumGeneratorMenu extends AbstractContainerMenu {

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
        this(windowId, inv, data, access, new ItemStackHandler(1));
    }

    /** Full server-side constructor including the real fuel-slot handler
     * (session 58, see {@code PrismiumGeneratorBlockEntity#fuelInventory}'s
     * doc). {@link PrismiumGeneratorBlockEntity#createMenu} calls this
     * overload directly with its actual handler; the shorter overload
     * above exists only so callers that don't care about the fuel slot
     * (there are none left in this codebase, but keeping the narrower
     * signature avoids a needless call-site update) still compile against
     * a harmless throwaway handler. */
    public PrismiumGeneratorMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access,
                                  ItemStackHandler fuelInventory) {
        super(ModMenuTypes.PRISMIUM_GENERATOR_MENU.get(), windowId);
        checkContainerDataCount(data, 5);
        this.data = data;
        this.access = access;
        addDataSlots(data);
        // Session 58: top-right corner of the 176x110 panel (see
        // PrismiumGeneratorScreen - clear of the flame gauge, energy bar,
        // and status/burn-time labels, matching the recessed slot artwork
        // baked into gen_prismium_generator_gui.py at the same
        // coordinates). This is the mod's first ever Slot-bearing menu -
        // every earlier GUI (Cell, Wardstone, Pylon, this one until now)
        // was a pure ContainerData status display with zero Slots, see
        // this class's own doc above (now partially stale - left in place
        // rather than rewritten, since the "zero slots, pure status
        // display" framing is still accurate for every *other* GUI in the
        // mod and is useful context for why this is a notable first).
        this.addSlot(new SlotItemHandler(fuelInventory, 0, 152, 8));
    }

    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumGeneratorBlockEntity generator) {
            return generator.getContainerData();
        }
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
        return new ItemStackHandler(1);
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_GENERATOR.get());
    }
}
