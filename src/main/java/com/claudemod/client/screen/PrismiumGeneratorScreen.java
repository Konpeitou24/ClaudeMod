package com.claudemod.client.screen;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for Prismium Generator's GUI (session 24) - see
 * {@link PrismiumGeneratorMenu} for the shared menu logic and why
 * Generator was picked as the mod's second GUI target.
 *
 * Structurally the same approach as
 * {@link PrismiumCellScreen} (proportional fills drawn in code rather
 * than baked into the texture as gauge sprites) with a second,
 * vertically-oriented gauge above the horizontal energy bar: a "flame"
 * column that fills bottom-to-top, echoing the warm ember palette
 * already established by the block's own lit-state texture
 * (scripts/textures/gen_prismium_generator.py's EMBER_LIT_* colors) so
 * the GUI reads as belonging to the same block rather than introducing a
 * third unrelated color language.
 *
 * <p>Session (TODO6 followup): grown to 176x214 (was 176x110) to add the
 * standard player-inventory + hotbar grid below the gauges, following
 * {@link PrismiumPulverizerScreen}'s established pattern for this mod's
 * first player-inventory-bearing screen (session 67) - see
 * {@link PrismiumGeneratorMenu}'s doc for why a lone fuel slot with no
 * visible/clickable player inventory made bulk fuel-loading impractical.
 */
public class PrismiumGeneratorScreen extends AbstractContainerScreen<PrismiumGeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/gui/container/prismium_generator.png");

    // Flame gauge geometry: a narrow vertical column that fills from the
    // bottom up, matching PrismiumGeneratorBlockEntity's cumulative
    // "shards queued" fraction (see PrismiumGeneratorMenu#getBurnFraction
    // for why this is not literally the vanilla furnace's per-item
    // countdown).
    private static final int FLAME_X = 12;
    private static final int FLAME_Y = 20;
    private static final int FLAME_WIDTH = 10;
    private static final int FLAME_HEIGHT = 32;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 62;
    private static final int BAR_WIDTH = 160;
    private static final int BAR_HEIGHT = 14;

    // Ember palette, reused verbatim from the block texture script (see
    // class doc) rather than inventing a new set of GUI-only colors.
    private static final int FLAME_WARM = 0xFFC6501F;
    private static final int FLAME_HOT = 0xFFFF9A3C;
    private static final int FLAME_CORE = 0xFFFFE9B0;

    // Same teal energy-bar palette as PrismiumCellScreen, so both
    // machines' GUIs read as members of the same FE-system family.
    private static final int FILL_BASE = 0xFF3FBDB8;
    private static final int FILL_HILITE = 0xFF66D9D2;

    // GitHub issue #8 status label colors (session, see
    // PrismiumGeneratorMenu#isGenerating's doc): green while actively
    // adding FE this tick, amber while paused because the buffer is
    // already full (queued fuel remains but has nowhere to go yet - the
    // exact situation GitHub issue #15 also flagged as confusing), gray
    // once there is no queued burn time left at all.
    private static final int STATUS_ACTIVE = 0xFF4CD97B;
    private static final int STATUS_FULL = 0xFFE0A83C;
    private static final int STATUS_NO_FUEL = 0xFF8A8A8A;

    public PrismiumGeneratorScreen(PrismiumGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        // Session (TODO6 followup): grown from 110 to 186 to make room for
        // the player inventory + hotbar grid PrismiumGeneratorMenu now
        // registers below the machine's own gauges (was a pure
        // status/gauge panel with no player-inventory slots at all before
        // this session - see that class's doc). Top gauge/status layout
        // (flame gauge, energy bar, labels) is otherwise unchanged from
        // session 24/58.
        this.imageHeight = 214;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Flame gauge: bottom-up fill, two-tone (warm base + hot core
        // stripe near the top of the filled region) similar in spirit to
        // the energy bar's base+hilite treatment below, but oriented
        // vertically and colored from the ember palette instead of teal.
        float burnFraction = menu.getBurnFraction();
        int filledHeight = Math.round(FLAME_HEIGHT * burnFraction);
        if (filledHeight > 0) {
            int flameX = leftPos + FLAME_X;
            int flameBottom = topPos + FLAME_Y + FLAME_HEIGHT;
            int flameTop = flameBottom - filledHeight;
            guiGraphics.fill(flameX, flameTop, flameX + FLAME_WIDTH, flameBottom, FLAME_WARM);
            int coreHeight = Math.min(4, filledHeight);
            guiGraphics.fill(flameX, flameTop, flameX + FLAME_WIDTH, flameTop + coreHeight, FLAME_HOT);
            if (burnFraction >= 0.999f) {
                // Full queue: brighten the very top pixel row further so a
                // "topped up" generator visibly differs from one that's
                // merely close to a full shard's worth.
                guiGraphics.fill(flameX, flameTop, flameX + FLAME_WIDTH, flameTop + 1, FLAME_CORE);
            }
        }

        int energy = menu.getEnergy();
        int max = Math.max(1, menu.getMaxEnergy());
        int filledWidth = (int) ((long) BAR_WIDTH * energy / max);
        if (filledWidth > 0) {
            int barX = leftPos + BAR_X;
            int barY = topPos + BAR_Y;
            guiGraphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, FILL_BASE);
            guiGraphics.fill(barX, barY, barX + filledWidth, barY + 3, FILL_HILITE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0x404040, false);

        // GitHub issue #8 ("UIを開いても発電できている様子はない"): an
        // explicit status word, matching the pattern already established
        // by Pylon/Wardstone's status lamp+label (see PrismiumPylonScreen)
        // instead of leaving the player to infer activity from the flame
        // gauge and energy bar alone. Three states rather than those two
        // machines' plain active/idle, because a Generator can be
        // "burning but temporarily paused" (buffer full) in a way a
        // pure FE-consumer never is - see PrismiumGeneratorMenu#isGenerating.
        String statusKey;
        int statusColor;
        if (menu.isGenerating()) {
            statusKey = "gui.claudemod.generator_status_active";
            statusColor = STATUS_ACTIVE;
        } else if (menu.getBurnTime() > 0) {
            statusKey = "gui.claudemod.generator_status_full";
            statusColor = STATUS_FULL;
        } else {
            statusKey = "gui.claudemod.generator_status_no_fuel";
            statusColor = STATUS_NO_FUEL;
        }
        Component statusText = Component.translatable(statusKey);
        guiGraphics.drawString(font, statusText, FLAME_X + FLAME_WIDTH + 8, FLAME_Y, statusColor, false);

        Component burnText = Component.translatable("gui.claudemod.burn_seconds", menu.getBurnSeconds());
        guiGraphics.drawString(font, burnText, FLAME_X + FLAME_WIDTH + 8, FLAME_Y + FLAME_HEIGHT / 2 - 4, 0x404040, false);

        // GitHub issue #15 follow-up comment ("生産側が0になる"): shows this
        // tick's actual generated/pushed FE side by side so a buffer that
        // reads near-0 next to a hungry consumer reads as "keeping up in
        // real time" rather than "broken" - see
        // PrismiumGeneratorBlockEntity's class-doc addendum for the full
        // investigation this responds to. Muted gray like burnText/energy
        // labels rather than the brighter status colors above, since this
        // is supplementary detail, not the primary status signal.
        Component rateText = Component.translatable("gui.claudemod.generator_rate",
                menu.getLastGenerated(), menu.getLastPushed());
        guiGraphics.drawString(font, rateText, FLAME_X + FLAME_WIDTH + 8, FLAME_Y + FLAME_HEIGHT / 2 - 4 + 10, 0x707070, false);

        // Session (TODO6 followup): moved from a bottom-anchored
        // (imageHeight - 12) position to a fixed row just below the
        // separator line, since imageHeight now includes ~76px of player
        // inventory grid below this text that the old bottom-relative
        // formula would have collided with.
        Component energyText = Component.translatable("gui.claudemod.fe_amount",
                menu.getEnergy(), menu.getMaxEnergy());
        int textWidth = font.width(energyText);
        guiGraphics.drawString(font, energyText, (imageWidth - textWidth) / 2, 88, 0xFFFFFF, false);
        // Deliberately no "Inventory" label for the player grid below -
        // see PrismiumPulverizerScreen's identical comment; this mod's
        // other screens never show one either, and the grid's position is
        // self-explanatory.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
