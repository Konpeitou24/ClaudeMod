package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumDeepWraithEntity;
import com.claudemod.entity.PrismiumCrawlerEntity;
import com.claudemod.entity.PrismiumDrifterEntity;
import com.claudemod.entity.PrismiumSentinelEntity;
import com.claudemod.entity.PrismiumWispEntity;
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
    // PrismiumWraithEntity converts into (via Mob#convertTo, see that
    // class's tickWaterConversion()) instead of vanilla Drowned. Same size
    // as the land Wraith (same base body/model). Neither of these two
    // classes extends vanilla Zombie as of 2026-08-26 (see
    // AbstractPrismiumMonster's javadoc).
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

    // Third mob, first ranged attacker (see PrismiumSentinelEntity's
    // javadoc): same size class as Skeleton (0.6x1.99) rather than the
    // Wraith pair's Zombie-derived 0.6x1.95, matching what it actually
    // extends.
    public static final RegistryObject<EntityType<PrismiumSentinelEntity>> PRISMIUM_SENTINEL =
            ENTITY_TYPES.register("prismium_sentinel",
                    () -> EntityType.Builder.of(PrismiumSentinelEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.99F)
                            .clientTrackingRange(8)
                            .build("prismium_sentinel"));

    // Fourth mob, first non-combat/environmental entity (see
    // PrismiumDrifterEntity's javadoc). MobCategory.WATER_CREATURE and the
    // 0.8x0.8 size both match vanilla Squid's own registration (confirmed
    // against the Minecraft Wiki's Squid page this session, since this mod
    // has never registered a water-category entity before).
    public static final RegistryObject<EntityType<PrismiumDrifterEntity>> PRISMIUM_DRIFTER =
            ENTITY_TYPES.register("prismium_drifter",
                    () -> EntityType.Builder.of(PrismiumDrifterEntity::new, MobCategory.WATER_CREATURE)
                            .sized(0.8F, 0.8F)
                            .clientTrackingRange(8)
                            .build("prismium_drifter"));

    // Fifth mob, first purely ambient *land* creature (see
    // PrismiumCrawlerEntity's javadoc). MobCategory.AMBIENT (rather than
    // CREATURE) matches vanilla Bat's own registration - a small,
    // harmless background critter that shouldn't compete with real
    // "farmable" animal spawn caps. Size (0.4x0.3) matches vanilla
    // Silverfish's own hitbox, since the renderer borrows
    // SilverfishModel's geometry wholesale (see PrismiumCrawlerRenderer).
    public static final RegistryObject<EntityType<PrismiumCrawlerEntity>> PRISMIUM_CRAWLER =
            ENTITY_TYPES.register("prismium_crawler",
                    () -> EntityType.Builder.of(PrismiumCrawlerEntity::new, MobCategory.AMBIENT)
                            .sized(0.4F, 0.3F)
                            .clientTrackingRange(6)
                            .build("prismium_crawler"));

    // Sixth mob, first flying ambient creature (see PrismiumWispEntity's
    // javadoc). MobCategory.AMBIENT matches PRISMIUM_CRAWLER's own
    // registration (a small, harmless background critter, not a real
    // "farmable" animal). Size (0.5x0.5) is smaller than Drifter's
    // 0.8x0.8 squid-derived hitbox despite sharing its model geometry -
    // a deliberate "small drifting light" read rather than "airborne
    // squid" (see PrismiumWispRenderer, which does not compensate with
    // any model-scale override, so the SquidModel geometry will render
    // a little larger than the hitbox - an accepted cosmetic trade-off,
    // not a bug).
    public static final RegistryObject<EntityType<PrismiumWispEntity>> PRISMIUM_WISP =
            ENTITY_TYPES.register("prismium_wisp",
                    () -> EntityType.Builder.of(PrismiumWispEntity::new, MobCategory.AMBIENT)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(6)
                            .build("prismium_wisp"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
