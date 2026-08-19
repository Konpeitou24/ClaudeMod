package com.claudemod.blockentity;

import com.claudemod.energy.PrismiumEnergyStorage;
import com.claudemod.menu.PrismiumPulverizerMenu;
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
 * Block entity for Prismium Pulverizer (session 67): the mod's first item-
 * processing machine and the answer to the long-standing roadmap gap
 * flagged in session 66 (see PROGRESS.md section 1 item 2, "機械(粉砕機、
 * 精錬機など)" - never implemented despite being on the very first
 * session's roadmap; PrismiumGeneratorBlockEntity/PrismiumCellBlockEntity/
 * PrismiumPylonBlockEntity/PrismiumRestorerBlockEntity/
 * PrismiumWardstoneBlockEntity, the mod's five existing GUI'd energy
 * blocks, all only ever moved FE - none of them read or write an
 * {@code Item} slot as part of their core behavior, Generator's single
 * fuel slot (session 58) being the closest precedent and the direct
 * template for the item-handling half of this class).
 *
 * <p>Design, spelled out in session 66's handoff before this session
 * existed: a 2-slot machine (input, output) with a hardcoded conversion
 * table, consuming FE from an internal buffer while a valid conversion is
 * queued - deliberately the "stage 1" scope from that handoff (item
 * slots + FE consumption + hardcoded table), except this session also
 * completed the GUI/menu half in the same pass rather than deferring it,
 * once it became clear {@link PrismiumGeneratorBlockEntity}'s existing
 * {@code ItemStackHandler}/{@code SlotItemHandler} pattern (session 58)
 * already covered everything needed - see {@link PrismiumPulverizerMenu}
 * for why a *second* GUI/menu class was still worth writing (full 36-slot
 * player inventory + working shift-click, neither of which any existing
 * menu in this mod has, see that class's doc) rather than reusing
 * Generator's Menu shape verbatim.
 *
 * <p>What the pulverizer actually does: it re-processes an already-mined
 * Prismium Ore or Deepslate Prismium Ore block (only obtainable via Silk
 * Touch, see {@code data/claudemod/loot_tables/blocks/prismium_ore.json}
 * - normal mining already drops 1-2 shards directly, with Fortune scaling
 * that further) into a *guaranteed* {@link #SHARDS_PER_ORE} Prismium
 * Shards, consuming FE instead of a tool enchantment. This gives Silk
 * Touch mining of this mod's ore a genuine second use beyond "look nice
 * as a placed block" (previously its only purpose), and gives the FE
 * system - which until now only had Pylon/Restorer/Wardstone as final FE
 * sinks, all passive area-effect blocks - its first sink that produces a
 * tangible, storable item as its output. See {@link #RECIPES} for the
 * exact (small, deliberately conservative for a first pass - see
 * PROGRESS.md) conversion table.
 *
 * <p><b>Energy shape</b>: copies {@link PrismiumWardstoneBlockEntity}'s
 * pure-sink storage exactly ({@link #CAPACITY} = 20,000, {@link
 * #MAX_RECEIVE} = 2,000, maxExtract 0) - this machine is a consumer, not
 * a source, same reasoning as Wardstone/Restorer/Pylon. {@link
 * #ENERGY_PER_TICK} * {@link #PROCESS_TIME_TICKS} = 2,000 FE per
 * operation, deliberately equal to {@link #SHARD_CHARGE_AMOUNT} (one
 * manually-fed shard exactly powers one pulverize operation) so a player
 * without any cable/generator infrastructure yet can still bootstrap the
 * machine by hand, same convention every other consumer block in this
 * mod already follows for its own manual-charge interaction (see
 * {@code PrismiumWardstoneBlock#use}, the template this block's {@code
 * use} method copies for the shard-charge path).
 *
 * <p><b>Unverified</b> (this sandbox cannot launch the client - see
 * PROGRESS.md standing note repeated in every block entity in this mod):
 * whether {@link #PROCESS_TIME_TICKS} (100 ticks/5s) and {@link
 * #ENERGY_PER_TICK} (20 FE/t) feel like a reasonable pace/cost next to
 * mining the ore normally, whether the pause-not-reset behavior when the
 * output slot fills up (mirrors {@link PrismiumGeneratorBlockEntity}'s
 * "pause rather than waste a tick" buffer-full handling) reads clearly
 * in the GUI, and whether the LIT blockstate toggling on/off every time
 * processing starts/stops/pauses looks reasonable rather than flickery
 * for a multi-operation queue (a full stack of ore queued in the input
 * slot will flip LIT on for 100 ticks, off for 0 ticks, on again -
 * effectively staying lit continuously while supplied, which was the
 * intent, but has not been seen running).
 */
public class PrismiumPulverizerBlockEntity extends BlockEntity implements MenuProvider {

    /** Total FE capacity - identical to Wardstone/Restorer/Pylon, see
     * class javadoc. */
    public static final int CAPACITY = 20_000;
    /** Max FE accepted per {@code receiveEnergy} call, both from the
     * capability and the manual shard charge below. */
    public static final int MAX_RECEIVE = 2_000;
    /** FE added per Prismium Shard via the manual charge interaction. */
    public static final int SHARD_CHARGE_AMOUNT = 2_000;
    /** Ticks to fully process one item once a valid recipe is queued and
     * energy keeps flowing. */
    public static final int PROCESS_TIME_TICKS = 100;
    /** FE spent per tick while actively processing. See class javadoc for
     * why this, times {@link #PROCESS_TIME_TICKS}, equals {@link
     * #SHARD_CHARGE_AMOUNT}. */
    public static final int ENERGY_PER_TICK = 20;
    /** Shards produced per ore item consumed - deliberately more than the
     * 1-2 (Fortune-scalable) a player gets from simply mining the ore
     * block normally, since obtaining the *block* itself to feed in here
     * requires Silk Touch (see class javadoc). */
    public static final int SHARDS_PER_ORE = 3;

    /** Hardcoded conversion table (session 67's deliberately conservative
     * "stage 1" scope - see class javadoc and PROGRESS.md for why this
     * was not built as a data-driven recipe type). Populated lazily from
     * {@link ModItems} inside {@link #recipeFor(Item)} rather than as a
     * static field, because {@code ModItems}' {@code RegistryObject}s are
     * not guaranteed populated at class-init time this class might first
     * be touched (same lazy-lookup caution already taken by other
     * cross-registry references in this mod, e.g. {@code
     * PrismiumGeneratorBlockEntity.fuelInventory}'s {@code
     * ModItems.PRISMIUM_SHARD.get()} call happening inside a method body,
     * not a static initializer).
     */
    private static Map<Item, ItemStack> recipes;

    private static Map<Item, ItemStack> recipes() {
        if (recipes == null) {
            Map<Item, ItemStack> map = new HashMap<>();
            Item shard = ModItems.PRISMIUM_SHARD.get();
            map.put(ModItems.PRISMIUM_ORE_ITEM.get(), new ItemStack(shard, SHARDS_PER_ORE));
            map.put(ModItems.DEEPSLATE_PRISMIUM_ORE_ITEM.get(), new ItemStack(shard, SHARDS_PER_ORE));
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
     * slot, used by {@link PrismiumPulverizerMenu#quickMoveStack} to
     * decide whether a shift-clicked stack should route into slot 0
     * (rather than just to the player's own inventory) - see that
     * method's doc. */
    public static boolean isValidInput(ItemStack stack) {
        return !stack.isEmpty() && recipeFor(stack.getItem()) != null;
    }

    // maxExtract is 0: pure sink, same shape as PrismiumWardstoneBlockEntity.
    private final PrismiumEnergyStorage energyStorage = new PrismiumEnergyStorage(CAPACITY, MAX_RECEIVE, 0);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energyStorage);

    /** Slot 0 = input (validated against {@link #recipeFor}), slot 1 =
     * output (rejects all player/hopper insertion via {@link
     * ItemStackHandler#isItemValid} returning false - {@link #serverTick}
     * writes to it directly with {@link ItemStackHandler#setStackInSlot},
     * which - unlike {@link ItemStackHandler#insertItem} - does not
     * consult {@code isItemValid}, exactly the same "internal writer
     * bypasses the public-facing validity gate" trick {@code
     * PrismiumGeneratorBlockEntity#serverTick} already relies on via
     * {@code extractItem} for its own fuel slot, just mirrored for
     * insertion instead of extraction). */
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
            // Read-only from the screen's perspective - same convention
            // every other machine's ContainerData in this mod follows.
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ContainerData getContainerData() {
        return containerData;
    }

    public PrismiumPulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_PULVERIZER.get(), pos, state);
    }

    public PrismiumEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /** Exposed for {@link PrismiumPulverizerMenu} to build its two
     * {@code SlotItemHandler}s against. */
    public ItemStackHandler getInventory() {
        return inventory;
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumPulverizerBlock#getTicker}.
     * Only ever invoked server-side, so no {@code level.isClientSide}
     * guard is needed - same situation as every other ticking block
     * entity in this mod.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumPulverizerBlockEntity pulverizer) {
        ItemStack inputStack = pulverizer.inventory.getStackInSlot(0);
        ItemStack recipeResult = inputStack.isEmpty() ? null : recipeFor(inputStack.getItem());

        boolean canProcess = false;
        if (recipeResult != null) {
            ItemStack outputStack = pulverizer.inventory.getStackInSlot(1);
            if (outputStack.isEmpty()) {
                canProcess = true;
            } else if (ItemStack.isSameItemSameTags(outputStack, recipeResult)
                    && outputStack.getCount() + recipeResult.getCount() <= outputStack.getMaxStackSize()) {
                canProcess = true;
            }
        }

        boolean nowActive = false;
        if (recipeResult == null) {
            // No valid item queued at all: drop any in-progress fraction,
            // mirroring vanilla furnace forgetting smelt progress once the
            // smelting item is pulled out mid-cook.
            pulverizer.progress = 0;
        } else if (canProcess && pulverizer.energyStorage.getEnergyStored() >= ENERGY_PER_TICK) {
            pulverizer.energyStorage.setEnergy(pulverizer.energyStorage.getEnergyStored() - ENERGY_PER_TICK);
            pulverizer.progress++;
            nowActive = true;
            if (pulverizer.progress >= PROCESS_TIME_TICKS) {
                pulverizer.progress = 0;
                pulverizer.inventory.extractItem(0, 1, false);
                ItemStack outputStack = pulverizer.inventory.getStackInSlot(1);
                if (outputStack.isEmpty()) {
                    pulverizer.inventory.setStackInSlot(1, recipeResult);
                } else {
                    outputStack.grow(recipeResult.getCount());
                    pulverizer.inventory.setStackInSlot(1, outputStack);
                }
            }
        }
        // else: a valid recipe is queued but blocked (output full, or not
        // enough energy) - pause and keep whatever progress has already
        // accumulated, same "don't waste, don't discard" philosophy as
        // PrismiumGeneratorBlockEntity's buffer-full pause.

        boolean wasLit = state.getValue(BlockStateProperties.LIT);
        if (nowActive != wasLit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, nowActive), 3);
        }

        pulverizer.active = nowActive;
        pulverizer.setChanged();
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
        return Component.translatable("block.claudemod.prismium_pulverizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumPulverizerMenu(windowId, inventory, containerData,
                ContainerLevelAccess.create(level, worldPosition), this.inventory);
    }
}
