package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.item.ModArmorMaterials;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Session 4: the Prismium armor set's first gameplay hook beyond raw stats.
 * PROGRESS.md (session 3, section 4-3) flagged that the armor set was a pure
 * stat upgrade with no unique ability, and section 5 asked for a simple set
 * bonus as the first concrete step.
 *
 * <p>Wearing a full Prismium set (helmet + chestplate + leggings + boots,
 * all crafted from {@link ModArmorMaterials#PRISMIUM}) grants a permanent,
 * icon-hidden Night Vision effect. This ties into the set's existing
 * crystal/light theme (Prismium Block glows at light level 6, Prismium Core
 * at 10) and previews the kind of "helps you explore the dark" utility the
 * mod wants Prism Realm gear to lean into later.
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>Re-applies the effect every server tick with a short buffer duration
 *   ({@link #EFFECT_DURATION_TICKS}, well over 1 tick) so it never runs out
 *   while the set stays on, without needing to detect equip/unequip events.
 *   This is the same "always-on gear effect" pattern used by many Forge
 *   mods for things like light-source armor or breathing gear.</li>
 *   <li>{@code ambient = true} makes the effect's particle border faint
 *   (matches how beacon effects render) and {@code showIcon = false} keeps
 *   it out of the player's HUD effect list, since this is meant to read as
 *   a passive property of the armor rather than a "buff" the player has to
 *   track.</li>
 *   <li>Runs on both logical sides (no {@code isClientSide} guard): calling
 *   {@link Player#addEffect} on the client is redundant once the server's
 *   effect syncs down, but harmless, and skipping the guard avoids relying
 *   on an unverified {@code Level} accessor name in this offline session.
 *   If a later session confirms the exact accessor, restricting this to the
 *   server side would be a minor, safe optimization.</li>
 * </ul>
 *
 * <p><b>Unverified</b>: like the rest of the armor set (see PROGRESS.md),
 * this has not been playtested in a running game in this sandbox (no
 * Minecraft client available here). The API shapes used
 * ({@code TickEvent.PlayerTickEvent}, {@code MobEffectInstance}'s 6-arg
 * constructor, {@code ArmorItem#getMaterial()}, {@code Inventory#armor})
 * were each cross-checked against Forge 1.20.1 javadocs during this session,
 * but only a real build/playtest can confirm the effect actually feels good
 * and doesn't, say, fight with vanilla Night Vision from potions/spectral
 * arrows in a confusing way (e.g. amplifier stacking, effect being
 * overwritten early when the buffer briefly lapses on lag spikes).
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class ArmorSetBonusHandler {

    // 11 seconds - far longer than the 1-tick refresh interval, so brief
    // lag spikes or a missed tick or two won't cause a visible flicker.
    private static final int EFFECT_DURATION_TICKS = 220;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (hasFullPrismiumSet(player)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0,
                    true, false, false));
        }
    }

    private static boolean hasFullPrismiumSet(Player player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) {
                return false;
            }
            if (armorItem.getMaterial() != ModArmorMaterials.PRISMIUM) {
                return false;
            }
        }
        return true;
    }
}
