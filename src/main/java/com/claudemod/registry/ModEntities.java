package com.claudemod.registry;

import com.claudemod.ClaudeMod;
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
