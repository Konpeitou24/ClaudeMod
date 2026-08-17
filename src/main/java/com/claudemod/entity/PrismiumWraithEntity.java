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
