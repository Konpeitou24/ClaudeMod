package com.claudemod.blockentity;

import com.claudemod.energy.EnergyPushHelper;
import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * Block entity for Prismium Cable (session 10): the mod's first relay
 * block for the "Prismium Energy" pillar, closing the gap the session 9
 * handoff notes called out explicitly - Prismium Generator and Prismium
 * Cell could only ever be tested touching each other directly, since
 * nothing in the mod could carry FE across a gap. This block is meant to
 * be chained in a line (or any connected shape) between a source (e.g.
 * Prismium Generator) and a sink (e.g. Prismium Cell) so they no longer
 * have to be placed face-to-face.
 *
 * <p>Design is deliberately symmetric and "dumb", unlike Generator/Cell:
 * a cable has no player interaction and no fuel/charge concept, just a
 * small pass-through buffer. Every neighbor-facing side exposes the same
 * {@link IEnergyStorage} capability with equal maxReceive/maxExtract, so
 * a cable can accept energy pushed into it (e.g. by
 * {@code PrismiumGeneratorBlockEntity}'s own push, or by a future
 * machine that pulls).
 *
 * <p><b>Revised session 55 (GitHub issue #15):</b> the original
 * "every cable independently pushes to its own six neighbors each tick,
 * so a straight run of N cables moves energy one hop per tick rather
 * than instantaneously end-to-end" design (kept below for history) turned
 * out to read as a hard failure rather than a mere delay once a player
 * tried more than a couple of cables in a row - see
 * {@link com.claudemod.energy.EnergyPushHelper#pushThroughNetwork}'s doc
 * for the fix. {@code PrismiumGeneratorBlockEntity} (and this class's own
 * {@link #serverTick}) now walk the whole connected run of cables in a
 * single bounded search and push straight through to the real receiver
 * at the far end, so a cable's own buffer is no longer load-bearing for
 * multi-hop delivery - it mainly exists now to hold energy that
 * momentarily had nowhere to go. Original design note, still true as
 * background: because every cable-to-cable capability exchange is
 * symmetric (including, if applicable, right back the way energy came
 * from), and because a straight per-block relay was the first-pass
 * approach before session 55, this mirrors how many tech-mod cable
 * networks start out before graduating to a shared network graph
 * (union-find over connected cables, cached per-network capacity, etc.) -
 * {@link com.claudemod.energy.EnergyPushHelper#pushThroughNetwork}'s BFS
 * is a lighter-weight step in that direction, not the full graph-cache
 * version, see PROGRESS.md for what a further pass could still do
 * (caching the reachable-receiver set across ticks instead of
 * recomputing the BFS every time a source has energy to push).
 */
public class PrismiumCableBlockEntity extends BlockEntity {

    /** Small on purpose - see class javadoc. A cable is meant to be a
     * conduit, not a reservoir. */
    public static final int CAPACITY = 400;
    /** Equal to CAPACITY so a cable can, in principle, fully drain or
     * fully fill in a single tick if a neighbor has the room/supply. */
    public static final int MAX_TRANSFER = 400;

    private final PrismiumEnergyStorage energyStorage =
            new PrismiumEnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    public PrismiumCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_CABLE.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /**
     * Static server tick, bound the same way as
     * {@link PrismiumGeneratorBlockEntity#serverTick}. Only ever invoked
     * server-side, so no {@code level.isClientSide} guard is needed here
     * either.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumCableBlockEntity cable) {
        if (cable.energyStorage.getEnergyStored() <= 0) {
            return;
        }
        // Session 55 (GitHub issue #15): was pushToNeighbors. A source
        // (Generator) now reaches this cable's whole network directly on
        // its own tick, so in the common case this cable's own buffer
        // stays near-empty and this call is a cheap no-op via the guard
        // above. It still matters for energy that got stuck here because
        // every reachable receiver was momentarily full - retrying via
        // the same network-wide search (starting from this cable) lets
        // that leftover keep looking for somewhere to go instead of only
        // ever being offered to this one cable's six immediate neighbors.
        if (EnergyPushHelper.pushThroughNetwork(level, pos, cable.energyStorage, MAX_TRANSFER)) {
            cable.setChanged();
        }
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
