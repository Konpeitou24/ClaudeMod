package com.claudemod.client.screen;

import com.claudemod.menu.PrismiumChronoflameMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen (client-only rendering) for Prismium Chronoflame's GUI
 * (scheduled session, direct feedback from こんぺいとう氏) - see
 * {@link PrismiumChronoflameMenu} for the shared menu logic and why this
 * block moved from a "hold a Clock" interaction to a real button screen.
 *
 * <p>Deliberately has no background texture (unlike every other GUI in
 * this mod, all of which blit a per-block PNG under
 * {@code textures/gui/container/} - see {@code PrismiumCellScreen}'s
 * class doc for that convention): this screen is only ever a title, two
 * vanilla {@link Button} widgets, and a one-line status/cooldown message,
 * so a hand-authored background frame would add art-asset work without
 * adding anything a plain dark panel (drawn with
 * {@link GuiGraphics#fill}, the same primitive every other screen here
 * already uses for its energy bars) doesn't already convey just as
 * clearly. {@link Button} itself supplies its own vanilla widget texture
 * (part of the base game's GUI atlas), so nothing new needed generating
 * there either.
 *
 * <p>Buttons disable themselves (via {@link Button#active}, refreshed
 * every client tick in {@link #containerTick()}) while
 * {@link PrismiumChronoflameMenu#getCooldownRemainingTicks()} is greater
 * than zero, and the status line below them shows the remaining seconds
 * in that case - directly addressing GitHub issue #16's "クールダウンが
 * わかりずらい" complaint with a persistent, always-visible indicator
 * instead of only the one-shot action-bar message a click still also
 * triggers server-side (see {@link PrismiumChronoflameMenu#clickMenuButton}
 * -> {@code PrismiumChronoflameBlock#tryActivate}).
 */
public class PrismiumChronoflameScreen extends AbstractContainerScreen<PrismiumChronoflameMenu> {

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 96;

    private static final int PANEL_BG_COLOR = 0xF0101010;
    private static final int PANEL_BORDER_COLOR = 0xFF3FBDB8;

    private Button advanceButton;
    private Button rewindButton;

    public PrismiumChronoflameScreen(PrismiumChronoflameMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        // No player-inventory slot grid exists in this menu (see
        // PrismiumChronoflameMenu), so renderLabels() below is fully
        // overridden rather than calling super, same as PrismiumCellScreen.
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 80;
        int buttonY = topPos + 40;
        advanceButton = Button.builder(
                        Component.translatable("gui.claudemod.prismium_chronoflame.advance"),
                        button -> clickServerButton(PrismiumChronoflameMenu.BUTTON_ADVANCE))
                .bounds(leftPos + (imageWidth / 2 - buttonWidth - 4), buttonY, buttonWidth, 20)
                .build();
        rewindButton = Button.builder(
                        Component.translatable("gui.claudemod.prismium_chronoflame.rewind"),
                        button -> clickServerButton(PrismiumChronoflameMenu.BUTTON_REWIND))
                .bounds(leftPos + (imageWidth / 2 + 4), buttonY, buttonWidth, 20)
                .build();
        addRenderableWidget(advanceButton);
        addRenderableWidget(rewindButton);
    }

    /** Sends the same vanilla button-click packet Beacon/Loom/Stonecutter
     * use for their own screens - see {@link PrismiumChronoflameMenu}'s
     * class doc for the mappings.dev citation confirming this method. */
    private void clickServerButton(int buttonId) {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean onCooldown = this.menu.getCooldownRemainingTicks() > 0;
        advanceButton.active = !onCooldown;
        rewindButton.active = !onCooldown;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BG_COLOR);
        // 1px border, same accent color as this mod's other energy-bar
        // fills (see PrismiumCellScreen's FILL_BASE), so the panel reads
        // as "this mod's UI" even without a custom background PNG.
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, PANEL_BORDER_COLOR);
        guiGraphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, PANEL_BORDER_COLOR);
        guiGraphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, PANEL_BORDER_COLOR);
        guiGraphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BORDER_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(font, title, imageWidth / 2, 8, 0xFFFFFF);

        Component status = this.menu.getCooldownRemainingTicks() > 0
                ? Component.translatable("gui.claudemod.prismium_chronoflame.cooldown",
                        (this.menu.getCooldownRemainingTicks() + 19) / 20)
                : Component.translatable("gui.claudemod.prismium_chronoflame.ready");
        guiGraphics.drawCenteredString(font, status, imageWidth / 2, 68, 0xA0A0A0);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Standard three-step AbstractContainerScreen#render override
        // (Forge docs "Screens" page, 1.20.x), same as every other screen
        // in this mod.
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
