package com.claudemod.client.overlay;

import com.claudemod.ClaudeMod;
import com.claudemod.client.GuiKeyStateTracker;
import com.claudemod.client.ModKeyMappings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A/D page-turning for {@link ItemDetailsOverlay}'s panel - added same
 * direct-chat session as the panel's progress bar, per the repo owner's
 * own suggestion ("Wを押しながらADでページング...ページをめくる音").
 *
 * <p>Reuses {@link GuiKeyStateTracker}'s "inside a GUI" key-press event
 * pattern (see its class javadoc for why {@code KeyMapping#isDown()}
 * alone is not reliable while a {@code Screen} has input focus): A/D are
 * read from {@code ScreenEvent.KeyPressed.Pre}, the same event family
 * used to track the W (details) key itself, rather than from a per-tick
 * poll - keeps this class consistent with the rest of the mod's proven
 * approach instead of introducing a second input-handling style.
 *
 * <p>Page changes are only accepted while W is held <em>and</em> the
 * panel has actually finished appearing ({@link
 * ItemDetailsOverlay#isPanelVisible()}), so pressing A/D while just the
 * progress bar is showing (or not holding W at all) does nothing -
 * avoids a page silently advancing before the user can see it, and stops
 * A/D from doing anything unexpected when the overlay isn't part of the
 * picture at all (e.g. normal inventory navigation).
 *
 * <p><b>Unverified this session</b> (no in-game testing available): the
 * {@code SoundEvents.BOOK_PAGE_TURN} constant name itself, and whether
 * {@code SimpleSoundInstance.forUI(SoundEvent, float)} is the right
 * factory overload for a UI-context sound versus a positional one -
 * both were matched against this mod's own existing sound-effect
 * conventions rather than a fresh page-turn-specific implementation
 * elsewhere in the codebase, since no prior ClaudeMod code plays a
 * vanilla sound from a client-side GUI event.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, value = Dist.CLIENT)
public final class ItemDetailsPaging {

    private ItemDetailsPaging() {
    }

    public static final int PAGE_COUNT = 2;

    private static int currentPage = 0;

    public static int currentPage() {
        return currentPage;
    }

    public static void resetPage() {
        currentPage = 0;
    }

    @SubscribeEvent
    public static void onKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        if (!GuiKeyStateTracker.isShowItemDetailsHeld() || !ItemDetailsOverlay.isPanelVisible()) {
            return;
        }

        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (ModKeyMappings.PAGE_PREVIOUS.isActiveAndMatches(key)) {
            changePage(-1);
        } else if (ModKeyMappings.PAGE_NEXT.isActiveAndMatches(key)) {
            changePage(1);
        }
    }

    private static void changePage(int delta) {
        int next = Math.floorMod(currentPage + delta, PAGE_COUNT);
        if (next == currentPage) {
            return;
        }
        currentPage = next;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
        }
    }
}
