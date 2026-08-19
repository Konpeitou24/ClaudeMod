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
 * Session 69 (scheduled): the Warhammer's gimmick, following the exact
 * same shape as {@link PrismiumSwordHandler} (the Sword's Glowing-on-hit
 * effect) rather than inventing a new API surface. See {@link
 * com.claudemod.registry.ModItems#PRISMIUM_WARHAMMER} for the item
 * registration/stat rationale (heavy damage, very slow speed - the mod's
 * first weapon built around Prismium Ingot rather than Prismium Shard,
 * finally giving the Ingot - added session 68, unused until now - a
 * crafting purpose).
 *
 * <p>Landing a hit with the Warhammer equipped has a
 * {@value #STAGGER_CHANCE}-in-1 chance to inflict a short Slowness on the
 * target, flavor-wise representing a "stagger" from the heavy blow. Kept
 * at a much higher chance than the Sword's {@code GLOW_CHANCE} (0.15)
 * because the Warhammer's own attack speed (see ModToolTiers/ModItems) is
 * roughly a third of the Sword's, so hits themselves are already rare -
 * this keeps the "on-hit gimmick" from feeling like it never happens in
 * practice.
 *
 * <p>Deliberately reuses the identical {@link LivingHurtEvent} hook,
 * {@code DamageSource#getEntity()} main-hand-item check, and
 * client/server guard that {@link PrismiumSwordHandler} already
 * establishes and that has shipped across many sessions without a
 * reported issue - see that class's own javadoc for the API rationale
 * this borrows rather than re-deriving.
 *
 * <p><b>Unverified</b>: no in-game playtest (no Minecraft client in this
 * sandbox, see PROGRESS.md's standing note). In particular whether a
 * 0.6-attacks/second weapon still reliably drives {@code LivingHurtEvent}
 * once per swing (no reason to expect otherwise - attack speed only
 * affects how often the player *can* swing, not whether a landed hit
 * fires the event) and whether Slowness II for 2 seconds reads as
 * "satisfying stagger" rather than "unnoticeable" or "excessive" in an
 * actual fight are both untested balance judgment calls.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumWarhammerHandler {

    // Much higher than the Sword's 0.15 - see class javadoc for why
    // (the Warhammer swings far less often, so the per-hit chance is
    // raised to compensate).
    private static final float STAGGER_CHANCE = 0.5f;

    // Slowness II, 2 seconds - noticeable mid-fight without being a
    // long-lasting lockdown.
    private static final int STAGGER_AMPLIFIER = 1;
    private static final int STAGGER_DURATION_TICKS = 40;

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof Player player)) {
            return;
        }
        if (player.getMainHandItem().getItem() != ModItems.PRISMIUM_WARHAMMER.get()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        if (ThreadLocalRandom.current().nextFloat() >= STAGGER_CHANCE) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, STAGGER_DURATION_TICKS, STAGGER_AMPLIFIER, false, true, true));
    }
}
