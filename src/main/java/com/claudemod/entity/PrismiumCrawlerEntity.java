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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Crawler - ClaudeMod's fifth mob, and its first purely ambient
 * *land* creature (the mod already had a passive water one, see
 * {@link PrismiumDrifterEntity}). A small, harmless, crystalline
 * critter that scurries around Prism Realm's surface in loose clusters -
 * pure "the world feels alive" exploration flavor per the MOD concept in
 * PROGRESS.md section 1, filling a category gap this mod has never had
 * (every prior mob was either a combat monster or, in Drifter's case, a
 * water-bound one - nothing simply decorates dry ground before this).
 *
 * <p>Extends {@code PathfinderMob} directly, not
 * {@link AbstractPrismiumMonster}, for the exact same reason
 * {@link PrismiumDrifterEntity} does (see its javadoc quoting
 * {@code AbstractPrismiumMonster}'s own "non-hostile mobs should extend
 * PathfinderMob/Mob directly" rule): this is not a {@code Monster}/
 * {@code Enemy} and should never be treated like one by other game
 * systems (e.g. golem retaliation, patrol captain logic, piglin
 * hostility checks that key off the {@code Enemy} interface).
 *
 * <p>AI is the land equivalent of Drifter's swimming set: {@link
 * PanicGoal} (flees when hurt, same "harmless but not defenseless" read)
 * plus {@link RandomStrollGoal} instead of {@code RandomSwimmingGoal} for
 * generic ground wandering (the same goal countless vanilla passive
 * mobs use - deliberately not a custom hand-rolled goal, to keep this
 * first ambient-land mob's movement as low-risk/well-trodden as
 * possible). No target selector goals at all: like Drifter, this mob
 * never attacks or retaliates.
 *
 * <p>Client model reuses vanilla's {@code SilverfishModel} geometry
 * wholesale (see {@link com.claudemod.entity.client.PrismiumCrawlerRenderer}),
 * the same "borrow a small non-humanoid vanilla model, reskin only the
 * texture" choice Drifter made with {@code SquidModel} - {@code
 * SilverfishModel<T extends Entity>} confirmed generic (not hardcoded to
 * vanilla {@code Silverfish}) via mappings.dev's 1.20.1 mojmap javadoc
 * this session (see PROGRESS.md for the verification note), the same
 * kind of API-shape check PROGRESS.md's section 4 asks every session to
 * do before relying on an unfamiliar class. Size (0.4 x 0.3) matches
 * vanilla Silverfish's own hitbox so the borrowed geometry doesn't clip
 * or float relative to its collision box.
 *
 * <p>Sounds intentionally do *not* reuse vanilla Silverfish's own
 * (hissy/insectoid) sound set - crystal chime sounds
 * ({@code AMETHYST_BLOCK_CHIME}/{@code _HIT}/{@code _BREAK}) are used
 * instead, matching this mod's established "Prismium blocks use the
 * AMETHYST sound type" convention (see e.g. Prismium Lantern/Pale
 * Prismium Lantern in PROGRESS.md) so the Crawler *sounds* like a piece
 * of living crystal rather than a bug.
 *
 * <p><b>Unverified</b>: this sandbox cannot locally build/playtest (see
 * PROGRESS.md) - natural spawning, the borrowed {@code SilverfishModel}
 * geometry actually looking correct with a reskinned texture, and the
 * crystal-chime sounds' actual in-game feel are all unconfirmed against a
 * running client.
 */
public class PrismiumCrawlerEntity extends PathfinderMob {

    public PrismiumCrawlerEntity(EntityType<? extends PrismiumCrawlerEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Small and fragile (4 HP, a little under half a vanilla Silverfish's
     * own 8) since this is meant to be a harmless background creature
     * that dies in one or two hits rather than something worth farming -
     * matches Drifter's "tankier for its role" reasoning in reverse (that
     * one needed to survive Prism Realm's hostile mobs long enough to
     * flee; this one lives on dry land alongside players far more often
     * and isn't meant to be a real survival concern either way). Slightly
     * faster than the vanilla-Silverfish-derived baseline movement speed
     * to read as "skittery".
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PanicGoal(this, 1.6D));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.1D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        // Deliberately no targetSelector goals: purely passive/ambient,
        // same as PrismiumDrifterEntity - never attacks or retaliates.
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
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
