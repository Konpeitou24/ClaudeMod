package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumWraithEntity;
import com.claudemod.registry.ModEntities;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mod-bus (both sides) registration for {@link PrismiumWraithEntity}:
 * its attribute supplier and its natural-spawn placement rule. Split out
 * from {@link ClientModEvents} because both of these must run on the
 * server too (attributes are needed to even construct the entity, and the
 * spawn placement predicate is checked by server-side world spawning).
 *
 * API notes (session 12, verified against Kaupenjoe's public Forge 1.20.1
 * course repo rather than guessed from memory, since PROGRESS.md flags
 * stale-version API drift as a recurring risk - see PROGRESS.md section
 * 4 item 8):
 * - EntityAttributeCreationEvent#put(EntityType, AttributeSupplier) is the
 *   1.20.1 way to attach an AttributeSupplier to a new EntityType.
 * - SpawnPlacementRegisterEvent (net.minecraftforge.event.entity, fired on
 *   the MOD bus despite the name looking like a Forge-bus event) is the
 *   1.20.1 way to register where an entity is allowed to spawn. We reuse
 *   Monster#checkMonsterSpawnRules (same predicate vanilla monsters use:
 *   sufficiently dark, valid mob-spawn light level) rather than inventing
 *   a custom predicate.
 * Actually adding the Wraith to biomes' spawn lists is handled separately,
 * data-driven, via forge:add_spawns in
 * data/claudemod/forge/biome_modifier/add_prismium_wraith_spawn.json -
 * this event only governs whether a given position is a *legal* spawn spot
 * once the game has already decided to try spawning one there.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PRISMIUM_WRAITH.get(), PrismiumWraithEntity.createAttributes().build());
        // Session 47: Prismium Deep Wraith needs its own attribute supplier
        // registered even though it is never naturally spawned - it is
        // still a full LivingEntity constructed via
        // PrismiumWraithEntity#tickWaterConversion (and by its spawn egg),
        // and attempting to tick a living entity with no registered
        // AttributeSupplier throws/crashes. (Method name updated 2026-08-26:
        // the water-conversion logic used to live in an overridden
        // doUnderWaterConversion() inherited from Zombie; since
        // PrismiumWraithEntity no longer extends Zombie, it's now a plain
        // hand-rolled tickWaterConversion() - see that class's javadoc.)
        event.put(ModEntities.PRISMIUM_DEEP_WRAITH.get(), com.claudemod.entity.PrismiumDeepWraithEntity.createAttributes().build());
        // Third mob (see PrismiumSentinelEntity's javadoc) - same
        // requirement, a registered AttributeSupplier is needed before
        // this LivingEntity subtype can even be constructed.
        event.put(ModEntities.PRISMIUM_SENTINEL.get(), com.claudemod.entity.PrismiumSentinelEntity.createAttributes().build());
        // Fourth mob (see PrismiumDrifterEntity's javadoc) - same
        // requirement as every prior mob, a registered AttributeSupplier
        // is needed before this LivingEntity subtype can be constructed.
        event.put(ModEntities.PRISMIUM_DRIFTER.get(), com.claudemod.entity.PrismiumDrifterEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacement(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.PRISMIUM_WRAITH.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        // Third mob: same standard monster spawn-rule predicate (dark
        // enough, valid mob-spawn light level) as Wraith - actually
        // placing it in the world is handled separately, data-driven, via
        // data/claudemod/forge/biome_modifier/add_prismium_sentinel_spawn_realm.json
        // (Prism Realm only, unlike Wraith which also spawns in the
        // overworld - see that file for why).
        event.register(ModEntities.PRISMIUM_SENTINEL.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        // Fourth mob (see PrismiumDrifterEntity's javadoc): first
        // IN_WATER spawn placement this mod has ever registered. Rather
        // than guess at vanilla Squid's own internal spawn-rule helper
        // method name/signature (unverifiable from this sandbox, see
        // PROGRESS.md's recurring "confirm API shape before using it"
        // rule), this uses a small inline predicate that only checks the
        // fluid at the candidate position (and the block above it) is
        // water - deliberately simple and self-contained rather than
        // depending on an assumed vanilla helper.
        event.register(ModEntities.PRISMIUM_DRIFTER.get(), SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
