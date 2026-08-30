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
