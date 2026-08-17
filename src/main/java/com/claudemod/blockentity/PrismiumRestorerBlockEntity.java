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
 * Block entity for Prismium Restorer (session 20): the mod's *second* FE
 * consumer, after Prismium Pylon (session 19). Where the Pylon spends FE
 * passively over time on a beacon-like area buff, the Restorer spends FE
 * only in direct response to a player action - right-clicking it while
 * holding a damaged item repairs some of that item's durability, paid for
 * out of the buffer below. This follows the "second consumer" idea
 * explicitly floated in PROGRESS.md's session 19 handoff (section 5,
 * "議論したい論点": "FEで耐久を回復する装置").
 *
 * <p>Deliberately has <b>no {@code BlockEntityTicker}</b>, unlike the
 * Pylon. All of this block's behaviour happens synchronously inside
 * {@link com.claudemod.block.PrismiumRestorerBlock#use}, the same shape as
 * {@link PrismiumCellBlockEntity} (session 8) - a passive energy buffer
 * with no per-tick logic of its own. It still participates fully in the
 * Cell/Generator/Cable network: {@code PrismiumCableBlockEntity}'s own
 * ticker pushes FE into this block purely through the
 * {@code IEnergyStorage} capability below, so no ticker is needed on the
 * receiving side for that to work (this mirrors how the Cell has received
 * pushed energy since session 8 without ever ticking itself).
 *
 * <p><b>Unverified</b> (see PROGRESS.md): whether the FE-per-durability
 * cost and per-use durability cap below feel good in play, and whether
 * repairing armor/tools this way interacts correctly with enchantments
 * (e.g. Mending) - only compiled and code-reviewed, not playtested.
 */
public class PrismiumRestorerBlockEntity extends BlockEntity {

    /** Total FE capacity. */
    public static final int CAPACITY = 30_000;
    /** Max FE this block will accept per {@code receiveEnergy} call, both
     * from the capability (a Cable pushing in) and the manual shard
     * charge below. */
    public static final int MAX_RECEIVE = 2_000;
    /** FE added per Prismium Shard via the manual charge interaction. */
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    /** FE spent per point of durability restored. */
    public static final int FE_PER_DURABILITY = 25;
    /** Max durability points restored in a single right-click, regardless
     * of how much energy is available or how damaged the item is - keeps
     * one click from instantly fully repairing a very damaged item in a
     * single hit even when the buffer is full. */
    public static final int MAX_DURABILITY_PER_USE = 64;

    // maxExtract is 0: like the Pylon, this is a pure sink - it never
    // exposes energy for another machine to pull out.
    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    public PrismiumRestorerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_RESTORER.get(), pos, state);
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
