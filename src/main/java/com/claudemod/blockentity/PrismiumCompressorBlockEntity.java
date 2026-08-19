package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumCompressorMenu;
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
 * Block entity for Prismium Compressor (session 70, scheduled): the
 * mod's third item-processing machine, extending the production chain
 * one step past {@link PrismiumSmelterBlockEntity} (session 68) -
 * Prismium Ore -[Pulverizer]-> Shard -[Smelter]-> Ingot -[this
 * class]-> Prismium Alloy Ingot, the mod's second refined-material
 * item (see {@code ModItems.PRISMIUM_ALLOY_INGOT}).
 *
 * <p>Structurally a direct copy of {@link PrismiumSmelterBlockEntity}
 * (2-slot {@code ItemStackHandler}, hardcoded single-entry recipe
 * table, pure-FE-sink energy storage, many-to-one "pause don't waste"
 * processing loop) - deliberately still not generalized into a shared
 * abstract base class even though this is now the third near-identical
 * machine (see PROGRESS.md session 68's own note flagging this as the
 * point where extraction might become worthwhile). This session chose
 * to prioritize shipping the third machine itself, with working,
 * already-reviewed logic copied verbatim from Smelter, over risking a
 * same-session refactor of two already-working machines with no local
 * build to verify the result - see PROGRESS.md handoff for why the
 * extraction is left as a clearly-flagged next step instead.
 *
 * <p><b>Recipe</b>: {@link #INGOTS_PER_ALLOY_INGOT} (4) Prismium Ingots
 * compress into 1 Prismium Alloy Ingot - the same 4:1 many-to-one
 * "refining" ratio Smelter uses (shard -> ingot), so the chain's three
 * machines read as three repetitions of the same "four raw units in,
 * one denser unit out" idea rather than three differently-tuned
 * mechanics a player has to learn separately. Energy shape is likewise
 * identical to Pulverizer/Smelter ({@link #CAPACITY} = 20,000, {@link
 * #MAX_RECEIVE} = 2,000, maxExtract 0, {@link #PROCESS_TIME_TICKS} =
 * 100, {@link #ENERGY_PER_TICK} = 20) - deliberately kept in sync with
 * the two already-reviewed machines rather than inventing new numbers
 * with even less grounding, same reasoning Smelter's own class doc
 * gives for reusing Pulverizer's numbers.
 *
 * <p>Manual charging (holding a Prismium Shard and right-clicking the
 * block, see {@link com.claudemod.block.PrismiumCompressorBlock#use})
 * uses {@link #SHARD_CHARGE_AMOUNT}, the same universal "Prismium Shard
 * = portable FE battery" convention every other energy block in this
 * mod (including Pulverizer/Smelter) already follows - this is
 * deliberately a different item from this machine's actual processing
 * input (Prismium Ingot), unlike Smelter where the charge item and the
 * process input happened to be the same item (Shard) by coincidence.
 *
 * <p><b>Unverified</b> (no local build/game client in this sandbox, see
 * PROGRESS.md standing note repeated in every block entity in this
 * mod): whether consuming 4 ingots for 1 alloy ingot feels like a fair
 * "endgame refining" trade given how much upstream cost is already
 * baked into an ingot (2 shards -[craft? no - see Smelter] 4 shards via
 * Smelter, which themselves come from Pulverizer), whether
 * {@link #PROCESS_TIME_TICKS}/{@link #ENERGY_PER_TICK} still feel
 * appropriately paced for a third-tier machine rather than a
 * fresh-tuned "should probably be slower/costlier than Smelter" one,
 * and whether Prismium Alloy Ingot's one immediate use this session
 * (Prismium Alloy Block, see recipes/prismium_alloy_block.json) is
 * reason enough to actually build this machine before a future session
 * gives Alloy Ingot an equipment-tier use as well (see PROGRESS.md
 * handoff for the smithing-upgrade idea this material is intended to
 * eventually feed).
 */
public class PrismiumCompressorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY = 20_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    public static final int PROCESS_TIME_TICKS = 100;
    public static final int ENERGY_PER_TICK = 20;
    /** Ingots consumed per alloy ingot produced. See class javadoc for
     * why this mirrors Smelter's SHARDS_PER_INGOT ratio. */
    public static final int INGOTS_PER_ALLOY_INGOT = 4;

    private static Map<Item, ItemStack> recipes;

    private static Map<Item, ItemStack> recipes() {
        if (recipes == null) {
            Map<Item, ItemStack> map = new HashMap<>();
            Item alloyIngot = ModItems.PRISMIUM_ALLOY_INGOT.get();
            map.put(ModItems.PRISMIUM_INGOT.get(), new ItemStack(alloyIngot, 1));
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
     * slot, used by {@link PrismiumCompressorMenu#quickMoveStack}. */
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

    public PrismiumCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_COMPRESSOR.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumCompressorBlock#getTicker}.
     * Only ever invoked server-side. Logic is a verbatim copy of {@link
     * PrismiumSmelterBlockEntity#serverTick} with names swapped - see
     * that method's doc for the "requires enough queued input before
     * counting as processable" many-to-one behaviour this also uses.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumCompressorBlockEntity compressor) {
        ItemStack inputStack = compressor.inventory.getStackInSlot(0);
        ItemStack recipeResult = inputStack.isEmpty() ? null : recipeFor(inputStack.getItem());
        boolean hasEnoughInput = recipeResult != null && inputStack.getCount() >= INGOTS_PER_ALLOY_INGOT;

        boolean canProcess = false;
        if (hasEnoughInput) {
            ItemStack outputStack = compressor.inventory.getStackInSlot(1);
            if (outputStack.isEmpty()) {
                canProcess = true;
            } else if (ItemStack.isSameItemSameTags(outputStack, recipeResult)
                    && outputStack.getCount() + recipeResult.getCount() <= outputStack.getMaxStackSize()) {
                canProcess = true;
            }
        }

        boolean nowActive = false;
        if (!hasEnoughInput) {
            compressor.progress = 0;
        } else if (canProcess && compressor.energyStorage.getEnergyStored() >= ENERGY_PER_TICK) {
            compressor.energyStorage.setEnergy(compressor.energyStorage.getEnergyStored() - ENERGY_PER_TICK);
            compressor.progress++;
            nowActive = true;
            if (compressor.progress >= PROCESS_TIME_TICKS) {
                compressor.progress = 0;
                compressor.inventory.extractItem(0, INGOTS_PER_ALLOY_INGOT, false);
                ItemStack outputStack = compressor.inventory.getStackInSlot(1);
                if (outputStack.isEmpty()) {
                    compressor.inventory.setStackInSlot(1, recipeResult);
                } else {
                    outputStack.grow(recipeResult.getCount());
                    compressor.inventory.setStackInSlot(1, outputStack);
                }
            }
        }
        // else: enough ingots are queued but blocked (output full, or
        // not enough energy) - pause and keep progress, same "don't
        // waste, don't discard" philosophy as every other machine.

        boolean wasLit = state.getValue(BlockStateProperties.LIT);
        if (nowActive != wasLit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, nowActive), 3);
        }

        compressor.active = nowActive;
        compressor.setChanged();
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
        return Component.translatable("block.claudemod.prismium_compressor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumCompressorMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition), this.inventory);
    }
}
