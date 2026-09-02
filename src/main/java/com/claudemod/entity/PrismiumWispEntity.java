package com.claudemod.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Wisp - ClaudeMod's sixth mob, and its first purely ambient
 * *flying* creature (TODO10 in PROGRESS.md section 2: "MOBのカテゴリ拡充
 * ... 飛行アンビエント ... の余地あり"). Rounds out the ambient-creature
 * trio alongside {@link PrismiumDrifterEntity} (water) and
 * {@link PrismiumCrawlerEntity} (land): a small drifting light that
 * bobs through the air of Prism Realm, never touching the ground,
 * pure exploration-atmosphere flavor per the MOD concept in PROGRESS.md
 * section 5 ("探索そのものが楽しくなるような...ギミックを継続的に追加").
 *
 * <p>Extends {@code PathfinderMob} directly, not
 * {@link AbstractPrismiumMonster}, for the same reason {@link
 * PrismiumDrifterEntity}/{@link PrismiumCrawlerEntity} do: this is not a
 * {@code Monster}/{@code Enemy} and should never be treated like one by
 * other game systems.
 *
 * <p><b>Flight setup</b> (this mod's first flying entity, so every piece
 * here was individually confirmed to exist against mappings.dev's 1.20.1
 * mojmap javadoc this session, rather than assumed - see PROGRESS.md
 * section 1's "未確認のJava APIは必ず出典を確認してから使う" rule, added
 * after the v0.37.0 {@code canPlace(BlockPlaceContext)} incident where a
 * guessed-at method name broke the build three times in a row):
 * <ul>
 *   <li>{@link #createNavigation(Level)} returning a {@link
 *   FlyingPathNavigation} is the same override signature already proven
 *   to compile in this codebase by {@link PrismiumDrifterEntity} (which
 *   returns a {@link net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation}
 *   from the identical {@code protected PathNavigation createNavigation(Level)}
 *   override) - only the concrete navigation class differs.</li>
 *   <li>{@link FlyingMoveControl}'s constructor
 *   {@code (Mob, int maxPitchChange, boolean hoversInPlace)} and {@link
 *   FlyingPathNavigation}'s constructor {@code (Mob, Level)} were both
 *   confirmed via mappings.dev this session (both public, both exactly
 *   this signature).</li>
 *   <li>{@link WaterAvoidingRandomFlyingGoal} (the goal vanilla Bee/Parrot
 *   use for ambient wandering flight) was likewise confirmed public with
 *   a {@code (PathfinderMob, double)} constructor.</li>
 *   <li>{@code Entity#setNoGravity(boolean)} (called in the constructor
 *   below, not overridden - a plain setter call carries none of the
 *   "does this override even exist" risk a method override does) was
 *   confirmed public via mappings.dev's {@code Entity} page this session.</li>
 * </ul>
 *
 * <p>Client model reuses vanilla's {@code SquidModel} geometry wholesale
 * (see {@link com.claudemod.entity.client.PrismiumWispRenderer}) - the
 * same "borrow a small non-humanoid vanilla model, reskin only the
 * texture" choice {@link PrismiumDrifterEntity} made, and doubly
 * low-risk here since {@code SquidModel<T extends Entity>}'s generic
 * bound and UV layout are already proven correct by Drifter's own
 * successful CI builds - the Wisp's texture is a recolor of Drifter's
 * own (see {@code gen_prismium_wisp.py}), so no new UV guessing was
 * needed either. A floating, tentacled light drifting through open air
 * (rather than water) reads as "ethereal spirit" rather than "fish out
 * of water" - intentional, not a reuse-of-convenience afterthought.
 *
 * <p>Sounds: {@code BEACON_AMBIENT} for its idle sound (a soft magical
 * hum, distinct from Crawler's crystal chime, confirmed to exist via
 * mappings.dev this session) layered with the same {@code AMETHYST_BLOCK}
 * hit/break sounds every other Prismium creature's hurt/death uses (see
 * PROGRESS.md's "Prismium blocks use the AMETHYST sound type" convention)
 * so it still reads as part of the same crystal-creature family on
 * damage/death even though its idle sound is unique.
 *
 * <p><b>Unverified</b>: this sandbox cannot locally build/playtest (see
 * PROGRESS.md) - natural spawning, the flight AI actually producing
 * smooth wandering movement rather than getting stuck, the borrowed
 * {@code SquidModel} geometry looking right airborne with a reskinned
 * texture, and the sounds' actual in-game feel are all unconfirmed
 * against a running client.
 */
public class PrismiumWispEntity extends PathfinderMob {

    public PrismiumWispEntity(EntityType<? extends PrismiumWispEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    /**
     * Small and fragile (5 HP, between Crawler's 4 and Drifter's 12)
     * since like both of its ambient siblings this is meant to be a
     * harmless background creature, not a survival concern. Movement
     * speed attribute kept modest - actual felt flight speed comes from
     * the {@link WaterAvoidingRandomFlyingGoal} speed modifier passed in
     * {@link #registerGoals()}, same split Drifter uses between its base
     * attribute and its {@code RandomSwimmingGoal} speed argument.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.6D));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        // Deliberately no targetSelector goals: purely passive/ambient,
        // same as PrismiumDrifterEntity/PrismiumCrawlerEntity - never
        // attacks or retaliates.
    }

    @Override
    public boolean isPushedByFluid() {
        // Never pushed around by water/lava currents while drifting
        // through the air - same override PrismiumDrifterEntity uses
        // (there to stay put in water; here to stay on its flight path
        // if it happens to clip through a puddle or Prism Realm's water).
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }
}
