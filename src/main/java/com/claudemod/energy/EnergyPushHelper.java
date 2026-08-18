package com.claudemod.energy;

import com.claudemod.blockentity.PrismiumCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** Max cable hops {@link #pushThroughNetwork} will walk out from a
     * source before giving up on that call - a safety/perf bound, not a
     * gameplay-tuned number. 128 blocks of cable is already a very large
     * network for this mod's current scale; this just prevents a
     * pathological loop-free-but-enormous network (or a future bug that
     * creates one) from making a single tick's push scan unboundedly
     * many blocks. */
    private static final int DEFAULT_MAX_CABLE_HOPS = 128;

    /**
     * GitHub issue #15 (session 55): "ケーブルが隣接する6方向にしか影響しない" -
     * cable only ever reaches its immediate neighbors. Root cause: prior
     * to this method, both {@link com.claudemod.blockentity.PrismiumGeneratorBlockEntity}
     * and {@link com.claudemod.blockentity.PrismiumCableBlockEntity} called
     * {@link #pushToNeighbors}, which only ever looks at the six blocks
     * touching the pushing block. A chain of cables therefore had to
     * relay energy one hop per tick (cable N receives this tick, cable
     * N+1 doesn't see it until cable N's *own* tick call, one game tick
     * later), each hop additionally bottlenecked by that cable's own
     * small {@link com.claudemod.blockentity.PrismiumCableBlockEntity#CAPACITY}/
     * {@code MAX_TRANSFER} (400 FE). Over more than a couple of cables
     * this reads as "power just doesn't arrive" rather than "arrives
     * slowly", matching the issue report.
     *
     * <p>This method fixes that by treating a whole run of connected
     * {@link com.claudemod.blockentity.PrismiumCableBlockEntity}s as a single
     * conduit rather than a chain of independent tiny buffers: starting
     * from {@code startPos}, it breadth-first-searches outward through
     * only cable block entities (bounded by {@code maxCableHops}), and at
     * every step also records any *non-cable* neighbor that exposes
     * {@code ForgeCapabilities.ENERGY} and currently {@code canReceive()}.
     * Once the search finishes, the caller's per-tick budget is handed
     * out directly to those real receivers in the order they were found -
     * i.e. energy now reaches a Cell/Restorer/Wardstone at the far end of
     * a cable run in the same tick it left the source (capped by the
     * source's own maxExtractPerTick, same as before), instead of
     * crawling forward one cable per tick. Individual cables no longer
     * need to hold a meaningful buffer for multi-hop relay to work - see
     * {@link com.claudemod.blockentity.PrismiumCableBlockEntity#serverTick},
     * which now also calls this method (starting from itself) so any
     * energy a cable is still sitting on for other reasons (e.g. every
     * receiver in range was momentarily full) keeps getting retried
     * network-wide rather than only offered to its own six neighbors.
     *
     * <p>Correctness notes: each physical receiver position is only ever
     * added to the result list once even if the BFS reaches it via
     * multiple cable paths (a looped or meshed network), so a receiver
     * cannot be double-credited - every FE actually moved is still
     * extracted exactly once from {@code storage} via
     * {@code storage.extractEnergy}, identical bookkeeping to
     * {@link #pushToNeighbors}, just fanned out to a farther set of
     * targets. A generator's own {@code maxReceive = 0} (see
     * {@link com.claudemod.blockentity.PrismiumGeneratorBlockEntity}) means
     * it is naturally skipped as a receiver even when the BFS walks back
     * past it, exactly as before.
     *
     * <p>Deliberately not moved to a background thread despite GitHub
     * issue #15's "別スレッドを使用して" (use a separate thread) suggestion:
     * {@code Level}/{@code BlockEntity} access (including capability
     * queries and {@code getBlockEntity}) is not thread-safe in
     * Forge/vanilla 1.20.1 - it must happen on the server tick thread.
     * Bounding the search (both the hop cap here and the early-exit once
     * {@code budget} reaches 0) is this session's answer to the
     * performance half of issue #15 instead: doing meaningfully less
     * work per tick (one bounded BFS instead of N independent per-cable
     * capability scans repeated every tick even when nothing changed) is
     * a safe way to cut cost without introducing cross-thread world
     * access. Not benchmarked - see PROGRESS.md.
     */
    public static boolean pushThroughNetwork(Level level, BlockPos startPos, PrismiumEnergyStorage storage, int maxExtractPerTick) {
        return pushThroughNetwork(level, startPos, storage, maxExtractPerTick, DEFAULT_MAX_CABLE_HOPS);
    }

    /** Overload of {@link #pushThroughNetwork(Level, BlockPos, PrismiumEnergyStorage, int)}
     * exposing the hop cap explicitly, mainly so it can be tuned or
     * exercised at a smaller bound without touching the default. */
    public static boolean pushThroughNetwork(Level level, BlockPos startPos, PrismiumEnergyStorage storage,
                                              int maxExtractPerTick, int maxCableHops) {
        int budget = Math.min(maxExtractPerTick, storage.getEnergyStored());
        if (budget <= 0) {
            return false;
        }

        Set<BlockPos> visitedCables = new HashSet<>();
        visitedCables.add(startPos);
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(startPos);

        Set<BlockPos> visitedReceivers = new HashSet<>();
        List<IEnergyStorage> receivers = new ArrayList<>();

        int hops = 0;
        while (!frontier.isEmpty() && hops < maxCableHops) {
            int frontierSize = frontier.size();
            for (int i = 0; i < frontierSize; i++) {
                BlockPos current = frontier.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos neighborPos = current.relative(direction);
                    BlockEntity neighbor = level.getBlockEntity(neighborPos);
                    if (neighbor == null) {
                        continue;
                    }
                    if (neighbor instanceof PrismiumCableBlockEntity) {
                        if (visitedCables.add(neighborPos)) {
                            frontier.add(neighborPos);
                        }
                        continue;
                    }
                    if (!visitedReceivers.add(neighborPos)) {
                        // Already reached this exact block via a shorter or
                        // equal path - avoid querying/crediting it twice.
                        continue;
                    }
                    LazyOptional<IEnergyStorage> cap = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
                    IEnergyStorage neighborStorage = cap.orElse(null);
                    if (neighborStorage != null && neighborStorage.canReceive()) {
                        receivers.add(neighborStorage);
                    }
                }
            }
            hops++;
        }

        if (receivers.isEmpty()) {
            return false;
        }

        boolean moved = false;
        for (IEnergyStorage receiver : receivers) {
            if (budget <= 0) {
                break;
            }
            int toSend = Math.min(budget, storage.getEnergyStored());
            if (toSend <= 0) {
                break;
            }
            int accepted = receiver.receiveEnergy(toSend, false);
            if (accepted > 0) {
                storage.extractEnergy(accepted, false);
                budget -= accepted;
                moved = true;
            }
        }
        return moved;
    }
}
