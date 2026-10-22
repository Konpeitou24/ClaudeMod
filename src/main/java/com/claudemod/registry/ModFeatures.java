package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.worldgen.feature.PrismiumDungeonFeature;
import com.claudemod.worldgen.feature.PrismiumLandFeature;
import com.claudemod.worldgen.feature.PrismiumSeafloorFeature;
import com.claudemod.worldgen.feature.PrismiumSoilFeature;
import com.claudemod.worldgen.feature.PrismiumStoneTransitionFeature;
import com.claudemod.worldgen.feature.PrismiumVaultFeature;
import net.minecraft.world.level.block.Blocks;
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
 * PrismiumSeafloorFeature for the full rationale. Fourth entry:
 * PRISMIUM_LAND (PROGRESS.md TODO9, "Prism Realmにまず陸地(平原などの
 * 基本地形)を追加する"), which raises broad noise-selected areas of the
 * otherwise fully-submerged Prism Realm above sea level into explorable
 * dry land - see PrismiumLandFeature for the full rationale.
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

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_LAND = FEATURES.register(
            "prismium_land", () -> new PrismiumLandFeature(NoneFeatureConfiguration.CODEC));

    // Fifth entry: PRISMIUM_DUNGEON (PROGRESS.md TODO9, direct request
    // 2026-10-17 "次はダンジョンを陸地に稀に作ってボスを出すとこまで") -
    // a rare procedurally-built room on Prism Realm land with a
    // PrismiumColossusEntity (the mod's first boss) waiting inside - see
    // PrismiumDungeonFeature for the full design writeup.
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_DUNGEON = FEATURES.register(
            "prismium_dungeon", () -> new PrismiumDungeonFeature(NoneFeatureConfiguration.CODEC));

    // Sixth entry: PRISMIUM_VAULT (PROGRESS.md TODO9 follow-up, the
    // "宝物庫だけで敵がいないもの" half of the two dungeon-type ideas
    // HANDOFF.md carried over after PRISMIUM_DUNGEON) - a smaller, more
    // common, enemy-free treasure room on Prism Realm land with a single
    // loot-table chest - see PrismiumVaultFeature for the full design
    // writeup.
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_VAULT = FEATURES.register(
            "prismium_vault", () -> new PrismiumVaultFeature(NoneFeatureConfiguration.CODEC));

    // Seventh/eighth entries: PRISMIUM_MOUNTAIN_DUNGEON/PRISMIUM_MOUNTAIN_VAULT
    // (v0.83.0, PROGRESS.md TODO53 follow-up "各バイオームに固有の見た目") -
    // second registrations of PrismiumDungeonFeature/PrismiumVaultFeature
    // using their new material-parameterized constructors (see those
    // classes' javadoc), so the overworld mountain biome_modifier can point
    // at a distinct, weathered vanilla-stone-brick ruin look
    // (mossy_stone_bricks/cracked_stone_bricks for the dungeon,
    // stone_bricks/mossy_stone_bricks for the smaller vault, deliberately
    // the reverse "which is the weathered one" pairing so the two room
    // types keep reading as visually distinct from each other, mirroring
    // how the Prism Realm originals use deepstone bricks (dark) for the
    // dungeon and surface bricks (light) for the vault) instead of
    // reusing the Prism Realm's prismium brick textures verbatim as
    // v0.82.0 did. Every other field/method (placement grid, flatness
    // check, light source, boss, loot table) is the exact same shared code
    // as PRISMIUM_DUNGEON/PRISMIUM_VAULT - only the four Blocks.* constants
    // below differ, and all four are long-standing vanilla fields, so this
    // needed no new unverified API (PROGRESS.md section 1).
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_MOUNTAIN_DUNGEON = FEATURES.register(
            "prismium_mountain_dungeon", () -> new PrismiumDungeonFeature(NoneFeatureConfiguration.CODEC,
                    () -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    () -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState()));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PRISMIUM_MOUNTAIN_VAULT = FEATURES.register(
            "prismium_mountain_vault", () -> new PrismiumVaultFeature(NoneFeatureConfiguration.CODEC,
                    () -> Blocks.STONE_BRICKS.defaultBlockState(),
                    () -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState()));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
