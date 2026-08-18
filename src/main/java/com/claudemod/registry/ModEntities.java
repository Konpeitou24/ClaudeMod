package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumDeepWraithEntity;
import com.claudemod.entity.PrismiumWraithEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for every {@link EntityType} added by ClaudeMod.
 *
 * Session 12 adds the mod's first living entity: Prismium Wraith. See
 * {@link PrismiumWraithEntity} for the design writeup and PROGRESS.md
 * section 1 for how this fits the "new MOB" roadmap pillar (untouched
 * since session 1 until now).
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ClaudeMod.MOD_ID);

    public static final RegistryObject<EntityType<PrismiumWraithEntity>> PRISMIUM_WRAITH =
            ENTITY_TYPES.register("prismium_wraith",
                    () -> EntityType.Builder.of(PrismiumWraithEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build("prismium_wraith"));

    // Session 47: Prismium Deep Wraith, the dedicated conversion target
    // PrismiumWraithEntity#doUnderWaterConversion redirects to instead of
    // vanilla Drowned. Same size as the land Wraith (same base body/model).
    // Deliberately has no SpawnPlacementRegisterEvent registration (see
    // ModEntityEvents) - it is never placed by natural chunk spawning, only
    // by conversion (or its spawn egg), so a spawn placement predicate
    // would never actually be consulted.
    public static final RegistryObject<EntityType<PrismiumDeepWraithEntity>> PRISMIUM_DEEP_WRAITH =
            ENTITY_TYPES.register("prismium_deep_wraith",
                    () -> EntityType.Builder.of(PrismiumDeepWraithEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build("prismium_deep_wraith"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
