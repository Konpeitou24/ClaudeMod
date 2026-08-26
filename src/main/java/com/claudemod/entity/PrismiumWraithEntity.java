package com.claudemod.entity;

import com.claudemod.registry.ModEntities;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Wraith - ClaudeMod's first mob (session 12), and the opening move
 * of the mod concept's "new MOB" pillar. See PROGRESS.md session 12 notes
 * for the original design writeup, and session 38/session directly-after
 * (issue #5/#10) for the Peaceful-difficulty despawn history that shaped
 * {@code shouldDespawnInPeaceful} being left at its inherited default
 * below.
 *
 * <p><b>Rewritten (2026-08-26, repo owner direct request) to no longer
 * extend vanilla {@code Zombie}.</b> The original implementation extended
 * {@code Zombie} directly to get its AI/model for free; the reported cost
 * of that shortcut was a two-session saga (session 47, then this session)
 * where a Prismium Wraith left in water would eventually turn into a plain
 * vanilla Drowned, because {@code Zombie} (and, it turned out, the
 * {@link PrismiumDeepWraithEntity} class created to redirect that
 * conversion) both carry a hardcoded water-conversion timer that isn't
 * visible anywhere in this mod's own code. Full history and root-cause
 * writeup: PROGRESS.md, the "Prismium Wraith water-conversion" sections.
 * The repo owner's conclusion, quoted directly: "MOBを何かのMOBベースに
 * 作るからこんなことになるんじゃないですか?...なのでMOBは追加するので
 * あれば頑張って自作してほしいです(MOBのAIとかも)".
 *
 * <p>This class now extends {@link AbstractPrismiumMonster} (itself a thin,
 * neutral wrapper around vanilla {@code Monster} - see that class's
 * javadoc) and builds its AI out of generic {@code Goal} classes via
 * {@link #registerGoals()} rather than inheriting a fixed goal set. The
 * water-to-Deep-Wraith conversion this mob is known for is now hand-rolled
 * in {@link #aiStep()} using a plain tick counter, instead of relying on
 * {@code Zombie}'s internal (and, as this rewrite proves, leaky) machinery.
 * Daylight burning is preserved via {@link #isSunBurnTick()}, which is a
 * neutral {@code Mob}-level helper (not Zombie-specific) that vanilla
 * Zombie/Skeleton/etc. all call internally - reusing it carries none of the
 * hidden-behaviour risk that reusing the rest of {@code Zombie} did.
 *
 * <p>Rendering still reuses vanilla's humanoid body geometry (same
 * {@code ModelLayers.ZOMBIE} baked layer as before) via a plain
 * {@code HumanoidModel} instead of the Zombie-specific {@code ZombieModel}
 * (which is generically bound to {@code T extends Zombie} and could not be
 * used once this class stopped extending {@code Zombie}) - see {@link
 * com.claudemod.entity.client.PrismiumWraithRenderer}. Known, accepted
 * cosmetic trade-off: this mob no longer plays the distinctive "arms held
 * forward" zombie shamble animation, since that pose is baked into
 * {@code AbstractZombieModel#setupAnim} and that class shares the same
 * {@code T extends Zombie} restriction. It now animates with a plain
 * humanoid walk/arm-swing instead. Purely visual, not a behaviour change;
 * flagged in PROGRESS.md as a nice-to-have follow-up (a small custom model
 * class could reproduce the forward-arms pose without needing {@code
 * Zombie} as a bound) rather than blocking this rewrite on it.
 *
 * <p><b>Unverified</b>: like everything else in this mod, this rewrite has
 * not been confirmed in a running client. If this mob behaves noticeably
 * worse than before (worse pathfinding, doesn't notice/chase players as
 * readily, etc.), that is the first thing to check - see
 * {@link AbstractPrismiumMonster}'s javadoc for the general trade-off this
 * new pattern accepts.
 */
public class PrismiumWraithEntity extends AbstractPrismiumMonster {

    /**
     * How long (in ticks) this mob's eyes must stay submerged before it
     * converts into a {@link PrismiumDeepWraithEntity}. 600 ticks (30s) is
     * carried over unchanged from the threshold vanilla {@code Zombie}
     * itself uses before starting its (here, hand-rolled instead of
     * inherited) water-conversion countdown - picked for continuity with
     * the mob's previous behaviour, not re-derived from scratch.
     */
    private static final int WATER_CONVERSION_TICKS = 600;

    private int waterConversionTimer;

    public PrismiumWraithEntity(EntityType<? extends PrismiumWraithEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void registerGoals() {
        this.registerBasicMeleeGoals(1.0D);
    }

    // (shouldDespawnInPeaceful intentionally left at the inherited Monster
    // default of true - see this class's pre-rewrite javadoc history in
    // PROGRESS.md session 38/issue #10 for why an override here was tried
    // and reverted long before this rewrite. Monster's default is unrelated
    // to Zombie, so this rewrite doesn't disturb that fix.)

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.isAlive()) {
            if (this.isSunBurnTick()) {
                this.setSecondsOnFire(8);
            }
            tickWaterConversion();
        }
        super.aiStep();
    }

    /**
     * Hand-rolled replacement for the water-conversion timer this mob used
     * to get for free (and get burned by) via inherited {@code Zombie}
     * behaviour. Counts ticks spent with eyes underwater and, once past
     * {@link #WATER_CONVERSION_TICKS}, swaps this entity for a {@link
     * PrismiumDeepWraithEntity} via {@code Mob#convertTo} - the same
     * generic, non-Zombie-specific entity-swap helper {@code Zombie}
     * itself uses internally, just called directly instead of through a
     * Zombie-only override.
     */
    private void tickWaterConversion() {
        if (this.isEyeInFluid(FluidTags.WATER)) {
            this.waterConversionTimer++;
            if (this.waterConversionTimer >= WATER_CONVERSION_TICKS) {
                PrismiumDeepWraithEntity deepWraith =
                        this.convertTo(ModEntities.PRISMIUM_DEEP_WRAITH.get(), false);
                if (deepWraith != null && !this.isSilent()) {
                    this.level().levelEvent(null, 1040, this.blockPosition(), 0);
                }
            }
        } else {
            this.waterConversionTimer = 0;
        }
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
