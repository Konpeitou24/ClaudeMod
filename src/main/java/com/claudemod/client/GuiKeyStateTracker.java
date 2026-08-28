package com.claudemod.client;

import com.claudemod.ClaudeMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Root-cause fix for GitHub issue #19 ("詳細表示のバグ" - holding W while
 * hovering an item in an inventory screen shows nothing). Several previous
 * sessions read {@link ItemDetailsOverlay} and {@link
 * com.claudemod.item.TooltipUsageHelper} - both of which decide whether to
 * reveal their content purely via {@link
 * ModKeyMappings#SHOW_ITEM_DETAILS}{@code .isDown()} - and could not find a
 * bug by inspection alone; a defensive try/catch was added to {@code
 * ItemDetailsOverlay} to at least log an exception if one were silently
 * firing every frame, but the repo owner's own follow-up comment on the
 * issue ("0.20.0を確認しましたが治ってません" - confirmed still broken as of
 * v0.20.0) shows the problem was real, not a one-off report.
 *
 * <p><b>Root cause, found this session via Forge's own documentation</b>
 * (docs.minecraftforge.net, both the "latest" and the "1.20.1"-pinned
 * version of the Key Mappings page return byte-for-byte identical guidance,
 * fetched and compared this session): the "Checking a KeyMapping" section
 * explicitly splits into two *different* mechanisms depending on context -
 * {@code KeyMapping#isDown()}/{@code #consumeClick()} polled from {@code
 * ClientTickEvent} for "within the game", versus {@code
 * IForgeKeyMapping#isActiveAndMatches(InputConstants.Key)} checked from a
 * {@code Screen}'s own {@code keyPressed}/{@code keyReleased} - or, "if you
 * do not own the screen", from {@link ScreenEvent.KeyPressed}/{@link
 * ScreenEvent.KeyReleased} {@code Pre}/{@code Post} - explicitly called out
 * as the "Inside a GUI" mechanism. Both {@code ItemDetailsOverlay} and
 * {@code TooltipUsageHelper} only ever need this key's state while an
 * inventory-style {@code Screen} is open (that is the entire point of the
 * feature), yet both call {@code isDown()} - the "within the game"
 * mechanism the docs describe as a separate code path from the GUI one.
 * This matches the reported symptom exactly: not a crash (nothing in
 * {@code ItemDetailsOverlay}'s guarded logging ever had a chance to fire),
 * just a key state that silently never becomes true while any {@code
 * Screen} has input focus, because raw key-down tracking and the
 * screen-routed input path are two separate systems and this mod was only
 * ever reading the wrong one for a GUI-only feature.
 *
 * <p>This class is the mod's own implementation of the documented "Inside a
 * GUI" pattern: it subscribes to {@link ScreenEvent.KeyPressed.Pre}/{@link
 * ScreenEvent.KeyReleased.Pre} on the main Forge event bus (this mod does
 * not own the inventory/chest/etc. screens the feature needs to work
 * inside, so the event-based variant - rather than overriding {@code
 * keyPressed}/{@code keyReleased} directly - is the only option, exactly as
 * the docs describe for that situation) and tracks its own held/released
 * state via {@code IForgeKeyMapping#isActiveAndMatches}, independent of
 * {@code isDown()}.
 * {@code Pre} (rather than {@code Post}) is used for both so this tracker
 * always sees the raw key transition even if some other mod's screen
 * cancels/consumes the event afterward. The corresponding {@link
 * ScreenEvent.Closing} listener defensively clears the held state when any
 * screen closes, so a release event that is somehow never delivered (e.g.
 * the screen closing by other means while the key is still down) cannot
 * leave this tracker stuck reporting "held" forever.
 *
 * <p><b>Unverified</b>, same standing caveat as every client-facing change
 * in this repo (no Minecraft client in this sandbox): that {@code
 * ScreenEvent.KeyPressed.Pre}/{@code KeyReleased.Pre} actually fire for
 * every physical W press/release while e.g. the survival inventory is open,
 * and that this concretely makes the previously-reported-broken overlay
 * and tooltip expansion appear in-game. The reasoning above is grounded
 * directly in Forge's own documented API contract rather than a guess, but
 * only an in-game test (or the repo owner's confirmation) can fully close
 * out issue #19.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, value = Dist.CLIENT)
public final class GuiKeyStateTracker {

    private GuiKeyStateTracker() {
    }

    private static boolean showItemDetailsHeldInGui = false;

    /**
     * Whether {@link ModKeyMappings#SHOW_ITEM_DETAILS} should be treated as
     * held right now. Prefers this class's own GUI-context tracking (see
     * class javadoc for why {@code isDown()} alone is not reliable while a
     * {@code Screen} is open); also ORs in the raw {@code isDown()} state as
     * a harmless fallback in case this key is ever polled outside of any
     * GUI context in the future.
     */
    public static boolean isShowItemDetailsHeld() {
        return showItemDetailsHeldInGui || ModKeyMappings.SHOW_ITEM_DETAILS.isDown();
    }

    @SubscribeEvent
    public static void onKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        if (matches(event.getKeyCode(), event.getScanCode())) {
            showItemDetailsHeldInGui = true;
        }
    }

    @SubscribeEvent
    public static void onKeyReleasedPre(ScreenEvent.KeyReleased.Pre event) {
        if (matches(event.getKeyCode(), event.getScanCode())) {
            showItemDetailsHeldInGui = false;
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        // Defensive reset: if a release event was ever missed (screen torn
        // down through some path other than a normal key release), don't
        // leave the overlay permanently stuck "on" until the next
        // unrelated press/release of this key.
        showItemDetailsHeldInGui = false;
    }

    private static boolean matches(int keyCode, int scanCode) {
        return ModKeyMappings.SHOW_ITEM_DETAILS.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }
}
