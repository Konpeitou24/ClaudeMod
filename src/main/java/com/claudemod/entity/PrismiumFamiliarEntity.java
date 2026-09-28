package com.claudemod.entity;

import com.claudemod.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
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
 *
 * <p><b>v0.60.0 addition - single-item carrying ("pack familiar")</b>:
 * once tamed, the owner can sneak + right-click while holding an item to
 * hand it to the familiar (it holds exactly one stack, no GUI - a small,
 * low-risk step towards HANDOFF.md's option (f) "使い魔に第二の機能を持
 * たせる案" without committing to a full inventory/menu implementation
 * this session), and sneak + right-click empty-handed to take it back
 * (returned to the player's inventory via {@link
 * net.minecraft.world.entity.player.Inventory#add}, or dropped at their
 * feet via {@link Player#drop(ItemStack, boolean)} if the inventory is
 * full - both confirmed to exist with this exact signature on
 * {@code forge-1.20.1}'s own javadoc mirror, see
 * {@code https://lexxie.dev/forge/1.20.1/net/minecraft/world/entity/player/Inventory.html}
 * / {@code .../Player.html}, this session). The stored stack survives
 * save/load via {@link #addAdditionalSaveData}/{@link
 * #readAdditionalSaveData} (both confirmed present on {@code Entity} -
 * and confirmed overridden with an identical {@code void(CompoundTag)}
 * signature by vanilla's own {@code Wolf} class, a sibling
 * {@code TamableAnimal}, on the same 1.20.1 javadoc mirror - and
 * {@link ItemStack#save}/{@link ItemStack#of} (confirmed via
 * mappings.dev's 1.20.1 mojmap page for {@code ItemStack}), the same
 * verify-before-{@code @Override} discipline as the rest of this class.
 *
 * <p><b>v0.61.0 addition - death drop and on-model render, options (h)/(i)
 * from HANDOFF.md</b>: {@link #dropCustomDeathLoot} (declared {@code
 * protected} on {@code Mob} itself, confirmed via mappings.dev's 1.20.1
 * mojmap page for {@code Mob} - a second, independent lexxie.dev javadoc
 * mirror lookup this session cross-checked the same signature) now
 * spawns the carried item as a standalone {@link ItemEntity} at the
 * familiar's position when it dies, using the exact {@code new
 * ItemEntity(Level, double, double, double, ItemStack)} +
 * {@code Level#addFreshEntity} pattern already proven to compile
 * elsewhere in this codebase ({@code PrismiumMiningHandler#spawnBonus})
 * rather than the unverified {@code LivingEntity#spawnAtLocation} helper
 * (this session's mappings.dev/lexxie.dev lookups for that method
 * returned inconsistent/incomplete results, so it was deliberately
 * avoided in favour of a helper this codebase already knows compiles).
 * The on-model render itself is handled by {@link
 * com.claudemod.entity.client.PrismiumFamiliarRenderer}, which reads
 * {@link #getCarriedItem()} - see that class's javadoc for the render-side
 * API verification.
 */
public class PrismiumFamiliarEntity extends TamableAnimal {

    /**
     * The single item this familiar is currently carrying for its owner,
     * or {@link ItemStack#EMPTY} if it isn't holding anything. See the
     * class javadoc's "v0.60.0 addition" section.
     */
    private ItemStack carriedItem = ItemStack.EMPTY;

    public PrismiumFamiliarEntity(EntityType<? extends PrismiumFamiliarEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    /**
     * @return the item currently being carried, or {@link ItemStack#EMPTY}.
     * Exposed for a future renderer enhancement (see class javadoc).
     */
    public ItemStack getCarriedItem() {
        return this.carriedItem;
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
        if (this.isTame() && this.isOwnedBy(player) && player.isCrouching()) {
            if (!stack.isEmpty() && this.carriedItem.isEmpty()) {
                if (!this.level().isClientSide) {
                    this.carriedItem = stack.copy();
                    player.setItemInHand(hand, ItemStack.EMPTY);
                    this.playCarryFeedback(true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (stack.isEmpty() && !this.carriedItem.isEmpty()) {
                if (!this.level().isClientSide) {
                    ItemStack returned = this.carriedItem;
                    this.carriedItem = ItemStack.EMPTY;
                    if (!player.getInventory().add(returned)) {
                        player.drop(returned, false);
                    }
                    this.playCarryFeedback(false);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        if (this.isTame() && this.isOwnedBy(player) && !player.isCrouching() && stack.isEmpty()) {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Particle + sound feedback for the carry/retrieve interaction above,
     * following the exact {@code ServerLevel#sendParticles}/{@code
     * #playSound} call pattern already established (and therefore already
     * proven to compile) in {@code PrismiumVitastoneHandler#playFeedback}
     * rather than a freshly-guessed particle/sound API shape. Both sound
     * events reused here ({@link SoundEvents#EXPERIENCE_ORB_PICKUP} and
     * {@link SoundEvents#ARMOR_EQUIP_GENERIC}) were already confirmed to
     * exist and compile elsewhere in this codebase/this session rather
     * than guessed - {@code SoundEvents.ITEM_PICKUP} was considered first
     * but could not be confirmed present on the 1.20.1 mapping this
     * session, so it was deliberately not used.
     *
     * @param stored {@code true} when an item was just handed to the
     *               familiar, {@code false} when one was just taken back.
     */
    private void playCarryFeedback(boolean stored) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + this.getBbHeight() / 2.0D, this.getZ(),
                    4, 0.25D, 0.25D, 0.25D, 0.01D);
            serverLevel.playSound(null, this.blockPosition(),
                    stored ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.ARMOR_EQUIP_GENERIC,
                    SoundSource.NEUTRAL, 0.5F, stored ? 1.4F : 1.0F);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!this.carriedItem.isEmpty()) {
            tag.put("CarriedItem", this.carriedItem.save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CarriedItem")) {
            this.carriedItem = ItemStack.of(tag.getCompound("CarriedItem"));
        } else {
            this.carriedItem = ItemStack.EMPTY;
        }
    }

    /**
     * Drops {@link #carriedItem} as a loose {@link ItemEntity} on death,
     * so a familiar carrying something for its owner doesn't just erase it
     * (TODO32/follow-up (i) from HANDOFF.md's 2026-09-27 notes). See the
     * class javadoc's "v0.61.0 addition" section for the API verification
     * behind both the override itself and the drop mechanism used inside
     * it.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int lootingLevel, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(damageSource, lootingLevel, recentlyHitIn);
        if (!this.carriedItem.isEmpty() && !this.level().isClientSide) {
            ItemEntity drop = new ItemEntity(this.level(),
                    this.getX(), this.getY(), this.getZ(), this.carriedItem);
            this.level().addFreshEntity(drop);
            this.carriedItem = ItemStack.EMPTY;
        }
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
