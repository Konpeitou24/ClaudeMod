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
public class PrismiumGeneratorBlockEntity extends BlockEntity {

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
    /** Internal buffer capacity. Deliberately small - this block is meant
     * to push energy out immediately, not accumulate a meaningful
     * reserve (that's Prismium Cell's job). It mainly exists so a tick or
     * two of generation isn't lost if neighbors momentarily can't accept
     * the full amount. */
    public static final int CAPACITY = 8000;
    /** Max FE pushed to *all* neighbors combined, per tick. */
    public static final int MAX_EXTRACT = 200;

    // maxReceive is 0: this generator only ever fills its own buffer via
    // the internal burn logic below (PrismiumEnergyStorage#setEnergy,
    // which bypasses maxReceive on purpose), never by another machine
    // pushing FE into it - it is a source, not a sink.
    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, 0, MAX_EXTRACT);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    private int burnTime = 0;

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
        boolean wasBurning = generator.burnTime > 0;
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
            if (EnergyPushHelper.pushToNeighbors(level, pos, generator.energyStorage, MAX_EXTRACT)) {
                changed = true;
            }
        }

        boolean isBurning = generator.burnTime > 0;
        if (isBurning != wasBurning) {
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
}
