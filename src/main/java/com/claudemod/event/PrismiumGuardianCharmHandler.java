package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Session 30: server-side {@link LivingDeathEvent} listener implementing
 * {@link com.claudemod.item.PrismiumGuardianCharmItem}'s entire behavior -
 * see that class's javadoc for why this couldn't be a simple {@code Item}
 * method override the way Shield/Bow's gimmicks are.
 *
 * <p><b>Why {@code LivingDeathEvent} and not vanilla's totem hook</b>:
 * confirmed this session (by cloning the real MinecraftForge 1.20.1 repo
 * and reading {@code LivingEntity.java.patch} directly rather than
 * guessing) that vanilla's totem-of-undying save happens *earlier*, inside
 * the damage-application path, hard-coded to
 * {@code Items.TOTEM_OF_UNDYING} and not redirectable to a custom item.
 * {@code LivingDeathEvent} fires later, right at the top of
 * {@code LivingEntity#die} (via {@code ForgeHooks.onLivingDeath}) - by the
 * time it fires, the entity is *actually* about to die (any vanilla totem
 * already had its chance and either wasn't present or was already
 * consumed by an earlier hit). Cancelling this event is documented Forge
 * behavior for "stop this entity from dying" and is a well-established
 * pattern for custom extra-life items in the wider modding ecosystem.
 *
 * <p>The revive numbers (Regeneration II for 900 ticks/45s, Absorption
 * II for 100 ticks/5s, Fire Resistance I for 800 ticks/40s) intentionally
 * mirror vanilla's own totem-of-undying effect durations/amplifiers
 * rather than inventing new ones, so the charm reads as "our totem"
 * rather than a mechanically unrelated new effect.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, see
 * PROGRESS.md's standing note on this): whether cancelling {@code
 * LivingDeathEvent} here actually leaves the entity in a fully sane state
 * with no leftover death-adjacent bookkeeping (death is prevented at the
 * very first check inside {@code die()} per the source read above, so in
 * theory nothing has run yet - but this has never been confirmed against
 * a live server); whether {@link ParticleTypes#TOTEM_OF_UNDYING} and
 * {@link SoundEvents#TOTEM_USE} are the correct 1.20.1 field names (high
 * confidence, long-stable vanilla identifiers, but not cross-checked
 * against decompiled source the way the death-event ordering was this
 * session - see PROGRESS.md); and whether skipping vanilla's
 * item-activation screen flash (the golden totem icon pop-up, which is
 * tied to the hard-coded totem item and deliberately not reproduced here)
 * makes the save feel abrupt/unclear in practice compared to a real totem.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumGuardianCharmHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // Same guard vanilla's own totem check applies (e.g. void
            // damage, /kill) - a charm that could save you from anything
            // with no exception would be a strictly-better vanilla totem,
            // which isn't the intent here.
            return;
        }

        ItemStack charm = ItemStack.EMPTY;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (stack.is(ModItems.PRISMIUM_GUARDIAN_CHARM.get())) {
                charm = stack;
                break;
            }
        }

        if (charm.isEmpty()) {
            return;
        }

        event.setCanceled(true);
        charm.shrink(1);

        entity.setHealth(1.0F);
        entity.removeAllEffects();
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0D, entity.getZ(),
                    30, 0.4D, 0.4D, 0.4D, 0.15D);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
