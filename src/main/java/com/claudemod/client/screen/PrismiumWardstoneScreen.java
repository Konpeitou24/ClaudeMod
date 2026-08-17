package com.claudemod.client.screen;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumWardstoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for Prismium Wardstone's GUI (session 27) - see
 * {@link PrismiumWardstoneMenu} for the shared menu logic and why
 * Wardstone was picked as the mod's fifth GUI target, the last of the
 * three original consumer blocks (Pylon session 19/GUI session 25,
 * Restorer session 20/GUI session 26, Wardstone session 21/GUI session
 * 27 - this class). With this screen, every energy block in the mod
 * (Cell, Generator, Pylon, Restorer, Wardstone) now has a GUI.
 *
 * Structurally identical to {@link PrismiumPylonScreen} (176x90, a
 * single horizontal energy bar plus a small square status lamp) rather
 * than {@link PrismiumRestorerScreen}'s lamp-less layout - Wardstone,
 * like Pylon, has a ticking "warding right now" boolean (see
 * {@link PrismiumWardstoneMenu#isActive()}) to visualize. The lamp uses
 * a red palette instead of Pylon's violet/cyan, directly reusing the
 * exact {@code RUNE_LIT_EDGE}/{@code RUNE_LIT_MID} hex values the
 * block's own lit-state texture uses
 * (scripts/textures/gen_prismium_wardstone.py) so the GUI reads as an
 * extension of the block's existing rune-glow language, the same
 * "GUI lamp echoes the block's own lit-state colors" strategy Pylon's
 * screen established in session 25.
 */
public class PrismiumWardstoneScreen extends AbstractContainerScreen<PrismiumWardstoneMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/gui/container/prismium_wardstone.png");

    private static final int BAR_X = 8;
    private static final int BAR_Y = 34;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 14;

    // Status lamp geometry: identical placement to PrismiumPylonScreen's
    // lamp, a small square sitting to the left of the status label,
    // above the energy bar.
    private static final int LAMP_X = 8;
    private static final int LAMP_Y = 18;
    private static final int LAMP_SIZE = 8;

    // Same teal energy-bar palette as every other machine's GUI - only
    // the lamp/frame colors change per-block (see class doc).
    private static final int FILL_BASE = 0xFF3FBDB8;
    private static final int FILL_HILITE = 0xFF66D9D2;

    // Wardstone-specific accents, copied verbatim from
    // scripts/textures/gen_prismium_wardstone.py's
    // RUNE_LIT_EDGE/RUNE_LIT_MID so the GUI's "active" lamp matches the
    // block's own lit rune glow.
    private static final int LAMP_IDLE = 0xFF4A5A58;
    private static final int LAMP_ACTIVE_CORE = 0xFFFF4A3D;
    private static final int LAMP_ACTIVE_EDGE = 0xFFB8221F;

    public PrismiumWardstoneScreen(PrismiumWardstoneMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 90;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        boolean active = menu.isActive();
        int lampX = leftPos + LAMP_X;
        int lampY = topPos + LAMP_Y;
        if (active) {
            guiGraphics.fill(lampX, lampY, lampX + LAMP_SIZE, lampY + LAMP_SIZE, LAMP_ACTIVE_EDGE);
            guiGraphics.fill(lampX + 2, lampY + 2, lampX + LAMP_SIZE - 2, lampY + LAMP_SIZE - 2, LAMP_ACTIVE_CORE);
        } else {
            guiGraphics.fill(lampX, lampY, lampX + LAMP_SIZE, lampY + LAMP_SIZE, LAMP_IDLE);
        }

        int energy = menu.getEnergy();
        int max = Math.max(1, menu.getMaxEnergy());
        int filled = (int) ((long) BAR_WIDTH * energy / max);
        if (filled > 0) {
            int barX = leftPos + BAR_X;
            int barY = topPos + BAR_Y;
            guiGraphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, FILL_BASE);
            guiGraphics.fill(barX, barY, barX + filled, barY + 3, FILL_HILITE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0x404040, false);

        Component statusText = Component.translatable(menu.isActive()
                ? "gui.claudemod.wardstone_status_active"
                : "gui.claudemod.wardstone_status_idle");
        guiGraphics.drawString(font, statusText, LAMP_X + LAMP_SIZE + 6, LAMP_Y, 0x404040, false);

        Component energyText = Component.translatable("gui.claudemod.fe_amount",
                menu.getEnergy(), menu.getMaxEnergy());
        int textWidth = font.width(energyText);
        guiGraphics.drawString(font, energyText, (imageWidth - textWidth) / 2, imageHeight - 34, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
