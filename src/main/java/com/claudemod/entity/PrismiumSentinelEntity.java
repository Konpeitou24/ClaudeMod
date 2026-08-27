package com.claudemod.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Sentinel - ClaudeMod's third mob, the mod's ranged attacker. See
 * the pre-rewrite version of this class (git history) for the original
 * session-58 design writeup that picked vanilla {@code Skeleton} as a base.
 *
 * <p><b>Rewritten (session following v0.24.0, continuing the "no vanilla
 * mob base classes" project) to no longer extend vanilla {@code Skeleton}.
 * </b> Same rationale as {@link PrismiumWraithEntity}'s rewrite (see that
 * class's javadoc for the repo owner's direct request and the water-
 * conversion bug that originally motivated it): a concrete vanilla mob
 * class carries hidden, species-specific behaviour this mod's own code
 * never writes and can't audit just by reading this file. Skeleton hadn't
 * been caught misbehaving yet, but the fix is applied proactively here
 * rather than waiting for a bug report, per the repo owner's "既存モブも
 * 順次置き換え" (replace the existing mobs too, one at a time) decision.
 *
 * <p>Now extends {@link AbstractPrismiumMonster} and implements
 * {@link RangedAttackMob} directly (the same interface {@code Skeleton}
 * itself implements) instead of inheriting it. {@code
 * AbstractPrismiumMonster#populateDefaultEquipmentSlots} is overridden
 * again here to hand this mob a plain vanilla bow - without that override,
 * it would inherit the "spawn with nothing" empty override and be a
 * harmless, weaponless archer. AI is a hand-picked set of generic
 * {@code Goal}s: {@link RangedBowAttackGoal} (bound to
 * {@code <T extends Mob & RangedAttackMob>}, not to {@code Skeleton}
 * specifically - confirmed via javadoc before use) plus the same
 * float/stroll/look goals {@link AbstractPrismiumMonster} already uses for
 * melee mobs. {@link #performRangedAttack} is a from-scratch
 * implementation (spawn a vanilla {@link Arrow}, aim it at the target,
 * shoot) rather than an inherited {@code Skeleton} method - functionally
 * equivalent to what {@code Skeleton} itself does, just written directly
 * so this class owns 100% of its own behaviour.
 *
 * <p>Rendering is unaffected by this rewrite: {@code SkeletonModel<T>} is
 * bound to {@code <T extends Mob & RangedAttackMob>} (confirmed via
 * javadoc), not to {@code Skeleton} itself, so
 * {@link com.claudemod.entity.client.PrismiumSentinelRenderer} did not
 * need any changes - this mob keeps its vanilla skeleton body geometry and
 * bow-draw animation exactly as before.
 *
 * <p><b>Unverified</b>: this rewrite has not been confirmed in a running
 * client (this sandbox cannot locally build/playtest - see PROGRESS.md).
 * If this mob's archery (aim, kiting distance, reaction time) feels
 * noticeably worse than before, {@link RangedBowAttackGoal}'s constructor
 * parameters (move speed / attack interval / attack radius) below are the
 * first thing to tune.
 */
public class PrismiumSentinelEntity extends AbstractPrismiumMonster implements RangedAttackMob {

    public PrismiumSentinelEntity(EntityType<? extends PrismiumSentinelEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Numbers carried over unchanged from the pre-rewrite version (see git
     * history) - a bit tankier and faster than a vanilla Skeleton (20 HP /
     * 0.25 speed), unchanged by this session's rewrite since it only
     * touches the base class, not the balance figures.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 18.0D);
    }

    /**
     * Overrides {@link AbstractPrismiumMonster}'s empty default (see that
     * class's javadoc) specifically for this mob: a ranged attacker is
     * useless without a bow in hand, since both {@link RangedBowAttackGoal}
     * and {@link #performRangedAttack} below assume one is equipped.
     */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Hand-written replacement for the ranged attack this mob used to get
     * for free via inherited {@code Skeleton} behaviour. Spawns a plain
     * vanilla {@link Arrow} (see {@link PrismiumSentinelEntity}'s
     * pre-rewrite javadoc for why a themed "Prismium Arrow" projectile has
     * been deferred across prior sessions - the exact UV layout
     * {@code ArrowRenderer} expects still hasn't been confirmed) and aims
     * it at the target using the same trajectory math vanilla ranged mobs
     * use (lead the target slightly upward based on horizontal distance,
     * scale spread by world difficulty).
     */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        AbstractArrow arrow = new Arrow(this.level(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontalDistance * 0.20000000298023224D, dz, 1.6F,
                (float) (14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    // shouldDespawnInPeaceful is deliberately left at the inherited
    // Monster default (true) - see PrismiumWraithEntity's javadoc for the
    // full GitHub issue #5/#10 history of why overriding this is a known
    // trap.

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
