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
 * Scheduled session (2026-10-14): server-side {@code
 * TickEvent.PlayerTickEvent} listener implementing {@link
 * com.claudemod.item.PrismiumAqualungCharmItem}'s entire behavior - the
 * tenth "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler}, {@link PrismiumEmberguardHandler},
 * {@link PrismiumVitastoneHandler}, {@link PrismiumMagnetCharmHandler},
 * {@link PrismiumAegisCharmHandler}, {@link
 * PrismiumFrostguardCharmHandler}, {@link
 * PrismiumAntivenomCharmHandler}, {@link PrismiumHasteCharmHandler} and
 * {@link PrismiumLuckCharmHandler}, and (like Haste/Luck) not a
 * damage-type mitigation.
 *
 * <p><b>Deliberately a {@code TickEvent.PlayerTickEvent} listener, not
 * a discrete trigger event</b>: this charm's effect (a standing buff)
 * has no single triggering event to hook, so it reuses the exact
 * {@code TickEvent.PlayerTickEvent} + {@code Phase.END} + server-only
 * ({@code player.level().isClientSide}) + "re-apply every tick with a
 * short buffer duration" shape already proven to compile in this
 * codebase by {@link ArmorSetBonusHandler}, {@link
 * PrismiumFamiliarAuraHandler}, {@link PrismiumHasteCharmHandler} and
 * {@link PrismiumLuckCharmHandler} - no new {@code @Override} or
 * unverified method signature is introduced at all, per this mod's
 * standing "未確認の Java API" rule.
 *
 * <p><b>{@code MobEffects.WATER_BREATHING}</b> is vanilla's Water
 * Breathing effect constant (backing the Potion of Water
 * Breathing/turtle helmet since their respective introductions),
 * confirmed to exist as a {@code public static final MobEffect} field
 * on {@code MobEffects} via mappings.dev before use - the same kind of
 * plain enum-like field reference as {@code MobEffects.DIG_SPEED}/
 * {@code LUCK}/{@code REGENERATION}/{@code NIGHT_VISION} already used
 * elsewhere in this codebase (a field reference, not a method whose
 * signature could be wrong), so it carries none of the risk the mod's
 * past {@code @Override} build accidents were caused by. Amplifier 0 is
 * the only level Water Breathing has ever shipped with in vanilla.
 *
 * <p><b>Design choice - {@code showIcon(true)}, same as {@link
 * PrismiumHasteCharmHandler}/{@link PrismiumLuckCharmHandler}</b>: the
 * only other persistent cue this charm's wearer gets is the absence of
 * an air meter underwater, so showing the Water Breathing icon in the
 * effects HUD is a second, always-visible confirmation that it is
 * active even on dry land.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether permanent Water Breathing
 * actually suppresses the drowning air-bubble countdown and HUD oxygen
 * meter the way the turtle helmet/potion do, whether it stacks cleanly
 * with Conduit Power if the player is ever near one, and whether the
 * visible Water Breathing icon reads clearly in the effects HUD.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumAqualungCharmHandler {

    // 7 seconds - comfortably longer than the 1-tick refresh interval,
    // matching Haste/Luck's buffer so a brief lag spike or missed tick
    // never visibly runs the effect out (and never lets the drowning
    // countdown start ticking back down).
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
        if (!hasAqualungCharm(player)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.WATER_BREATHING, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                true, false, true));
    }

    private static boolean hasAqualungCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsAqualungCharm(inventory.items)
                || containsAqualungCharm(inventory.armor)
                || containsAqualungCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as every other charm in this family -
        // see CuriosCompat's javadoc for why the ModList guard must live here
        // rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_AQUALUNG_CHARM.get());
    }

    private static boolean containsAqualungCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_AQUALUNG_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
