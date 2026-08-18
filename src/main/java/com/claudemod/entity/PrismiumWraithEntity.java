package com.claudemod.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Wraith - ClaudeMod's first mob (session 12), and the opening move
 * of the mod concept's "new MOB" pillar which had been completely untouched
 * across 11 prior sessions (blocks/energy/equipment all existed, no living
 * entity did). See PROGRESS.md session 12 notes for the full design writeup.
 *
 * Implementation choice: rather than writing a brand new AI/model/renderer
 * stack from scratch (high risk in a sandbox that cannot locally compile or
 * playtest), this extends vanilla {@link Zombie} directly. That means it
 * inherits Zombie's entire proven AI goal set (melee attack, target nearest
 * player, sunlight-burning, water avoidance, etc.) for free, and can reuse
 * vanilla's ZombieModel for rendering (see PrismiumWraithRenderer) with only
 * a custom texture swapped in. The only behavioural changes made here are:
 * - Reworked attributes (tankier, harder hitting than a plain zombie, to
 *   read as a "guardian" rather than a generic shambler).
 * - No default equipment (populateDefaultEquipmentSlots is a no-op) so it
 *   never randomly spawns holding vanilla iron gear, which would clash with
 *   the Prismium theme.
 * - Distinct ambient/hurt/death sounds (borrowed from Vex) for a more
 *   otherworldly feel than the default zombie groan.
 * Burning in daylight is intentionally left as inherited default behaviour:
 * it reinforces the intended flavor of a guardian that lurks in caves near
 * Prismium ore and punishes players who drag a fight up to the surface.
 */
public class PrismiumWraithEntity extends Zombie {

    public PrismiumWraithEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    /**
     * GitHub issue #5 (session 38) reported "spawns via spawn egg then
     * vanishes immediately" on a Peaceful-difficulty world. Session 38's
     * diagnosis was correct (vanilla {@code Monster} subclasses despawn
     * instantly on Peaceful via {@code shouldDespawnInPeaceful() == true},
     * regardless of spawn method), but its fix - overriding that method to
     * {@code false} so the Wraith would never despawn on Peaceful - was a
     * misdiagnosis of what needed fixing: that despawn behaviour is not a
     * ClaudeMod bug, it is the same standard, expected vanilla rule every
     * hostile mob follows (using a hostile-mob spawn egg on a
     * Peaceful-difficulty world despawns the mob instantly - this is
     * intentional upstream behaviour, confirmed by the very same Forge
     * Forums thread cited in the old revision of this javadoc, where the
     * "bug" turned out to be the reporter's world being on Peaceful).
     *
     * That override then caused a real regression, reported directly by
     * the repo owner as GitHub issue #10 ("ピースフルでレイスがスポーンして
     * しまう" - a Wraith ends up existing/visible on a Peaceful world):
     * because the override made the Wraith immune to the peaceful despawn
     * sweep, any Wraith already alive when a player switched their world
     * to Peaceful (or spawned one via egg while on Peaceful) would now
     * stick around indefinitely instead of vanishing like every other
     * hostile mob - which reads as "a hostile mob spawns/persists even on
     * Peaceful", clearly not the intended behaviour for a plain
     * MobCategory.MONSTER entity.
     *
     * Fix (this session): removed the override entirely, restoring the
     * inherited {@code Monster} default ({@code true}). The Wraith now
     * despawns on Peaceful exactly like a vanilla Zombie, matching player
     * expectations and closing issue #10. Natural spawning was never the
     * problem in either direction - {@link com.claudemod.event.ModEntityEvents}
     * already registers {@code Monster::checkMonsterSpawnRules} (which
     * itself refuses to naturally spawn anything while
     * {@code Difficulty.PEACEFUL}) as this entity's spawn placement
     * predicate, so natural overworld/Prism Realm spawning was already
     * correctly gated; only spawn-egg/summon-triggered instances plus the
     * "already alive, difficulty changed under it" case were affected by
     * this override, and both are fixed by removing it.
     * <b>Unverified</b>: like all worldgen/entity behaviour in this mod,
     * not confirmed in an actual running game client from this sandbox -
     * if a hostile-mob-on-Peaceful report resurfaces after this change,
     * re-open this method as the first thing to check.
     */
    // (shouldDespawnInPeaceful intentionally left at the inherited Monster
    // default of true - see the javadoc above for why an override here was
    // tried and reverted.)

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Deliberately empty: a Prismium Wraith should never spawn holding
        // random vanilla armor/weapons like a normal Zombie can. Known
        // limitation (documented in PROGRESS.md): this also means it cannot
        // "pick up" dropped items mid-fight the way a zombie can, since we
        // never give it starting gear to build on - considered an acceptable
        // trade-off for staying on-theme.
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VEX_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.WITHER_SKELETON_STEP;
    }
}
