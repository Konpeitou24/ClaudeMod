package com.claudemod.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Sentinel - ClaudeMod's third mob and second brand-new one added
 * to the Prism Realm roster (Prismium Wraith/session 12 and Prismium Deep
 * Wraith/session 47 are both melee; this is the mod's first ranged
 * attacker). Session's response to PROGRESS.md handoff item 9(i), raised
 * in priority across sessions 57-58 for "quality vs. quantity" balance
 * after two consecutive sessions of pure fixes/UX work with no new
 * content.
 *
 * <p>Implementation choice, following the same low-risk precedent
 * {@link PrismiumWraithEntity} set (see that class's javadoc): rather than
 * building new ranged-attack AI from scratch in a sandbox with no way to
 * compile or playtest, this extends vanilla {@link Skeleton} directly and
 * inherits its entire proven goal set (bow AI, strafing/kiting behavior,
 * melee fallback when cornered) for free. {@link #populateDefaultEquipmentSlots}
 * is deliberately left un-overridden (unlike Wraith's empty override) -
 * a Skeleton's ranged AI is conditioned on actually holding a bow, so
 * removing the default equipment here would silently produce an unarmed,
 * non-functional archer. This also means, for now, Prismium Sentinel
 * fires plain vanilla arrows rather than a themed projectile - GitHub
 * issue-adjacent PROGRESS.md item 9(d) (a themed "Prismium Arrow") has
 * been deferred across three prior sessions specifically because the
 * exact UV layout {@code ArrowRenderer} expects could not be confirmed;
 * reusing the stock arrow here sidesteps that unresolved risk entirely
 * rather than re-attempting it blind.
 *
 * <p>Sound choice: {@link SoundEvents#ILLUSIONER_AMBIENT}/HURT/DEATH for
 * an otherworldly "casting a curse" feel distinct from the family's other
 * two mobs (Wraith uses Vex sounds, Deep Wraith uses Guardian sounds - see
 * those classes). Footstep sound is left at the inherited
 * {@link Skeleton} default (bone rattle) rather than overridden, since it
 * already fits a skeletal frame regardless of retexturing.
 */
public class PrismiumSentinelEntity extends Skeleton {

    public PrismiumSentinelEntity(EntityType<? extends Skeleton> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Numbers are first-guess, unbalanced, like every other mob/equipment
     * stat in this mod (see PROGRESS.md) - a bit tankier and faster than a
     * vanilla Skeleton (20 HP / 0.25 speed) to read as a dedicated
     * "sentinel" guarding the Prism Realm rather than a reskinned
     * overworld skeleton, without going as far as Prismium Wraith's much
     * larger melee stat bumps (a ranged attacker that also hits as hard
     * as a tanky melee brute would be considerably harder to balance
     * blind).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 18.0D);
    }

    // shouldDespawnInPeaceful is deliberately left at the inherited
    // Monster default (true) - see PrismiumWraithEntity's javadoc for the
    // full GitHub issue #5/#10 history of why overriding this is a known
    // trap: hostile mobs despawning instantly when spawned/left alive on
    // a Peaceful-difficulty world is standard, expected vanilla behavior,
    // not a bug to "fix" here.

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ILLUSIONER_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ILLUSIONER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ILLUSIONER_DEATH;
    }
}
