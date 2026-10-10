package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import com.claudemod.compat.curios.CuriosCompat;
import net.minecraftforge.fml.ModList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Scheduled session (2026-10-10): server-side {@code
 * TickEvent.PlayerTickEvent} listener implementing {@link
 * com.claudemod.item.PrismiumHasteCharmItem}'s entire behavior - the
 * eighth "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler}, {@link PrismiumEmberguardHandler},
 * {@link PrismiumVitastoneHandler}, {@link PrismiumMagnetCharmHandler},
 * {@link PrismiumAegisCharmHandler}, {@link
 * PrismiumFrostguardCharmHandler} and {@link
 * PrismiumAntivenomCharmHandler}, but the first one whose effect is
 * not a damage-type mitigation - see {@link
 * com.claudemod.item.PrismiumHasteCharmItem}'s javadoc for why this
 * family's next entry was chosen to be a non-damage-reduction effect.
 *
 * <p><b>Deliberately a {@code TickEvent.PlayerTickEvent} listener, not
 * a {@code LivingDamageEvent}/{@code LivingHealEvent} one</b>: this
 * charm's effect (a standing buff) has no discrete triggering event to
 * hook, so it reuses the exact {@code TickEvent.PlayerTickEvent} +
 * {@code Phase.END} + server-only ({@code player.level().isClientSide})
 * + "re-apply every tick with a short buffer duration" shape already
 * proven to compile in this codebase by {@link ArmorSetBonusHandler}
 * (Night Vision/Water Breathing for a full Prismium armor set) and
 * {@link PrismiumFamiliarAuraHandler} (Regeneration while a tamed
 * Familiar is nearby) - no new {@code @Override} or unverified method
 * signature is introduced at all, per this mod's standing "未確認の
 * Java API" rule.
 *
 * <p><b>{@code MobEffects.DIG_SPEED}</b> is vanilla's Haste effect
 * constant, the same kind of plain enum-like field as {@code
 * MobEffects.REGENERATION}/{@code NIGHT_VISION}/{@code
 * WATER_BREATHING} already used elsewhere in this codebase (a field
 * reference, not a method whose signature could be wrong) - amplifier
 * 0 is Haste I (20% faster block breaking, matching the vanilla Haste
 * I potion).
 *
 * <p><b>Design choice - {@code showIcon(true)}, unlike the other
 * tick-refreshed passive effects in this mod</b>: {@link
 * ArmorSetBonusHandler}'s Night Vision/Water Breathing and {@link
 * PrismiumFamiliarAuraHandler}'s Regeneration both hide their HUD icon
 * because the player already has an obvious, continuously-visible cue
 * that the effect is active (wearing visible armor; a tamed Familiar
 * flying alongside them). A Haste Charm sitting anywhere in a player's
 * inventory has no such visible cue, so showing the Haste icon in the
 * effects HUD is this charm's only confirmation that it is working -
 * the same reasoning that led every damage-reduction charm in this
 * family to add an explicit particle/sound cue on trigger, just
 * applied to a standing buff instead of a one-off event.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether permanent Haste I feels like a
 * fair "just carry it" bonus rather than trivializing mining compared
 * to the vanilla Haste potion/beacon (both temporary/positional), and
 * whether the visible Haste icon is a clear enough "this is working"
 * signal without being a nag.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumHasteCharmHandler {

    // 7 seconds - comfortably longer than the 1-tick refresh interval,
    // matching PrismiumFamiliarAuraHandler's buffer so a brief lag spike
    // or missed tick never visibly runs the effect out.
    private static final int EFFECT_DURATION_TICKS = 140;
    private static final int EFFECT_AMPLIFIER = 0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        if (!hasHasteCharm(player)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                true, false, true));
    }

    private static boolean hasHasteCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsHasteCharm(inventory.items)
                || containsHasteCharm(inventory.armor)
                || containsHasteCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as every other charm in this family -
        // see CuriosCompat's javadoc for why the ModList guard must live here
        // rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_HASTE_CHARM.get());
    }

    private static boolean containsHasteCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_HASTE_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
