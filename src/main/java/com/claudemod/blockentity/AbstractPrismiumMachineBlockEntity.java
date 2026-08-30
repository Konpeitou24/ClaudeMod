package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * Session #77 (定期実行) extraction: shared base class for {@link
 * PrismiumPulverizerBlockEntity}, {@link PrismiumSmelterBlockEntity} and
 * {@link PrismiumCompressorBlockEntity}, three machines that had been
 * deliberately left as near-identical copy-paste siblings (see each
 * class's own javadoc, e.g. Smelter's "deliberately not generalized into
 * a shared abstract base class yet... rather than guessing at the right
 * abstraction from only two examples"). With a third machine
 * (Compressor) following the exact same shape, the "only two examples"
 * caveat no longer applies, and this PROGRESS.md backlog item ("3機械の
 * 共通基底クラス抽出") has been open since session 70 - this session
 * finally does the extraction.
 *
 * <p>Captures everything that was byte-for-byte identical across all
 * three block entities: the 2-slot {@code ItemStackHandler} (slot 0 =
 * input gated by {@link #recipeFor(Item)}, slot 1 = output that rejects
 * external insertion), the pure-FE-sink {@link PrismiumEnergyStorage}
 * (capacity/max-receive/process-cost numbers, see the constants below),
 * the 4-value {@link ContainerData} (energy/maxEnergy/progress/active),
 * NBT save/load, and capability exposure.
 *
 * <p>The one real behavioral difference between Pulverizer (1-to-many,
 * consumes 1 input item per operation) and Smelter/Compressor (many-to-
 * one, consumes a fixed count of input items per operation) is captured
 * by {@link #inputCountPerOperation()}, which defaults to 1 and is
 * overridden by the many-to-one machines. Everything else that varies
 * per machine (the hardcoded recipe table, the menu class, the
 * translation key) stays in the subclass, same as before.
 *
 * <p><b>Unverified</b> (still no local build/launch in this sandbox):
 * this is a pure refactor with no intended behavior change versus the
 * three previous standalone classes - the tick logic, NBT keys, and
 * capability wiring are unchanged line-for-line other than routing
 * through {@link #recipeFor(Item)}/{@link #inputCountPerOperation()}
 * instead of each class's private static method. CI build success does
 * not exercise actual gameplay, so whether all three machines still
 * behave identically to before (energy draw pacing, output stacking,
 * LIT toggling, GUI progress bar/energy bar via {@code ContainerData})
 * has not been re-confirmed in a running client.
 */
public abstract class AbstractPrismiumMachineBlockEntity extends BlockEntity implements MenuProvider {

    /** Total FE capacity - identical across all three machines. */
    public static final int CAPACITY = 20_000;
    /** Max FE accepted per {@code receiveEnergy} call. */
    public static final int MAX_RECEIVE = 2_000;
    /** FE added per Prismium Shard via the manual charge interaction. */
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    /** Ticks to fully process one operation once a valid recipe is
     * queued and energy keeps flowing. */
    public static final int PROCESS_TIME_TICKS = 100;
    /** FE spent per tick while actively processing. */
    public static final int ENERGY_PER_TICK = 20;

    // maxExtract is 0: pure sink, same shape for all three machines.
    protected final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    protected final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return recipeFor(stack.getItem()) != null;
            }
            return false;
        }
    };
    private final LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> inventory);

    private int progress = 0;
    private boolean active = false;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> progress;
                case 3 -> active ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Deliberately a no-op - correct now that the client-side
            // menu constructor (PrismiumPulverizerMenu/SmelterMenu/
            // CompressorMenu) never reuses this real instance to receive
            // synced values, always using a fresh SimpleContainerData
            // instead (session #84 bugfix, see those classes' resolveData
            // doc for the full explanation of why this method being a
            // no-op used to freeze every GUI in the mod).
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    protected AbstractPrismiumMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** Subclass-supplied hardcoded conversion table lookup - each
     * subclass keeps its own lazily-populated {@code Map<Item,
     * ItemStack>} exactly as before (see e.g. {@link
     * PrismiumPulverizerBlockEntity#isValidInput}), this just routes the
     * shared tick logic through it. Must return a copy safe to mutate
     * (subclasses already return {@code result.copy()}). */
    @Nullable
    protected abstract ItemStack recipeFor(Item item);

    /** Input items consumed per completed operation. Pulverizer's
     * one-to-many recipe consumes exactly 1 (the default); Smelter/
     * Compressor's many-to-one recipes override this with their
     * respective {@code SHARDS_PER_INGOT}/{@code
     * INGOTS_PER_ALLOY_INGOT} constant. */
    protected int inputCountPerOperation() {
        return 1;
    }

    /**
     * Shared server tick, called by each subclass's own {@code
     * serverTick} static method (kept per-subclass so {@code
     * BaseEntityBlock#createTickerHelper} type inference in each
     * machine's Block class needs no changes). Behavior is unchanged
     * from the three previous standalone implementations - see class
     * javadoc.
     */
    protected static void tick(Level level, BlockPos pos, BlockState state, AbstractPrismiumMachineBlockEntity machine) {
        ItemStack inputStack = machine.inventory.getStackInSlot(0);
        ItemStack recipeResult = inputStack.isEmpty() ? null : machine.recipeFor(inputStack.getItem());
        int required = machine.inputCountPerOperation();
        boolean hasEnoughInput = recipeResult != null && inputStack.getCount() >= required;

        boolean canProcess = false;
        if (hasEnoughInput) {
            ItemStack outputStack = machine.inventory.getStackInSlot(1);
            if (outputStack.isEmpty()) {
                canProcess = true;
            } else if (ItemStack.isSameItemSameTags(outputStack, recipeResult)
                    && outputStack.getCount() + recipeResult.getCount() <= outputStack.getMaxStackSize()) {
                canProcess = true;
            }
        }

        boolean nowActive = false;
        if (!hasEnoughInput) {
            // No valid recipe queued, or not enough input items yet to
            // finish one - drop any in-progress fraction, same "forget
            // partial progress" rule every machine in this mod uses.
            machine.progress = 0;
        } else if (canProcess && machine.energyStorage.getEnergyStored() >= ENERGY_PER_TICK) {
            machine.energyStorage.setEnergy(machine.energyStorage.getEnergyStored() - ENERGY_PER_TICK);
            machine.progress++;
            nowActive = true;
            if (machine.progress >= PROCESS_TIME_TICKS) {
                machine.progress = 0;
                machine.inventory.extractItem(0, required, false);
                ItemStack outputStack = machine.inventory.getStackInSlot(1);
                if (outputStack.isEmpty()) {
                    machine.inventory.setStackInSlot(1, recipeResult);
                } else {
                    outputStack.grow(recipeResult.getCount());
                    machine.inventory.setStackInSlot(1, outputStack);
                }
            }
        }
        // else: a valid recipe is queued but blocked (output full, or
        // not enough energy) - pause and keep progress, same "don't
        // waste, don't discard" philosophy as every machine in this mod.

        boolean wasLit = state.getValue(BlockStateProperties.LIT);
        if (nowActive != wasLit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, nowActive), 3);
        }

        machine.active = nowActive;
        machine.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergy(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
        itemOptional.invalidate();
    }
}
