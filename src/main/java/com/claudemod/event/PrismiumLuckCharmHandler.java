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
 * Scheduled session (2026-10-12): server-side {@code
 * TickEvent.PlayerTickEvent} listener implementing {@link
 * com.claudemod.item.PrismiumLuckCharmItem}'s entire behavior - the
 * ninth "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler}, {@link PrismiumEmberguardHandler},
 * {@link PrismiumVitastoneHandler}, {@link PrismiumMagnetCharmHandler},
 * {@link PrismiumAegisCharmHandler}, {@link
 * PrismiumFrostguardCharmHandler}, {@link
 * PrismiumAntivenomCharmHandler} and {@link
 * PrismiumHasteCharmHandler}, and (like Haste) not a damage-type
 * mitigation.
 *
 * <p><b>Deliberately a {@code TickEvent.PlayerTickEvent} listener, not
 * a discrete trigger event</b>: this charm's effect (a standing buff)
 * has no single triggering event to hook, so it reuses the exact
 * {@code TickEvent.PlayerTickEvent} + {@code Phase.END} + server-only
 * ({@code player.level().isClientSide}) + "re-apply every tick with a
 * short buffer duration" shape already proven to compile in this
 * codebase by {@link ArmorSetBonusHandler}, {@link
 * PrismiumFamiliarAuraHandler} and {@link PrismiumHasteCharmHandler} -
 * no new {@code @Override} or unverified method signature is
 * introduced at all, per this mod's standing "未確認の Java API" rule.
 *
 * <p><b>{@code MobEffects.LUCK}</b> is vanilla's Luck effect constant
 * (backing the Potion of Luck/tipped arrow since 1.9), confirmed to
 * exist as a {@code public static final MobEffect} field on {@code
 * MobEffects} via mappings.dev before use - the same kind of plain
 * enum-like field reference as {@code MobEffects.DIG_SPEED}/{@code
 * REGENERATION}/{@code NIGHT_VISION} already used elsewhere in this
 * codebase (a field reference, not a method whose signature could be
 * wrong), so it carries none of the risk the mod's past {@code
 * @Override} build accidents were caused by. Amplifier 0 is Luck I,
 * matching the vanilla Luck I potion's loot-table bonus.
 *
 * <p><b>Design choice - {@code showIcon(true)}, same as {@link
 * PrismiumHasteCharmHandler}</b>: a Luck Charm sitting anywhere in a
 * player's inventory has no other visible cue that it is active (it
 * only ever changes probabilities, never anything on-screen), so
 * showing the Luck icon in the effects HUD is this charm's only
 * confirmation that it is working.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether permanent Luck I's effect on
 * loot/fishing/bartering odds is noticeable enough to feel worthwhile
 * as a "just carry it" bonus, and whether the visible Luck icon reads
 * clearly in the effects HUD.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumLuckCharmHandler {

    // 7 seconds - comfortably longer than the 1-tick refresh interval,
    // matching PrismiumHasteCharmHandler's buffer so a brief lag spike
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
        if (!hasLuckCharm(player)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.LUCK, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                true, false, true));
    }

    private static boolean hasLuckCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsLuckCharm(inventory.items)
                || containsLuckCharm(inventory.armor)
                || containsLuckCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as every other charm in this family -
        // see CuriosCompat's javadoc for why the ModList guard must live here
        // rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_LUCK_CHARM.get());
    }

    private static boolean containsLuckCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_LUCK_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
