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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
 *
 * <p><b>Since the scheduled 2026-10-23 session</b>, this exact class is
 * also registered a second time as {@code
 * com.claudemod.registry.ModEntities#PRISMIUM_MOUNTAIN_COLOSSUS} (closing
 * out PROGRESS.md TODO53's "各バイオームに固有のボス" piece): identical
 * stats/AI/loot, only a different {@code EntityType} id/spawn egg and a
 * distinct weathered-stone/moss texture (see {@code
 * PrismiumColossusRenderer}), spawned by {@code PrismiumDungeonFeature}'s
 * mountain-biome registration instead of {@code PRISMIUM_COLOSSUS}.
 *
 * <p><b>Since the scheduled 2026-10-24 session</b>, a third hand-rolled
 * behaviour was added to {@link #aiStep()}: a "ground slam" shockwave that
 * fires periodically (see {@link #SLAM_COOLDOWN_TICKS}) once {@link
 * #enraged} is true, pushing every nearby {@link Player} back and
 * applying a brief Slowness, giving the "second phase" (PROGRESS.md
 * HANDOFF.md option (w), "Prismium Colossusの第二形態・特殊攻撃の拡充")
 * something beyond the plain Strength/Speed self-buff it had since
 * v0.77.0. Deliberately built from only API calls this codebase already
 * has a compiling precedent for elsewhere, rather than anything newly
 * assumed - the exact failure mode this mod's "未確認のJava API" rule
 * (PROGRESS.md section 1) exists to prevent:
 * <ul>
 *   <li>{@code Level#getEntitiesOfClass(Class, AABB, Predicate)} - same
 *   method already called with three different {@code Class} arguments
 *   elsewhere in this codebase ({@code ItemEntity} in {@link
 *   com.claudemod.event.PrismiumMagnetCharmHandler}, {@code
 *   PrismiumFamiliarEntity} in {@link com.claudemod.event.PrismiumFamiliarAuraHandler},
 *   {@code Mob} in {@link com.claudemod.item.PrismiumPulseCharmItem}); a
 *   fourth {@code Class} argument ({@code Player}) is the same generic
 *   method, not a new API surface.</li>
 *   <li>{@code Entity#getBoundingBox()}/{@code #inflate(double)} and
 *   {@code Entity#position()} - called on {@code this} here, but already
 *   called on a {@code Player} receiver by {@link
 *   com.claudemod.event.PrismiumMagnetCharmHandler}; both methods are
 *   defined on the base {@code Entity} class (that handler's own javadoc
 *   makes exactly this "no new surface, just a new receiver" argument
 *   already), and {@code PrismiumColossusEntity} is an {@code Entity}
 *   just like {@code Player} is.</li>
 *   <li>{@code Entity#getDeltaMovement()}/{@code #setDeltaMovement(Vec3)}
 *   plus flipping the public {@code hurtMarked} field - the exact
 *   push-and-resync pattern {@link com.claudemod.event.PrismiumMagnetCharmHandler}
 *   already uses, just pushing a {@code Player} away from a point instead
 *   of pulling an {@code ItemEntity} toward one.</li>
 *   <li>{@code LivingEntity#addEffect(MobEffectInstance)} called on a
 *   {@code Player} target rather than {@code this} - already proven both
 *   by {@link com.claudemod.event.PrismiumFamiliarAuraHandler} (applies
 *   Regeneration to the nearby player) and by {@link
 *   com.claudemod.item.PrismiumPulseCharmItem} (applies Glowing to every
 *   nearby hostile {@code Mob} it finds) - this method already compiles
 *   on both directions (self and other), so calling it on a scanned
 *   {@code Player} here is not new.</li>
 *   <li>{@code MobEffects.MOVEMENT_SLOWDOWN} (vanilla Slowness) - already
 *   a compiling field reference elsewhere in this codebase (grepped this
 *   session to confirm rather than assumed), just newly applied to a
 *   target other than the item/charm that originally used it.</li>
 *   <li>{@code ParticleTypes.SONIC_BOOM} - already used by {@link
 *   com.claudemod.item.PrismiumPulseCharmItem} for its own detection
 *   pulse; reused here purely for its "shockwave" visual read, with no
 *   gameplay meaning borrowed from that item.</li>
 * </ul>
 * No new {@code @Override} and no new unverified method signature is
 * introduced by this feature at all - every call above already compiles
 * somewhere else in this exact repository. Deliberately restricted to
 * {@link Player} targets only (not every {@code LivingEntity}) so the
 * slam cannot shove around the boss's own future allies or unrelated
 * passive mobs, mirroring {@code PrismiumPulseCharmItem}'s own choice to
 * scope its scan rather than grabbing every entity indiscriminately.
 * {@code slamCooldown} is a plain {@code int} field, not persisted across
 * save/load - the same risk-tolerance precedent {@link #enraged} already
 * documents above (a reloaded world just starts the cooldown over, purely
 * a combat-feel detail, not game-breaking).
 *
 * <p><b>Since the scheduled 2026-10-25 session</b>, a fourth hand-rolled
 * behaviour was added to {@link #aiStep()}: a "roar" ({@link #tickRoar()})
 * that periodically blinds every nearby {@link Player} once {@link
 * #enraged}, picking up HANDOFF.md's other carried-over option (ee)
 * ("咆哮による一時的な視界阻害") alongside the ground slam added the prior
 * session. Deliberately runs on its own independent cooldown/radius/timer
 * (see {@link #ROAR_COOLDOWN_TICKS}/{@link #ROAR_INITIAL_DELAY_TICKS}/
 * {@link #ROAR_RADIUS}) rather than being folded into the slam, so the two
 * attacks interleave at different moments of a fight instead of always
 * landing together. Exactly the same "only already-compiling API calls"
 * discipline as the ground slam:
 * <ul>
 *   <li>{@code Level#getEntitiesOfClass(Player.class, AABB, Predicate)} -
 *   same call {@link #tickGroundSlam()} already makes above, just with a
 *   larger inflate radius (a roar is heard farther than a melee shockwave
 *   is felt).</li>
 *   <li>{@code LivingEntity#addEffect(MobEffectInstance)} on a scanned
 *   {@code Player} - the exact same call {@link #knockBackAndStagger(Player)}
 *   already makes above (and {@link com.claudemod.event.PrismiumFamiliarAuraHandler}/
 *   {@link com.claudemod.item.PrismiumPulseCharmItem} before that), just
 *   constructing a {@code MobEffectInstance} with {@link
 *   MobEffects#BLINDNESS} instead of {@code MOVEMENT_SLOWDOWN}.</li>
 *   <li>{@code MobEffects.BLINDNESS} - not yet referenced anywhere else in
 *   this codebase, so (per PROGRESS.md's "未確認のJava API" rule) its
 *   existence was independently confirmed this session via two external
 *   javadoc mirrors rather than assumed: the legacy MCP-mapped {@code
 *   net.minecraft.init.MobEffects} class (1.12.2 ForgeJavaDocs mirror,
 *   field name {@code BLINDNESS} there too) and, more importantly, {@code
 *   mappings.dev}'s 1.20.1 Mojang-mapped {@code
 *   net.minecraft.world.effect.MobEffects} class, which lists {@code
 *   BLINDNESS} as a public static field sitting between {@code
 *   INVISIBILITY} and {@code NIGHT_VISION}, alongside the exact same
 *   neighbouring names this class already uses successfully ({@code
 *   MOVEMENT_SPEED}/{@code MOVEMENT_SLOWDOWN}/{@code DAMAGE_BOOST}) -
 *   confirming the Mojmap name did not drift the way {@code
 *   getEntitiesAround} once did (see this class's and {@code
 *   PrismiumFamiliarEntity}'s "2026-09-28" PROGRESS.md entry for why a
 *   single mirror's answer alone was not trusted here).</li>
 *   <li>{@code ParticleTypes.WITCH} - already a compiling field reference
 *   via {@link com.claudemod.event.PrismiumAntivenomCharmHandler} (its own
 *   purple "hex" visual for a debuff charm); reused here purely for its
 *   dark, curse-like read on a blinding roar, no gameplay meaning borrowed
 *   from that handler.</li>
 * </ul>
 * {@code roarCooldown} is a plain {@code int} field, not persisted across
 * save/load - same risk-tolerance precedent as {@link #slamCooldown}.
 */
public class PrismiumColossusEntity extends AbstractPrismiumMonster {

    /** How often (in ticks) the idle particle aura emits. */
    private static final int AURA_PARTICLE_INTERVAL_TICKS = 10;

    /** Enrage threshold: a fraction of max health. */
    private static final float ENRAGE_HEALTH_FRACTION = 1.0F / 3.0F;

    /** Duration (ticks) of the one-shot enrage buff; long enough to cover
     * the rest of a realistic fight without needing to be refreshed. */
    private static final int ENRAGE_EFFECT_DURATION_TICKS = 6000;

    /** Delay (ticks) between the enrage trigger and the first ground
     * slam, so the roar/buff has a moment to read before the first hit. */
    private static final int SLAM_INITIAL_DELAY_TICKS = 40;

    /** Ticks between ground slams once enraged; a slam that finds no
     * player in range does not consume this cooldown (see {@link
     * #tickGroundSlam()}), so it only ever fires when it can actually hit
     * someone. */
    private static final int SLAM_COOLDOWN_TICKS = 100;

    /** Radius (blocks) of the ground slam's push/slow effect, centered on
     * the boss itself - same order of magnitude as every other radius
     * query already in this codebase (6 blocks, see {@link
     * com.claudemod.event.PrismiumMagnetCharmHandler}/{@link
     * com.claudemod.event.PrismiumFamiliarAuraHandler}), just slightly
     * smaller since this is a melee-range punish rather than a
     * companion-proximity aura. */
    private static final double SLAM_RADIUS = 5.0D;

    private static final double SLAM_KNOCKBACK_HORIZONTAL = 0.9D;
    private static final double SLAM_KNOCKBACK_VERTICAL = 0.45D;

    /** Slowness II for a short burst, reading as "staggered by the
     * shockwave" without being a full disable. */
    private static final int SLAM_SLOW_DURATION_TICKS = 100;
    private static final int SLAM_SLOW_AMPLIFIER = 1;

    /** Delay (ticks) between the enrage trigger and the first roar.
     * Deliberately different from {@link #SLAM_INITIAL_DELAY_TICKS} so
     * the two attacks do not always land in lockstep. */
    private static final int ROAR_INITIAL_DELAY_TICKS = 70;

    /** Ticks between roars once enraged. Like {@link #SLAM_COOLDOWN_TICKS},
     * a roar that finds no player in range does not consume this cooldown
     * (see {@link #tickRoar()}). */
    private static final int ROAR_COOLDOWN_TICKS = 140;

    /** Radius (blocks) of the roar's blinding effect - wider than {@link
     * #SLAM_RADIUS} since a roar is heard/seen farther than a melee
     * shockwave is felt. */
    private static final double ROAR_RADIUS = 8.0D;

    /** Blindness duration/amplifier for the roar - meaningful enough to
     * matter mid-fight without being an outright stun. */
    private static final int ROAR_BLINDNESS_DURATION_TICKS = 100;
    private static final int ROAR_BLINDNESS_AMPLIFIER = 0;

    private boolean enraged;

    /** Ticks remaining until the next ground slam is available. Only
     * meaningful once {@link #enraged} is true; not persisted across
     * save/load, same as {@link #enraged} itself. */
    private int slamCooldown;

    /** Ticks remaining until the next roar is available. Only meaningful
     * once {@link #enraged} is true; not persisted across save/load, same
     * as {@link #slamCooldown}. */
    private int roarCooldown;

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
            tickGroundSlam();
            tickRoar();
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
            this.slamCooldown = SLAM_INITIAL_DELAY_TICKS;
            this.roarCooldown = ROAR_INITIAL_DELAY_TICKS;
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

    /**
     * The boss's "second phase" special attack (see class javadoc for the
     * full API-precedent breakdown): once {@link #enraged}, periodically
     * pushes every nearby {@link Player} away from the boss and staggers
     * them with a brief Slowness. Only ever consumes {@link
     * #slamCooldown} when it actually finds a player to hit, so a boss
     * left alone (nobody nearby) does not "waste" slams while idle - the
     * first slam after a player re-enters range always lands rather than
     * being eaten by a cooldown that ticked down in an empty room.
     */
    private void tickGroundSlam() {
        if (!this.enraged) {
            return;
        }
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = this.getBoundingBox().inflate(SLAM_RADIUS);
        List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(Player.class, area, Player::isAlive);
        if (nearbyPlayers.isEmpty()) {
            return;
        }

        this.slamCooldown = SLAM_COOLDOWN_TICKS;
        for (Player player : nearbyPlayers) {
            knockBackAndStagger(player);
        }

        if (!this.isSilent()) {
            // Same WITHER_HURT reuse rationale as tickEnrage()'s roar -
            // a different pitch/volume gives this a distinct "impact"
            // read without introducing a fourth SoundEvents field.
            this.playSound(SoundEvents.WITHER_HURT, 1.0F, 0.5F);
        }
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                this.getX(), this.getY() + 0.1D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /**
     * Pushes {@code player} directly away from the boss's current
     * position (horizontal push plus a small upward lift) and applies a
     * brief Slowness. See class javadoc for why every call here already
     * has a compiling precedent elsewhere in this codebase.
     */
    private void knockBackAndStagger(Player player) {
        Vec3 away = player.position().subtract(this.position());
        Vec3 direction = (away.x * away.x + away.z * away.z < 1.0E-4D)
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(away.x, 0.0D, away.z).normalize();

        Vec3 push = direction.scale(SLAM_KNOCKBACK_HORIZONTAL).add(0.0D, SLAM_KNOCKBACK_VERTICAL, 0.0D);
        player.setDeltaMovement(player.getDeltaMovement().add(push));
        player.hurtMarked = true;

        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, SLAM_SLOW_DURATION_TICKS, SLAM_SLOW_AMPLIFIER, false, true, true));
    }

    /**
     * The boss's second "second phase" special attack (see class javadoc
     * for the full API-precedent breakdown): once {@link #enraged},
     * periodically blinds every nearby {@link Player} with a roar. Runs on
     * its own independent cooldown from {@link #tickGroundSlam()} so the
     * two attacks interleave rather than always firing together. Like the
     * ground slam, only ever consumes {@link #roarCooldown} when it
     * actually finds a player to hit, so an idle boss with nobody nearby
     * does not "waste" roars while waiting.
     */
    private void tickRoar() {
        if (!this.enraged) {
            return;
        }
        if (this.roarCooldown > 0) {
            this.roarCooldown--;
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = this.getBoundingBox().inflate(ROAR_RADIUS);
        List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(Player.class, area, Player::isAlive);
        if (nearbyPlayers.isEmpty()) {
            return;
        }

        this.roarCooldown = ROAR_COOLDOWN_TICKS;
        for (Player player : nearbyPlayers) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS, ROAR_BLINDNESS_DURATION_TICKS, ROAR_BLINDNESS_AMPLIFIER, false, true, true));
        }

        if (!this.isSilent()) {
            // Same WITHER_HURT reuse rationale as the enrage roar/ground
            // slam impact above - a third distinct pitch/volume gives this
            // its own "shriek" read without introducing a fourth
            // SoundEvents field.
            this.playSound(SoundEvents.WITHER_HURT, 1.2F, 0.4F);
        }
        serverLevel.sendParticles(ParticleTypes.WITCH,
                this.getX(), this.getY() + this.getBbHeight() * 0.75D, this.getZ(),
                12, this.getBbWidth() * 0.5D, 0.3D, this.getBbWidth() * 0.5D, 0.02D);
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
