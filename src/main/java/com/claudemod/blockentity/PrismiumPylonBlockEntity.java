package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumPylonMenu;
import com.claudemod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Block entity for Prismium Pylon (session 19): the mod's first FE
 * *consumer*. Prismium Cell (session 8) can store FE, Prismium Generator
 * (session 9) can produce it, Prismium Cable (session 10) can relay it -
 * but nothing before this ever actually spent it on a gameplay effect, a
 * gap flagged repeatedly in PROGRESS.md's handoff notes (section 4 item
 * 7 / section 5 item 7, "no consumer block exists yet"). This block
 * closes that loop: every {@link #PULSE_INTERVAL} ticks, if any players
 * are within {@link #RADIUS} blocks and the buffer holds enough FE, it
 * drains {@link #COST_PER_PLAYER_PER_PULSE} FE per nearby player and
 * grants them Regeneration - conceptually a vanilla Beacon analogue, fed
 * by this mod's own energy network instead of a pyramid+star.
 *
 * <p>Design choices mirrored from existing machines rather than invented
 * fresh, per the repeatedly-stated "avoid new API surface where an
 * existing pattern already works" lesson (PROGRESS.md, sessions 16-18):
 * <ul>
 *   <li>{@code maxExtract} on the internal {@link PrismiumEnergyStorage} is
 *   0 - this block is a pure sink, symmetric with
 *   {@link com.claudemod.block.PrismiumGeneratorBlock}'s pure-source
 *   storage ({@code maxReceive} 0). The tick logic spends energy directly
 *   via {@link PrismiumEnergyStorage#setEnergy}, the same technique
 *   {@code PrismiumGeneratorBlockEntity#serverTick} already uses to *add*
 *   energy from burning fuel, just subtracting instead of adding.</li>
 *   <li>Player detection uses {@code Level#getEntitiesOfClass(Player.class,
 *   AABB)} and the effect is applied via {@code Player#addEffect}, the
 *   exact same call {@link com.claudemod.event.ArmorSetBonusHandler}
 *   (session 4/5) already uses for the armor set's Night Vision/Water
 *   Breathing bonus - both long-stable vanilla APIs, no new symbol to
 *   verify this session.</li>
 *   <li>The pulse only runs once every {@link #PULSE_INTERVAL} ticks
 *   rather than every tick, both to keep the AABB entity scan cheap and
 *   because a 30-tick effect duration comfortably covers the 10-tick gap
 *   between pulses with no visible flicker (same "reapply with a buffer"
 *   idea as {@code ArmorSetBonusHandler}, just on a much shorter
 *   timescale since this is a per-block area effect, not a permanently
 *   worn one).</li>
 * </ul>
 *
 * <p><b>Unverified</b> (see PROGRESS.md): whether the radius/cost/effect
 * numbers below feel good in play, whether the LIT swap is visually
 * correct in-game, and whether the AABB scan every 10 ticks is cheap
 * enough with many Pylons placed at once - none of this has been
 * playtested, only compiled and code-reviewed, consistent with every
 * other machine in this mod.
 */
public class PrismiumPylonBlockEntity extends BlockEntity implements MenuProvider {

    /** Total FE capacity. */
    public static final int CAPACITY = 20_000;
    /** Max FE this block will accept per {@code receiveEnergy} call, both
     * from the capability (e.g. a Cable pushing in) and the manual shard
     * charge below. Deliberately set comfortably above both so neither
     * path is silently short-changed by the cap. */
    public static final int MAX_RECEIVE = 2_000;
    /** FE added per Prismium Shard via the manual charge interaction. */
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    /** Radius (blocks) players are scanned within, each pulse. */
    public static final double RADIUS = 6.0;
    /** Ticks between pulses (10 ticks = 0.5s). */
    public static final int PULSE_INTERVAL = 10;
    /** FE spent per affected player, per pulse. */
    public static final int COST_PER_PLAYER_PER_PULSE = 20;
    /** Regeneration duration granted per pulse - longer than
     * PULSE_INTERVAL so back-to-back pulses don't let the effect expire
     * and visibly flicker. */
    public static final int EFFECT_DURATION_TICKS = 30;

    // maxExtract is 0: see class javadoc, this is a sink, not a source -
    // its own tick logic spends energy directly rather than exposing it
    // to be extracted by another machine.
    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    private int pulseTimer = 0;
    private boolean active = false;

    /** Backs this block entity's GUI (session 25), following the pattern
     * established by Prismium Cell (session 23) and Prismium Generator
     * (session 24). Index 0/1 are current/max energy (CAPACITY = 20,000,
     * still comfortably inside Short.MAX_VALUE on its own, same situation
     * as Generator - see PrismiumPylonMenu's class doc), index 2 is
     * {@link #active} encoded as 0/1 since ContainerData only carries
     * ints. {@code set} is a no-op for the same reason as Cell/Generator:
     * the screen only ever reads, the underlying state changes through
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
            // Deliberately a no-op - correct now that this block's Menu
            // class's client-side constructor never reuses this real
            // instance to receive synced values (session #84 bugfix: it
            // used to, which silently discarded every synced update and
            // froze this GUI's bars - see that Menu's resolveData doc).
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ContainerData getContainerData() {
        return containerData;
    }

    public PrismiumPylonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_PYLON.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /** Whether the most recent pulse actually radiated (had energy and at
     * least one nearby player) - drives the LIT blockstate/status message. */
    public boolean isActive() {
        return active;
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumPylonBlock#getTicker}. Only
     * ever invoked server-side (see that method), so no
     * {@code level.isClientSide} guard is needed here - same situation as
     * {@code PrismiumGeneratorBlockEntity#serverTick}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumPylonBlockEntity pylon) {
        pylon.pulseTimer++;
        if (pylon.pulseTimer < PULSE_INTERVAL) {
            return;
        }
        pylon.pulseTimer = 0;

        List<Player> nearby = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(RADIUS));

        boolean nowActive = false;
        if (!nearby.isEmpty()) {
            int cost = COST_PER_PLAYER_PER_PULSE * nearby.size();
            if (pylon.energyStorage.getEnergyStored() >= cost) {
                pylon.energyStorage.setEnergy(pylon.energyStorage.getEnergyStored() - cost);
                for (Player player : nearby) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 0,
                            true, false, false));
                }
                nowActive = true;
            }
        }

        boolean wasActive = pylon.active;
        pylon.active = nowActive;

        boolean wasLit = state.getValue(BlockStateProperties.LIT);
        if (nowActive != wasLit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, nowActive), 3);
        }

        if (nowActive != wasActive || nowActive) {
            pylon.setChanged();
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
        return Component.translatable("block.claudemod.prismium_pylon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumPylonMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition));
    }
}
