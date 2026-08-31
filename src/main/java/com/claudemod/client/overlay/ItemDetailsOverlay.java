package com.claudemod.client.overlay;

import com.claudemod.ClaudeMod;
import com.claudemod.client.GuiKeyStateTracker;
import com.claudemod.client.ModKeyMappings;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
 *   shown. A follow-up request in the same conversation added a small
 *   progress bar (drawn just under the vanilla tooltip while the key is
 *   held but before the threshold - see {@link #onRenderTooltipPre}/
 *   {@link #renderProgressBar}) and A/D page-turning once the panel is
 *   visible (see {@link ItemDetailsPaging}), with page 2 showing a
 *   comparison against whatever the player currently has equipped in the
 *   relevant slot (see {@link #buildComparisonLines}).</li>
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
 * {@code RenderTooltipEvent.Pre} (used by {@link #onRenderTooltipPre} for
 * the progress bar's position) was likewise confirmed against Forge's
 * {@code RenderTooltipEvent.java} source on the same branch: it exposes
 * {@code getX()}/{@code getY()} (the tooltip box's own top-left) and
 * {@code getComponents()} (the {@code ClientTooltipComponent} list used
 * here to estimate the box's height, since no version of this event
 * exposes a height directly).
 *
 * <p><b>Unverified</b> (no Minecraft client in this sandbox, per
 * PROGRESS.md's standing note): whether the frame-counted hold threshold
 * "feels" right at real framerates, whether the panel's fixed screen-top
 * position ever overlaps something important on any of the five existing
 * GUI screens, whether {@code getSlotUnderMouse()} returns null cleanly
 * (rather than throwing) before a screen's first {@code init()}
 * repositions its slots - guarded defensively here regardless - and,
 * newly this session, whether the progress bar lands visually just under
 * the real tooltip (the height estimate in {@link #onRenderTooltipPre} is
 * an approximation, not a byte-for-byte match of vanilla's own padding
 * math) and whether the attribute-modifier comparison on page 2 reads as
 * intended for this mod's actual gear (only a fixed, common subset of
 * attributes is compared - see {@link #COMPARE_ATTRIBUTES}).
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

    private static final int PROGRESS_BAR_WIDTH = 60;
    private static final int PROGRESS_BAR_HEIGHT = 3;
    private static final int PROGRESS_BAR_GAP = 4;
    private static final int PROGRESS_BAR_BACKGROUND = 0xFF2A2A2E;

    /** Attributes compared on page 2 (see {@link #buildComparisonLines}).
     * A fixed, deliberately small subset covering this mod's armor/tool/
     * weapon gear rather than every vanilla {@link Attribute}, so the
     * comparison page cannot balloon into a wall of zero-difference
     * lines for attributes no ClaudeMod item ever touches. */
    private static final Attribute[] COMPARE_ATTRIBUTES = {
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.MOVEMENT_SPEED,
    };

    private static int holdTicks = 0;
    private static Item lastHoveredItem = null;

    /** Position/height of the vanilla tooltip most recently rendered this
     * frame, captured by {@link #onRenderTooltipPre} so the progress bar
     * in {@link #renderProgressBar} can be drawn just underneath it. -1
     * means "no tooltip observed yet" (nothing should be drawn). */
    private static int lastTooltipX = -1;
    private static int lastTooltipBottomY = -1;

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

    /**
     * Captures the vanilla tooltip's box position/estimated height for
     * {@link #renderProgressBar} to use. Fires for every tooltip in the
     * game (this mod doesn't own the container screens it needs to read
     * this from, so - same reasoning as {@code GuiKeyStateTracker}'s key
     * events - a Forge event is the only way to see it), which is cheap
     * enough to not bother filtering by "is the details key even held"
     * here; {@link #renderProgressBar} is what actually gates on that.
     */
    @SubscribeEvent
    public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
        List<ClientTooltipComponent> components = event.getComponents();
        // Vanilla's own tooltip box adds a small fixed padding above/
        // below the stacked component heights (see
        // GuiGraphics#renderTooltipInternal) plus 2px of breathing room
        // after the first line when there is more than one - this mirrors
        // that shape closely enough for "roughly under the tooltip"
        // without claiming byte-for-byte precision (no version of this
        // event exposes a ready-made height).
        int height = 6;
        for (int i = 0; i < components.size(); i++) {
            height += components.get(i).getHeight();
            if (i == 0 && components.size() > 1) {
                height += 2;
            }
        }
        lastTooltipX = event.getX();
        lastTooltipBottomY = event.getY() + height;
    }

    private static void renderIfHeld(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        ItemStack stack;
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot hovered = containerScreen.getSlotUnderMouse();
            if (hovered == null || !hovered.hasItem()) {
                reset();
                return;
            }
            stack = hovered.getItem();
        } else {
            // 2026-08-31 direct-chat feedback (PROGRESS.md TODO4): "JEIの
            // レシピ画面上でWキー長押しの詳細表示が効かない". Root cause:
            // JEI's recipe-view Screen (and any other non-inventory Screen
            // a compat mod might open) is a plain Screen, not an
            // AbstractContainerScreen - getSlotUnderMouse() only exists on
            // the latter, so this whole overlay previously bailed out
            // unconditionally the instant such a screen had focus,
            // regardless of the key state. Properly identifying "which
            // ingredient is under the mouse" inside a foreign mod's own
            // Screen would need a compileOnly dependency on JEI's plugin
            // API, which this mod does not have yet (see PROGRESS.md "4.
            // その他"). As a first, dependency-free step this falls back
            // to the player's own held (main hand) item instead of
            // whatever is under the cursor, so the key at least does
            // *something* useful on these screens rather than silently
            // nothing - not a full fix (still doesn't inspect the
            // specific recipe ingredient the cursor is over), left as a
            // follow-up (PROGRESS.md "3. 問題点").
            Minecraft minecraftInstance = Minecraft.getInstance();
            if (minecraftInstance.player == null) {
                reset();
                return;
            }
            stack = minecraftInstance.player.getMainHandItem();
            if (stack.isEmpty()) {
                reset();
                return;
            }
        }
        // GitHub issue #19 fix: was ModKeyMappings.SHOW_ITEM_DETAILS.isDown(),
        // which per Forge's own "Key Mappings" docs ("Within the Game" vs.
        // "Inside a GUI" sections) is not the mechanism meant for detecting
        // a key's state while a Screen has input focus - see
        // GuiKeyStateTracker's class javadoc for the full investigation.
        if (!GuiKeyStateTracker.isShowItemDetailsHeld()) {
            reset();
            return;
        }

        if (stack.getItem() != lastHoveredItem) {
            lastHoveredItem = stack.getItem();
            holdTicks = 0;
            ItemDetailsPaging.resetPage();
        }
        holdTicks++;
        if (holdTicks < HOLD_THRESHOLD_TICKS) {
            renderProgressBar(event.getGuiGraphics(), holdTicks);
            return;
        }

        float slideProgress = Math.min(1f, (holdTicks - HOLD_THRESHOLD_TICKS) / (float) SLIDE_IN_TICKS);
        renderPanel(event.getGuiGraphics(), screen, stack, slideProgress);
    }

    /** Whether the full panel (as opposed to just the progress bar, or
     * nothing) is currently being shown - {@link ItemDetailsPaging} only
     * turns pages while this is true, so A/D can't advance a page that
     * isn't visible yet. */
    public static boolean isPanelVisible() {
        return holdTicks >= HOLD_THRESHOLD_TICKS;
    }

    private static void reset() {
        holdTicks = 0;
        lastHoveredItem = null;
        lastTooltipX = -1;
        lastTooltipBottomY = -1;
        ItemDetailsPaging.resetPage();
    }

    private static void renderProgressBar(GuiGraphics guiGraphics, int ticks) {
        if (lastTooltipX < 0 || lastTooltipBottomY < 0) {
            // No tooltip observed this hover yet (e.g. the very first
            // frame of a new hover, before onRenderTooltipPre has fired) -
            // nothing sensible to draw under.
            return;
        }
        float progress = Math.min(1f, ticks / (float) HOLD_THRESHOLD_TICKS);
        int barX = lastTooltipX;
        int barY = lastTooltipBottomY + PROGRESS_BAR_GAP;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        guiGraphics.fill(barX, barY, barX + PROGRESS_BAR_WIDTH, barY + PROGRESS_BAR_HEIGHT, PROGRESS_BAR_BACKGROUND);
        int filledWidth = Math.round(PROGRESS_BAR_WIDTH * progress);
        if (filledWidth > 0) {
            guiGraphics.fill(barX, barY, barX + filledWidth, barY + PROGRESS_BAR_HEIGHT, PANEL_BORDER);
        }
        guiGraphics.pose().popPose();
    }

    private static void renderPanel(GuiGraphics guiGraphics, Screen screen, ItemStack stack, float slideProgress) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int page = ItemDetailsPaging.currentPage();
        Component name = stack.getHoverName();
        List<FormattedCharSequence> bodyLines = new ArrayList<>();
        if (page == 0) {
            bodyLines.addAll(font.split(resolveDescription(stack), DESCRIPTION_WRAP_WIDTH));
        } else {
            for (Component line : buildComparisonLines(stack)) {
                bodyLines.addAll(font.split(line, DESCRIPTION_WRAP_WIDTH));
            }
        }

        int contentWidth = font.width(name);
        for (FormattedCharSequence line : bodyLines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }
        int panelWidth = contentWidth + PANEL_PADDING * 2;
        int panelHeight = PANEL_PADDING * 2 + font.lineHeight + 4 + bodyLines.size() * font.lineHeight;

        int panelX = (screen.width - panelWidth) / 2;
        int startY = -panelHeight - 4;
        int panelY = startY + Math.round((PANEL_TOP_Y - startY) * slideProgress);

        // Repo owner-reported bug (direct chat, after v0.25.3): item icons
        // and the vanilla tooltip box from the underlying container screen
        // were rendering *on top of* this panel. First attempt at a fix
        // (flush() + translate to GuiGraphics.MAX_GUI_Z, i.e. 10000) made
        // things worse - the panel stopped rendering at all. Checked
        // vanilla's own GuiGraphics#renderTooltipInternal source directly
        // (Forge's GuiGraphics.java.patch, 1.20.x branch, github.com) for
        // how the game itself guarantees tooltips draw on top of item
        // icons: it does exactly one thing for this - pushPose(), then
        // translate(0, 0, 400.0F) - no manual flush() call around the
        // translated draws at all. 400 is a comfortably small Z bump
        // relative to normal GUI content (Z=0), whereas 10000
        // (MAX_GUI_Z) most likely exceeded this screen's orthographic
        // projection's far clip plane and got the whole panel clipped by
        // the GPU instead of drawn in front - which matches "disappeared
        // entirely" far better than "still behind items" would. This now
        // mirrors vanilla's own proven value/pattern exactly instead of
        // reaching for the most extreme constant available.
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BACKGROUND);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, PANEL_BORDER);

        int textX = panelX + PANEL_PADDING;
        int textY = panelY + PANEL_PADDING;
        guiGraphics.drawString(font, name, textX, textY, 0xFFFFFF, false);
        textY += font.lineHeight + 4;
        for (FormattedCharSequence line : bodyLines) {
            guiGraphics.drawString(font, line, textX, textY, 0xC0C0C0, false);
            textY += font.lineHeight;
        }

        if (ItemDetailsPaging.PAGE_COUNT > 1) {
            String pageIndicator = (page + 1) + "/" + ItemDetailsPaging.PAGE_COUNT;
            int indicatorX = panelX + panelWidth - PANEL_PADDING - font.width(pageIndicator);
            guiGraphics.drawString(font, pageIndicator, indicatorX, panelY + PANEL_PADDING, 0x808080, false);
        }

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

    /**
     * Page 2 content (repo owner's own suggestion, direct chat): compares
     * the hovered item's default attribute modifiers against whatever the
     * player currently has equipped in the same slot. Armor uses {@link
     * ArmorItem#getEquipmentSlot()}; anything else is treated as a
     * mainhand item if - and only if - it actually carries mainhand
     * attribute modifiers (covers this mod's weapons/tools generically,
     * without hardcoding a list of item classes), otherwise there is
     * nothing meaningful to compare and a fallback line is shown instead.
     */
    private static List<Component> buildComparisonLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        EquipmentSlot slot = resolveComparisonSlot(stack);
        if (slot == null) {
            lines.add(Component.translatable("tooltip.claudemod.compare_not_equippable")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            lines.add(Component.translatable("tooltip.claudemod.compare_not_equippable")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        ItemStack equipped = player.getItemBySlot(slot);
        if (equipped.isEmpty()) {
            lines.add(Component.translatable("tooltip.claudemod.compare_none_equipped")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if (ItemStack.isSameItem(equipped, stack)) {
            lines.add(Component.translatable("tooltip.claudemod.compare_same_item")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }

        Multimap<Attribute, AttributeModifier> hoveredMods = stack.getAttributeModifiers(slot);
        Multimap<Attribute, AttributeModifier> equippedMods = equipped.getAttributeModifiers(slot);

        Set<Attribute> attributesToShow = new LinkedHashSet<>();
        for (Attribute attribute : COMPARE_ATTRIBUTES) {
            if (hoveredMods.containsKey(attribute) || equippedMods.containsKey(attribute)) {
                attributesToShow.add(attribute);
            }
        }
        if (attributesToShow.isEmpty()) {
            lines.add(Component.translatable("tooltip.claudemod.compare_not_equippable")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        for (Attribute attribute : attributesToShow) {
            // Repo owner-reported bug (direct chat, screenshot): this page
            // originally compared raw modifier sums only (e.g. a hoe's own
            // -1.0 attack speed modifier), which didn't match the number
            // shown in the vanilla tooltip's own "when held in main hand"
            // section just below (e.g. 3.0). Checked vanilla's actual
            // tooltip-building code (ItemStack#getTooltipLines,
            // decompiled 1.20.1 source) to find why: it special-cases
            // exactly two modifier IDs - Item.BASE_ATTACK_DAMAGE_UUID and
            // Item.BASE_ATTACK_SPEED_UUID - adding the player's current
            // LivingEntity#getAttributeBaseValue(Attribute) (1.0 attack
            // damage / 4.0 attack speed by default) before display; every
            // other attribute (armor, armor toughness, knockback
            // resistance, movement speed) is shown as a plain modifier
            // delta with no base value added, same as this page already
            // did. So only ATTACK_DAMAGE/ATTACK_SPEED need the base value
            // added here to match; the others are left as pure deltas,
            // which was already correct. getAttributeBaseValue(Attribute)
            // confirmed present on LivingEntity in the 1.20.1 line via
            // WebSearch this session (Forge javadoc mirrors, adjacent
            // 1.19.3/1.18.2 versions with the same signature; the
            // Attribute-typed overload, not the later Holder<Attribute>
            // one introduced after 1.20.1).
            boolean showsAsTotal = attribute == Attributes.ATTACK_DAMAGE || attribute == Attributes.ATTACK_SPEED;
            double base = showsAsTotal ? player.getAttributeBaseValue(attribute) : 0.0;
            double before = base + sumAmount(equippedMods, attribute);
            double after = base + sumAmount(hoveredMods, attribute);
            double diff = after - before;
            ChatFormatting diffColor = diff > 0 ? ChatFormatting.GREEN
                    : diff < 0 ? ChatFormatting.RED : ChatFormatting.GRAY;
            String sign = diff > 0 ? "+" : "";
            lines.add(Component.translatable(attribute.getDescriptionId())
                    .append(Component.literal(": " + formatAmount(before) + " -> " + formatAmount(after) + " ("))
                    .append(Component.literal(sign + formatAmount(diff)).withStyle(diffColor))
                    .append(Component.literal(")"))
                    .withStyle(ChatFormatting.WHITE));
        }
        return lines;
    }

    private static EquipmentSlot resolveComparisonSlot(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot();
        }
        if (!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty()) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    private static double sumAmount(Multimap<Attribute, AttributeModifier> modifiers, Attribute attribute) {
        double total = 0.0;
        for (AttributeModifier modifier : modifiers.get(attribute)) {
            total += modifier.getAmount();
        }
        return total;
    }

    private static String formatAmount(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
