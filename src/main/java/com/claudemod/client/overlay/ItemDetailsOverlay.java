package com.claudemod.client.overlay;

import com.claudemod.ClaudeMod;
import com.claudemod.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Issue #7's third/final comment (session 60 already handled the first two
 * - see {@link com.claudemod.item.TooltipUsageHelper}'s javadoc for the
 * full quote chain) asks for something the one-line "hold W" tooltip
 * upgrade does not cover: a "専用のアニメーションUI" (dedicated animation
 * UI) that explains *what a block fundamentally is*, reachable via a
 * longer W hold from the tooltip, since the existing per-block
 * {@code .usage} lines (see {@code EnergyStorageBlockItem}) are all
 * phrased around "how to operate it" rather than "what this is" - the
 * user's own words: "それがどのようなブロックであることは依然として
 * わからないまま" (I still don't know what kind of block it actually
 * is). The comment explicitly says a first version can be plain text
 * only and that adding the animation UI can be treated as "one of the
 * things to do" (i.e. an incremental, not all-at-once, feature) - this
 * class is exactly that first increment.
 *
 * <p><b>Design choices, and why (per the standing handoff item that this
 * session picked up - see PROGRESS.md §5 item 3, agreed over sessions
 * #59/#60):</b>
 * <ol>
 *   <li>Reuses the existing {@link ModKeyMappings#SHOW_ITEM_DETAILS}
 *   binding rather than adding a second key - one binding, two
 *   thresholds (a short hold already expands the tooltip via
 *   {@code TooltipUsageHelper}; holding past {@link #HOLD_THRESHOLD_TICKS}
 *   escalates to this overlay).</li>
 *   <li>This is a full-screen <em>overlay drawn on top of the current
 *   Screen</em> (via {@link ScreenEvent.Render.Post}), not a second
 *   {@code Minecraft#setScreen}-swapped {@code Screen}. Actually
 *   navigating to a brand new {@code Screen} while a container screen is
 *   open is far riskier to get right blind (menu-close side effects,
 *   losing the original screen's state, re-opening it afterwards) than
 *   drawing extra content on top of the screen that is already open and
 *   already working - and the user's own comment only asked for an
 *   "animation UI" to be reachable, not specifically for it to be a
 *   distinct {@code Screen} instance. This still reads as a dedicated,
 *   self-contained panel in-game while carrying much less regression
 *   risk to the five existing GUI {@code Screen}s.</li>
 *   <li>"Animation" is intentionally simple: a one-axis slide-down of the
 *   panel from off-screen as the hold continues, computed purely from
 *   how many extra frames the key has been held past the threshold. No
 *   alpha blending on the text itself, since {@code GuiGraphics#drawString}
 *   treats a color with a zero alpha byte as "force fully opaque" (a
 *   documented quirk this mod has not previously had to route around) -
 *   fading the panel's height/position sidesteps that trap entirely
 *   while still reading as a deliberate reveal animation rather than a
 *   hard cut.</li>
 *   <li>Content, per the comment's own "最初は文字列のみで構いません"
 *   allowance: the item's display name plus one description line, sourced
 *   from a new {@code <descriptionId>.details} lang key when one exists
 *   (added this session only for the six {@code EnergyStorageBlockItem}
 *   blocks the comment specifically calls out - see en_us.json/ja_jp.json),
 *   falling back to the existing {@code .usage} line, and finally to a
 *   generic {@code tooltip.claudemod.no_details} line so no item can ever
 *   show a blank panel.</li>
 * </ol>
 *
 * <p><b>API research this session</b> (nothing here was in this mod's
 * existing vocabulary): {@code ScreenEvent.Render.Post} fires on the
 * default (FORGE) mod-bus, client logical side only, and exposes
 * {@code getScreen()}/{@code getGuiGraphics()} - confirmed directly
 * against MinecraftForge's own {@code ScreenEvent.java} source on the
 * {@code 1.20.x} branch (fetched via github.com this session; 1.20.1's
 * rendering rewrite means the constructor takes a {@code GuiGraphics},
 * unlike the {@code PoseStack}-based 1.19.3 javadoc mirror that turns up
 * first in search results - the branch source was checked specifically
 * to avoid reusing the wrong version's shape, learning from last
 * session's Yarn-vs-Forge-mapping mismatch, see {@code PrismiumDrifterEntity}
 * neighbor {@code ModEntityEvents}' session 61 note). {@code
 * AbstractContainerScreen#getSlotUnderMouse()} is a Forge-added public
 * accessor for the vanilla-private {@code hoveredSlot} field - confirmed
 * against MinecraftForge's {@code AbstractContainerScreen.java.patch} on
 * the same branch, which adds exactly {@code public Slot getSlotUnderMouse()
 * { return this.hoveredSlot; }} - used instead of reflection, which would
 * have been the fallback had this accessor not existed. {@code
 * Font#split(FormattedText, int)} and the {@code GuiGraphics#drawString}
 * overload taking a {@code FormattedCharSequence} are long-stable vanilla
 * APIs confirmed unchanged from 1.17 through the 1.21.x NeoForge fork.
 *
 * <p><b>Unverified</b> (no Minecraft client in this sandbox, per
 * PROGRESS.md's standing note): whether the frame-counted hold threshold
 * "feels" right at real framerates, whether the panel's fixed screen-top
 * position ever overlaps something important on any of the five existing
 * GUI screens, and whether {@code getSlotUnderMouse()} returns null
 * cleanly (rather than throwing) before a screen's first {@code init()}
 * repositions its slots - guarded defensively here regardless.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, value = Dist.CLIENT)
public final class ItemDetailsOverlay {

    private ItemDetailsOverlay() {
    }

    /** Frames the key must be held, continuously over the same item,
     * before this overlay appears - deliberately longer than the near-
     * instant threshold {@code TooltipUsageHelper} uses for the plain
     * tooltip expansion, so the two behaviours read as two distinct
     * "steps" of the same long-press rather than firing simultaneously. */
    private static final int HOLD_THRESHOLD_TICKS = 25;

    /** Extra frames over which the panel finishes sliding into place
     * once the threshold above is reached. */
    private static final int SLIDE_IN_TICKS = 8;

    private static final int PANEL_TOP_Y = 20;
    private static final int PANEL_PADDING = 6;
    private static final int DESCRIPTION_WRAP_WIDTH = 220;
    private static final int PANEL_BACKGROUND = 0xE8121218;
    private static final int PANEL_BORDER = 0xFF39E6D6;

    private static int holdTicks = 0;
    private static Item lastHoveredItem = null;

    /** GitHub issue #19 ("詳細表示のバグ" - holding the details key shows
     * nothing at all) follow-up: this session's code review (see
     * PROGRESS.md) could not find a confirmed root cause for that report
     * by reading alone - {@link ModKeyMappings#SHOW_ITEM_DETAILS}'s
     * registration, this class's {@code AbstractContainerScreen} check,
     * and {@code getSlotUnderMouse()}'s Forge-added accessor all read as
     * correct against this class's own citations. Rather than guess at a
     * fix blind, this listener body is now wrapped so that *if* some
     * exception is actually being thrown here every frame (which would
     * silently and completely disable the overlay with zero visible
     * symptom besides "nothing happens" - exactly matching the report),
     * it gets logged once instead of vanishing, giving the next session
     * (or the repo owner, if they can share a log) a concrete stack
     * trace to work from instead of another round of blind code review.
     * Guarded by {@link #loggedFailure} so a real per-frame exception
     * cannot spam the log. */
    private static boolean loggedFailure = false;

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        try {
            renderIfHeld(event);
        } catch (Exception e) {
            if (!loggedFailure) {
                loggedFailure = true;
                ClaudeMod.LOGGER.error(
                        "ItemDetailsOverlay failed while rendering (GitHub issue #19 investigation - "
                                + "this stack trace is the concrete evidence that session lacked)", e);
            }
        }
    }

    private static void renderIfHeld(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            reset();
            return;
        }
        Slot hovered = containerScreen.getSlotUnderMouse();
        if (hovered == null || !hovered.hasItem() || !ModKeyMappings.SHOW_ITEM_DETAILS.isDown()) {
            reset();
            return;
        }

        ItemStack stack = hovered.getItem();
        if (stack.getItem() != lastHoveredItem) {
            lastHoveredItem = stack.getItem();
            holdTicks = 0;
        }
        holdTicks++;
        if (holdTicks < HOLD_THRESHOLD_TICKS) {
            return;
        }

        float slideProgress = Math.min(1f, (holdTicks - HOLD_THRESHOLD_TICKS) / (float) SLIDE_IN_TICKS);
        renderPanel(event.getGuiGraphics(), screen, stack, slideProgress);
    }

    private static void reset() {
        holdTicks = 0;
        lastHoveredItem = null;
    }

    private static void renderPanel(GuiGraphics guiGraphics, Screen screen, ItemStack stack, float slideProgress) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        Component name = stack.getHoverName();
        Component description = resolveDescription(stack);
        List<FormattedCharSequence> descriptionLines = font.split(description, DESCRIPTION_WRAP_WIDTH);

        int contentWidth = font.width(name);
        for (FormattedCharSequence line : descriptionLines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }
        int panelWidth = contentWidth + PANEL_PADDING * 2;
        int panelHeight = PANEL_PADDING * 2 + font.lineHeight + 4 + descriptionLines.size() * font.lineHeight;

        int panelX = (screen.width - panelWidth) / 2;
        int startY = -panelHeight - 4;
        int panelY = startY + Math.round((PANEL_TOP_Y - startY) * slideProgress);

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BACKGROUND);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, PANEL_BORDER);

        int textX = panelX + PANEL_PADDING;
        int textY = panelY + PANEL_PADDING;
        guiGraphics.drawString(font, name, textX, textY, 0xFFFFFF, false);
        textY += font.lineHeight + 4;
        for (FormattedCharSequence line : descriptionLines) {
            guiGraphics.drawString(font, line, textX, textY, 0xC0C0C0, false);
            textY += font.lineHeight;
        }
    }

    /**
     * Prefers a dedicated "what is this" {@code .details} lang entry (new
     * this session, currently only authored for the six energy blocks -
     * see en_us.json/ja_jp.json), falls back to the existing operation-
     * focused {@code .usage} line so every item that already had a
     * tooltip still shows something, and finally to a generic line so no
     * item ever renders an empty panel.
     */
    private static Component resolveDescription(ItemStack stack) {
        String descriptionId = stack.getDescriptionId();
        String detailsKey = descriptionId + ".details";
        String usageKey = descriptionId + ".usage";
        if (I18n.exists(detailsKey)) {
            return Component.translatable(detailsKey);
        }
        if (I18n.exists(usageKey)) {
            return Component.translatable(usageKey);
        }
        return Component.translatable("tooltip.claudemod.no_details");
    }
}
