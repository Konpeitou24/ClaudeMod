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
 * a cable can both accept energy pushed into it (e.g. by
 * {@code PrismiumGeneratorBlockEntity}'s own neighbor push, or by a
 * future machine that pulls) and, every server tick, immediately try to
 * push whatever it is currently holding onward to ITS neighbors via the
 * same {@link EnergyPushHelper#pushToNeighbors} helper Generator uses.
 * The buffer capacity is intentionally small (see {@link #CAPACITY}) so a
 * cable behaves like a wire that energy passes *through*, not a small
 * battery that meaningfully stores charge - any energy left sitting in a
 * cable across ticks is essentially just "in transit" waiting for a
 * downstream neighbor to have room.
 *
 * <p>Known simplification worth flagging in PROGRESS.md: because every
 * cable independently pushes to every capability-exposing neighbor each
 * tick (including, if applicable, right back the way energy came from),
 * a straight run of N cables moves energy one hop per tick rather than
 * instantaneously end-to-end - functionally correct but introduces a
 * small transmission delay proportional to cable count. This mirrors how
 * most tech-mod cable networks with per-block ticking (rather than a
 * shared network graph) behave, and was an accepted tradeoff to avoid
 * building actual network/graph logic (union-find over connected cables,
 * cached per-network capacity, etc.) in this first pass.
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
        if (EnergyPushHelper.pushToNeighbors(level, pos, cable.energyStorage, MAX_TRANSFER)) {
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
