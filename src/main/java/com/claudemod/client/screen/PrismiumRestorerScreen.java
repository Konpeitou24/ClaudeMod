package com.claudemod.client.screen;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumRestorerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for Prismium Restorer's GUI (session 26) - see
 * {@link PrismiumRestorerMenu} for the shared menu logic and why Restorer
 * was picked as the mod's fourth GUI target (PROGRESS.md session 24/25
 * handoff, option (a): "同じMenu/Screenパターンを消費ブロック3種
 * (Pylon・Restorer・Wardstone)へさらに展開する", Restorer chosen second
 * among the three consumer blocks, following the order they were
 * originally added - Pylon session 19, Restorer session 20, Wardstone
 * session 21).
 *
 * Structurally identical to {@link PrismiumCellScreen} (176x90, a single
 * horizontal energy bar, no status lamp) rather than
 * {@link PrismiumPylonScreen}'s lamp+bar layout - Restorer has no ticking
 * "active/idle" boolean to visualize (see {@link PrismiumRestorerMenu}'s
 * class doc), only the energy buffer that its manual shard-charge and
 * repair actions already read/write via
 * {@link com.claudemod.block.PrismiumRestorerBlock#use}. The one
 * deliberate visual departure from Cell's screen is the panel's outline
 * color: gold/amber instead of Cell/Generator's teal, matching
 * Restorer's own "mending cross" block texture accent
 * (scripts/textures/gen_prismium_restorer.py's CROSS_MID/CROSS_EDGE) -
 * the same "let the GUI's frame color echo the block's own accent"
 * strategy Pylon's violet outline established in session 25, continuing
 * the plan floated in PROGRESS.md's session 25 "議論したい論点"
 * ("消費ブロックの見分けやすさ、GUIにも波及").
 */
public class PrismiumRestorerScreen extends AbstractContainerScreen<PrismiumRestorerMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/gui/container/prismium_restorer.png");

    private static final int BAR_X = 8;
    private static final int BAR_Y = 34;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 14;

    // Same teal energy-bar fill as every other machine's GUI - only the
    // panel's frame color changes per-block (see class doc), the energy
    // bar itself stays part of the shared "FE system" visual language.
    private static final int FILL_BASE = 0xFF3FBDB8;
    private static final int FILL_HILITE = 0xFF66D9D2;

    public PrismiumRestorerScreen(PrismiumRestorerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 90;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

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
