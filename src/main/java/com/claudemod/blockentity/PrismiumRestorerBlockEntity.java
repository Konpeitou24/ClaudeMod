package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumRestorerMenu;
import com.claudemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * Block entity for Prismium Restorer (session 20): the mod's *second* FE
 * consumer, after Prismium Pylon (session 19). Where the Pylon spends FE
 * passively over time on a beacon-like area buff, the Restorer spends FE
 * only in direct response to a player action - right-clicking it while
 * holding a damaged item repairs some of that item's durability, paid for
 * out of the buffer below. This follows the "second consumer" idea
 * explicitly floated in PROGRESS.md's session 19 handoff (section 5,
 * "議論したい論点": "FEで耐久を回復する装置").
 *
 * <p>Deliberately has <b>no {@code BlockEntityTicker}</b>, unlike the
 * Pylon. All of this block's behaviour happens synchronously inside
 * {@link com.claudemod.block.PrismiumRestorerBlock#use}, the same shape as
 * {@link PrismiumCellBlockEntity} (session 8) - a passive energy buffer
 * with no per-tick logic of its own. It still participates fully in the
 * Cell/Generator/Cable network: {@code PrismiumCableBlockEntity}'s own
 * ticker pushes FE into this block purely through the
 * {@code IEnergyStorage} capability below, so no ticker is needed on the
 * receiving side for that to work (this mirrors how the Cell has received
 * pushed energy since session 8 without ever ticking itself).
 *
 * <p><b>Unverified</b> (see PROGRESS.md): whether the FE-per-durability
 * cost and per-use durability cap below feel good in play, and whether
 * repairing armor/tools this way interacts correctly with enchantments
 * (e.g. Mending) - only compiled and code-reviewed, not playtested.
 *
 * <p>Session 26 adds a GUI (see {@link PrismiumRestorerMenu}), the mod's
 * fourth after Prismium Cell (session 23), Prismium Generator (session
 * 24) and Prismium Pylon (session 25). Unlike Pylon this block has no
 * ticking "active" state to expose (all of its behaviour is a synchronous
 * response to a right-click, see class doc above) - so its
 * {@link ContainerData} mirrors {@link PrismiumCellBlockEntity}'s minimal
 * shape almost exactly (2 ints: current/max energy, no third slot),
 * rather than Pylon's 3-int shape with its extra boolean.
 */
public class PrismiumRestorerBlockEntity extends BlockEntity implements MenuProvider {

    /** Total FE capacity. */
    public static final int CAPACITY = 30_000;
    /** Max FE this block will accept per {@code receiveEnergy} call, both
     * from the capability (a Cable pushing in) and the manual shard
     * charge below. */
    public static final int MAX_RECEIVE = 2_000;
    /** FE added per Prismium Shard via the manual charge interaction. */
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    /** FE spent per point of durability restored. */
    public static final int FE_PER_DURABILITY = 25;
    /** Max durability points restored in a single right-click, regardless
     * of how much energy is available or how damaged the item is - keeps
     * one click from instantly fully repairing a very damaged item in a
     * single hit even when the buffer is full. */
    public static final int MAX_DURABILITY_PER_USE = 64;

    // maxExtract is 0: like the Pylon, this is a pure sink - it never
    // exposes energy for another machine to pull out.
    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    /** Backs this block entity's GUI (session 26). {@link PrismiumRestorerBlockEntity#CAPACITY}
     * (30,000) is, like Pylon's (20,000) and Generator's (8,000), safely
     * under {@code Short.MAX_VALUE} (32,767) on its own - so unlike
     * Prismium Cell (100,000 capacity) no {@code ENERGY_SYNC_DIVISOR}-style
     * scaling is needed here, the raw FE values can be synced directly.
     * {@code set} is a no-op, same reasoning as every other machine's
     * {@code ContainerData}: the screen only ever reads, the underlying
     * state changes through {@link com.claudemod.block.PrismiumRestorerBlock#use}
     * (and the energy capability) only. */
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Deliberately a no-op - correct now that this block's Menu
            // class's client-side constructor never reuses this real
            // instance to receive synced values (session #84 bugfix: it
            // used to, which silently discarded every synced update and
            // froze this GUI's bars - see that Menu's resolveData doc).
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ContainerData getContainerData() {
        return containerData;
    }

    public PrismiumRestorerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_RESTORER.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.claudemod.prismium_restorer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumRestorerMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergy(tag.getInt("Energy"));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }
}
