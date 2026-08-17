package com.claudemod.menu;

import com.claudemod.blockentity.PrismiumCellBlockEntity;
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
 * Menu (server+client shared logic) for Prismium Cell's GUI (session 23,
 * see {@link ModMenuTypes#PRISMIUM_CELL_MENU} for why Cell was chosen as
 * the first block to get one).
 *
 * Deliberately has zero {@code Slot}s: the GUI is a pure status display
 * (current/max FE), not an item-transfer interface - charging the cell
 * with a Prismium Shard remains a direct right-click action (see
 * {@link com.claudemod.block.PrismiumCellBlock#use}), unchanged from
 * session 8. This keeps the menu free of the slot/quickMove bookkeeping
 * that would otherwise be this mod's first, since there is nothing to
 * move between inventories. {@link #quickMoveStack} is only implemented
 * because {@link AbstractContainerMenu} declares it abstract; with no
 * slots it can never actually be invoked by the vanilla shift-click
 * handling.
 *
 * FE amount is synced to the client via {@link ContainerData} (2 ints:
 * current energy, max energy) using {@link #addDataSlots}, the same
 * mechanism vanilla's furnace uses for burn time/cook time - this runs
 * automatically once per server tick via
 * {@code AbstractContainerMenu#broadcastChanges()} for as long as a
 * player has this menu open, no extra networking code needed on this
 * mod's side.
 *
 * IMPORTANT (found via Forge's own 1.20.x GUI docs while writing this,
 * not something the mod had needed to know before): a {@code DataSlot}
 * (and therefore every value read through {@code ContainerData}) is only
 * synced as a **short** - the 16 high-order bits of whatever int is
 * returned by {@code ContainerData#get} are silently discarded on the
 * network. Prismium Cell's capacity (100,000 FE) is already past
 * {@code Short.MAX_VALUE} (32,767), so syncing raw FE values directly
 * would wrap/truncate for most of the cell's actual charge range - this
 * would have been a real, easy-to-miss bug. {@link PrismiumCellBlockEntity#getContainerData()}
 * divides both values by {@link PrismiumCellBlockEntity#ENERGY_SYNC_DIVISOR}
 * before exposing them (100,000 / 8 = 12,500, safely inside short range
 * even if a future block's capacity is several times larger), and
 * {@link #getEnergy()} / {@link #getMaxEnergy()} multiply back by the same
 * divisor here. The trade-off is the displayed FE amount is only accurate
 * to the nearest {@code ENERGY_SYNC_DIVISOR} (8) FE, which is imperceptible
 * for a five-to-six-digit energy value and an acceptable rounding error for
 * a status display.
 */
public class PrismiumCellMenu extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by {@link ModMenuTypes#PRISMIUM_CELL_MENU}'s
     * factory once the open-menu packet (carrying just the BlockPos, see
     * {@link com.claudemod.block.PrismiumCellBlock#use}) arrives. */
    public PrismiumCellMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /** Server-side constructor, used directly by
     * {@link PrismiumCellBlockEntity#createMenu} where the block entity's
     * own {@link ContainerData} instance is already at hand (avoids a
     * redundant block-entity lookup by BlockPos). */
    public PrismiumCellMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.PRISMIUM_CELL_MENU.get(), windowId);
        checkContainerDataCount(data, 2);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    /** Client-side: {@code inv.player.level()} is the client level, and the
     * block entity there is a plain data-only copy synced by vanilla's
     * normal block entity sync, so its own {@code ContainerData} works
     * fine as the read side of this menu too (only {@code get} is ever
     * called client-side; {@code addDataSlots} overwrites the values from
     * the server each tick regardless of what this returns initially). */
    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof PrismiumCellBlockEntity cell) {
            return cell.getContainerData();
        }
        return new SimpleContainerData(2);
    }

    /** Current FE, already scaled back up from the short-safe synced value
     * (see class doc) - accurate to the nearest {@code ENERGY_SYNC_DIVISOR}. */
    public int getEnergy() {
        return data.get(0) * PrismiumCellBlockEntity.ENERGY_SYNC_DIVISOR;
    }

    /** Max FE (capacity). 100,000 is exactly divisible by
     * {@code ENERGY_SYNC_DIVISOR} (8), so this one is always exact, unlike
     * {@link #getEnergy()}. */
    public int getMaxEnergy() {
        return data.get(1) * PrismiumCellBlockEntity.ENERGY_SYNC_DIVISOR;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_CELL.get());
    }
}
