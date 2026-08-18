package com.claudemod.blockentity;

import com.claudemod.energy.EnergyPushHelper;
import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumGeneratorMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * Block entity for Prismium Generator (session 9): the mod's first
 * {@link net.minecraft.world.level.block.entity.BlockEntityTicker} and
 * first block that automatically pushes Forge Energy into a neighboring
 * capability provider, rather than relying on a player right-clicking to
 * move energy by hand. Closes the loop opened by Prismium Cell (session
 * 8, see PROGRESS.md section 1 item 2): the Cell can *store* FE and
 * expose the capability, but until now nothing in the mod ever exercised
 * the "external push" side of that capability - every FE transfer so far
 * was the player manually feeding a Cell a Prismium Shard. This class is
 * meant to be placed directly adjacent to a Prismium Cell (or any future
 * FE-consuming machine) and left alone.
 *
 * <p>Design: burning a Prismium Shard grants {@link #BURN_TIME_PER_SHARD}
 * ticks of burn time (deliberately reusing vanilla coal's 1600-tick
 * duration as a familiar reference point - see
 * {@link com.claudemod.block.PrismiumGeneratorBlock#use}). While burning,
 * each tick adds {@link #GENERATION_PER_TICK} FE to a small internal
 * buffer (capacity {@link #CAPACITY}), then every tick (burning or not)
 * up to {@link #MAX_EXTRACT} FE from that buffer is distributed across
 * whichever of the six neighbors expose {@code ForgeCapabilities.ENERGY}.
 * A full shard therefore yields 1600 * 10 = 16,000 FE total - four times
 * {@code PrismiumCellBlock.SHARD_CHARGE_AMOUNT} (4,000 FE) for the same
 * one shard fed directly into a Cell by hand. That's an intentional
 * tradeoff, not an oversight: manual feeding is instant but stingy,
 * automating it with this block is slower (burns over ~80 real-world
 * seconds) but noticeably more efficient in total FE per shard - the
 * usual "automation pays off over time" incentive from the tech-mod
 * genre, here expressed with the mod's very first machine pair rather
 * than through a crafting-cost bump. All these numbers (burn ticks,
 * generation rate, buffer size, push rate) are first-guess estimates with
 * no playtesting behind them, exactly like Prismium Cell's numbers - see
 * PROGRESS.md.
 *
 * <p>If the internal buffer is already full, a burning generator pauses
 * (does not decrement burn time or add energy) rather than wasting the
 * tick's worth of generation - mirrors the idea that a real generator
 * with nowhere to put its output would stop consuming fuel, and means an
 * un-connected generator (no neighbor able to receive) will not silently
 * burn through a player's shards for nothing once its small buffer tops
 * out.
 *
 * <p>API notes (confirmed via WebSearch against 1.20.1-era Forge
 * documentation/tutorials, session 9 - this is the mod's first use of
 * both APIs): {@code BaseEntityBlock#createTickerHelper} is the standard
 * way to hand back a {@code BlockEntityTicker} bound to a static
 * {@code tick(Level, BlockPos, BlockState, T)} method, returning
 * {@code null} on the client since this block entity has no client-side
 * behavior. Reading a neighbor's capability is
 * {@code level.getBlockEntity(neighborPos).getCapability(ForgeCapabilities.ENERGY,
 * direction.getOpposite())} - the passed side is which face of the
 * *neighbor* is being touched, i.e. the side facing back towards this
 * generator, not this generator's own side. Session 10 pulled this
 * neighbor-pushing logic out into {@link EnergyPushHelper#pushToNeighbors}
 * so {@link com.claudemod.blockentity.PrismiumCableBlockEntity} could reuse
 * it verbatim instead of copy-pasting a third time; behavior is unchanged.
 */
public class PrismiumGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    /** Ticks of burn time granted per Prismium Shard fed in. Matches
     * vanilla coal's 1600-tick (80 second) burn duration on purpose - a
     * familiar reference point for anyone who has used a furnace, even
     * though this mod's generator does something different with the
     * heat. */
    public static final int BURN_TIME_PER_SHARD = 1600;
    /** FE generated per tick while burning. See class javadoc for why
     * 1600 * 10 = 16,000 FE/shard is deliberately more than
     * PrismiumCellBlock's 4,000 FE/shard manual-charge amount. */
    public static final int GENERATION_PER_TICK = 10;
    /** Internal buffer capacity. GitHub issue #15 (session 55): originally
     * 8,000 - exactly half of a full shard's documented 16,000 FE total
     * yield (see class javadoc above, "A full shard therefore yields
     * 1600 * 10 = 16,000 FE total"). Because {@link #serverTick} pauses
     * burning (and therefore generation) once the buffer is full rather
     * than discarding the overflow, a lone unconnected generator fed one
     * shard would silently stop at 8,000/8,000 "full" looking finished,
     * then - the moment something started draining it - resume burning
     * and eventually emit another 8,000 FE from the very same shard's
     * still-queued burn time. A player watching the buffer would see it
     * drain to 0 and a downstream Cell end up with 16,000 FE total from
     * "one shard that looked like it only made 8,000", which reads
     * exactly like a duplication bug even though every individual FE
     * transfer was correct (see {@link EnergyPushHelper#pushThroughNetwork}
     * doc - no double-crediting happens there either). Raised to match
     * {@link #BURN_TIME_PER_SHARD} * {@link #GENERATION_PER_TICK} exactly
     * so one full shard now fits in the buffer without ever needing to
     * pause mid-burn (assuming nothing drains it faster than it can
     * fill), removing the confusing pause-then-resume behavior at its
     * root rather than papering over it with a UI indicator. Still
     * small relative to Prismium Cell's 100,000 FE - this is a buffer
     * sized for "one shard's worth", not a general-purpose battery. */
    public static final int CAPACITY = BURN_TIME_PER_SHARD * GENERATION_PER_TICK;
    /** Max FE pushed to *all* neighbors combined, per tick. */
    public static final int MAX_EXTRACT = 200;

    /** Ceiling applied to the burn-time value exposed through
     * {@link #getContainerData()} (session 24, see
     * {@link PrismiumGeneratorMenu}'s class doc). Unlike energy (capped at
     * {@link #CAPACITY} = 16,000 (session 55, was 8,000), still comfortably short-safe), burnTime
     * is not capped anywhere else in this class - {@link #addFuel()} adds
     * {@link #BURN_TIME_PER_SHARD} unconditionally, so a player who feeds
     * dozens of shards at once (or repeatedly before any of it burns off)
     * really could push the raw value past {@code Short.MAX_VALUE}
     * (32,767 ticks, about 27 minutes), which would silently wrap/truncate
     * over the network exactly like the FE-value bug avoided for Prismium
     * Cell in session 23. Clamping here avoids that; a burn-time gauge
     * saturating at "still well over 27 minutes of fuel queued" is
     * indistinguishable from correct in practice, so no precision is lost
     * for any realistic amount of fuel.
     */
    public static final int BURN_TIME_SYNC_CAP = Short.MAX_VALUE;

    // maxReceive is 0: this generator only ever fills its own buffer via
    // the internal burn logic below (PrismiumEnergyStorage#setEnergy,
    // which bypasses maxReceive on purpose), never by another machine
    // pushing FE into it - it is a source, not a sink.
    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, 0, MAX_EXTRACT);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    private int burnTime = 0;

    /** Backs this block entity's GUI (session 24, following the pattern
     * {@link com.claudemod.blockentity.PrismiumCellBlockEntity} established
     * in session 23). Three slots instead of Cell's two: index 0/1 are
     * current/max energy (both already well inside short range - see
     * {@link #CAPACITY} - so unlike Cell no division is needed here),
     * index 2 is burnTime clamped to {@link #BURN_TIME_SYNC_CAP} (see that
     * constant's doc for why). {@code set} is a no-op for the same reason
     * as Cell's: the screen only ever reads, the underlying state changes
     * through {@link #serverTick} and {@link #addFuel()} only. */
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> Math.min(burnTime, BURN_TIME_SYNC_CAP);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the screen's perspective, see field doc.
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ContainerData getContainerData() {
        return containerData;
    }

    public PrismiumGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_GENERATOR.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /** Adds one shard's worth of burn time. Cumulative - feeding multiple
     * shards stacks up burn time like vanilla furnace fuel. */
    public void addFuel() {
        burnTime += BURN_TIME_PER_SHARD;
        setChanged();
    }

    public int getBurnSeconds() {
        return burnTime / 20;
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumGeneratorBlock#getTicker}.
     * Only ever invoked server-side (the block returns {@code null} for
     * the client ticker) so there is no {@code level.isClientSide} guard
     * needed here, unlike {@code ArmorSetBonusHandler} which ticks on
     * both sides and has to check explicitly.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumGeneratorBlockEntity generator) {
        boolean changed = false;

        if (generator.burnTime > 0
                && generator.energyStorage.getEnergyStored() < generator.energyStorage.getMaxEnergyStored()) {
            generator.burnTime--;
            int newAmount = Math.min(
                    generator.energyStorage.getEnergyStored() + GENERATION_PER_TICK,
                    generator.energyStorage.getMaxEnergyStored());
            generator.energyStorage.setEnergy(newAmount);
            changed = true;
        }

        if (generator.energyStorage.getEnergyStored() > 0) {
            // Session 55 (GitHub issue #15): was pushToNeighbors (six
            // immediate neighbors only) - now walks any connected
            // Prismium Cable run to reach distant receivers in the same
            // tick. See EnergyPushHelper#pushThroughNetwork's doc.
            if (EnergyPushHelper.pushThroughNetwork(level, pos, generator.energyStorage, MAX_EXTRACT)) {
                changed = true;
            }
        }

        // GitHub issue #8 (session 38): reported "burn-time display goes
        // up but nothing looks like it's actually generating". Root
        // cause found by inspection (no in-game repro available - see
        // PROGRESS.md standing note): this method used to compare a
        // *locally recomputed* wasBurning/isBurning pair, both derived
        // from generator.burnTime at the start and end of this same
        // tick. burnTime only ever changes by at most 1 per tick (the
        // decrement above), so that comparison could only ever observe
        // the 1->0 transition (fuel running out) - the 0->positive
        // transition (addFuel() being called from PrismiumGeneratorBlock
        // #use, an entirely separate method invocation on a previous
        // tick) was invisible to it, because by the time this method's
        // "wasBurning" line ran, burnTime was already > 0 from the start
        // of the tick. Net effect: the LIT blockstate could switch from
        // true to false, but could never switch from false to true - the
        // block's glowing "active" texture would never turn on even
        // though burnTime/energy generation were working correctly the
        // whole time, exactly matching the "no sign of it operating"
        // complaint. Fixed by comparing against the block's *actual*
        // current LIT value (read from the passed-in BlockState, which
        // reflects whatever was last written to the world) instead of a
        // second burnTime sample - this correctly catches both
        // directions of the transition.
        boolean isBurning = generator.burnTime > 0;
        if (state.getValue(BlockStateProperties.LIT) != isBurning) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isBurning), 3);
        }

        if (changed) {
            generator.setChanged();
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("BurnTime", burnTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergy(tag.getInt("Energy"));
        burnTime = tag.getInt("BurnTime");
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
        return Component.translatable("block.claudemod.prismium_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumGeneratorMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition));
    }
}
