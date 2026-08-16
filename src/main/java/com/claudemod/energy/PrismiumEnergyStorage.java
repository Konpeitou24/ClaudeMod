package com.claudemod.energy;

import net.minecraftforge.energy.EnergyStorage;

/**
 * Thin subclass of Forge's {@link EnergyStorage} that adds a way to
 * directly set the stored amount. The base class only exposes
 * {@code energy} as {@code protected} with no public setter, but
 * {@link com.claudemod.blockentity.PrismiumCellBlockEntity} needs to
 * restore a persisted value from NBT on load rather than route it through
 * receiveEnergy/extractEnergy (which are for normal gameplay energy flow
 * and are capped by maxReceive/maxExtract).
 *
 * API confirmed against 1.20.1 sources (see PROGRESS.md, session 8):
 * {@code net.minecraftforge.energy.EnergyStorage} does NOT implement NBT
 * serialization itself - no readFromNBT/writeToNBT on the Forge class,
 * unlike the older standalone CoFH RF-API EnergyStorage of the same simple
 * name. The BlockEntity is responsible for persisting the int itself.
 */
public class PrismiumEnergyStorage extends EnergyStorage {

    public PrismiumEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract, 0);
    }

    /**
     * Directly sets the stored energy, clamped to [0, capacity]. Only meant
     * to be used when restoring from NBT - normal gameplay energy flow
     * should go through receiveEnergy/extractEnergy so maxReceive/
     * maxExtract limits are respected.
     */
    public void setEnergy(int value) {
        this.energy = Math.max(0, Math.min(value, this.capacity));
    }
}
