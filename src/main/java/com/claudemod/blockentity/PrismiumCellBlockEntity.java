package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
public class PrismiumCellBlockEntity extends BlockEntity {

    /** Total FE capacity. Round number in line with other early-tier
     * storage blocks in similar tech mods; no balancing pass done yet. */
    public static final int CAPACITY = 100_000;
    /** Max FE moved per tick, either via capability or the manual charge
     * interaction. */
    public static final int MAX_TRANSFER = 800;

    private final PrismiumEnergyStorage energyStorage =
            new PrismiumEnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    public PrismiumCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_CELL.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
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
