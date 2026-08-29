package com.claudemod.blockentity;

import com.claudemod.menu.PrismiumCompressorMenu;
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
 * Block entity for Prismium Compressor (session 70): the mod's third
 * item-processing machine, taking the production chain one step further
 * again - Prismium Ingot -[this class]-> Prismium Alloy Ingot, mirroring
 * Smelter's many-to-one "refining" shape rather than Pulverizer's
 * one-to-many "grinding" shape.
 *
 * <p><b>Session #77 update</b>: now extends {@link
 * AbstractPrismiumMachineBlockEntity} along with {@link
 * PrismiumPulverizerBlockEntity} and {@link PrismiumSmelterBlockEntity} -
 * this being the third machine built to the exact same shape as the
 * other two is what finally justified the extraction (see the base
 * class's javadoc). Overrides {@link #inputCountPerOperation()} to
 * return {@link #INGOTS_PER_ALLOY_INGOT}, the same many-to-one pattern
 * Smelter uses.
 *
 * <p><b>Unverified</b> (this sandbox cannot launch the client - see
 * PROGRESS.md standing note repeated in every block entity in this mod):
 * whether the session #77 refactor preserved behavior exactly (no
 * intended change versus the previous standalone implementation).
 */
public class PrismiumCompressorBlockEntity extends AbstractPrismiumMachineBlockEntity {

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
        return INGOTS_PER_ALLOY_INGOT;
    }

    /** Public check for whether an item would be accepted by the input
     * slot, used by {@link PrismiumCompressorMenu#quickMoveStack}. */
    public static boolean isValidInput(ItemStack stack) {
        return !stack.isEmpty() && recipeForStatic(stack.getItem()) != null;
    }

    public PrismiumCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRISMIUM_COMPRESSOR.get(), pos, state);
    }

    /**
     * Static server tick, bound via {@code BaseEntityBlock#createTickerHelper}
     * in {@link com.claudemod.block.PrismiumCompressorBlock#getTicker}.
     * Only ever invoked server-side. Delegates to the shared {@link
     * AbstractPrismiumMachineBlockEntity#tick}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PrismiumCompressorBlockEntity compressor) {
        AbstractPrismiumMachineBlockEntity.tick(level, pos, state, compressor);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.claudemod.prismium_compressor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new PrismiumCompressorMenu(windowId, inventory, getContainerData(),
                ContainerLevelAccess.create(level, worldPosition), getInventory());
    }
}
