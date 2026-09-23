package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import com.claudemod.compat.curios.CuriosCompat;
import net.minecraftforge.fml.ModList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Scheduled session (2026-09-23): server-side {@link LivingDamageEvent}
 * listener implementing {@link
 * com.claudemod.item.PrismiumAntivenomCharmItem}'s entire behavior -
 * the seventh "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler} (fall), {@link PrismiumEmberguardHandler}
 * (fire/lava), {@link PrismiumVitastoneHandler} (healing), {@link
 * PrismiumAegisCharmHandler} (explosion) and {@link
 * PrismiumFrostguardCharmHandler} (freeze), reusing the exact same
 * overall shape (multiply, don't cancel; scan the whole inventory, not
 * just hands; emit a small particle/sound cue when the reduction
 * actually fires) rather than inventing a new pattern.
 *
 * <p><b>Damage-type check</b>: 1.20.1 vanilla has no dedicated "poison"
 * {@code DamageType} - the {@code Poison}/{@code Harming} mob effects
 * deal their tick damage via {@code DamageSource.magic()}, which maps
 * to {@link DamageTypes#MAGIC}. Wither effect damage has its own
 * dedicated {@link DamageTypes#WITHER} key. Both fields were confirmed
 * to exist on 1.20.1 via {@code
 * mappings.dev/1.20.1/net/minecraft/world/damagesource/DamageTypes.html}
 * before use, per the mod's standing "unconfirmed Java API" rule. This
 * charm checks both, matching its "毒/衰弱耐性" (poison/wither
 * resistance) concept from PROGRESS.md.
 *
 * <p><b>Known trade-off</b>: {@code DamageTypes.MAGIC} is broader than
 * "poison damage" alone - it also covers other indirect magic damage
 * sources (e.g. a thrown Harming splash potion, or non-projectile
 * magic attacks that route through {@code DamageSource.magic()}).
 * Unlike {@code DamageTypes.FREEZE} (used by Frostguard Charm), there
 * is no narrower vanilla damage type that isolates poison-effect tick
 * damage specifically. This is treated as acceptable scope creep in
 * the charm's favor (still thematically "resist toxins/dark magic")
 * rather than a bug, but is called out here in case play feedback
 * suggests it feels too strong.
 *
 * <p><b>Design choice - 50%, same as Emberguard/Aegis/Frostguard, not
 * Featherstone's 75%</b>: consistent with the mod's established
 * pattern of reserving the stronger 75% reduction for Featherstone
 * (fall damage, the one hazard with zero vanilla mitigation options)
 * and using the conservative 50% for every other charm, since poison
 * and wither already have partial vanilla counters (milk buckets,
 * Milk removes both status effects entirely, and Regeneration can
 * outpace tick damage).
 *
 * <p><b>Feedback sound/particle choice</b>: {@link
 * SoundEvents#AMETHYST_BLOCK_HIT} (already used by Aegis Charm and
 * Frostguard Charm) paired with {@link ParticleTypes#WITCH} - both
 * confirmed vanilla-existing on 1.20.1, and {@code WITCH} (already
 * used by vanilla for splash-potion-adjacent effects) reads as
 * "toxin/dark magic" without inventing an unconfirmed particle type.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether a flat 50% reduction feels
 * balanced in practice against both poison and wither damage at once;
 * whether the {@code WITCH} + {@code AMETHYST_BLOCK_HIT} feedback cue
 * reads sensibly; whether the {@code DamageTypes.MAGIC} scope-creep
 * trade-off noted above is actually noticeable in play.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumAntivenomCharmHandler {

    private static final float DAMAGE_MULTIPLIER = 0.5F;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        if (event.getAmount() <= 0.0F) {
            return;
        }

        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.WITHER) && !source.is(DamageTypes.MAGIC)) {
            return;
        }
        if (!hasAntivenomCharm(player)) {
            return;
        }

        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
        playFeedback(player);
    }

    private static void playFeedback(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    player.getX(), player.getY() + player.getBbHeight() / 2.0D, player.getZ(),
                    10, 0.4D, 0.4D, 0.4D, 0.02D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.PLAYERS, 0.5F, 0.9F);
        }
    }

    private static boolean hasAntivenomCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsAntivenomCharm(inventory.items)
                || containsAntivenomCharm(inventory.armor)
                || containsAntivenomCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as every other charm in this
        // family - see CuriosCompat's javadoc for why the ModList guard
        // must live here rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_ANTIVENOM_CHARM.get());
    }

    private static boolean containsAntivenomCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_ANTIVENOM_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
