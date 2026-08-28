package com.claudemod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Session 60 (scheduled) originally added a "hold {@link
 * com.claudemod.client.ModKeyMappings#SHOW_ITEM_DETAILS} to reveal the
 * full usage hint inline in the tooltip" behaviour here, on top of the
 * separate full-screen panel in {@link
 * com.claudemod.client.overlay.ItemDetailsOverlay}. The repo owner later
 * pointed out (direct chat, after issue #19's fix shipped in v0.25.3)
 * that having both the tooltip line *and* the overlay panel react to the
 * same key press reads as two redundant "detail" surfaces - one is
 * enough. This class now only ever shows the short, static "hold W for
 * details" prompt; {@link
 * com.claudemod.client.overlay.ItemDetailsOverlay} is the sole surface
 * that reveals the full {@code .usage}/{@code .details} text on a hold.
 *
 * <p>Every one of this mod's ~13 existing {@code *.usage} tooltip call
 * sites (across the various {@code Item}/{@code EnergyStorageBlockItem}
 * subclasses and {@code PrismiumGearTooltipHandler}) call {@link
 * #usageLine(String)} instead of building this line inline, so this is
 * still the single place to change if the prompt's wording/styling ever
 * needs to change again.
 */
public final class TooltipUsageHelper {

    private TooltipUsageHelper() {
    }

    /**
     * Builds the one-line, always-short "hold W for details" prompt for
     * the given item/block translation key (i.e. {@code
     * getDescriptionId()}). The full description is shown exclusively by
     * {@link com.claudemod.client.overlay.ItemDetailsOverlay} when the key
     * is actually held - this line itself never expands, so this method no
     * longer needs to read {@code descriptionId} at all, but keeps taking
     * it so every existing call site's shape stays unchanged.
     */
    public static Component usageLine(String descriptionId) {
        return Component.translatable("tooltip.claudemod.hold_for_details",
                        com.claudemod.client.ModKeyMappings.SHOW_ITEM_DETAILS.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.DARK_GRAY);
    }
}
