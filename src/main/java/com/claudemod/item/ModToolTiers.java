package com.claudemod.item;

import com.claudemod.registry.ModBlockTags;
import com.claudemod.registry.ModItems;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

/**
 * Custom tool tiers added by ClaudeMod.
 *
 * PRISMIUM sits just above vanilla diamond: same harvest level (mines
 * everything diamond can), but with better durability/speed/damage and a
 * very high enchantability, repaired with Prismium Shard. Since session 2,
 * it also has an exclusive harvesting niche: Prismium Core (see ModBlocks)
 * requires this tier and cannot be mined correctly with diamond or below.
 * That exclusivity is expressed through the tier's own tag,
 * {@link ModBlockTags#INCORRECT_FOR_PRISMIUM_TOOL}, rather than reusing
 * vanilla's INCORRECT_FOR_DIAMOND_TOOL - see ModBlockTags' javadoc for why.
 */
public final class ModToolTiers {

    public static final ForgeTier PRISMIUM = new ForgeTier(
            3,                                       // harvest level: same as diamond
            1900,                                     // durability: above diamond's 1561
            9.0f,                                       // mining speed: above diamond's 8.0
            3.5f,                                       // attack damage bonus: above diamond's 3.0
            14,                                         // enchantability: above diamond's 10
            ModBlockTags.INCORRECT_FOR_PRISMIUM_TOOL,   // blocks this tier fails to harvest correctly
            () -> Ingredient.of(ModItems.PRISMIUM_SHARD.get())
    );

    private ModToolTiers() {
    }
}
