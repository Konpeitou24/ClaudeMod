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
 * Session 6: the Sword's gimmick, the last of the five Prismium tools to
 * get one (see {@link PrismiumMiningHandler} for Pickaxe/Axe/Shovel and
 * {@link PrismiumHoeHandler} for the Hoe's).
 *
 * <p>Landing a hit with the Prismium Sword equipped has a
 * {@value #GLOW_CHANCE}-in-1 chance to inflict a short Glowing effect on
 * the target, on top of the normal damage. Flavor/design intent: this is
 * an exploration-and-combat aid (see-through-walls outline makes it easier
 * to track a target that flees, e.g. into foliage or around a corner)
 * rather than a damage-number power spike, matching the rest of the set's
 * "utility over raw stats" design language (see armor's Night Vision/Water
 * Breathing in {@link ArmorSetBonusHandler}).
 *
 * <p>Hooks {@link LivingHurtEvent} (confirmed against MinecraftForge's own
 * 1.20.x branch source this session): fired for both PvP and PvE, carries
 * the {@link DamageSource} (whose {@code getEntity()} is the direct cause
 * of damage - the attacking player, for a melee sword hit) and the hurt
 * entity via the inherited {@code getEntity()} from {@code LivingEvent}.
 * Applied only server-side (checked via the hurt entity's level) so the
 * effect syncs to clients the normal way rather than being applied
 * redundantly on both sides.
 *
 * <p><b>Unverified</b>: no in-game playtest (no Minecraft client in this
 * sandbox). In particular: whether {@code DamageSource#getEntity()}
 * reliably returns the attacking player for a plain melee hit in 1.20.1
 * (versus, say, only being populated for projectile/indirect sources) is
 * based on long-standing, stable vanilla behavior but has not been
 * exercised by a real fight in this session.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumSwordHandler {

    // 1 in ~7 hits (kept modest since, unlike the Pickaxe/Axe/Shovel
    // gimmicks, this can trigger many times per second in combat).
    private static final float GLOW_CHANCE = 0.15f;

    // 5 seconds - enough to matter mid-fight/mid-chase without lingering
    // long after.
    private static final int GLOW_DURATION_TICKS = 100;

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof Player player)) {
            return;
        }
        if (player.getMainHandItem().getItem() != ModItems.PRISMIUM_SWORD.get()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        if (ThreadLocalRandom.current().nextFloat() >= GLOW_CHANCE) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                MobEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, true, true));
    }
}
