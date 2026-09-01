package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.worldgen.feature.PrismiumSeafloorFeature;
import com.claudemod.worldgen.feature.PrismiumSoilFeature;
import com.claudemod.worldgen.feature.PrismiumStoneTransitionFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for ClaudeMod's custom worldgen Feature types (as opposed to
 * configured/placed features, which are plain datapack JSON referencing
 * these by ID). First entry: PRISMIUM_SOIL (scheduled session #45), used
 * to give the Prism Realm dimension its own ground block - see
 * PrismiumSoilFeature for the full rationale and
 * data/claudemod/worldgen/configured_feature/prismium_soil.json for the
 * configured-feature JSON that references it. Second entry:
 * PRISMIUM_STONE_TRANSITION (GitHub issue #23 follow-up), which
 * scatters the Prismium Stone/Deepstone boundary using noise - see
 * PrismiumStoneTransitionFeature and com.claudemod.worldgen.noise for
 * the reusable noise utilities it is built on. Third entry:
 * PRISMIUM_SEAFLOOR ("Prism Realmの海底が完全に平らで不自然"), which
 * gives the otherwise perfectly flat seafloor plane gentle mounds and
 * depressions using the same noise utilities - see
 * PrismiumSeafloorFeature for the full rationale.
 */
public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, ClaudeMod.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_SOIL = FEATURES.register(
            "prismium_soil", () -> new PrismiumSoilFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_STONE_TRANSITION = FEATURES.register(
            "prismium_stone_transition", () -> new PrismiumStoneTransitionFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_SEAFLOOR = FEATURES.register(
            "prismium_seafloor", () -> new PrismiumSeafloorFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
