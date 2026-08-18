package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumWraithEntity;
import com.claudemod.registry.ModEntities;
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
        // PrismiumWraithEntity#doUnderWaterConversion (and by its spawn
        // egg), and attempting to tick a living entity with no registered
        // AttributeSupplier throws/crashes.
        event.put(ModEntities.PRISMIUM_DEEP_WRAITH.get(), com.claudemod.entity.PrismiumDeepWraithEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacement(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.PRISMIUM_WRAITH.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
