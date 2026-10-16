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
 * Scheduled session (2026-10-16): server-side {@code
 * TickEvent.PlayerTickEvent} listener implementing {@link
 * com.claudemod.item.PrismiumSatiationCharmItem}'s entire behavior - the
 * twelfth "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler}, {@link PrismiumEmberguardHandler},
 * {@link PrismiumVitastoneHandler}, {@link PrismiumMagnetCharmHandler},
 * {@link PrismiumAegisCharmHandler}, {@link
 * PrismiumFrostguardCharmHandler}, {@link
 * PrismiumAntivenomCharmHandler}, {@link PrismiumHasteCharmHandler},
 * {@link PrismiumLuckCharmHandler}, {@link PrismiumAqualungCharmHandler}
 * and {@link PrismiumLeapCharmHandler}, and (like Haste/Luck/Aqualung/
 * Leap) not a damage-type mitigation.
 *
 * <p><b>Deliberately a {@code TickEvent.PlayerTickEvent} listener, not a
 * discrete trigger event</b>: this charm's effect (a standing buff) has
 * no single triggering event to hook, so it reuses the exact {@code
 * TickEvent.PlayerTickEvent} + {@code Phase.END} + server-only ({@code
 * player.level().isClientSide}) + "re-apply every tick with a short
 * buffer duration" shape already proven to compile in this codebase by
 * {@link ArmorSetBonusHandler}, {@link PrismiumFamiliarAuraHandler},
 * {@link PrismiumHasteCharmHandler}, {@link PrismiumLuckCharmHandler},
 * {@link PrismiumAqualungCharmHandler} and {@link
 * PrismiumLeapCharmHandler} - no new {@code @Override} or unverified
 * method signature is introduced at all, per this mod's standing
 * "未確認の Java API" rule.
 *
 * <p><b>{@code MobEffects.SATURATION}</b> is vanilla's Saturation effect
 * constant (the effect that tops up the hunger/saturation gauges each
 * tick it is active, e.g. briefly from eating certain foods), confirmed
 * to exist as a {@code public static final MobEffect} field on {@code
 * MobEffects} via mappings.dev before use - the same kind of plain
 * enum-like field reference as {@code MobEffects.JUMP}/{@code LUCK}/
 * {@code WATER_BREATHING} already used elsewhere in this codebase (a
 * field reference, not a method whose signature could be wrong), so it
 * carries none of the risk the mod's past {@code @Override} build
 * accidents were caused by. Amplifier 0 (Saturation I) matches the
 * other eleven standing-effect charms' amplifier choice.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether a permanently-reapplied
 * Saturation I actually keeps the hunger gauge from ever dropping (as
 * opposed to merely slowing its depletion), whether it shows a
 * noticeable/legible icon in the effects HUD, and whether reapplying it
 * every tick causes the hunger/saturation gauges to visibly "flicker"
 * each time the brief 140-tick instance is refreshed (the same refresh
 * shape Haste/Luck/Aqualung/Leap already use without a reported issue,
 * but none of those are food-gauge effects, so this is a new interaction
 * to watch for specifically).
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumSatiationCharmHandler {

    // 7 seconds - comfortably longer than the 1-tick refresh interval,
    // matching Haste/Luck/Aqualung/Leap's buffer so a brief lag spike or
    // missed tick never visibly runs the effect out.
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
        if (!hasSatiationCharm(player)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.SATURATION, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                true, false, true));
    }

    private static boolean hasSatiationCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsSatiationCharm(inventory.items)
                || containsSatiationCharm(inventory.armor)
                || containsSatiationCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as every other charm in this family -
        // see CuriosCompat's javadoc for why the ModList guard must live here
        // rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_SATIATION_CHARM.get());
    }

    private static boolean containsSatiationCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_SATIATION_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
