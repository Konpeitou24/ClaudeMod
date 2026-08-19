package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumSmelterMenu;
import com.claudemod.registry.ModBlockEntities;
import com.claudemod.registry.ModItems;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Block entity for Prismium Smelter (session 68): the mod's second
 * item-processing machine, extending the production chain
 * {@link PrismiumPulverizerBlockEntity} (session 67) started one step
 * further - Prismium Ore -[Silk Touch + Pulverizer]-> Prismium Shard
 * -[this class]-> Prismium Ingot, the mod's first refined-material item
 * (see {@code ModItems.PRISMIUM_INGOT}). Structurally almost identical
 * to {@link PrismiumPulverizerBlockEntity} (2-slot {@code
 * ItemStackHandler}, hardcoded conversion table, pure-FE-sink energy
 * storage, "pause don't waste/discard" processing loop) - deliberately
 * not generalized into a shared abstract base class yet, matching this
 * mod's established convention of tolerating some duplication across a
 * young pattern (see {@code EnergyPushHelper}'s doc for the one time
 * this mod *did* extract a shared helper, once a second caller actually
 * showed up and the duplicated logic itself, not just its shape, was
 * identical) rather than guessing at the right abstraction from only two
 * examples.
 *
 * <p><b>Recipe</b>: {@link #SHARDS_PER_INGOT} (4) Prismium Shards produce
 * 1 Prismium Ingot. Deliberately a many-to-one "refining" ratio (unlike
 * Pulverizer's one-to-many "grinding" ratio) so the two machines feel
 * like opposite ends of the same chain rather than two copies of the
 * same mechanic - shards are relatively abundant (an ore block already
 * yields 1-2 directly from mining, or {@code SHARDS_PER_ORE} = 3 via
 * Pulverizer), so consuming 4 for 1 ingot keeps ingots feeling like a
 * genuinely refined, comparatively scarce material without this session
 * having to invent a whole new ore/resource just to gate them.
 *
 * <p><b>Energy shape</b>: identical numbers to Pulverizer ({@link
 * #CAPACITY} = 20,000, {@link #MAX_RECEIVE} = 2,000, maxExtract 0,
 * {@link #PROCESS_TIME_TICKS} = 100, {@link #ENERGY_PER_TICK} = 20,
 * {@link #SHARD_CHARGE_AMOUNT} = 2,000 so one manually-fed shard powers
 * exactly one smelt) - deliberately kept in sync with Pulverizer's
 * already-reviewed numbers rather than inventing new ones with even less
 * grounding, same reasoning as every other consumer block in this mod
 * reusing Wardstone's original 20,000/2,000 shape.
 *
 * <p><b>Unverified</b> (this sandbox cannot launch the client - see
 * PROGRESS.md standing note repeated in every block entity in this mod):
 * whether consuming 4 shards for 1 ingot feels like a fair trade in
 * practice, whether {@link #PROCESS_TIME_TICKS}/{@link #ENERGY_PER_TICK}
 * (unchanged from Pulverizer) still feel appropriately paced for a
 * "refining" step rather than a "grinding" one, and - since Prismium
 * Ingot has no crafting use yet in this same session (see PROGRESS.md) -
 * whether players will have any reason to actually use this machine
 * before a future session gives ingots a recipe to feed into.
 */
public class PrismiumSmelterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY = 20_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    public static final int PROCESS_TIME_TICKS = 100;
    public static final int ENERGY_PER_TICK = 20;
    /** Shards consumed per ingot produced. See class javadoc for why
     * this is a many-to-one ratio, the inverse shape of Pulverizer's
     * one-to-many {@code SHARDS_PER_ORE}. */
    public static final int SHARDS_PER_INGOT = 4;

    private static Map<Item, ItemStack> recipes;

    private static Map<Item, ItemStack> recipes() {
        if (recipes == null) {
            Map<Item, ItemStack> map = new HashMap<>();
            Item ingot = ModItems.PRISMIUM_INGOT.get();
            map.put(ModItems.PRISMIUM_SHARD.get(), new ItemStack(ingot, 1));
            recipes = map;
        }
        return recipes;
    }

    @Nullable
    private static ItemStack recipeFor(Item item) {
        ItemStack result = recipes().get(item);
        return result == null ? null : result.copy();
    }

    /** Public check for whether an item would be accepted by the input
     * slot, used by {@link PrismiumSmelterMenu#quickMoveStack}. Unlike
     * Pulverizer's {@code isValidInput}, the recipe table above maps to
     * a 1-count result per input item, but {@link #serverTick} still
     * requires {@link #SHARDS_PER_INGOT} shards to actually be present
     * in the input slot before it starts consuming - see that method. */
    public static boolean isValidInput(ItemStack stack) {
        return !stack.isEmpty() && recipeFor(stack.getItem()) != null;
    }

    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
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
            // Read-only from the screen's perspective.
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ContainerData getContainerData() {
        return containerData;
    }

    public PrismiumSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_SMELTER.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumSmelterBlock#getTicker}. Only
     * ever invoked server-side.
     *
     * <p>Differs from {@link PrismiumPulverizerBlockEntity#serverTick} in
     * one place: because the recipe is many-to-one ({@link
     * #SHARDS_PER_INGOT} consumed per 1 ingot produced, rather than
     * Pulverizer's 1-consumed-per-many-produced), {@code canProcess} here
     * also requires the input slot to actually hold at least {@link
     * #SHARDS_PER_INGOT} items before counting as processable - a queued
     * single shard with no companions correctly does nothing (progress
     * stays at 0) rather than starting a smelt it cannot finish.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumSmelterBlockEntity smelter) {
        ItemStack inputStack = smelter.inventory.getStackInSlot(0);
        ItemStack recipeResult = inputStack.isEmpty() ? null : recipeFor(inputStack.getItem());
        boolean hasEnoughInput = recipeResult != null && inputStack.getCount() >= SHARDS_PER_INGOT;

        boolean canProcess = false;
        if (hasEnoughInput) {
            ItemStack outputStack = smelter.inventory.getStackInSlot(1);
            if (outputStack.isEmpty()) {
                canProcess = true;
            } else if (ItemStack.isSameItemSameTags(outputStack, recipeResult)
                    && outputStack.getCount() + recipeResult.getCount() <= outputStack.getMaxStackSize()) {
                canProcess = true;
            }
        }

        boolean nowActive = false;
        if (!hasEnoughInput) {
            // No valid recipe queued, or not enough shards yet to finish
            // one - drop any in-progress fraction, same "forget partial
            // progress once the ingredient is gone/insufficient" rule
            // PrismiumPulverizerBlockEntity and vanilla furnace both use.
            smelter.progress = 0;
        } else if (canProcess && smelter.energyStorage.getEnergyStored() >= ENERGY_PER_TICK) {
            smelter.energyStorage.setEnergy(smelter.energyStorage.getEnergyStored() - ENERGY_PER_TICK);
            smelter.progress++;
            nowActive = true;
            if (smelter.progress >= PROCESS_TIME_TICKS) {
                smelter.progress = 0;
                smelter.inventory.extractItem(0, SHARDS_PER_INGOT, false);
                ItemStack outputStack = smelter.inventory.getStackInSlot(1);
                if (outputStack.isEmpty()) {
                    smelter.inventory.setStackInSlot(1, recipeResult);
                } else {
                    outputStack.grow(recipeResult.getCount());
                    smelter.inventory.setStackInSlot(1, outputStack);
                }
            }
        }
        // else: enough shards are queued but blocked (output full, or not
        // enough energy) - pause and keep progress, same "don't waste,
        // don't discard" philosophy as every other machine in this mod.

        boolean wasLit = state.getValue(BlockStateProperties.LIT);
        if (nowActive != wasLit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, nowActive), 3);
        }

        smelter.active = nowActive;
        smelter.setChanged();
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.claudemod.prismium_smelter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumSmelterMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition), this.inventory);
    }
}
