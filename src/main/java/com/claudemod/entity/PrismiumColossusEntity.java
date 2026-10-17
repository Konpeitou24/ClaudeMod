package com.claudemod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Colossus - ClaudeMod's eighth mob and this mod's first boss-tier
 * monster (scheduled session, 2026-10-17), opening the "各バイオーム固有
 * のボスを伴うダンジョン" roadmap item (PROGRESS.md TODO9 / section 5 item
 * 3) that every HANDOFF.md since v0.76.0 flagged as the next big direction
 * once the "完全パッシブcharm" family ran out of new vanilla effects to
 * grant.
 *
 * <p><b>Scope of this session, stated plainly</b>: this adds only the boss
 * entity itself (stats, AI, loot, a spawn egg) using exclusively this mod's
 * already-proven mob patterns (see below) - it does NOT yet add the
 * mountain-biome dungeon structure TODO9 envisions housing it in. Wiring
 * this mob into an actual structure/room generator is a materially
 * different, higher-risk kind of change (this mod has never generated a
 * multi-block structure before, only single-feature placements like
 * {@code PrismiumLandFeature}) that deserves its own session rather than
 * being rushed alongside a new entity. Until that lands, this mob is
 * deliberately *not* registered for natural spawn placement (see
 * {@code ModEntityEvents}, same choice already made for {@link
 * PrismiumDeepWraithEntity}) - it exists via spawn egg only, so nothing
 * about world generation changes this session and there is no risk of a
 * boss-tier mob wandering into an ordinary player's early game unannounced.
 *
 * <p>Follows {@link AbstractPrismiumMonster}'s established "no vanilla mob
 * base class" rule (see that class's javadoc for the repo owner's direct
 * request) and reuses the exact same goal set every prior melee mob in
 * this mod already uses successfully ({@link #registerBasicMeleeGoals}),
 * just with much heavier stats. Rendering reuses the same
 * {@code HumanoidModel} baked from {@code ModelLayers.ZOMBIE} that {@link
 * PrismiumWraithEntity}/{@link PrismiumDeepWraithEntity} already use
 * successfully (see {@link com.claudemod.entity.client.PrismiumColossusRenderer})
 * rather than attempting a bespoke "giant" model class - this mod cannot
 * locally render/verify a new model's geometry, so every other mob so far
 * has stuck to reusing an already-baked vanilla layer, and this boss
 * follows the same discipline. The "giant" read is carried by this mob's
 * {@code .sized(1.2F, 2.7F)} hitbox (see {@code ModEntities}, a plain
 * builder call with no override risk) and its texture/stats/particle aura,
 * not by a scaled-up render - a known, accepted cosmetic trade-off already
 * precedented by {@link PrismiumWispEntity}'s renderer javadoc (model
 * geometry not perfectly matching the hitbox is fine, not a bug).
 *
 * <p>Two small behaviours are hand-rolled in {@link #aiStep()}, both built
 * only from generic {@code LivingEntity}/{@code ServerLevel} methods this
 * mod has already used successfully elsewhere (see {@link
 * com.claudemod.event.PrismiumFamiliarAuraHandler} for the identical
 * {@code ServerLevel#sendParticles} / {@code LivingEntity#addEffect} calls
 * reused here) rather than any new, unverified API:
 * <ul>
 *   <li>A faint ambient particle aura (every 10 ticks, server-side only)
 *   so the boss reads as something otherworldly even before it attacks.</li>
 *   <li>A one-time "enrage" trigger once health drops below a third of
 *   max: grants itself Strength I and Speed I for the rest of the fight
 *   and plays a roar. Implemented as a plain boolean flag plus
 *   {@code this.addEffect(...)} (the exact same method {@link
 *   com.claudemod.event.PrismiumFamiliarAuraHandler} already calls on a
 *   {@code Player}, here called on {@code this} instead - both are
 *   {@code LivingEntity}, so this is not a new API surface, just a new
 *   caller) - deliberately not persisted across save/load (same
 *   risk-tolerance precedent as {@link PrismiumWraithEntity}'s
 *   non-persisted {@code waterConversionTimer}), so a reloaded world
 *   resets the enrage state; acceptable since it is purely a combat-feel
 *   flourish, not game-breaking if it resets.</li>
 * </ul>
 *
 * <p><b>Unverified</b>: like every other mob in this mod, this has not
 * been confirmed in a running client (this sandbox cannot locally
 * build/playtest - see PROGRESS.md). Balance numbers (150 HP / 14 attack
 * damage / heavy knockback resistance) are a first guess at "boss that
 * takes a real fight, but is still soloable with decent gear" and are
 * flagged in PROGRESS.md as needing real playtesting before they are
 * trusted.
 */
public class PrismiumColossusEntity extends AbstractPrismiumMonster {

    /** How often (in ticks) the idle particle aura emits. */
    private static final int AURA_PARTICLE_INTERVAL_TICKS = 10;

    /** Enrage threshold: a fraction of max health. */
    private static final float ENRAGE_HEALTH_FRACTION = 1.0F / 3.0F;

    /** Duration (ticks) of the one-shot enrage buff; long enough to cover
     * the rest of a realistic fight without needing to be refreshed. */
    private static final int ENRAGE_EFFECT_DURATION_TICKS = 6000;

    private boolean enraged;

    public PrismiumColossusEntity(EntityType<? extends PrismiumColossusEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * First boss-tier attribute set in this mod. Numbers are a first guess
     * (see class javadoc's "Unverified" paragraph) rather than tuned
     * against real combat: roughly 6x a vanilla Zombie's health, double a
     * Zombie's attack damage, near-immune to knockback (a "heavy" boss
     * should not be flung around by a single hit), and deliberately
     * slower than every other ClaudeMod melee mob so a player can
     * disengage if underprepared.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.registerBasicMeleeGoals(1.0D);
    }

    // shouldDespawnInPeaceful is deliberately left at the inherited
    // Monster default - see PrismiumWraithEntity's javadoc for the full
    // GitHub issue #5/#10 history of why overriding this is a known trap.
    // A boss despawning on Peaceful is also the correct behaviour anyway
    // (it is still a Monster), so there is no reason to touch this here.

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.isAlive()) {
            tickAuraParticles();
            tickEnrage();
        }
        super.aiStep();
    }

    /**
     * Faint ambient glow so the boss reads as supernatural even before it
     * notices a player. Reuses the exact {@code ServerLevel#sendParticles}
     * call {@link com.claudemod.event.PrismiumFamiliarAuraHandler} already
     * uses successfully in this codebase (see that class), just triggered
     * from the mob's own tick instead of a player-tick event handler.
     */
    private void tickAuraParticles() {
        if (this.level().getGameTime() % AURA_PARTICLE_INTERVAL_TICKS != 0) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + this.getBbHeight() * 0.6D, this.getZ(),
                2, this.getBbWidth() * 0.4D, this.getBbHeight() * 0.3D, this.getBbWidth() * 0.4D, 0.01D);
    }

    /**
     * One-shot "second phase" trigger: once health drops under {@link
     * #ENRAGE_HEALTH_FRACTION} of max, grant Strength I + Speed I for the
     * rest of the fight. Deliberately simple (a boolean flag, checked
     * every tick while alive) rather than anything event-driven, mirroring
     * {@link PrismiumWraithEntity#tickWaterConversion()}'s hand-rolled
     * tick-counter style.
     */
    private void tickEnrage() {
        if (this.enraged) {
            return;
        }
        if (this.getHealth() <= 0.0F) {
            return;
        }
        if (this.getHealth() / this.getMaxHealth() <= ENRAGE_HEALTH_FRACTION) {
            this.enraged = true;
            this.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST, ENRAGE_EFFECT_DURATION_TICKS, 0, false, true, true));
            this.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, ENRAGE_EFFECT_DURATION_TICKS, 0, false, true, true));
            if (!this.isSilent()) {
                // Reuses the same WITHER_HURT constant getHurtSound()
                // below already references (rather than introducing a
                // fourth distinct SoundEvents field), just played lower
                // and louder for a one-off "roar" read.
                this.playSound(SoundEvents.WITHER_HURT, 1.0F, 0.6F);
            }
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
