package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.blockentity.PrismiumCableBlockEntity;
import com.claudemod.blockentity.PrismiumCellBlockEntity;
import com.claudemod.blockentity.PrismiumGeneratorBlockEntity;
import com.claudemod.blockentity.PrismiumPulverizerBlockEntity;
import com.claudemod.blockentity.PrismiumSmelterBlockEntity;
import com.claudemod.blockentity.PrismiumPylonBlockEntity;
import com.claudemod.blockentity.PrismiumRestorerBlockEntity;
import com.claudemod.blockentity.PrismiumWardstoneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for every {@link BlockEntityType} added by ClaudeMod.
 *
 * Session 8 adds the mod's first block entity: Prismium Cell, the opening
 * move of the long-dormant "Prismium Energy" roadmap pillar (see
 * PROGRESS.md section 1, item 2 - untouched since session 1).
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ClaudeMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<PrismiumCellBlockEntity>> PRISMIUM_CELL =
            BLOCK_ENTITIES.register("prismium_cell",
                    () -> BlockEntityType.Builder.of(PrismiumCellBlockEntity::new,
                            ModBlocks.PRISMIUM_CELL.get()).build(null));

    public static final RegistryObject<BlockEntityType<PrismiumGeneratorBlockEntity>> PRISMIUM_GENERATOR =
            BLOCK_ENTITIES.register("prismium_generator",
                    () -> BlockEntityType.Builder.of(PrismiumGeneratorBlockEntity::new,
                            ModBlocks.PRISMIUM_GENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<PrismiumCableBlockEntity>> PRISMIUM_CABLE =
            BLOCK_ENTITIES.register("prismium_cable",
                    () -> BlockEntityType.Builder.of(PrismiumCableBlockEntity::new,
                            ModBlocks.PRISMIUM_CABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PrismiumPylonBlockEntity>> PRISMIUM_PYLON =
            BLOCK_ENTITIES.register("prismium_pylon",
                    () -> BlockEntityType.Builder.of(PrismiumPylonBlockEntity::new,
                            ModBlocks.PRISMIUM_PYLON.get()).build(null));

    public static final RegistryObject<BlockEntityType<PrismiumRestorerBlockEntity>> PRISMIUM_RESTORER =
            BLOCK_ENTITIES.register("prismium_restorer",
                    () -> BlockEntityType.Builder.of(PrismiumRestorerBlockEntity::new,
                            ModBlocks.PRISMIUM_RESTORER.get()).build(null));

    public static final RegistryObject<BlockEntityType<PrismiumWardstoneBlockEntity>> PRISMIUM_WARDSTONE =
            BLOCK_ENTITIES.register("prismium_wardstone",
                    () -> BlockEntityType.Builder.of(PrismiumWardstoneBlockEntity::new,
                            ModBlocks.PRISMIUM_WARDSTONE.get()).build(null));

    public static final RegistryObject<BlockEntityType<PrismiumPulverizerBlockEntity>> PRISMIUM_PULVERIZER =
            BLOCK_ENTITIES.register("prismium_pulverizer",
                    () -> BlockEntityType.Builder.of(PrismiumPulverizerBlockEntity::new,
                            ModBlocks.PRISMIUM_PULVERIZER.get()).build(null));

    public static final RegistryObject<BlockEntityType<PrismiumSmelterBlockEntity>> PRISMIUM_SMELTER =
            BLOCK_ENTITIES.register("prismium_smelter",
                    () -> BlockEntityType.Builder.of(PrismiumSmelterBlockEntity::new,
                            ModBlocks.PRISMIUM_SMELTER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
