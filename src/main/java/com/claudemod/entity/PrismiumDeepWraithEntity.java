package com.claudemod.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Deep Wraith - session 47, added at the repo owner's request as
 * the dedicated aquatic form {@link PrismiumWraithEntity} converts into
 * instead of a plain vanilla {@code Drowned}.
 *
 * <p><b>Rewritten (2026-08-26, repo owner direct request) to no longer
 * extend vanilla {@code Zombie}.</b> The session 47 fix redirected {@link
 * PrismiumWraithEntity}'s water conversion to spawn this class instead of
 * {@code Drowned} - but this class itself still extended {@code Zombie}
 * and never overrode its water-conversion timer, so a Deep Wraith (whose
 * entire habitat is underwater) would reliably hit that *inherited,
 * unredirected* timer and turn into a plain {@code Drowned} anyway, just
 * one extra silent hop later. That was patched in the previous session by
 * overriding {@code convertsInWater()} to {@code false}. This rewrite goes
 * further: this class no longer extends {@code Zombie} at all (see {@link
 * AbstractPrismiumMonster}'s javadoc for the full reasoning), so there is
 * no inherited conversion machinery left to disable in the first place -
 * this class simply never had any such code path to begin with.
 *
 * <p>Rendering still reuses vanilla's humanoid body geometry (same
 * {@code ModelLayers.ZOMBIE} baked layer as before) via a plain
 * {@code HumanoidModel} instead of the Zombie-specific {@code ZombieModel}
 * - see {@link com.claudemod.entity.client.PrismiumDeepWraithRenderer} and
 * {@link PrismiumWraithEntity}'s javadoc for the same cosmetic trade-off
 * (loses the "arms held forward" zombie shamble pose, gains nothing-hidden
 * behaviour).
 *
 * <p>Behavioural deltas from a plain melee ClaudeMod monster (see
 * {@link AbstractPrismiumMonster#registerBasicMeleeGoals(double)}):
 * <ul>
 *   <li>{@link #canBreatheUnderwater()} returns {@code true} so it never
 *   takes drowning damage and never itself tries to surface for air.</li>
 *   <li>Slightly more health/less raw hit strength than the land Wraith
 *   (unchanged from session 47's original tuning, still unverified in
 *   actual combat).</li>
 *   <li>Ambient/hurt/death sounds borrowed from vanilla's Guardian instead
 *   of the land Wraith's Vex sounds, for an "aquatic menace" register.</li>
 * </ul>
 *
 * <p>Deliberately <b>not</b> given real smooth-swimming pathfinding (no
 * custom {@code MoveControl}/{@code PathNavigation}) - this rewrite reuses
 * {@code WaterAvoidingRandomStrollGoal} from {@link
 * AbstractPrismiumMonster#registerBasicMeleeGoals}, which is a faithful
 * behavioural match for what this mob already did before (it inherited the
 * exact same goal from {@code Zombie}, unchanged, even though it's an odd
 * fit for a mob that lives in water) rather than a new regression. Giving
 * this mob water-seeking wander behaviour instead is flagged in
 * PROGRESS.md as a follow-up idea, not attempted in this rewrite.
 *
 * <p><b>Unverified</b>: like everything else in this mod, none of the
 * above has been confirmed in a running client.
 */
public class PrismiumDeepWraithEntity extends AbstractPrismiumMonster {

    public PrismiumDeepWraithEntity(EntityType<? extends PrismiumDeepWraithEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void registerGoals() {
        this.registerBasicMeleeGoals(1.0D);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GUARDIAN_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GUARDIAN_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.WITHER_SKELETON_STEP;
    }
}
