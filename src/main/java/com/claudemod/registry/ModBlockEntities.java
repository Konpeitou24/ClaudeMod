package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.blockentity.PrismiumCellBlockEntity;
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

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
