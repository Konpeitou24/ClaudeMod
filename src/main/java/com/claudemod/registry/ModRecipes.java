package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.item.PrismiumCompendiumRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for every custom {@link RecipeSerializer} added by ClaudeMod.
 *
 * <p>This mod's first custom (NBT-producing) crafting recipe is {@link
 * PrismiumCompendiumRecipe} (PROGRESS.md TODO13, scheduled session) - see
 * that class's doc for why an ordinary shapeless recipe JSON cannot
 * express it. {@link SimpleCraftingRecipeSerializer} is vanilla's own
 * generic serializer for exactly this kind of code-driven "special"
 * recipe (used by {@code BookCloningRecipe}, {@code
 * ArmorDyeRecipe}, etc.), confirmed via mappings.dev (1.20.1) this
 * session: its single constructor takes a {@code Factory<T>} - a
 * {@code (ResourceLocation, CraftingBookCategory) -> T} functional
 * interface - which the recipe class's own {@code (ResourceLocation,
 * CraftingBookCategory)} constructor satisfies directly via a method
 * reference, so no extra factory class is needed here.
 */
public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ClaudeMod.MOD_ID);

    public static final RegistryObject<SimpleCraftingRecipeSerializer<PrismiumCompendiumRecipe>> PRISMIUM_COMPENDIUM_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("prismium_compendium",
                    () -> new SimpleCraftingRecipeSerializer<>(PrismiumCompendiumRecipe::new));

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
