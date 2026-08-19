package com.claudemod.client.screen;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumPulverizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for Prismium Pulverizer's GUI (session 67) - see {@link
 * PrismiumPulverizerMenu} for the slot layout/shift-click logic this
 * class only renders.
 *
 * Structurally follows {@link PrismiumGeneratorScreen}'s established
 * split (background/gauges baked into a 256x256 texture with real
 * artwork confined to the top-left panel, proportional fills drawn here
 * in code) but is the mod's first screen to also draw vanilla-style item
 * slots plus a full player-inventory grid underneath, following the
 * same 176x148 (was going to be 166 like vanilla furnace, but this panel
 * has no separate fuel slot/flame gauge to make room for above the
 * player inventory grid, so it is shorter - see class constants below,
 * which must match {@link PrismiumPulverizerMenu}'s slot coordinates
 * exactly for the icons to line up).
 */
public class PrismiumPulverizerScreen extends AbstractContainerScreen<PrismiumPulverizerMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/gui/container/prismium_pulverizer.png");

    // Progress track: a short horizontal bar between the input and output
    // slots (both at y=20, 18px tall - vertically centered against them).
    private static final int PROGRESS_X = 82;
    private static final int PROGRESS_Y = 26;
    private static final int PROGRESS_WIDTH = 28;
    private static final int PROGRESS_HEIGHT = 6;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 46;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 10;

    // Same teal energy-bar palette as every other machine's GUI in this
    // mod (Cell/Generator/Pylon/Restorer/Wardstone), so this reads as a
    // member of the same FE-system family.
    private static final int FILL_BASE = 0xFF3FBDB8;
    private static final int FILL_HILITE = 0xFF66D9D2;

    // Progress fill: a warm amber/purple blend distinct from the teal
    // energy bar (this bar tracks *item* progress, not FE), echoing the
    // magenta Prismium accent color used across the mod's gem-family
    // items rather than inventing an unrelated hue.
    private static final int PROGRESS_BASE = 0xFFB35FE0;
    private static final int PROGRESS_HILITE = 0xFFDA9CF5;

    private static final int STATUS_ACTIVE = 0xFF4CD97B;
    private static final int STATUS_IDLE = 0xFF8A8A8A;

    public PrismiumPulverizerScreen(PrismiumPulverizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 148;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        float progressFraction = menu.getProgressFraction();
        int filledWidth = Math.round(PROGRESS_WIDTH * progressFraction);
        if (filledWidth > 0) {
            int px = leftPos + PROGRESS_X;
            int py = topPos + PROGRESS_Y;
            guiGraphics.fill(px, py, px + filledWidth, py + PROGRESS_HEIGHT, PROGRESS_BASE);
            guiGraphics.fill(px, py, px + filledWidth, py + 2, PROGRESS_HILITE);
        }

        int energy = menu.getEnergy();
        int max = Math.max(1, menu.getMaxEnergy());
        int filledEnergyWidth = (int) ((long) BAR_WIDTH * energy / max);
        if (filledEnergyWidth > 0) {
            int barX = leftPos + BAR_X;
            int barY = topPos + BAR_Y;
            guiGraphics.fill(barX, barY, barX + filledEnergyWidth, barY + BAR_HEIGHT, FILL_BASE);
            guiGraphics.fill(barX, barY, barX + filledEnergyWidth, barY + 3, FILL_HILITE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0x404040, false);

        Component statusText = Component.translatable(menu.isActive()
                ? "gui.claudemod.pulverizer_status_active"
                : "gui.claudemod.pulverizer_status_idle");
        int statusColor = menu.isActive() ? STATUS_ACTIVE : STATUS_IDLE;
        guiGraphics.drawString(font, statusText, BAR_X, BAR_Y - 10, statusColor, false);

        Component energyText = Component.translatable("gui.claudemod.fe_amount", menu.getEnergy(), menu.getMaxEnergy());
        int textWidth = font.width(energyText);
        guiGraphics.drawString(font, energyText, (imageWidth - textWidth) / 2, BAR_Y + BAR_HEIGHT + 3, 0xFFFFFF, false);
        // Deliberately no "Inventory" label for the player grid below -
        // see constructor comment; this mod's other screens never show
        // one either, and the grid's position is self-explanatory.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
