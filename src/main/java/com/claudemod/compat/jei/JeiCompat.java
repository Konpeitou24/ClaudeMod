package com.claudemod.compat.jei;

import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 2026-08-31 direct-chat session follow-up fix for the item-details
 * overlay's JEI support - see {@link ClaudeModJeiPlugin}'s class doc for
 * the full backstory (repo owner: "ＪＥＩの修正は混乱を招くからちゃんと
 * 修正してくれ").
 *
 * <p><b>API used, confirmed this session</b> (WebFetch against JEI's own
 * {@code 1.20.1} branch source on GitHub, {@code CommonApi/src/main/java/
 * mezz/jei/api/runtime/IScreenHelper.java} and {@code
 * IClickableIngredient.java} - the {@code 1.20.1} branch was confirmed to
 * exist and match this mod's target version by cross-checking its
 * {@code gradle.properties}, which declares {@code minecraftVersion=
 * 1.20.1} and {@code specificationVersion=15.49.0}, matching the
 * {@code jei_version=15.56.0.204} pinned in this mod's own
 * gradle.properties): {@link IJeiRuntime#getScreenHelper()} (added JEI
 * spec 11.5.0, still present and undeprecated at 15.49.0+) exposes
 * {@code Stream<IClickableIngredient<?>> getClickableIngredientUnderMouse
 * (Screen screen, double mouseX, double mouseY)}, which - per its own
 * javadoc - aggregates hover info "from plugins ... and from vanilla and
 * JEI chat ingredient links", i.e. it works for *any* {@link Screen} JEI
 * knows how to inspect (its own recipe-view popup, ingredient list
 * overlay, bookmark overlay), not just this mod's own container screens.
 * {@link IClickableIngredient#getIngredient()} (added 11.7.0) returns the
 * raw ingredient object, which for an {@link ItemStack}-typed ingredient
 * (this mod's own items, and the vast majority of vanilla/modded content)
 * actually is one - filtered via {@code instanceof} below since the
 * generic type is erased to {@code ?} at the call site.
 *
 * <p><b>Why this exists as a separate file from {@link
 * ClaudeModJeiPlugin}</b>: same soft-dependency reasoning as {@code
 * CuriosCompat}/{@code CuriosSetupEvents} - {@code
 * ItemDetailsOverlay} (always loaded, client-side) calls into this class
 * *after* confirming {@code ModList.get().isLoaded("jei")}, so the JEI
 * types referenced in this file's method signatures are never resolved
 * unless JEI is actually present. {@link #runtime} starts {@code null}
 * and is only ever set by {@link ClaudeModJeiPlugin} (which itself is
 * only ever instantiated by JEI's own plugin discovery) - so even on a
 * client with JEI installed, {@link #getHoveredItemStack} degrades to
 * "no ingredient found" (rather than throwing) for the brief window
 * before JEI's runtime becomes available (e.g. before a world is
 * joined), exactly mirroring {@code CuriosCompat}'s own defensive style.
 *
 * <p><b>Unverified in-game</b> (no Minecraft client in this sandbox, see
 * PROGRESS.md standing note): whether {@code getClickableIngredientUnderMouse}
 * actually reports the expected ingredient while JEI's recipe-view popup
 * is open (the original reported symptom), whether the mouse-coordinate
 * conversion in {@link com.claudemod.client.overlay.ItemDetailsOverlay}
 * matches what JEI expects (screen-space, not raw window pixels - see
 * that class for the conversion), and whether this correctly returns
 * empty (rather than throwing) when hovering a JEI element that is not
 * an {@link ItemStack} (e.g. a modded fluid ingredient).
 */
public final class JeiCompat {

    private JeiCompat() {
    }

    @Nullable
    private static volatile IJeiRuntime runtime;

    /** Called only by {@link ClaudeModJeiPlugin}. */
    static void setRuntime(@Nullable IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /**
     * Returns the {@link ItemStack} ingredient JEI reports under the
     * mouse for the given screen/coordinates, if any and if JEI's
     * runtime is currently available. Callers MUST have already
     * confirmed {@code ModList.get().isLoaded("jei")} before calling
     * this - see {@code ItemDetailsOverlay}.
     */
    public static Optional<ItemStack> getHoveredItemStack(Screen screen, double mouseX, double mouseY) {
        IJeiRuntime activeRuntime = runtime;
        if (activeRuntime == null) {
            return Optional.empty();
        }
        return activeRuntime.getScreenHelper()
                .getClickableIngredientUnderMouse(screen, mouseX, mouseY)
                .findFirst()
                .map(IClickableIngredient::getIngredient)
                .filter(ItemStack.class::isInstance)
                .map(ItemStack.class::cast);
    }

    /**
     * Convenience overload that reads the current mouse position from
     * {@link Minecraft}'s own {@code MouseHandler}, converted to
     * screen-space coordinates the same way vanilla's own {@code
     * Minecraft#runTick} computes the {@code mouseX}/{@code mouseY} it
     * passes into {@code Screen#render} (raw window pixel position
     * scaled by {@code guiScaledWidth/screenWidth}, NOT a simple
     * division by the GUI scale factor, so this stays correct even with
     * non-integer scale factors or window letterboxing).
     */
    public static Optional<ItemStack> getHoveredItemStackAtMouse(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        double mouseX = minecraft.mouseHandler.xpos() * (double) minecraft.getWindow().getGuiScaledWidth()
                / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) minecraft.getWindow().getGuiScaledHeight()
                / (double) minecraft.getWindow().getScreenHeight();
        return getHoveredItemStack(screen, mouseX, mouseY);
    }
}
