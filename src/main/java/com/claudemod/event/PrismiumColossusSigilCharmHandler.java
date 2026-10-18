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
 * Scheduled session (2026-10-18): server-side {@code
 * TickEvent.PlayerTickEvent} listener implementing {@link
 * com.claudemod.item.PrismiumColossusSigilCharmItem}'s entire behavior -
 * the thirteenth "just carry it" passive accessory, reusing the exact
 * {@code TickEvent.PlayerTickEvent} + {@code Phase.END} + server-only
 * ({@code player.level().isClientSide}) + "re-apply every tick with a
 * short buffer duration" shape already proven to compile in this
 * codebase by {@link PrismiumHasteCharmHandler}, {@link
 * PrismiumLuckCharmHandler}, {@link PrismiumAqualungCharmHandler},
 * {@link PrismiumLeapCharmHandler} and {@link
 * PrismiumSatiationCharmHandler} - no new {@code @Override} or
 * unverified method signature is introduced at all, per this mod's
 * standing "未確認のJava API" rule.
 *
 * <p><b>{@code MobEffects.DAMAGE_BOOST}</b> is vanilla's Strength
 * effect constant, the same kind of plain enum-like field reference
 * (not a method whose signature could be wrong) as {@code
 * MobEffects.DIG_SPEED}/{@code LUCK}/{@code WATER_BREATHING}/{@code
 * JUMP}/{@code SATURATION} already used by this exact family of
 * handlers - amplifier 0 is Strength I (+3 attack damage, matching the
 * vanilla Strength I potion).
 *
 * <p><b>{@code showIcon(true)}</b>, matching {@link
 * PrismiumHasteCharmHandler}/{@link PrismiumLuckCharmHandler}/etc.
 * rather than {@link ArmorSetBonusHandler}/{@link
 * PrismiumFamiliarAuraHandler}: a Colossus Sigil Charm sitting
 * anywhere in a player's inventory has no other visible cue that it is
 * working, so the HUD effect icon is the only confirmation.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether permanent Strength I feels
 * like a fair "just carry it" bonus for a boss-trophy-tier item, and
 * whether the recipe cost (one Prismium Colossus Core, a boss-only
 * drop) reads as appropriately rare compared to the other twelve
 * charms in this family, all of which are craftable from purely
 * overworld-obtainable materials.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumColossusSigilCharmHandler {

    // 7 seconds - comfortably longer than the 1-tick refresh interval,
    // matching every other charm in this family's buffer so a brief lag
    // spike or missed tick never visibly runs the effect out.
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
        if (!hasColossusSigilCharm(player)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                true, false, true));
    }

    private static boolean hasColossusSigilCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsColossusSigilCharm(inventory.items)
                || containsColossusSigilCharm(inventory.armor)
                || containsColossusSigilCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as every other charm in this family -
        // see CuriosCompat's javadoc for why the ModList guard must live here
        // rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_COLOSSUS_SIGIL_CHARM.get());
    }

    private static boolean containsColossusSigilCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_COLOSSUS_SIGIL_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
