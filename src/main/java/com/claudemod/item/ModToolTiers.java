package com.claudemod.item;

import com.claudemod.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

import java.util.function.Supplier;

/**
 * Custom tool tiers added by ClaudeMod.
 *
 * PRISMIUM sits just above vanilla diamond: same harvest level (mines
 * everything diamond can - no dedicated "needs_prismium_tool" block exists
 * yet), but with better durability/speed/damage and a very high
 * enchantability, repaired with Prismium Shard. A future session may add
 * a "needs_prismium_tool" block tag for Prism Realm content so this tier
 * gets an exclusive harvesting niche too (see PROGRESS.md).
 */
public final class ModToolTiers {

    public static final ForgeTier PRISMIUM = new ForgeTier(
            3,                                  // harvest level: same as diamond
            1900,                                // durability: above diamond's 1561
            9.0f,                                 // mining speed: above diamond's 8.0
            3.5f,                                 // attack damage bonus: above diamond's 3.0
            14,                                   // enchantability: above diamond's 10
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // blocks this tier fails to harvest correctly
            () -> Ingredient.of(ModItems.PRISMIUM_SHARD.get())
    );

    private ModToolTiers() {
    }
}
