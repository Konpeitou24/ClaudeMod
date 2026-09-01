package com.claudemod.blockentity;

import com.claudemod.menu.PrismiumSmelterMenu;
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
 * Block entity for Prismium Smelter (session 68): the mod's second
 * item-processing machine, extending the production chain
 * {@link PrismiumPulverizerBlockEntity} (session 67) started one step
 * further - Prismium Ore -[Silk Touch + Pulverizer]-> Prismium Shard
 * -[this class]-> Prismium Ingot, the mod's first refined-material item
 * (see {@code ModItems.PRISMIUM_INGOT}).
 *
 * <p><b>Session #77 update</b>: now extends {@link
 * AbstractPrismiumMachineBlockEntity}, which absorbed the energy/
 * inventory/ContainerData/NBT/capability plumbing this class used to
 * duplicate byte-for-byte from Pulverizer (see that base class's
 * javadoc). This class overrides {@link #inputCountPerOperation()} to
 * return {@link #SHARDS_PER_INGOT}, capturing the one real behavioral
 * difference from Pulverizer: a many-to-one "refining" ratio (unlike
 * Pulverizer's one-to-many "grinding" ratio) so the two machines feel
 * like opposite ends of the same chain.
 *
 * <p><b>Recipe</b>: {@link #SHARDS_PER_INGOT} (4) Prismium Shards produce
 * 1 Prismium Ingot.
 *
 * <p><b>Unverified</b> (this sandbox cannot launch the client - see
 * PROGRESS.md standing note repeated in every block entity in this mod):
 * whether the session #77 refactor preserved behavior exactly (no
 * intended change), on top of pre-existing unverified items - whether
 * consuming 4 shards for 1 ingot feels like a fair trade in practice,
 * and whether the process pacing still feels appropriate for a
 * "refining" step.
 */
public class PrismiumSmelterBlockEntity extends AbstractPrismiumMachineBlockEntity {

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

    /**
     * Read-only view of {@link #recipes()} for {@code
     * com.claudemod.compat.jei.ClaudeModJeiPlugin} to build JEI recipe
     * entries from, added this session for GitHub issue #21 ("プリズミウム
     * のインゴットなどのアイテムは粉砕、精錬などこのMODの製法で作られた
     * アイテムに対応されていません" - JEI doesn't show this mod's own
     * machine recipes). Deliberately reads from the same hardcoded map
     * this class's own {@code recipeFor}/{@code isValidInput} already
     * use as their single source of truth, rather than duplicating the
     * input/output pairs a second time in the JEI plugin package, so the
     * two can never silently drift apart if a future session edits one
     * without the other. Only ever called when JEI is installed (see
     * {@code ClaudeModJeiPlugin}'s own soft-dependency isolation
     * javadoc) - {@code java.util.Map} is already imported by every
     * caller of this class regardless, so this adds no new dependency.
     */
    public static java.util.Map<Item, ItemStack> jeiRecipes() {
        return java.util.Collections.unmodifiableMap(recipes());
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

    @Override
    protected int inputCountPerOperation() {
        return SHARDS_PER_INGOT;
    }

    /** Public check for whether an item would be accepted by the input
     * slot, used by {@link PrismiumSmelterMenu#quickMoveStack}. Unlike
     * Pulverizer's {@code isValidInput}, the recipe table above maps to
     * a 1-count result per input item, but the shared tick logic still
     * requires {@link #SHARDS_PER_INGOT} shards to actually be present
     * in the input slot before it starts consuming. */
    public static boolean isValidInput(ItemStack stack) {
        return !stack.isEmpty() && recipeForStatic(stack.getItem()) != null;
    }

    public PrismiumSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_SMELTER.get(), pos, state);
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumSmelterBlock#getTicker}. Only
     * ever invoked server-side. Delegates to the shared {@link
     * AbstractPrismiumMachineBlockEntity#tick}, which already handles the
     * many-to-one {@link #inputCountPerOperation()} case generically.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumSmelterBlockEntity smelter) {
        AbstractPrismiumMachineBlockEntity.tick(level, pos, state, smelter);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.claudemod.prismium_smelter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumSmelterMenu(windowId, inventory, getContainerData(),
                ContainerLevelAccess.create(level, worldPosition), getInventory());
    }
}
