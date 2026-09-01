package com.claudemod.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * GitHub issue #21 ("プリズミウムのインゴットなどのアイテムは粉砕、精錬など
 * このMODの製法で作られたアイテムに対応されていません" - this mod's own
 * machine outputs don't show up in JEI at all). This mod's three
 * item-processing machines (Pulverizer/Smelter/Compressor, see {@link
 * com.claudemod.blockentity.AbstractPrismiumMachineBlockEntity}) were built
 * with a deliberately simple hardcoded {@code Map<Item, ItemStack>} per
 * machine rather than a datapack-driven {@code Recipe<Container>} type (see
 * each machine's own class javadoc for that "stage 1" scope decision) - so
 * unlike vanilla furnace/crafting recipes, there is no {@code RecipeManager}
 * entry for JEI's usual "read recipes from the recipe manager" integration
 * path to find. JEI's own plugin API supports this exact situation directly
 * (see {@code IRecipeRegistration#addRecipes}'s javadoc, which only asks for
 * a {@code List<T>} of plugin-supplied objects, with no requirement that
 * {@code T} be a vanilla {@code Recipe}), so this small record-like POJO is
 * this mod's synthetic "recipe" purely for JEI's consumption - built
 * directly from each machine's own {@code jeiRecipes()} accessor (see e.g.
 * {@link com.claudemod.blockentity.PrismiumPulverizerBlockEntity#jeiRecipes()})
 * so the input/output pairs are never duplicated a second time here.
 */
public final class MachineJeiRecipe {

    private final ItemStack input;
    private final ItemStack output;
    private final int energyCostFe;

    public MachineJeiRecipe(ItemStack input, ItemStack output, int energyCostFe) {
        this.input = input;
        this.output = output;
        this.energyCostFe = energyCostFe;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getEnergyCostFe() {
        return energyCostFe;
    }
}
