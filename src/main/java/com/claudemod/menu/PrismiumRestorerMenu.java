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
