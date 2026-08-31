package com.claudemod.menu;

import com.claudemod.block.PrismiumChronoflameBlock;
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
 * Menu (server+client shared logic) for Prismium Chronoflame's GUI
 * (scheduled session, direct feedback from こんぺいとう氏 - see
 * {@link PrismiumChronoflameBlock}'s class doc for the full story of why
 * this replaced the old "must hold a Clock" interaction).
 *
 * <p>Zero {@code Slot}s, same reasoning as {@code PrismiumCellMenu} (the
 * mod's first zero-slot menu): this GUI is two buttons and a status line,
 * not an item-transfer interface. Unlike every other menu in this mod,
 * this one is also not backed by any {@code BlockEntity} - Chronoflame
 * remains the plain, stateless {@code Block} it always was (see
 * {@link PrismiumChronoflameBlock}'s class doc on why a BlockEntity would
 * be pure overhead here); the position it needs is only ever used to
 * validate {@link #stillValid} and to play the confirmation sound at the
 * right spot, both handled through {@link ContainerLevelAccess} exactly
 * like every other menu in this mod already does for its own
 * {@code stillValid}.
 *
 * <p><b>Buttons, not slots</b>: {@link #clickMenuButton} is vanilla's own
 * mechanism for a Screen's button widgets to trigger *server-side*
 * effects without this mod needing to invent its own network packet -
 * confirmed this session (WebSearch cross-checked against mappings.dev's
 * 1.20.1-pinned {@code AbstractContainerMenu}/{@code MultiPlayerGameMode}
 * signatures) as the same mechanism vanilla's own Beacon/Loom/Stonecutter
 * screens use for their button widgets: the client calls
 * {@code Minecraft.getInstance().gameMode.handleInventoryButtonClick(
 * containerId, buttonId)}, which sends a
 * {@code ServerboundContainerButtonClickPacket} that the server resolves
 * straight into a call to this method - no {@code SimpleChannel}/custom
 * packet class needed, and this is the first time this mod has needed
 * any of that (every earlier GUI here is a pure status display with no
 * player-triggered action beyond opening/closing it).
 */
public class PrismiumChronoflameMenu extends AbstractContainerMenu {

    /** Button IDs passed to {@link #clickMenuButton} - matched 1:1 by the
     * two {@code Button} widgets {@code PrismiumChronoflameScreen} adds. */
    public static final int BUTTON_ADVANCE = 0;
    public static final int BUTTON_REWIND = 1;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor, used by
     * {@link ModMenuTypes#PRISMIUM_CHRONOFLAME_MENU}'s factory once the
     * open-menu packet (carrying just the BlockPos, see
     * {@link PrismiumChronoflameBlock#use}) arrives. */
    public PrismiumChronoflameMenu(int windowId, Inventory inv, BlockPos pos) {
        this(windowId, inv, resolveData(inv, pos), ContainerLevelAccess.create(inv.player.level(), pos));
    }

    /** Server-side constructor, used directly by the
     * {@link net.minecraft.world.SimpleMenuProvider} built in
     * {@link PrismiumChronoflameBlock#use}, where the real
     * per-player cooldown {@link ContainerData} is already at hand. */
    public PrismiumChronoflameMenu(int windowId, Inventory inv, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.PRISMIUM_CHRONOFLAME_MENU.get(), windowId);
        checkContainerDataCount(data, 1);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    /** Client-side {@link ContainerData} for the GUI-open packet's menu
     * factory. <b>Deliberately always a fresh {@link SimpleContainerData}</b> -
     * see {@code PrismiumCellMenu#resolveData}'s class doc (the v0.31.2
     * frozen-GUI bugfix this mirrors exactly) for why the client-side
     * constructor must never reuse a real, no-op-{@code set()}
     * {@code ContainerData} instance. */
    private static ContainerData resolveData(Inventory inv, BlockPos pos) {
        return new SimpleContainerData(1);
    }

    /** Ticks remaining on the current player's cooldown (0 if none) -
     * see {@link PrismiumChronoflameBlock#tryActivate}. Used by
     * {@code PrismiumChronoflameScreen} to gray out both buttons and show
     * a countdown instead of letting a click on cooldown do nothing with
     * no feedback (the original complaint behind GitHub issue #16). */
    public int getCooldownRemainingTicks() {
        return data.get(0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_ADVANCE && id != BUTTON_REWIND) {
            return false;
        }
        boolean rewind = id == BUTTON_REWIND;
        // clickMenuButton is only ever invoked server-side (the client
        // side of this round trip is a bare network send, see class doc),
        // so access.execute's Level here is always the real ServerLevel -
        // PrismiumChronoflameBlock#tryActivate itself still defensively
        // re-checks instanceof ServerLevel for the same reason its old
        // use()-based implementation did.
        access.execute((level, pos) -> PrismiumChronoflameBlock.tryActivate(level, pos, player, rewind));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRISMIUM_CHRONOFLAME.get());
    }
}
