package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumWardstoneMenu;
import com.claudemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Block entity for Prismium Wardstone (session 21): the mod's *third* FE
 * consumer, after Prismium Pylon (session 19, buffs players) and Prismium
 * Restorer (session 20, repairs items on demand). Where Pylon radiates a
 * positive effect onto nearby players, Wardstone is the defensive mirror
 * image: every {@link #PULSE_INTERVAL} ticks, if any hostile mobs
 * ({@link Monster}) are within {@link #RADIUS} blocks and the buffer holds
 * enough FE, it drains {@link #COST_PER_MOB_PER_PULSE} FE per affected mob
 * and applies Weakness + Slowness to each - a "ward" that makes the
 * immediate area around a base measurably safer without killing anything
 * outright.
 *
 * <p>Deliberately copies {@code PrismiumPylonBlockEntity}'s structure
 * almost verbatim (same maxExtract-0 sink storage, same
 * {@code getEntitiesOfClass}/{@code addEffect} combo, same pulse/LIT
 * bookkeeping) per the repeated "reuse an already-working pattern instead
 * of inventing new API surface" lesson in PROGRESS.md. The one deliberate
 * departure worth calling out: this block never touches
 * {@code net.minecraft.world.damagesource} - dealing direct damage would be
 * genuinely new, unverified API surface for this mod (a grep of the whole
 * codebase turns up no place that *constructs* a {@code DamageSource}
 * itself; {@code PrismiumSwordHandler}, the only other place this mod deals
 * with damage, merely *reads* one off an existing event). Keeping the
 * effect to status effects only reuses exactly what Pylon already proved
 * compiles and behaves as expected (as much as "compiles" can be proven in
 * this sandbox - see PROGRESS.md).
 *
 * <p>Session 27 adds {@link #getContainerData()}/{@link #createMenu} for
 * this block's GUI (the mod's fifth, and the last of the three original
 * consumer blocks to get one - Pylon session 25, Restorer session 26),
 * copying {@code PrismiumPylonBlockEntity}'s exact 3-int ContainerData
 * shape (energy, max energy, active-flag) since Wardstone shares Pylon's
 * ticking active/idle state, unlike Restorer which has none.
 *
 * <p><b>Unverified</b> (see PROGRESS.md): whether the radius/cost/effect
 * numbers feel good in play, whether hostile mobs are actually visibly
 * slowed/weakened, whether the LIT swap renders correctly, whether
 * scanning for {@link Monster} (as opposed to a narrower/broader class)
 * misses anything a player would expect to be affected - note in
 * particular that {@code Slime}/{@code MagmaCube} extend {@code Mob} but
 * NOT {@code Monster}, so they are deliberately (if perhaps surprisingly)
 * excluded by this scan; flagged in PROGRESS.md as worth reconsidering -
 * and now also whether the new GUI opens/renders correctly in-game
 * (zero playtesting, same as every other GUI in this mod so far).
 */
public class PrismiumWardstoneBlockEntity extends BlockEntity implements MenuProvider {

    /** Total FE capacity. */
    public static final int CAPACITY = 20_000;
    /** Max FE this block will accept per {@code receiveEnergy} call, both
     * from the capability (e.g. a Cable pushing in) and the manual shard
     * charge below. */
    public static final int MAX_RECEIVE = 2_000;
    /** FE added per Prismium Shard via the manual charge interaction. */
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    /** Radius (blocks) hostile mobs are scanned within, each pulse -
     * deliberately wider than Pylon's 6, since a defensive "ward" reads
     * better as covering a whole base perimeter rather than a tight
     * huddle-around-the-block range. */
    public static final double RADIUS = 8.0;
    /** Ticks between pulses (20 ticks = 1s - slower than Pylon's 10, since
     * scanning + double-effect application per hostile mob is marginally
     * more expensive and a 1s cadence is still comfortably faster than the
     * effect duration below). */
    public static final int PULSE_INTERVAL = 20;
    /** FE spent per affected mob, per pulse. */
    public static final int COST_PER_MOB_PER_PULSE = 30;
    /** Effect duration granted per pulse - longer than PULSE_INTERVAL so
     * back-to-back pulses don't let the effect expire and flicker. */
    public static final int EFFECT_DURATION_TICKS = 50;
    /** Amplifier for both Weakness and Slowness (0 = level I, so this is
     * level II for both - noticeable without being instantly lethal on
     * its own). */
    public static final int EFFECT_AMPLIFIER = 1;

    // maxExtract is 0: see class javadoc, this is a sink, not a source -
    // its own tick logic spends energy directly rather than exposing it
    // to be extracted by another machine. Same shape as Pylon's storage.
    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    private int pulseTimer = 0;
    private boolean active = false;

    /** Backs this block entity's GUI (session 27), following the exact
     * shape established by {@code PrismiumPylonBlockEntity} (session 25).
     * Index 0/1 are current/max energy (CAPACITY = 20,000, comfortably
     * inside Short.MAX_VALUE on its own), index 2 is {@link #active}
     * encoded as 0/1 since ContainerData only carries ints. {@code set}
     * is a no-op for the same reason as every other machine's GUI: the
     * screen only ever reads, the underlying state changes through
     * {@link #serverTick} only. */
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> active ? 1 : 0;
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

    public PrismiumWardstoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_WARDSTONE.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /** Whether the most recent pulse actually warded (had energy and at
     * least one nearby hostile mob) - drives the LIT blockstate/status
     * message. */
    public boolean isActive() {
        return active;
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumWardstoneBlock#getTicker}. Only
     * ever invoked server-side (see that method), so no
     * {@code level.isClientSide} guard is needed here - same situation as
     * {@code PrismiumPylonBlockEntity#serverTick}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumWardstoneBlockEntity wardstone) {
        wardstone.pulseTimer++;
        if (wardstone.pulseTimer < PULSE_INTERVAL) {
            return;
        }
        wardstone.pulseTimer = 0;

        List<Monster> nearby = level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(RADIUS));

        boolean nowActive = false;
        if (!nearby.isEmpty()) {
            int cost = COST_PER_MOB_PER_PULSE * nearby.size();
            if (wardstone.energyStorage.getEnergyStored() >= cost) {
                wardstone.energyStorage.setEnergy(wardstone.energyStorage.getEnergyStored() - cost);
                for (Monster mob : nearby) {
                    mob.addEffect(new MobEffectInstance(
                            MobEffects.WEAKNESS, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                            true, false, false));
                    mob.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                            true, false, false));
                }
                nowActive = true;
            }
        }

        boolean wasActive = wardstone.active;
        wardstone.active = nowActive;

        boolean wasLit = state.getValue(BlockStateProperties.LIT);
        if (nowActive != wasLit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, nowActive), 3);
        }

        if (nowActive != wasActive || nowActive) {
            wardstone.setChanged();
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.claudemod.prismium_wardstone");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumWardstoneMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition));
    }
}
