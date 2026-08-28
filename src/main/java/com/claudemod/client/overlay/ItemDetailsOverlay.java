package com.claudemod.client.overlay;

import com.claudemod.ClaudeMod;
import com.claudemod.client.GuiKeyStateTracker;
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
 *   binding rather than adding a second key. Originally this held key
 *   also expanded the tooltip itself via {@code TooltipUsageHelper}
 *   ("one binding, two thresholds"), but the repo owner asked for that
 *   redundant second surface to be dropped (direct chat, after v0.25.3)
 *   since seeing both the tooltip expand *and* this panel appear read as
 *   two overlapping "detail" displays for the same press. {@code
 *   TooltipUsageHelper} now only ever shows the static short prompt;
 *   this overlay, appearing once the key is held past {@link
 *   #HOLD_THRESHOLD_TICKS}, is the sole place the full description is
 *   shown.</li>
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
     * before this overlay appears. Originally kept deliberately long so
     * this panel read as a distinct "second step" after {@code
     * TooltipUsageHelper}'s (now-removed) instant tooltip expansion; left
     * unchanged since this overlay is now the sole reveal mechanism and a
     * short, non-instant hold still avoids the panel flashing in on a
     * single accidental tap. */
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
     * nothing at all): root cause found and fixed this session - see
     * {@link GuiKeyStateTracker}'s class javadoc for the full writeup.
     * In short, this class was reading {@code
     * ModKeyMappings#SHOW_ITEM_DETAILS.isDown()}, which Forge's own docs
     * describe as the "within the game" mechanism, not the "inside a
     * GUI" one this feature actually needs (this overlay only ever
     * renders while an {@code AbstractContainerScreen} has input focus).
     * {@link GuiKeyStateTracker#isShowItemDetailsHeld()} now backs this
     * check instead. The exception-guard below predates that fix (added
     * by an earlier session that could not find the cause by reading
     * alone) and is kept regardless, on the general principle that a
     * per-frame render listener silently swallowing exceptions is worth
     * guarding against either way; it is no longer expected to be the
     * source of the originally reported symptom. Guarded by {@link
     * #loggedFailure} so a real per-frame exception cannot spam the
     * log. */
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
        // GitHub issue #19 fix: was ModKeyMappings.SHOW_ITEM_DETAILS.isDown(),
        // which per Forge's own "Key Mappings" docs ("Within the Game" vs.
        // "Inside a GUI" sections) is not the mechanism meant for detecting
        // a key's state while a Screen has input focus - see
        // GuiKeyStateTracker's class javadoc for the full investigation.
        if (hovered == null || !hovered.hasItem() || !GuiKeyStateTracker.isShowItemDetailsHeld()) {
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

        // Repo owner-reported bug (direct chat, after v0.25.3): item icons
        // and the vanilla tooltip box from the underlying container screen
        // were rendering *on top of* this panel, even though this method
        // only ever runs from ScreenEvent.Render.Post - i.e. strictly after
        // the screen (including its tooltip) has already drawn. GuiGraphics
        // batches draw calls per-RenderType rather than physically
        // submitting them to the GPU in Java call order, so something
        // queued earlier (e.g. renderItem's 3D-ish item render type, or the
        // tooltip's own render type) can still end up flushed after - and
        // therefore visually on top of - a plain fill()/drawString() call
        // issued later in code but never explicitly flushed. See the
        // "flush()"/"drawManaged()" guidance in GuiGraphics's own 1.20.x
        // migration notes (docs.neoforged.net) and the Forge forums thread
        // on exactly this "custom overlay ends up behind items" symptom -
        // both point at the same two-part fix used here:
        //   1. flush() *before* drawing, so every already-queued call from
        //      the screen's own render (slots, items, tooltip) is actually
        //      submitted to the GPU first, instead of possibly still
        //      sitting in a buffer that gets flushed after ours.
        //   2. Push the pose stack to GuiGraphics#MAX_GUI_Z (the same
        //      "always in front" Z plane vanilla's own tooltip rendering
        //      uses) before drawing, then pop it back afterward, so even
        //      if GPU depth testing is still enabled from the screen's own
        //      3D-ish item rendering, this panel cannot lose a depth test
        //      against anything drawn at a lower Z.
        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0, 0.0, GuiGraphics.MAX_GUI_Z);

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

        // Flush our own draws immediately too (rather than leaving them to
        // whatever flushes next), then restore the pose stack so this
        // method never leaks a Z offset into anything rendered afterward.
        guiGraphics.flush();
        guiGraphics.pose().popPose();
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
