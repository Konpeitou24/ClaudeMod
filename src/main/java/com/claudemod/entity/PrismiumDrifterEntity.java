package com.claudemod.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Drifter - ClaudeMod's fourth mob, its only non-combat /
 * "environmental" entity. See the pre-rewrite version of this class (git
 * history) for the original session-60 design writeup that picked vanilla
 * {@link net.minecraft.world.entity.animal.Squid} as a base.
 *
 * <p><b>Rewritten (session following v0.24.0, continuing the "no vanilla
 * mob base classes" project) to no longer extend vanilla {@code Squid}.
 * </b> Same rationale as {@link PrismiumWraithEntity} and
 * {@link PrismiumSentinelEntity} (see their javadocs): reusing a concrete
 * vanilla mob class means silently inheriting whatever species-specific
 * behaviour lives inside it. This mob was never observed misbehaving the
 * way Wraith was, but per the repo owner's "既存モブも順次置き換え"
 * decision it is rewritten proactively rather than waiting on a bug
 * report.
 *
 * <p>Unlike {@link PrismiumWraithEntity}/{@link PrismiumSentinelEntity}
 * (both extend {@link AbstractPrismiumMonster}, i.e. vanilla {@code
 * Monster}), this is ClaudeMod's first entity extending {@code
 * PathfinderMob} directly - per {@link AbstractPrismiumMonster}'s own
 * javadoc ("non-hostile mobs should extend {@code PathfinderMob}/
 * {@code Mob} directly"), a passive drifting fish has no business being a
 * {@code Monster}/{@code Enemy}. AI is entirely generic, non-aquatic-
 * specific {@code Goal}s: {@link RandomSwimmingGoal} (the same goal
 * vanilla's own fish - Cod/Salmon/TropicalFish - use for their drifting
 * movement, confirmed via javadoc to be declared against plain
 * {@code PathfinderMob}, not any fish-specific class) plus a
 * {@link PanicGoal} so it still flees when attacked, matching the
 * "harmless but not defenseless" read the original {@code Squid}-based
 * version aimed for via Squid's inherited ink-cloud reaction. This mob has
 * no target selector goals at all - it never attacks or retaliates.
 *
 * <p>Swims (rather than walks) via {@link WaterBoundPathNavigation} (the
 * same generic navigation class vanilla fish/dolphins use) plus
 * {@link #canBreatheUnderwater()}/{@link #isPushedByFluid()} overrides.
 * Known, accepted trade-off versus the old {@code Squid}-based version:
 * this mob no longer has {@code Squid}'s specific ink-cloud-on-damage
 * particle reaction, or its distinctive tumbling swim-rotation animation
 * ({@code SquidRenderer#setupRotations}, which is why
 * {@link com.claudemod.entity.client.PrismiumDrifterRenderer} already
 * used a plain {@code MobRenderer} instead of the Squid-specific renderer
 * even before this rewrite - see that class's javadoc). Purely cosmetic,
 * not a behaviour change.
 *
 * <p><b>Unverified</b>: this rewrite has not been confirmed in a running
 * client (this sandbox cannot locally build/playtest - see PROGRESS.md).
 * This is also the first ClaudeMod mob whose swimming movement is entirely
 * hand-assembled from generic goals/navigation rather than inherited
 * wholesale, so if it moves noticeably worse than the old Squid-based
 * version (gets stuck, doesn't drift naturally, etc.), that is the first
 * thing to investigate.
 */
public class PrismiumDrifterEntity extends PathfinderMob {

    public PrismiumDrifterEntity(EntityType<? extends PrismiumDrifterEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    /**
     * Unchanged from the pre-rewrite version (see git history): slightly
     * tankier than a vanilla Squid (10 HP) to survive a little longer
     * against Prism Realm's own hostile mobs before fleeing. Base builder
     * switched from {@code Squid.createAttributes()} to the neutral
     * {@link Mob#createMobAttributes()} since this class no longer extends
     * {@code Squid} - same base attribute set (max health, follow range,
     * knockback resistance, movement speed, armor/armor toughness) either
     * way.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 1.0D, 20));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        // Deliberately no targetSelector goals: this mob is purely
        // passive/environmental and should never attack or retaliate,
        // matching the old Squid-based version's behaviour.
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GLOW_SQUID_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GLOW_SQUID_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLOW_SQUID_DEATH;
    }
}
