package com.claudemod.client.screen;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumCellMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen (client-only rendering) for Prismium Cell's GUI - see
 * {@link PrismiumCellMenu} for the shared menu logic and why this is the
 * mod's first GUI (session 23).
 *
 * Deliberately compact (176x90, no player-inventory slot grid is drawn
 * since the menu has no slots at all - see {@link PrismiumCellMenu}'s
 * class doc) rather than reusing vanilla's 176x166 furnace-style
 * dimensions padded with an unused inventory area. The energy bar itself
 * is not baked into the background texture as a pre-rendered gauge
 * sprite; instead the texture only supplies the static frame/casing
 * artwork and this class draws the proportional fill with
 * {@link GuiGraphics#fill} each frame. This mirrors how vanilla's
 * enchanting table level-cost bar (numeric, not textured) trades a bit of
 * texture polish for zero risk of the fill rectangle's pixels drifting
 * out of alignment with a hand-authored gauge sprite - acceptable for a
 * first GUI where the priority is validating the Menu/Screen wiring
 * itself.
 */
public class PrismiumCellScreen extends AbstractContainerScreen<PrismiumCellMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/gui/container/prismium_cell.png");

    private static final int BAR_X = 8;
    private static final int BAR_Y = 34;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 14;

    /** Two-tone fill (darker base + lighter core stripe) so the bar reads
     * as "glowing energy" rather than a flat rectangle, echoing the
     * PRISMIUM_MID/PRISMIUM_HILITE pairing used across the mod's block
     * textures (see e.g. scripts/textures/gen_prismium_cell.py) without
     * needing a second texture asset. */
    private static final int FILL_BASE = 0xFF3FBDB8;
    private static final int FILL_HILITE = 0xFF66D9D2;

    public PrismiumCellScreen(PrismiumCellMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 90;
        // No player-inventory slot grid exists in this menu (see
        // PrismiumCellMenu), so renderLabels() below is fully overridden
        // rather than calling super - vanilla's default "Inventory"
        // label is never drawn.
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // leftPos/topPos are precomputed by AbstractContainerScreen#init()
        // from (width - imageWidth) / 2 etc. - reused here rather than
        // recomputed, per the standard pattern (Forge docs "Screens" page,
        // 1.20.x: renderBg's canonical body is a single blit at
        // (leftPos, topPos)). The background PNG is a full 256x256 canvas
        // with artwork only in the top-left 176x90 (see
        // scripts/textures/gen_prismium_cell_gui.py) - the 7-int blit
        // overload used below always normalizes its u/v/width/height
        // against an assumed 256x256 source image, so the file must be
        // that size even though only a small corner of it is drawn.
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int energy = menu.getEnergy();
        int max = Math.max(1, menu.getMaxEnergy());
        int filled = (int) ((long) BAR_WIDTH * energy / max);
        if (filled > 0) {
            int barX = leftPos + BAR_X;
            int barY = topPos + BAR_Y;
            guiGraphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, FILL_BASE);
            // Thin brighter core stripe along the top of the fill for a
            // subtle "glow" look, matching the mod's established
            // PRISMIUM_MID -> PRISMIUM_HILITE gradient direction (dark at
            // the edges, bright toward the center/top of a glass window).
            guiGraphics.fill(barX, barY, barX + filled, barY + 3, FILL_HILITE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0x404040, false);

        Component energyText = Component.translatable("gui.claudemod.fe_amount",
                menu.getEnergy(), menu.getMaxEnergy());
        int textWidth = font.width(energyText);
        guiGraphics.drawString(font, energyText, (imageWidth - textWidth) / 2, imageHeight - 34, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Standard three-step AbstractContainerScreen#render override
        // (Forge docs "Screens" page, 1.20.x): dim the world behind the
        // GUI, let the superclass drive renderBg/renderLabels/widgets,
        // then draw tooltips on top of everything else.
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
