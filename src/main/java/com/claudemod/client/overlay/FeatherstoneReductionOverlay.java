package com.claudemod.client.overlay;

import com.claudemod.ClaudeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Issue #17's outstanding half of "羽石の効果がわかりずらい" (Featherstone's
 * effect is hard to understand). {@link
 * com.claudemod.event.PrismiumFeatherstoneHandler} already fixed the other
 * half - the reporter's "ジャンプダッシュする度に...表示され続け正直邪魔
 * です" (it keeps showing on every jump-dash, which is annoying) complaint -
 * by only firing at all when a fall would actually deal damage. What is
 * left is the reporter's separate, still-unaddressed request in that same
 * comment: "HP表示に何か工夫を入れた上" (add some improvement to the HP
 * display). The previous implementation's feedback was a plain vanilla
 * action-bar line via {@code Player#displayClientMessage(component, true)} -
 * functional, but not actually "HP表示" (the health display) in any visual
 * sense, just generic status text in the same slot vanilla uses for e.g.
 * "Can't sleep now" or held-item names. This class replaces that with a
 * small dedicated HUD panel anchored directly to the health/armor/mount-
 * health stack itself (see {@link #render} for how {@link
 * ForgeGui#leftHeight} is used for this), which is what "improve the HP
 * display" was actually asking for.
 *
 * <p><b>Design choice - a custom overlay, not a bigger action-bar
 * message</b>: reusing the action bar again (just with fancier text) would
 * still be the same generic slot the reporter's first-ever comment on this
 * issue already flagged as unwelcome for notifications ("チャット欄に通知
 * など、邪魔でわかりずらいものはダメです" - notifications in the chat
 * area, if they get in the way or are hard to follow, are not acceptable).
 * A panel drawn adjacent to the hearts themselves reads as part of the HUD's
 * existing health display rather than as a new pop-up notification.
 *
 * <p><b>Design choice - position via {@code leftHeight}, not a hardcoded Y
 * coordinate</b>: {@link ForgeGui#leftHeight} is the running total (in
 * pixels) of everything already stacked above the hotbar on the left side -
 * hearts, armor, and (per Forge's own {@code VanillaGuiOverlay} ordering)
 * mount health when riding. Anchoring this panel's Y position to {@code
 * screenHeight - leftHeight} means it always sits just above whatever the
 * player's current HUD state actually is (e.g. still correctly placed above
 * the armor row for an armored player, or above the mount-health bar while
 * riding), instead of assuming a fixed vertical offset that would overlap
 * those rows whenever they are present.
 *
 * <p><b>Design choice - a scale-based pop/shrink animation, not an alpha
 * fade</b>: {@link com.claudemod.client.overlay.ItemDetailsOverlay}'s own
 * javadoc already documents a discovered quirk in this Minecraft version -
 * {@code GuiGraphics#drawString} treats a fully-zero alpha byte as "force
 * fully opaque" rather than invisible - so a naive alpha fade-out would
 * visibly snap back to opaque right at the end instead of smoothly
 * vanishing. This reuses that same lesson by animating the panel's {@code
 * PoseStack} scale (a brief pop-in overshoot, then a shrink-away at the
 * end) instead of its color's alpha channel, exactly as {@code
 * ItemDetailsOverlay} chose position/slide animation over alpha for the
 * same documented reason.
 *
 * <p><b>No new networking risk beyond the packet itself</b>: this class
 * never touches the network directly - {@link
 * com.claudemod.network.FeatherstoneReductionMessage#handle} is the only
 * caller of {@link #trigger}, already wrapped in {@code
 * DistExecutor#unsafeRunWhenOn(Dist.CLIENT, ...)} on the packet-handling
 * side, so this class's own {@code @Mod.EventBusSubscriber(value =
 * Dist.CLIENT)} annotation is what keeps {@link RegisterGuiOverlaysEvent}
 * (a client-only event class) from ever being referenced while loading on a
 * dedicated server - the same pattern already used by {@code
 * GuiKeyStateTracker} and {@code ItemDetailsOverlay} elsewhere in this
 * package/sibling package.
 *
 * <p><b>Unverified</b> (no Minecraft client in this sandbox, per
 * PROGRESS.md's standing note): whether the panel's position next to the
 * hearts reads clearly at a glance versus the previous action-bar text now
 * removed, whether the pop/shrink animation timing (see the tick constants
 * below) feels smooth at real framerates, and whether {@code leftHeight} at
 * the moment this overlay renders has already been updated by vanilla's own
 * health/armor rendering earlier in the same frame (it is registered {@code
 * registerAbove(VanillaGuiOverlay.MOUNT_HEALTH, ...)} - see {@link
 * #onRegisterOverlays} - specifically so every vanilla left-side overlay
 * (health, armor, mount health) has already run and left a final {@code
 * leftHeight} value for this class to read) - only an in-game test or the
 * repo owner's confirmation can close this out.
 */
// RegisterGuiOverlaysEvent implements IModBusEvent (confirmed against
// Forge's 1.20.1 javadoc mirror, lexxie.dev - "This event is fired on the
// mod-specific event bus"), unlike every other event this mod's client
// package has subscribed to so far (ScreenEvent, ClientTickEvent, etc.,
// all plain Forge-bus events). Without an explicit `bus =
// Mod.EventBusSubscriber.Bus.MOD` here, this class's default subscription
// to the ordinary Forge event bus would simply never see this event fire
// at all - a silent no-op, not a crash, so this is called out explicitly
// to avoid ever "fixing" a future report of the overlay never appearing by
// re-deriving this same fact from scratch.
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FeatherstoneReductionOverlay {

    private FeatherstoneReductionOverlay() {
    }

    private static final long NANOS_PER_TICK = 50_000_000L;

    /** Frames over which the panel pops in from a slight overshoot down to
     * its resting scale. */
    private static final double POP_IN_TICKS = 4.0D;
    /** Frames the panel stays at its resting scale after popping in. */
    private static final double HOLD_TICKS = 32.0D;
    /** Frames over which the panel shrinks away at the end. */
    private static final double SHRINK_OUT_TICKS = 10.0D;
    private static final double TOTAL_TICKS = POP_IN_TICKS + HOLD_TICKS + SHRINK_OUT_TICKS;

    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 3;
    /** Gap between the top of the vanilla health/armor stack and the
     * bottom of this panel. */
    private static final int GAP_ABOVE_HEALTH = 3;

    private static final int PANEL_BACKGROUND = 0xB2481018;
    private static final int PANEL_BORDER = 0xFFEF5A5A;
    private static final int TEXT_COLOR = 0xFFFFE8E8;

    private static boolean active = false;
    private static long triggeredAtNanos = 0L;
    private static int lastReductionPercent = 0;

    /**
     * Called from {@link com.claudemod.network.FeatherstoneReductionMessage#handle}
     * once the packet's {@code enqueueWork}/{@code DistExecutor} dance has
     * confirmed this is running on the client main thread. Safe to call
     * repeatedly in quick succession (e.g. two falls in close order) since
     * it simply resets the start time - the panel just restarts its
     * animation with the newest percentage rather than stacking multiple
     * panels.
     */
    public static void trigger(int reductionPercent) {
        active = true;
        triggeredAtNanos = System.nanoTime();
        lastReductionPercent = reductionPercent;
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Registered above MOUNT_HEALTH rather than PLAYER_HEALTH: Forge's
        // own VanillaGuiOverlay enum (see this class's javadoc) renders
        // PLAYER_HEALTH, then ARMOR_LEVEL, then FOOD_LEVEL/AIR_LEVEL, then
        // MOUNT_HEALTH, in that fixed order, and every one of the
        // left-side rows (health, armor, mount health) mutates the shared
        // ForgeGui#leftHeight this class reads in render() below.
        // Anchoring right above PLAYER_HEALTH would run this overlay
        // before armor ever gets a chance to stack itself on top of
        // health, so an armored player would see this panel overlap the
        // armor row instead of sitting above it. MOUNT_HEALTH is the last
        // vanilla overlay that can still touch leftHeight this frame, so
        // registering above it guarantees leftHeight already reflects the
        // full final left-side stack (health, armor, and mount health if
        // present) by the time this class reads it, regardless of which
        // of those rows actually drew anything this frame.
        event.registerAbove(VanillaGuiOverlay.MOUNT_HEALTH.id(), "featherstone_reduction", OVERLAY);
    }

    private static final IGuiOverlay OVERLAY = FeatherstoneReductionOverlay::render;

    private static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        double elapsedTicks = (System.nanoTime() - triggeredAtNanos) / (double) NANOS_PER_TICK;
        if (elapsedTicks >= TOTAL_TICKS) {
            active = false;
            return;
        }

        double scale;
        double extraLift;
        if (elapsedTicks < POP_IN_TICKS) {
            double progress = elapsedTicks / POP_IN_TICKS;
            scale = 1.35D - 0.35D * progress;
            extraLift = 0.0D;
        } else if (elapsedTicks < POP_IN_TICKS + HOLD_TICKS) {
            scale = 1.0D;
            extraLift = 0.0D;
        } else {
            double progress = (elapsedTicks - POP_IN_TICKS - HOLD_TICKS) / SHRINK_OUT_TICKS;
            scale = 1.0D - 0.5D * progress;
            extraLift = 10.0D * progress;
        }

        Font font = mc.font;
        Component text = Component.translatable("message.claudemod.prismium_featherstone.reduced", lastReductionPercent);
        int textWidth = font.width(text);
        int panelWidth = textWidth + PANEL_PADDING_X * 2;
        int panelHeight = font.lineHeight + PANEL_PADDING_Y * 2;

        int panelX = (screenWidth - panelWidth) / 2;
        int panelBottomY = screenHeight - gui.leftHeight - GAP_ABOVE_HEALTH;
        int panelY = panelBottomY - panelHeight;

        double centerX = panelX + panelWidth / 2.0D;
        double centerY = panelY + panelHeight / 2.0D - extraLift;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        guiGraphics.pose().translate(centerX, centerY, 0.0D);
        guiGraphics.pose().scale((float) scale, (float) scale, 1.0F);
        guiGraphics.pose().translate(-panelWidth / 2.0D, -panelHeight / 2.0D, 0.0D);

        guiGraphics.fill(0, 0, panelWidth, panelHeight, PANEL_BACKGROUND);
        guiGraphics.fill(0, 0, panelWidth, 1, PANEL_BORDER);
        guiGraphics.fill(0, panelHeight - 1, panelWidth, panelHeight, PANEL_BORDER);
        guiGraphics.drawString(font, text, PANEL_PADDING_X, PANEL_PADDING_Y, TEXT_COLOR, false);

        guiGraphics.pose().popPose();
    }
}
