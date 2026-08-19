package com.claudemod.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Prismium Drifter - ClaudeMod's fourth mob, and its first non-combat /
 * "environmental" entity. Wraith and Deep Wraith are melee attackers,
 * Sentinel is the mod's first ranged attacker (see their javadocs); every
 * mob added so far has been hostile. PROGRESS.md session 60's discussion
 * section flagged "next MOB should be non-combat/environmental" as the
 * suggested next step once the mod had one melee-pair + one ranged mob,
 * and separately flagged that Prism Realm is "currently just a flat water
 * world" with no living creatures actually swimming in it - a passive
 * aquatic drifter addresses both points at once.
 *
 * Implementation choice, continuing the same low-risk pattern established
 * by PrismiumWraithEntity/PrismiumSentinelEntity (extend a vanilla entity
 * directly rather than hand-rolling new AI/model/renderer code that this
 * sandbox cannot compile or playtest locally): this extends vanilla
 * {@link Squid} outright. That means it inherits Squid's entire movement
 * AI (drifting/swimming, fleeing when attacked, ink-cloud + bubble
 * particles on taking damage, suffocation-out-of-water handling) for
 * free, and reuses vanilla's SquidModel for rendering (see
 * PrismiumDrifterRenderer) with only a custom texture swapped in. Squid
 * was chosen specifically (over e.g. Bat or Cod) because:
 * - It is a completely harmless {@code Animal}/{@code WaterAnimal}, so no
 *   equipment-slot or attack-AI overrides are needed at all (unlike every
 *   prior mob in this mod) - this is about as low-risk as a new mob can
 *   get.
 * - Its passive ink-cloud defensive reaction (already wired up by the
 *   vanilla class) reads well thematically as "the Prism Realm's local
 *   wildlife, harmless but not defenseless."
 * - {@link MobCategory#WATER_CREATURE} + an {@code IN_WATER} spawn
 *   placement fit the "currently just a flat water world" Prism Realm
 *   biome far better than another land-walker would.
 *
 * Sound choice: uses {@code GLOW_SQUID_*} sounds (added for Glow Squid in
 * 1.17) rather than plain {@code SQUID_*} - both are already vanilla
 * assets so this needed no new asset work, and the slightly more
 * "otherworldly/musical" glow squid ambient chirp fits a bioluminescent
 * Prismium creature better than the plain squid's ambient sound.
 *
 * <b>Unverified</b> (see PROGRESS.md for the full list): this is the
 * mod's first non-Monster/non-Zombie/non-Skeleton entity base class, so
 * the {@code Squid.createAttributes()} builder shape, the exact vanilla
 * {@code SquidModel}/{@code ModelLayers.SQUID} texture UV layout and the
 * assumed 64x32 texture canvas size are all new assumptions this mod
 * hasn't exercised before. Verified via public documentation (Yarn
 * mappings javadoc for the equivalent {@code SquidEntityModel} class,
 * and the Minecraft Wiki's Squid page for hitbox size / mob category)
 * rather than guessed from memory where possible, but none of it has
 * been confirmed against an actual running game client.
 */
public class PrismiumDrifterEntity extends Squid {

    public PrismiumDrifterEntity(EntityType<? extends Squid> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Slightly tankier than a vanilla Squid (10 HP / 5 hearts per the
     * Minecraft Wiki) to survive a little longer against Prism Realm's
     * own hostile mobs (Sentinel, Wraith variants) before fleeing, but
     * otherwise unchanged from whatever base attributes
     * {@code Squid.createAttributes()} supplies.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Squid.createAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GLOW_SQUID_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.GLOW_SQUID_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLOW_SQUID_DEATH;
    }

    @Override
    protected SoundEvent getSquirtSound() {
        return SoundEvents.GLOW_SQUID_SQUIRT;
    }
}
