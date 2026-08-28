package com.claudemod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Session 60 (scheduled): centralizes the "hold {@link
 * com.claudemod.client.ModKeyMappings#SHOW_ITEM_DETAILS} to reveal the
 * full usage hint" behaviour requested twice on Issue #7 (see that key
 * mapping's own javadoc for the full quote/reasoning).
 *
 * <p>Every one of this mod's ~13 existing {@code *.usage} tooltip call
 * sites (across the various {@code Item}/{@code EnergyStorageBlockItem}
 * subclasses and {@code PrismiumGearTooltipHandler}) used the identical
 * one-line shape {@code Component.translatable(key + ".usage")
 * .withStyle(ChatFormatting.GRAY)}. Rather than duplicating the
 * "isDown()/short-prompt-fallback" logic at each of those sites, they now
 * all call {@link #usageLine(String)} instead, which mechanically
 * preserves each call site's existing {@code tooltip.add(...)} shape.
 *
 * <p><b>Client/server safety</b>: {@code Item#appendHoverText} and {@code
 * ItemTooltipEvent} are both, per {@code PrismiumGearTooltipHandler}'s
 * own research note (Forge javadoc, confirmed via WebSearch that
 * session), only ever invoked while a client is actually rendering a
 * tooltip - never on a dedicated server. This method still defensively
 * checks {@link FMLEnvironment#dist} before touching the client-only
 * {@code KeyMapping} class, so that even a hypothetical future
 * server-side call degrades to always showing the full line rather than
 * risking a {@code NoClassDefFoundError}, instead of relying purely on
 * "this is never called server-side" holding forever.
 *
 * <p><b>Unverified</b>: whether the short prompt line reads naturally
 * in-game, whether holding W while hovering a tooltip in an inventory
 * screen behaves as expected (no unwanted interaction with movement -
 * see {@code ModKeyMappings}'s javadoc for why this should be safe in
 * theory), and general tooltip line-wrapping/layout with the new short
 * line swapped in - this sandbox has no Minecraft client to check any of
 * that against, per PROGRESS.md's standing note.
 */
public final class TooltipUsageHelper {

    private TooltipUsageHelper() {
    }

    /**
     * Builds the one-line usage hint for the given item/block translation
     * key (i.e. {@code getDescriptionId()}), showing the full hint text
     * (from the existing {@code <key>.usage} lang entry) while the detail
     * key is held, and a short, compact "hold W for details" prompt
     * otherwise.
     */
    public static Component usageLine(String descriptionId) {
        String usageKey = descriptionId + ".usage";
        if (FMLEnvironment.dist != Dist.CLIENT || isDetailKeyDown()) {
            return Component.translatable(usageKey).withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("tooltip.claudemod.hold_for_details",
                        com.claudemod.client.ModKeyMappings.SHOW_ITEM_DETAILS.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static boolean isDetailKeyDown() {
        // GitHub issue #19 fix (see com.claudemod.client.GuiKeyStateTracker's
        // class javadoc for the full root-cause writeup): raw
        // KeyMapping#isDown() is not reliably updated while a Screen (e.g.
        // the inventory this tooltip is drawn inside) has input focus - use
        // the mod's own GUI-context key tracker instead.
        return com.claudemod.client.GuiKeyStateTracker.isShowItemDetailsHeld();
    }
}
