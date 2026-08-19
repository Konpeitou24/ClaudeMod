package com.claudemod.client;

import com.claudemod.ClaudeMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Session 60 (scheduled): the mod's first {@link KeyMapping}.
 *
 * <p>Issue #7 received two separate comments from Konpeitou24 that both
 * converge on the same request: the always-on one-line gray "usage" hint
 * added by sessions 45/56 (see {@code EnergyStorageBlockItem},
 * {@code PrismiumGearTooltipHandler}, and every other {@code *.usage}
 * tooltip call site) is "大きく、かなり邪魔に感じます" (large and gets in
 * the way) when just browsing an inventory. The first comment explicitly
 * suggested "ツールチップ上でWキーを押している間だけ表示される" (only show
 * it while holding W); the second, later comment asked for a more
 * elaborate animated explanation screen reachable the same way, but said
 * a first version can be plain text only. This key mapping implements the
 * common "hold to reveal" part both comments ask for; see
 * {@link com.claudemod.item.TooltipUsageHelper} for how it is consumed.
 * The full animated/screen-based explanation from the second comment is
 * intentionally NOT attempted this session - see PROGRESS.md's handoff for
 * why (new Screen infrastructure is higher risk than can be verified in
 * this sandbox) and treat this as the incremental first step.
 *
 * <p>Default key is {@code W}, exactly as the user suggested. This
 * intentionally duplicates vanilla's "move forward" binding; the Controls
 * screen will show both as a (harmless, cosmetic) conflict warning, but
 * they do not functionally interfere: this binding is only ever polled
 * (see {@code isDown()} in {@code TooltipUsageHelper}) while a tooltip is
 * being drawn, which only happens while a {@code Screen} (e.g. the
 * inventory) is open - and movement input is already suppressed by
 * vanilla whenever a Screen is open, so holding W over a tooltip cannot
 * also move the player. {@code KeyConflictContext.UNIVERSAL} is used
 * (rather than {@code IN_GAME}) specifically so the key still registers
 * as held while a Screen has focus, since tooltips are only ever shown
 * from within a Screen; confirmed against a real Forge 1.20.1 mod's
 * {@code KeyMapping} construction (TerraFirmaCraft's
 * {@code TFCKeyBindings}, fetched this session) for the exact constructor
 * shape used below.
 */
public final class ModKeyMappings {

    private ModKeyMappings() {
    }

    public static final String CATEGORY = "key.categories." + ClaudeMod.MOD_ID;

    public static final KeyMapping SHOW_ITEM_DETAILS = new KeyMapping(
            "key." + ClaudeMod.MOD_ID + ".show_details",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_W,
            CATEGORY);
}
