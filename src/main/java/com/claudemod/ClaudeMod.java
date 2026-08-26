package com.claudemod;

import com.claudemod.registry.ModBlockEntities;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModCreativeTabs;
import com.claudemod.registry.ModEntities;
import com.claudemod.registry.ModFeatures;
import com.claudemod.registry.ModItems;
import com.claudemod.registry.ModMenuTypes;
import com.claudemod.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClaudeMod - a content-heavy Forge mod adding a new dimension, energy system,
 * blocks, mobs and equipment aimed at making exploration more fun.
 *
 * This file is intentionally kept small: it only wires up the mod's registries.
 * Actual content lives under the {@code registry}, {@code block}, {@code item},
 * {@code world} etc. packages so the project stays organized as content grows.
 */
@Mod(ClaudeMod.MOD_ID)
public class ClaudeMod {

    public static final String MOD_ID = "claudemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ClaudeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModSounds.register(modEventBus);

        LOGGER.info("ClaudeMod initializing - {}", MOD_ID);
    }
}
