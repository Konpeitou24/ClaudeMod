package com.claudemod.energy;

import com.claudemod.blockentity.PrismiumCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

    /** Every how many ticks {@link #visualizeFlow} is allowed to actually
     * run its (cheap but non-zero) cable-path BFS and emit particles.
     * GitHub issue #15 comment (session 56 handoff item 3-b): "電力の流れが
     * 目視できない" (can't see energy actually moving through the network).
     * Purely cosmetic and server-authoritative (uses
     * {@link ServerLevel#sendParticles}, which itself only actually
     * bothers networking to clients that are within tracking range - see
     * vanilla's own {@code ChunkMap}), so throttling this hard is just
     * extra insurance against a large network doing a visualization BFS
     * every single tick on top of the real energy-push BFS. */
    private static final int FLOW_PARTICLE_INTERVAL_TICKS = 10;

    /** Cap on how many of the (potentially many) cable positions found
     * along a network get a particle spawned per call - a long run of
     * cable does not need a particle at every single block to read as
     * "energy is flowing here", and capping this keeps the packet count
     * bounded regardless of network size.
     *
     * @deprecated no longer used now that {@link #visualizeFlow} animates a
     * single traveling pulse (bounded by {@link #PULSE_TRAIL_LENGTH}
     * instead) rather than sampling random positions; kept only so any
     * external reference/tuning note pointing at this constant's old value
     * doesn't silently disappear. */
    @Deprecated
    private static final int MAX_FLOW_PARTICLE_POSITIONS = 6;

    /** How many game ticks the pulse head spends at each cable-path index
     * before advancing to the next one - i.e. its speed. Smaller = faster
     * travel. Must divide evenly into how often {@link #visualizeFlow} is
     * actually invoked ({@link #FLOW_PARTICLE_INTERVAL_TICKS}, gated by the
     * caller) to avoid the head appearing to skip steps unevenly; 5 means
     * the head advances 2 path-positions between each throttled call at
     * the default 10-tick interval. */
    private static final int PULSE_STEP_TICKS = 5;

    /** How many trailing positions behind the pulse head still show a
     * (dimmer) particle, giving the pulse a visible length/tail instead of
     * a single point. Also used as the head's "wind-up" distance before
     * the path start, so the pulse appears to emerge from the source
     * rather than popping in already mid-network. */
    private static final int PULSE_TRAIL_LENGTH = 3;

    /**
     * Purely cosmetic companion to {@link #pushThroughNetwork}: walks the
     * same kind of connected-cable run (a lighter, receiver-agnostic BFS -
     * it does not move or query any energy, so it is safe to call even on
     * ticks where {@code pushThroughNetwork} found nothing to push) and
     * spawns a handful of small spark particles along a sampled subset of
     * the cable positions it finds, so a player looking at a Prismium
     * Cable run can actually see that something is moving through it
     * instead of only reading numbers off a GUI. Intentionally separate
     * from {@code pushThroughNetwork} rather than folded into it (e.g. by
     * changing that method's return type to include the visited-cable
     * list): keeps the already-fixed (GitHub issue #15) energy-movement
     * BFS untouched and easy to reason about in isolation, at the cost of
     * a second, independent, much-throttled traversal.
     *
     * <p>Callers are expected to only invoke this from a *source*
     * (Generator) tick, not from every individual cable's own tick -
     * calling it once per network per tick (from the source) is enough
     * for the visual effect and avoids O(cable count) redundant BFS runs
     * across the same physical network that calling it from every cable
     * would cause. Callers should also gate the call behind
     * {@code level.getGameTime() % FLOW_PARTICLE_INTERVAL_TICKS == 0}
     * (see {@link #FLOW_PARTICLE_INTERVAL_TICKS}) before invoking this at
     * all, so the BFS itself is skipped entirely on most ticks rather
     * than run-but-throttled-after-the-fact.
     *
     * <p>No-op on the logical client and no-op if {@code level} is not a
     * {@link ServerLevel} (particles are spawned via
     * {@code ServerLevel#sendParticles} so they actually reach nearby
     * clients, matching the pattern already used by
     * {@code PrismiumFeatherstoneHandler}/{@code PrismiumEmberguardHandler}/
     * {@code PrismiumVitastoneHandler}/{@code PrismiumGuardianCharmHandler}
     * elsewhere in this mod).
     *
     * <p><b>2026-09-01 update (PROGRESS.md TODO/section-5 item: "ケーブルの
     * エネルギーフロー視覚化が不十分"):</b> the original implementation
     * spawned a handful of {@link ParticleTypes#ELECTRIC_SPARK} bursts at
     * random positions sampled from the cable run every call, which reads
     * as "energy is present somewhere in this network" but not as flow -
     * nothing conveyed direction or speed. This method now instead
     * animates a single traveling pulse: the ordered {@code cablePath}
     * list already reflects BFS hop-distance from {@code startPos} (the
     * source), so it doubles as a "how far along the network" ordering.
     * A pulse head position is derived deterministically from
     * {@link Level#getGameTime()} (so every client/observer sees the same
     * animation without any extra networking) and walks that list from
     * index -{@link #PULSE_TRAIL_LENGTH} (just before the first cable, i.e.
     * emerging from the source) up to {@code cablePath.size() - 1} (the far
     * end of the network) before looping back to start a new pulse. A
     * bright {@link ParticleTypes#ELECTRIC_SPARK} marks the head and a
     * short trail of dimmer {@link ParticleTypes#GLOW} particles follows
     * behind it, so a player watching a cable run sees a spark visibly
     * travel from the generator outward rather than a static sprinkle.
     *
     * <p><b>Not yet verified in-game</b> (no local build available in this
     * sandbox - see PROGRESS.md): the particle types, pulse speed
     * ({@link #PULSE_STEP_TICKS}), and whether calling this once every
     * {@link #FLOW_PARTICLE_INTERVAL_TICKS} ticks still reads as smooth
     * continuous travel rather than a series of visible jumps are all
     * first-guess values.
     */
    /** Convenience overload using the same hop cap as {@link #pushThroughNetwork(Level, BlockPos, PrismiumEnergyStorage, int)}'s default, so callers don't have to duplicate that magic number. */
    public static void visualizeFlow(Level level, BlockPos startPos) {
        visualizeFlow(level, startPos, DEFAULT_MAX_CABLE_HOPS);
    }

    public static void visualizeFlow(Level level, BlockPos startPos, int maxCableHops) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Set<BlockPos> visitedCables = new HashSet<>();
        visitedCables.add(startPos);
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(startPos);
        List<BlockPos> cablePath = new ArrayList<>();

        int hops = 0;
        while (!frontier.isEmpty() && hops < maxCableHops) {
            int frontierSize = frontier.size();
            for (int i = 0; i < frontierSize; i++) {
                BlockPos current = frontier.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos neighborPos = current.relative(direction);
                    BlockEntity neighbor = level.getBlockEntity(neighborPos);
                    if (neighbor instanceof PrismiumCableBlockEntity && visitedCables.add(neighborPos)) {
                        frontier.add(neighborPos);
                        cablePath.add(neighborPos);
                    }
                }
            }
            hops++;
        }

        if (cablePath.isEmpty()) {
            return;
        }

        // Deterministic traveling pulse: head index ranges from
        // -PULSE_TRAIL_LENGTH (just before the source end of the path, so
        // the pulse visibly "emerges" from the generator) to
        // cablePath.size() - 1 (the far end), then wraps to start a new
        // pulse. Using getGameTime() directly (rather than a stored/random
        // offset) keeps this fully server-authoritative and consistent
        // across repeated calls with no extra state to track.
        int totalSteps = cablePath.size() + PULSE_TRAIL_LENGTH;
        long step = level.getGameTime() / PULSE_STEP_TICKS;
        int headIndex = (int) (step % totalSteps) - PULSE_TRAIL_LENGTH;

        for (int offset = 0; offset <= PULSE_TRAIL_LENGTH; offset++) {
            int idx = headIndex - offset;
            if (idx < 0 || idx >= cablePath.size()) {
                continue;
            }
            BlockPos p = cablePath.get(idx);
            double px = p.getX() + 0.5;
            double py = p.getY() + 0.5;
            double pz = p.getZ() + 0.5;
            if (offset == 0) {
                // Bright head of the pulse.
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        px, py, pz, 2, 0.15, 0.15, 0.15, 0.01);
            } else {
                // Dimmer trailing glow, sparser the further back it is.
                serverLevel.sendParticles(ParticleTypes.GLOW,
                        px, py, pz, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }
}
