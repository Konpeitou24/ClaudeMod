package com.claudemod.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Small shared utility for "actively push my buffered FE into whichever
 * neighbors expose the energy capability" - the exact behavior
 * {@link com.claudemod.blockentity.PrismiumGeneratorBlockEntity} introduced
 * in session 9 as a private method, and that
 * {@link com.claudemod.blockentity.PrismiumCableBlockEntity} (session 10)
 * now needs too for its relay behavior. Pulled out here rather than left
 * duplicated in both classes so the next machine that needs to push energy
 * doesn't have to copy-paste it a third time.
 *
 * <p>Behavior notes carried over unchanged from the original Generator
 * implementation (see PROGRESS.md session 9 for the API citations this was
 * originally verified against): iterates all six {@link Direction}s,
 * queries {@code ForgeCapabilities.ENERGY} on the neighboring block entity
 * using {@code direction.getOpposite()} (the face of the *neighbor* that
 * faces back towards the pushing block), and moves energy via
 * {@code receiveEnergy}/{@code extractEnergy} respecting each side's own
 * transfer caps. Skips neighbors with no block entity, no capability, or
 * whose {@code canReceive()} is false (e.g. Prismium Generator itself,
 * which deliberately sets {@code maxReceive} to 0 - see
 * {@code PrismiumGeneratorBlockEntity}, so pushing back into a generator is
 * naturally a no-op rather than something this helper needs to special-case).
 */
public final class EnergyPushHelper {

    private EnergyPushHelper() {
    }

    /**
     * Pushes up to {@code maxExtractPerTick} FE out of {@code storage} into
     * whichever of the six neighbors of {@code pos} expose the energy
     * capability and can currently accept it. Returns whether any energy
     * actually moved (callers use this to decide whether to call
     * {@code setChanged()}).
     */
    public static boolean pushToNeighbors(Level level, BlockPos pos, PrismiumEnergyStorage storage, int maxExtractPerTick) {
        int budget = Math.min(maxExtractPerTick, storage.getEnergyStored());
        boolean moved = false;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) {
                break;
            }
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) {
                continue;
            }
            LazyOptional<IEnergyStorage> cap = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
            IEnergyStorage neighborStorage = cap.orElse(null);
            if (neighborStorage == null || !neighborStorage.canReceive()) {
                continue;
            }
            int toSend = Math.min(budget, storage.getEnergyStored());
            int accepted = neighborStorage.receiveEnergy(toSend, false);
            if (accepted > 0) {
                storage.extractEnergy(accepted, false);
                budget -= accepted;
                moved = true;
            }
        }
        return moved;
    }
}
