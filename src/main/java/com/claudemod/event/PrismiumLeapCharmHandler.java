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
 * Scheduled session (2026-10-15): server-side {@code
 * TickEvent.PlayerTickEvent} listener implementing {@link
 * com.claudemod.item.PrismiumLeapCharmItem}'s entire behavior - the
 * eleventh "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler}, {@link PrismiumEmberguardHandler},
 * {@link PrismiumVitastoneHandler}, {@link PrismiumMagnetCharmHandler},
 * {@link PrismiumAegisCharmHandler}, {@link
 * PrismiumFrostguardCharmHandler}, {@link
 * PrismiumAntivenomCharmHandler}, {@link PrismiumHasteCharmHandler},
 * {@link PrismiumLuckCharmHandler} and {@link
 * PrismiumAqualungCharmHandler}, and (like Haste/Luck/Aqualung) not a
 * damage-type mitigation.
 *
 * <p><b>Deliberately a {@code TickEvent.PlayerTickEvent} listener, not
 * a discrete trigger event</b>: this charm's effect (a standing buff)
 * has no single triggering event to hook, so it reuses the exact
 * {@code TickEvent.PlayerTickEvent} + {@code Phase.END} + server-only
 * ({@code player.level().isClientSide}) + "re-apply every tick with a
 * short buffer duration" shape already proven to compile in this
 * codebase by {@link ArmorSetBonusHandler}, {@link
 * PrismiumFamiliarAuraHandler}, {@link PrismiumHasteCharmHandler},
 * {@link PrismiumLuckCharmHandler} and {@link
 * PrismiumAqualungCharmHandler} - no new {@code @Override} or
 * unverified method signature is introduced at all, per this mod's
 * standing "未確認の Java API" rule.
 *
 * <p><b>{@code MobEffects.JUMP}</b> is vanilla's Jump Boost effect
 * constant (backing the Potion of Leaping since its introduction),
 * confirmed to exist as a {@code public static final MobEffect} field
 * on {@code MobEffects} via mappings.dev before use (field name {@code
 * JUMP}, not {@code JUMP_BOOST} - the latter is only the display/UI
 * alias) - the same kind of plain enum-like field reference as {@code
 * MobEffects.DIG_SPEED}/{@code LUCK}/{@code WATER_BREATHING} already
 * used elsewhere in this codebase (a field reference, not a method
 * whose signature could be wrong), so it carries none of the risk the
 * mod's past {@code @Override} build accidents were caused by.
 * Amplifier 0 (Jump Boost I) matches the other nine standing-effect
 * charms' amplifier choice.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether the jump height increase feels
 * noticeable/useful without feeling disorienting (fall damage from a
 * boosted jump is still governed separately - this charm does not
 * combine Jump Boost with fall damage immunity, unlike a Jump
 * Boost+Featherstone pairing a player could choose to carry
 * themselves), whether it stacks cleanly if the player also drinks a
 * vanilla Potion of Leaping of a different amplifier, and whether the
 * visible Jump Boost icon reads clearly in the effects HUD.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumLeapCharmHandler {

    // 7 seconds - comfortably longer than the 1-tick refresh interval,
    // matching Haste/Luck/Aqualung's buffer so a brief lag spike or
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
        if (!hasLeapCharm(player)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.JUMP, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                true, false, true));
    }

    private static boolean hasLeapCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsLeapCharm(inventory.items)
                || containsLeapCharm(inventory.armor)
                || containsLeapCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as every other charm in this family -
        // see CuriosCompat's javadoc for why the ModList guard must live here
        // rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_LEAP_CHARM.get());
    }

    private static boolean containsLeapCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_LEAP_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
