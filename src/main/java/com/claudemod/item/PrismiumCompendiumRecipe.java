package com.claudemod.item;

import com.claudemod.registry.ModItems;
import com.claudemod.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Scheduled session (PROGRESS.md TODO13): lets a player who lost their
 * starting Prismium Compendium (see {@link PrismiumCompendiumFactory},
 * {@link PrismiumCompendiumHandler}) craft a replacement, instead of the
 * item only ever being reachable via the creative inventory or the
 * one-time automatic first-login grant.
 *
 * <p><b>Why a {@link CustomRecipe} instead of an ordinary shapeless
 * recipe JSON</b>: a plain {@code data/claudemod/recipes/*.json} shaped/
 * shapeless recipe can only specify its output as a bare
 * {@code item}+{@code count} pair - it has no way to attach the NBT
 * (the {@code author}/{@code pages}/{@code resolved} tags {@link
 * PrismiumCompendiumFactory} builds) that makes the compendium's pages
 * actually show this mod's guide text instead of an empty book. Vanilla
 * hits the exact same wall for its own NBT-bearing crafting outputs
 * (dyeing leather armor, cloning a written book, firework rockets, map
 * cloning/extending, repairing via the grid) and solves it the same way:
 * a special, code-driven {@code CustomRecipe} subclass whose {@link
 * #assemble} builds the output {@link ItemStack} programmatically. This
 * mod follows that exact pattern rather than inventing a new one.
 *
 * <p><b>Shape confirmed via mappings.dev (1.20.1) this session</b>
 * (following the "未確認のJava APIは必ず出典を確認してから使う" rule in
 * PROGRESS.md, since every method here is an {@code @Override}): {@code
 * CustomRecipe}'s only constructor is {@code (ResourceLocation id,
 * CraftingBookCategory category)}, and it already implements {@code
 * getResultItem(RegistryAccess)} (returns an empty placeholder stack -
 * fine here since the recipe book/JEI preview isn't this mod's concern
 * for a "how do I re-get my guide book" recovery recipe) and {@code
 * isSpecial()} (returns true) for every subclass, so only {@code
 * matches}, {@code assemble}, {@code canCraftInDimensions} and {@code
 * getSerializer} need overriding here - mirroring vanilla's own {@code
 * BookCloningRecipe} shape.
 *
 * <p>Ingredients (shapeless, order-independent across the whole crafting
 * grid): exactly one vanilla {@code minecraft:book}, one {@link
 * ModItems#PRISMIUM_INGOT}, and three {@link ModItems#PRISMIUM_SHARD} -
 * any other item present, or a different count of any of the three,
 * fails {@link #matches}. Chosen to echo "a book infused with Prismium
 * material" rather than reusing the mod's Rift Shard/Anchor materials
 * (those are thematically about the Prism Realm/teleportation, not about
 * the compendium's own subject matter).
 *
 * <p><b>Unverified in-game</b> (no Minecraft client in this sandbox, see
 * PROGRESS.md's standing note): that the crafting grid actually accepts
 * this combination and produces a compendium whose pages render
 * correctly - only confirmed here that the code compiles against the
 * real 1.20.1 API shapes above.
 */
public class PrismiumCompendiumRecipe extends CustomRecipe {

    private static final int REQUIRED_SHARDS = 3;

    public PrismiumCompendiumRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int bookCount = 0;
        int ingotCount = 0;
        int shardCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.BOOK)) {
                bookCount++;
            } else if (stack.is(ModItems.PRISMIUM_INGOT.get())) {
                ingotCount++;
            } else if (stack.is(ModItems.PRISMIUM_SHARD.get())) {
                shardCount++;
            } else {
                return false;
            }
        }

        return bookCount == 1 && ingotCount == 1 && shardCount == REQUIRED_SHARDS;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return PrismiumCompendiumFactory.createStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PRISMIUM_COMPENDIUM_RECIPE_SERIALIZER.get();
    }
}
