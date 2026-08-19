package com.claudemod.client.screen;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumSmelterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for Prismium Smelter's GUI (session 68) - see {@link
 * PrismiumSmelterMenu} for the slot layout/shift-click logic. Structurally
 * a direct copy of {@link PrismiumPulverizerScreen} (same 176x148 panel,
 * same progress/energy bar geometry) with one palette change: the
 * progress fill uses a warm gold/amber gradient (matching Prismium
 * Ingot's own item-icon palette) instead of Pulverizer's magenta, so the
 * two "twin" GUIs still read as visually distinct machines.
 */
public class PrismiumSmelterScreen extends AbstractContainerScreen<PrismiumSmelterMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/gui/container/prismium_smelter.png");

    private static final int PROGRESS_X = 82;
    private static final int PROGRESS_Y = 26;
    private static final int PROGRESS_WIDTH = 28;
    private static final int PROGRESS_HEIGHT = 6;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 46;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 10;

    private static final int FILL_BASE = 0xFF3FBDB8;
    private static final int FILL_HILITE = 0xFF66D9D2;

    // Gold/amber progress fill - matches Prismium Ingot's own item-icon
    // metal palette (see gen_prismium_ingot.py's METAL_BASE/METAL_HILITE),
    // distinct from Pulverizer's magenta so the two GUIs stay visually
    // distinguishable despite sharing a layout.
    private static final int PROGRESS_BASE = 0xFFC88A2E;
    private static final int PROGRESS_HILITE = 0xFFF6D488;

    private static final int STATUS_ACTIVE = 0xFF4CD97B;
    private static final int STATUS_IDLE = 0xFF8A8A8A;

    public PrismiumSmelterScreen(PrismiumSmelterMenu menu, Inventory playerInventory, Component title) {
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
                ? "gui.claudemod.smelter_status_active"
                : "gui.claudemod.smelter_status_idle");
        int statusColor = menu.isActive() ? STATUS_ACTIVE : STATUS_IDLE;
        guiGraphics.drawString(font, statusText, BAR_X, BAR_Y - 10, statusColor, false);

        Component energyText = Component.translatable("gui.claudemod.fe_amount", menu.getEnergy(), menu.getMaxEnergy());
        int textWidth = font.width(energyText);
        guiGraphics.drawString(font, energyText, (imageWidth - textWidth) / 2, BAR_Y + BAR_HEIGHT + 3, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
