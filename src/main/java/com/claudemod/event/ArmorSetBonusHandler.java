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
 * Session 4 added the Prismium armor set's first gameplay hook beyond raw
 * stats: full-set Night Vision. Session 5 builds on it in two ways flagged
 * in PROGRESS.md's "next candidates" list (section 5):
 * <ol>
 *   <li>A second full-set effect, Water Breathing, making the set a genuine
 *   "explore anywhere" package (dark caves + underwater) rather than a
 *   single-trick pony.</li>
 *   <li>A server-side-only guard, now that {@code Entity#level()} /
 *   {@code Level#isClientSide} were confirmed against MinecraftForge's own
 *   1.20.x branch source (ForgeEventFactory.java uses both
 *   {@code entity.level()} as a method call and {@code level.isClientSide}
 *   as a plain field access - no parentheses on the field). Session 4 had
 *   deliberately skipped this guard because the accessor names were
 *   unverified at the time; this session confirms them and adds the guard
 *   as the "safe optimization" that was called out as a good next step.</li>
 * </ol>
 *
 * <p>Wearing a full Prismium set (helmet + chestplate + leggings + boots,
 * all crafted from {@link ModArmorMaterials#PRISMIUM}) grants permanent,
 * icon-hidden Night Vision and Water Breathing. Both effects share the same
 * "re-apply every server tick with a short buffer duration" pattern so
 * neither needs explicit equip/unequip detection.
 *
 * <p><b>Unverified</b>: like the rest of the armor set (see PROGRESS.md),
 * this has not been playtested in a running game in this sandbox (no
 * Minecraft client available here). The server-only guard added this
 * session is a compile-level API confirmation, not a playtest - it has not
 * been confirmed that skipping client-side calls to
 * {@link Player#addEffect} avoids any visible desync/flicker versus the
 * previous both-sides behavior. Also unverified: how Water Breathing here
 * interacts with vanilla sources of the same effect (potions, turtle shell
 * helmet - though a Prismium helmet replaces the turtle shell slot outright
 * so that particular overlap can't happen), and whether the 220-tick
 * refresh buffer is short enough to avoid ever "running out" visibly.
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
        // Server-only: effects synced from the server reach the client
        // automatically, so re-applying them client-side too was redundant
        // work every tick for every player. See class javadoc for the
        // source confirming `level.isClientSide` is a plain field.
        if (player.level().isClientSide) {
            return;
        }
        if (hasFullPrismiumSet(player)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0,
                    true, false, false));
            player.addEffect(new MobEffectInstance(
                    MobEffects.WATER_BREATHING, EFFECT_DURATION_TICKS, 0,
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
