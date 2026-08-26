package com.claudemod.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Common base class for ClaudeMod's hostile mobs, starting with this
 * session's rewrite of {@link PrismiumWraithEntity} / {@link
 * PrismiumDeepWraithEntity}.
 *
 * <p><b>Why this exists</b> (repo owner, direct chat, 2026-08-26, verbatim):
 * "というかMOBを何かのMOBベースに作るからこんなことになるんじゃないですか?
 * 今後こういうのがMOBを増やすごとに増えて対応に追われるのはごめんです。
 * (雷に打たれて別のバニラのMOBになるとかも)なのでMOBは追加するのであれば
 * 頑張って自作してほしいです(MOBのAIとかも)". Every ClaudeMod mob up to
 * this point ({@link PrismiumWraithEntity}, {@link
 * PrismiumDeepWraithEntity}, {@code PrismiumSentinelEntity} extends
 * vanilla {@code Skeleton}, {@code PrismiumDrifterEntity} extends vanilla
 * {@code Squid}) was built by extending a concrete vanilla mob class to get
 * its AI/model for free. The recurring cost of that shortcut is that those
 * concrete classes also carry hardcoded, species-specific behaviour that
 * this mod's own code never writes and can easily forget exists - Zombie's
 * water-to-Drowned conversion timer being the exact bug that triggered this
 * rewrite (see PrismiumWraithEntity/PrismiumDeepWraithEntity's own javadoc
 * for the two-session history of that bug). Other vanilla mob classes carry
 * their own equivalent traps this mod hasn't hit yet purely by luck (e.g.
 * lightning-strike conversions on some vanilla base classes).
 *
 * <p>Going forward, new ClaudeMod hostile mobs should extend this class
 * (non-hostile mobs should extend {@code PathfinderMob}/{@code Mob}
 * directly) and build their AI out of generic {@code Goal} classes rather
 * than inheriting a vanilla subclass's fixed goal set. Those Goal classes
 * (see {@link #registerBasicMeleeGoals(double)} below) are declared
 * generically against {@code Mob}/{@code PathfinderMob}/{@code
 * LivingEntity}, not against any specific vanilla species, so reusing them
 * carries none of the hidden-behaviour risk that reusing an entire vanilla
 * mob class does - what you see in this file and your own subclass is
 * genuinely the whole behaviour, nothing extra rides along silently.
 *
 * <p>{@link Monster} itself (unlike its concrete subclasses Zombie/
 * Skeleton/Creeper/etc.) is a safe, neutral base to extend: it only adds
 * "hostile mob" bookkeeping (the {@code Enemy} interface, despawning on
 * Peaceful difficulty - see PrismiumWraithEntity's issue #5/#10 history for
 * why that specific default matters), none of the per-species quirks that
 * live further down the vanilla hierarchy.
 *
 * <p><b>Trade-off, stated plainly</b>: this approach requires writing and
 * tuning AI by hand instead of getting a battle-tested vanilla goal set for
 * free, and this sandbox cannot locally playtest any of it. If a
 * self-authored mob's AI turns out to behave noticeably worse than the
 * vanilla-derived mobs it replaces (worse pathfinding, less responsive
 * targeting, etc.), that is the first thing to investigate and tune -
 * report it and it can be adjusted goal-by-goal without needing to
 * reintroduce a vanilla base class.
 */
public abstract class AbstractPrismiumMonster extends Monster {

    protected AbstractPrismiumMonster(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Shared across every ClaudeMod monster: never spawn holding random
        // vanilla gear, so nothing here clashes with the Prismium theme.
    }

    /**
     * Registers the standard "hostile mob that walks up to the nearest
     * player and hits them" goal set: float in water so it doesn't sink and
     * drown pointlessly, melee-attack whatever it's targeting, otherwise
     * wander and look around, and target whichever player hurt it or is
     * simply nearby. This intentionally mirrors the goal set vanilla
     * {@code Zombie} registers (float / attack / wander / look-at-player /
     * look-around, hurt-by-target / nearest-player-target) - the point of
     * this rewrite was to stop inheriting {@code Zombie}'s hidden
     * behaviour, not to reinvent what a basic melee mob's AI should look
     * like from first principles.
     *
     * <p>Call this from a subclass's {@code registerGoals()} override; add
     * further goals with priorities between the ones registered here (or
     * simply don't call this at all) for mobs that need something other
     * than "plain melee chaser".
     */
    protected void registerBasicMeleeGoals(double moveSpeed) {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, moveSpeed, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, moveSpeed));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
