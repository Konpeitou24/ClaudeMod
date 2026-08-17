package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumCellMenu;
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
 * Block entity for Prismium Cell (session 8): the first piece of the
 * "Prismium Energy" roadmap pillar (PROGRESS.md section 1, item 2),
 * untouched since session 1. Deliberately the "minimal first step"
 * suggested in the session 7 handoff notes: a block entity that exposes
 * {@link IEnergyStorage} through the standard Forge capability system,
 * with no GUI and no automatic generation yet. Any future machine or
 * cable added to the mod can push/pull energy through the capability
 * exactly like it would with a vanilla-adjacent FE-compatible block. In
 * the meantime, since there is no generator or GUI to observe it with,
 * players can manually charge the cell by right-clicking it with a
 * Prismium Shard and check its charge by right-clicking empty-handed (see
 * {@link com.claudemod.block.PrismiumCellBlock#use}) - this doubles as the
 * only way to verify the capability plumbing actually works this session.
 *
 * Session 23 adds a real GUI ({@link PrismiumCellMenu} /
 * {@link com.claudemod.client.screen.PrismiumCellScreen}) as an
 * additional way to see the same energy state, on top of (not replacing)
 * the shard-charge interaction; see {@link #createMenu} and
 * {@link #getContainerData()}.
 *
 * API notes (confirmed against 1.20.1-pinned sources, see PROGRESS.md
 * session 8): {@code ForgeCapabilities.ENERGY}
 * (net.minecraftforge.common.capabilities) is the non-deprecated
 * capability token for 1.20.1 - the older
 * {@code net.minecraftforge.energy.CapabilityEnergy.ENERGY} still compiles
 * but just forwards to this one, and is deprecated for removal.
 * {@code saveAdditional}/{@code load} take a bare {@code CompoundTag} in
 * 1.20.1 (no {@code HolderLookup.Provider} parameter - that was added in
 * 1.20.5+, outside this mod's target version).
 */
public class PrismiumCellBlockEntity extends BlockEntity implements MenuProvider {

    /** Total FE capacity. Round number in line with other early-tier
     * storage blocks in similar tech mods; no balancing pass done yet. */
    public static final int CAPACITY = 100_000;
    /** Max FE moved per tick, either via capability or the manual charge
     * interaction. */
    public static final int MAX_TRANSFER = 800;

    /** Divisor applied to FE values before exposing them through
     * {@link #getContainerData()} - see {@link PrismiumCellMenu}'s class
     * doc for why this exists (ContainerData/DataSlot network sync is
     * effectively short-limited, and CAPACITY already exceeds
     * Short.MAX_VALUE). Must match the multiplier used by
     * {@link PrismiumCellMenu#getEnergy()}/{@code #getMaxEnergy()}. */
    public static final int ENERGY_SYNC_DIVISOR = 8;

    private final PrismiumEnergyStorage energyStorage =
            new PrismiumEnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    /** Backs this block entity's GUI (session 23). Index 0 = current
     * energy / ENERGY_SYNC_DIVISOR, index 1 = max energy /
     * ENERGY_SYNC_DIVISOR. {@code set} is intentionally a no-op: the
     * screen never writes back through this interface, it only ever
     * reads (the only way energy actually changes is through the
     * capability or the shard-charge interaction, both of which go
     * through {@link PrismiumEnergyStorage} directly). */
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() / ENERGY_SYNC_DIVISOR;
                case 1 -> energyStorage.getMaxEnergyStored() / ENERGY_SYNC_DIVISOR;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the screen's perspective, see field doc.
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public PrismiumCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_CELL.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ContainerData getContainerData() {
        return containerData;
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.claudemod.prismium_cell");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumCellMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition));
    }
}
