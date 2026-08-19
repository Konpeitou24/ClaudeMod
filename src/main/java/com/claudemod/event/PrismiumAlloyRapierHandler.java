package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Session 71 (scheduled): the Alloy Rapier's gimmick, following the exact
 * same shape as {@link PrismiumSwordHandler} (Sword's Glowing-on-hit) and
 * {@link PrismiumWarhammerHandler} (Warhammer's stagger-on-hit) rather
 * than inventing a new API surface. See
 * {@link com.claudemod.registry.ModItems#PRISMIUM_ALLOY_RAPIER} for the
 * item registration/stat rationale (very low damage, very fast speed -
 * the mod's first item built around Prismium Alloy Ingot, finally giving
 * that material - added session 70, unused until now - an equipment
 * purpose, per PROGRESS.md's session 70 handoff item 1).
 *
 * <p>Landing a hit with the Rapier equipped has a
 * {@value #WEAKEN_CHANCE}-in-1 chance to inflict a short Weakness on the
 * target, flavor-wise "the alloy blade saps the target's strength" -
 * deliberately a different effect from the Sword's Glowing and the
 * Warhammer's Slowness, so all three melee gimmicks stay distinguishable.
 * Kept lower than the Warhammer's 0.5 chance (and close to the Sword's
 * 0.15) because the Rapier's attack speed is roughly double the Sword's
 * and several times the Warhammer's (see ModItems), so landed hits are
 * far more frequent - a high per-hit chance here would read as
 * "permanent Weakness uptime" rather than an occasional proc.
 *
 * <p>Deliberately reuses the identical {@link LivingHurtEvent} hook,
 * {@code DamageSource#getEntity()} main-hand-item check, and
 * client/server guard that {@link PrismiumSwordHandler} and
 * {@link PrismiumWarhammerHandler} already establish - see those
 * classes' own javadocs for the API rationale this borrows rather than
 * re-deriving.
 *
 * <p><b>Unverified</b>: no in-game playtest (no Minecraft client in this
 * sandbox, see PROGRESS.md's standing note). In particular whether a
 * ~3-attacks/second weapon reapplying a short Weakness pulse this often
 * reads as "a fair trade for low damage" or as "oppressive uptime" in an
 * actual fight is an untested balance judgment call, same caveat every
 * other melee gimmick in this mod already carries.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumAlloyRapierHandler {

    // Close to the Sword's 0.15 - see class javadoc for why (the Rapier
    // swings much more often than the Warhammer, so the per-hit chance
    // is kept low to avoid near-permanent uptime).
    private static final float WEAKEN_CHANCE = 0.12f;

    // Weakness I, 1.5 seconds - a brief, frequently-refreshed nuisance
    // rather than a long lockdown, matching the "many small hits" rhythm
    // of a fast weapon.
    private static final int WEAKEN_AMPLIFIER = 0;
    private static final int WEAKEN_DURATION_TICKS = 30;

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof Player player)) {
            return;
        }
        if (player.getMainHandItem().getItem() != ModItems.PRISMIUM_ALLOY_RAPIER.get()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        if (ThreadLocalRandom.current().nextFloat() >= WEAKEN_CHANCE) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS, WEAKEN_DURATION_TICKS, WEAKEN_AMPLIFIER, false, true, true));
    }
}
