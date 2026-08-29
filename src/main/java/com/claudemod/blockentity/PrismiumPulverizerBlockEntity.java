package com.claudemod.blockentity;

import com.claudemod.menu.PrismiumPulverizerMenu;
import com.claudemod.registry.ModBlockEntities;
import com.claudemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
 * <p><b>Session #77 update</b>: the energy/inventory/ContainerData/NBT/
 * capability plumbing that used to live directly in this class has been
 * moved up into {@link AbstractPrismiumMachineBlockEntity}, shared with
 * {@link PrismiumSmelterBlockEntity} and {@link
 * PrismiumCompressorBlockEntity} now that a third machine confirmed the
 * three really were the same shape (see that class's javadoc for why
 * this had deliberately been left un-generalized until now). This class
 * now only keeps what is genuinely specific to the pulverizer: its
 * recipe table, its 1-to-many {@link #SHARDS_PER_ORE} ratio (the default
 * {@code inputCountPerOperation() == 1} from the base class matches this
 * without needing an override), its menu type, and its translation key.
 *
 * <p>What the pulverizer actually does: it re-processes an already-mined
 * Prismium Ore or Deepslate Prismium Ore block (only obtainable via Silk
 * Touch, see {@code data/claudemod/loot_tables/blocks/prismium_ore.json}
 * - normal mining already drops 1-2 shards directly, with Fortune scaling
 * that further) into a *guaranteed* {@link #SHARDS_PER_ORE} Prismium
 * Shards, consuming FE instead of a tool enchantment.
 *
 * <p><b>Unverified</b> (this sandbox cannot launch the client - see
 * PROGRESS.md standing note repeated in every block entity in this mod):
 * whether the session #77 refactor into {@link
 * AbstractPrismiumMachineBlockEntity} preserved behavior exactly (no
 * intended change, see that class's javadoc), on top of the pulverizer's
 * own pre-existing unverified items (process pacing, pause-not-reset
 * behavior, LIT toggling under a queued stack).
 */
public class PrismiumPulverizerBlockEntity extends AbstractPrismiumMachineBlockEntity {

    /** Shards produced per ore item consumed - deliberately more than the
     * 1-2 (Fortune-scalable) a player gets from simply mining the ore
     * block normally, since obtaining the *block* itself to feed in here
     * requires Silk Touch (see class javadoc). */
    public static final int SHARDS_PER_ORE = 3;

    /** Hardcoded conversion table (session 67's deliberately conservative
     * "stage 1" scope - see class javadoc and PROGRESS.md for why this
     * was not built as a data-driven recipe type). Populated lazily from
     * {@link ModItems} inside {@link #recipeForStatic(Item)} rather than
     * as a static field, because {@code ModItems}' {@code
     * RegistryObject}s are not guaranteed populated at class-init time
     * this class might first be touched. */
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
    private static ItemStack recipeForStatic(Item item) {
        ItemStack result = recipes().get(item);
        return result == null ? null : result.copy();
    }

    @Nullable
    @Override
    protected ItemStack recipeFor(Item item) {
        return recipeForStatic(item);
    }

    /** Public check for whether an item would be accepted by the input
     * slot, used by {@link PrismiumPulverizerMenu#quickMoveStack} to
     * decide whether a shift-clicked stack should route into slot 0
     * (rather than just to the player's own inventory) - see that
     * method's doc. */
    public static boolean isValidInput(ItemStack stack) {
        return !stack.isEmpty() && recipeForStatic(stack.getItem()) != null;
    }

    public PrismiumPulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_PULVERIZER.get(), pos, state);
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumPulverizerBlock#getTicker}.
     * Only ever invoked server-side. Delegates to the shared {@link
     * AbstractPrismiumMachineBlockEntity#tick} - see that method for the
     * actual processing loop, unchanged from before the session #77
     * refactor.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumPulverizerBlockEntity pulverizer) {
        AbstractPrismiumMachineBlockEntity.tick(level, pos, state, pulverizer);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.claudemod.prismium_pulverizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumPulverizerMenu(windowId, inventory, getContainerData(),
                ContainerLevelAccess.create(level, worldPosition), getInventory());
    }
}
