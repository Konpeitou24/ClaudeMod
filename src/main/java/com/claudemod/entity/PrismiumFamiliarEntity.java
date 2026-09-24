package com.claudemod.entity;

import com.claudemod.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Familiar - ClaudeMod's seventh mob, and its first tamable
 * companion (option (d), "使い魔的MOB案", tracked in PROGRESS.md section 4
 * across several sessions' HANDOFF.md notes once the "完全パッシブcharm"
 * and "壁掛け" content families both ran out of natural next steps).
 * A small ambient light spirit, visually a sibling of
 * {@link PrismiumWispEntity} (same flight AI, same borrowed
 * {@code SquidModel} geometry, only the texture and behaviour differ),
 * that a player can win over with a Prismium Shard and who will then
 * follow them around and can be told to stay put - a "familiar", not a
 * combat pet (it never fights, same as every other ambient creature in
 * this mod).
 *
 * <p><b>Extends {@code TamableAnimal}</b> (this mod's first use of that
 * class) rather than the {@code PathfinderMob} every prior ambient
 * mob (Drifter/Crawler/Wisp) uses, since owner-tracking/sitting/taming
 * state (synced data, NBT save/load, {@code isOwnedBy}) is exactly what
 * {@code TamableAnimal} exists to provide and hand-rolling that state
 * machine again would just be reinventing a proven vanilla class (Wolf/
 * Cat/Parrot all extend it). Every override below was individually
 * checked against mappings.dev's 1.20.1 mojmap javadoc this session
 * (per PROGRESS.md's "未確認のJava APIは必ず出典を確認してから使う"
 * rule) rather than assumed from general modding knowledge:
 * <ul>
 *   <li>{@code TamableAnimal}'s own method list (checked directly) confirms
 *   {@link #isTame()}, {@link #tame(Player)}, {@link #isOwnedBy}, {@link
 *   #isOrderedToSit()}/{@link #setOrderedToSit(boolean)}, and the
 *   protected {@code spawnTamingParticles(boolean)} hook used below all
 *   exist with exactly these signatures.</li>
 *   <li>{@code Animal} (TamableAnimal's superclass) does <em>not</em>
 *   implement {@code AgeableMob#getBreedOffspring} itself (confirmed by
 *   reading both classes' method tables) - this class must supply it,
 *   see {@link #getBreedOffspring} below, which simply returns
 *   {@code null} since {@link #isFood} always returns {@code false} and
 *   breeding can therefore never actually trigger.</li>
 *   <li>{@link FollowOwnerGoal}'s constructor
 *   {@code (TamableAnimal, double speed, float minDistance, float
 *   maxDistance, boolean leavesAllowed)} and {@link
 *   SitWhenOrderedToGoal}'s constructor {@code (TamableAnimal)} were both
 *   confirmed via mappings.dev this session.</li>
 *   <li>The flight setup ({@link FlyingMoveControl}, {@link
 *   FlyingPathNavigation}, {@link WaterAvoidingRandomFlyingGoal},
 *   {@code setNoGravity(boolean)}) is the exact combination already
 *   proven to compile and fly in {@link PrismiumWispEntity} - copied
 *   verbatim rather than re-derived.</li>
 * </ul>
 *
 * <p><b>Taming</b>: right-clicking with a Prismium Shard ({@link
 * ModItems#PRISMIUM_SHARD}) has a 1-in-3 chance to tame it (same odds
 * vanilla Parrot uses for its own seed-taming, chosen for the same
 * "not guaranteed, so it feels like winning the creature over" reason
 * rather than a plain always-succeeds interaction), consuming the shard
 * either way (unless the player is in creative), matching this mod's
 * existing "consume the input item on right-click" convention already
 * used by {@code PrismiumGeneratorBlock}/{@code PrismiumCellBlock}
 * (see their own {@code getAbilities().instabuild} checks). Once tamed,
 * right-clicking empty-handed while owning it toggles sit/follow, the
 * same interaction Wolf/Cat use.
 *
 * <p>Deliberately has no targetSelector goals and no attack behaviour at
 * all - this is a companion, not a combat pet, matching this mod's
 * established "ambient creatures never fight" convention (see
 * PrismiumDrifterEntity/PrismiumCrawlerEntity/PrismiumWispEntity).
 *
 * <p><b>Unverified</b>: this sandbox cannot locally build/playtest (see
 * PROGRESS.md) - the 1-in-3 taming chance actually firing correctly,
 * FollowOwnerGoal actually working smoothly for a flying entity (it has
 * only ever been proven with ground-bound Wolf/Cat in vanilla, though
 * PrismiumWispEntity's javadoc notes {@link FlyingPathNavigation} itself
 * is already proven in this codebase), sit-toggle responsiveness, and
 * the reskinned texture's in-game look are all unconfirmed against a
 * running client.
 */
public class PrismiumFamiliarEntity extends TamableAnimal {

    public PrismiumFamiliarEntity(EntityType<? extends PrismiumFamiliarEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    /**
     * Slightly heartier than Wisp (8 HP vs 5) since this one is meant to
     * stick around as a long-term companion rather than a background
     * ambient light, but still far below any combat mob's health - it
     * has no way to fight back if attacked.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
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
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 4.0F, 2.0F, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // Deliberately no targetSelector goals - a familiar never fights,
        // same convention as every other ambient creature in this mod.
    }

    @Override
    public boolean isPushedByFluid() {
        // Same reasoning as PrismiumWispEntity: stay on its flight path
        // rather than getting shoved around by water/lava currents.
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isTame() && stack.is(ModItems.PRISMIUM_SHARD.get())) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!this.level().isClientSide) {
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.spawnTamingParticles(true);
                } else {
                    this.spawnTamingParticles(false);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && stack.isEmpty()) {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        // No breeding - a familiar is a one-of-a-kind companion, not a
        // farmable animal. getBreedOffspring() below is never reached as
        // a result, but is still implemented defensively (see javadoc).
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
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
