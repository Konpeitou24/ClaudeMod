package com.claudemod.client.screen;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumPylonMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for Prismium Pylon's GUI (session 25) - see
 * {@link PrismiumPylonMenu} for the shared menu logic and why Pylon was
 * picked as the mod's third GUI target (PROGRESS.md session 24 handoff,
 * option (a): "同じMenu/Screenパターンを消費ブロック3種
 * (Pylon・Restorer・Wardstone)へさらに展開する", Pylon chosen first among
 * the three since it was the first consumer block added, session 19).
 *
 * Structurally closest to {@link PrismiumCellScreen} (176x90, a single
 * horizontal energy bar, no second gauge) rather than
 * {@link PrismiumGeneratorScreen} (176x110, an extra vertical flame
 * gauge) - Pylon has nothing analogous to Generator's burn-time counter
 * to visualize, only a boolean "radiating right now" state
 * (see {@link PrismiumPylonMenu#isActive()}), so this screen adds a small
 * square status lamp instead of a second bar: dim gray while idle, a
 * two-tone violet/cyan glow while active, directly reusing the exact
 * {@code PRISMIUM_ACCENT}/{@code CYAN_ACCENT} hex values the block's own
 * lit-state texture uses (scripts/textures/gen_prismium_pylon.py) so the
 * GUI reads as an extension of the block's existing crystal-glow
 * language rather than inventing a fourth unrelated indicator color.
 */
public class PrismiumPylonScreen extends AbstractContainerScreen<PrismiumPylonMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/gui/container/prismium_pylon.png");

    private static final int BAR_X = 8;
    private static final int BAR_Y = 34;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 14;

    // Status lamp geometry: a small square sitting to the left of the
    // status label, above the energy bar.
    private static final int LAMP_X = 8;
    private static final int LAMP_Y = 18;
    private static final int LAMP_SIZE = 8;

    // Same teal energy-bar palette as PrismiumCellScreen/PrismiumGeneratorScreen,
    // so all three machines' GUIs read as members of the same FE-system family.
    private static final int FILL_BASE = 0xFF3FBDB8;
    private static final int FILL_HILITE = 0xFF66D9D2;

    // Pylon-specific accents, copied verbatim from
    // scripts/textures/gen_prismium_pylon.py's PRISMIUM_ACCENT/CYAN_ACCENT
    // so the GUI's "active" lamp matches the block's own lit crystal glow.
    private static final int LAMP_IDLE = 0xFF4A5A58;
    private static final int LAMP_ACTIVE_CORE = 0xFFC97BFF;
    private static final int LAMP_ACTIVE_EDGE = 0xFF39E6D6;

    public PrismiumPylonScreen(PrismiumPylonMenu menu, Inventory playerInventory, Component title) {
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
                ? "gui.claudemod.pylon_status_active"
                : "gui.claudemod.pylon_status_idle");
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
