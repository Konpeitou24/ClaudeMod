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

/**
 * Menu for Prismium Generator's GUI (session 24), the mod's second block
 * to get one after Prismium Cell (session 23, see
 * {@link com.claudemod.menu.PrismiumCellMenu} for the pattern this class
 * follows almost verbatim: zero {@link net.minecraft.world.inventory.Slot}s,
 * a pure status display, fuel is still added by right-clicking the block
 * with a Prismium Shard exactly as before - see
 * {@link com.claudemod.block.PrismiumGeneratorBlock#use}).
 *
 * The interesting difference from Cell, and the reason Generator was
 * picked as the second GUI target (see PROGRESS.md session 23 handoff,
 * "(a) Generatorは燃焼ゲージという2つ目の同期すべき値がある"): this menu
 * syncs **three** {@code ContainerData} ints instead of Cell's two -
 * current energy, max energy, and burn time - giving
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
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /** Server-side constructor, used directly by
     * {@link PrismiumGeneratorBlockEntity#createMenu}. */
    public PrismiumGeneratorMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.PRISMIUM_GENERATOR_MENU.get(), windowId);
        checkContainerDataCount(data, 3);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumGeneratorBlockEntity generator) {
            return generator.getContainerData();
        }
        return new SimpleContainerData(3);
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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_GENERATOR.get());
    }
}
