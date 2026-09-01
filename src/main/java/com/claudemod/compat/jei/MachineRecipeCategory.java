package com.claudemod.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Single shared {@link IRecipeCategory} implementation for all three of this
 * mod's item-processing machines (Pulverizer/Smelter/Compressor) - see
 * {@link MachineJeiRecipe}'s javadoc for why a JEI category is needed here
 * at all. One parametrized class rather than three near-identical ones
 * mirrors this mod's own established pattern for these three machines (see
 * {@code AbstractPrismiumMachineBlockEntity}'s javadoc: "a third machine
 * confirmed the three really were the same shape").
 *
 * <p><b>API confirmed this session</b> directly against JEI's own {@code
 * 1.20.1} branch source (fetched via {@code raw.githubusercontent.com};
 * note the plain {@code 1.20} branch on the same repo tracks a *newer*,
 * incompatible API surface - e.g. it already references {@code
 * DataComponentPatch}, a Minecraft 1.20.5+ concept - so this was
 * double-checked against {@code 1.20.1} specifically, matching this mod's
 * pinned {@code jei_version} for Minecraft 1.20.1): {@link
 * IRecipeCategory#getBackground()} has been nullable-and-deprecated since
 * JEI 15.20.0 in favor of overriding {@link #getWidth()}/{@link
 * #getHeight()} directly and drawing everything in {@link #draw}, which is
 * what this class does - no background texture asset was created for this,
 * since a plain panel plus a drawn arrow conveys "input becomes output" on
 * its own without needing new art. {@link IRecipeLayoutBuilder#addInputSlot(int, int)}/
 * {@code #addOutputSlot(int, int)} (JEI 15.20.0+) and {@code
 * IIngredientAcceptor#addItemStack(ItemStack)} were both confirmed against
 * the same source rather than assumed from memory of older JEI API
 * generations, which used a materially different {@code
 * IGuiItemStackGroup}-based setup this mod's pinned 15.56.0.204 no longer
 * exposes.
 *
 * <p><b>Unverified in-game</b> (no Minecraft client in this sandbox, per
 * PROGRESS.md's standing note): the actual pixel layout (slot spacing,
 * whether the arrow/energy-cost text overlaps the vanilla recipe-category
 * tab list or the JEI "+"/bookmark buttons at these exact dimensions), and
 * whether {@link Minecraft#font} is safe to read at the point {@link #draw}
 * runs (every other GuiGraphics-based renderer already in this mod's {@code
 * client} package reads it the same way, e.g. {@code
 * FeatherstoneReductionOverlay}, so this follows established precedent
 * rather than guessing a new pattern, but JEI's own recipe-GUI render
 * timing has not specifically been exercised before in this mod).
 */
public final class MachineRecipeCategory implements IRecipeCategory<MachineJeiRecipe> {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 46;
    private static final int SLOT_SIZE = 18;
    private static final int INPUT_SLOT_X = 8;
    private static final int OUTPUT_SLOT_X = WIDTH - SLOT_SIZE - 8;
    private static final int SLOT_Y = 6;
    private static final int ARROW_Y = SLOT_Y + SLOT_SIZE / 2 - 4;
    private static final int ENERGY_TEXT_Y = SLOT_Y + SLOT_SIZE + 6;

    private final RecipeType<MachineJeiRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;

    public MachineRecipeCategory(RecipeType<MachineJeiRecipe> recipeType, Component title,
                                  ItemStack iconStack, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<MachineJeiRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(INPUT_SLOT_X, SLOT_Y).addItemStack(recipe.getInput());
        builder.addOutputSlot(OUTPUT_SLOT_X, SLOT_Y).addItemStack(recipe.getOutput());
    }

    /**
     * Draws the arrow between the two slots and the fixed energy-cost line
     * underneath, both as plain text rather than sprite art - see class
     * javadoc for why no background/arrow texture asset was created this
     * session. The arrow glyph itself ("→", U+2192) is drawn via {@code
     * GuiGraphics#drawString}, the same vanilla font every other text this
     * mod renders (tooltips, HUD overlays) already goes through, so no new
     * font/glyph dependency is introduced.
     */
    @Override
    public void draw(MachineJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        String arrow = "→";
        int arrowWidth = font.width(arrow);
        int arrowX = (INPUT_SLOT_X + SLOT_SIZE + OUTPUT_SLOT_X) / 2 - arrowWidth / 2;
        guiGraphics.drawString(font, arrow, arrowX, ARROW_Y, 0xFF404040, false);

        Component energyText = Component.translatable("jei.claudemod.category.energy_cost", recipe.getEnergyCostFe());
        int energyWidth = font.width(energyText);
        guiGraphics.drawString(font, energyText, (WIDTH - energyWidth) / 2, ENERGY_TEXT_Y, 0xFF808080, false);
    }
}
