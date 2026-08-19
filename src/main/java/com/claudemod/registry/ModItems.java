package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.blockentity.PrismiumCableBlockEntity;
import com.claudemod.blockentity.PrismiumCellBlockEntity;
import com.claudemod.blockentity.PrismiumGeneratorBlockEntity;
import com.claudemod.blockentity.PrismiumPylonBlockEntity;
import com.claudemod.blockentity.PrismiumPulverizerBlockEntity;
import com.claudemod.blockentity.PrismiumRestorerBlockEntity;
import com.claudemod.blockentity.PrismiumSmelterBlockEntity;
import com.claudemod.blockentity.PrismiumWardstoneBlockEntity;
import com.claudemod.item.EnergyStorageBlockItem;
import com.claudemod.item.ModArmorMaterials;
import com.claudemod.item.ModToolTiers;
import com.claudemod.item.PrismiumBowItem;
import com.claudemod.item.PrismiumChronoflameBlockItem;
import com.claudemod.item.PrismiumEmberguardItem;
import com.claudemod.item.PrismiumFeatherstoneItem;
import com.claudemod.item.PrismiumGrapplingHookItem;
import com.claudemod.item.PrismiumGuardianCharmItem;
import com.claudemod.item.PrismiumLocatorItem;
import com.claudemod.item.PrismiumMagnetCharmItem;
import com.claudemod.item.PrismiumPulseCharmItem;
import com.claudemod.item.PrismiumRiftAnchorItem;
import com.claudemod.item.PrismiumRiftShardItem;
import com.claudemod.item.PrismiumShieldItem;
import com.claudemod.item.PrismiumVitastoneItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for every item added by ClaudeMod, including the
 * BlockItem forms of blocks registered in {@link ModBlocks}.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ClaudeMod.MOD_ID);

    // Raw crystal shard, the mod's first crafting resource. Mined from
    // Prismium Ore, will later fuel the Prismium Energy system.
    public static final RegistryObject<Item> PRISMIUM_SHARD = ITEMS.register("prismium_shard",
            () -> new Item(new Item.Properties()));

    // Prismium Ingot (session 68): the mod's first refined-material
    // item, produced by Prismium Smelter from Prismium Shards - see
    // PrismiumSmelterBlockEntity. Plain Item like PRISMIUM_SHARD; no
    // crafting recipe consumes it yet this session (see PROGRESS.md),
    // deliberately left as a forward-looking resource for a future
    // session the same way PRISMIUM_SHARD itself once was.
    public static final RegistryObject<Item> PRISMIUM_INGOT = ITEMS.register("prismium_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_ORE_ITEM = ITEMS.register("prismium_ore",
            () -> new BlockItem(ModBlocks.PRISMIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_PRISMIUM_ORE_ITEM = ITEMS.register("deepslate_prismium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_PRISMIUM_ORE.get(), new Item.Properties()));

    // Session 47: BlockItem for Prismium Stone (see ModBlocks.PRISMIUM_STONE).
    public static final RegistryObject<Item> PRISMIUM_STONE_ITEM = ITEMS.register("prismium_stone",
            () -> new BlockItem(ModBlocks.PRISMIUM_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_BLOCK_ITEM = ITEMS.register("prismium_block",
            () -> new BlockItem(ModBlocks.PRISMIUM_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_CORE_ITEM = ITEMS.register("prismium_core",
            () -> new BlockItem(ModBlocks.PRISMIUM_CORE.get(), new Item.Properties()));

    // Chiseled Prismium Block (session 34): BlockItem for the decorative
    // masonry variant. See ModBlocks.CHISELED_PRISMIUM_BLOCK.
    public static final RegistryObject<Item> CHISELED_PRISMIUM_BLOCK_ITEM = ITEMS.register("chiseled_prismium_block",
            () -> new BlockItem(ModBlocks.CHISELED_PRISMIUM_BLOCK.get(), new Item.Properties()));

    // Prismium Block Slab / Wall (session 34): BlockItems for the mod's
    // first building-variety blocks. See ModBlocks.PRISMIUM_BLOCK_SLAB /
    // PRISMIUM_BLOCK_WALL.
    public static final RegistryObject<Item> PRISMIUM_BLOCK_SLAB_ITEM = ITEMS.register("prismium_block_slab",
            () -> new BlockItem(ModBlocks.PRISMIUM_BLOCK_SLAB.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_BLOCK_WALL_ITEM = ITEMS.register("prismium_block_wall",
            () -> new BlockItem(ModBlocks.PRISMIUM_BLOCK_WALL.get(), new Item.Properties()));

    // Prismium Block Stairs (session 35): BlockItem for the third
    // building-variety block. See ModBlocks.PRISMIUM_BLOCK_STAIRS.
    public static final RegistryObject<Item> PRISMIUM_BLOCK_STAIRS_ITEM = ITEMS.register("prismium_block_stairs",
            () -> new BlockItem(ModBlocks.PRISMIUM_BLOCK_STAIRS.get(), new Item.Properties()));

    // Prismium Core building variety (session 36). See ModBlocks.
    public static final RegistryObject<Item> PRISMIUM_CORE_SLAB_ITEM = ITEMS.register("prismium_core_slab",
            () -> new BlockItem(ModBlocks.PRISMIUM_CORE_SLAB.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_CORE_WALL_ITEM = ITEMS.register("prismium_core_wall",
            () -> new BlockItem(ModBlocks.PRISMIUM_CORE_WALL.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_CORE_STAIRS_ITEM = ITEMS.register("prismium_core_stairs",
            () -> new BlockItem(ModBlocks.PRISMIUM_CORE_STAIRS.get(), new Item.Properties()));

    // Chiseled Prismium Core (session 37): BlockItem for the decorative
    // masonry variant. See ModBlocks.CHISELED_PRISMIUM_CORE.
    public static final RegistryObject<Item> CHISELED_PRISMIUM_CORE_ITEM = ITEMS.register("chiseled_prismium_core",
            () -> new BlockItem(ModBlocks.CHISELED_PRISMIUM_CORE.get(), new Item.Properties()));

    // Prismium Lantern (session 4): a cheap, tool-tier-independent light
    // source block for exploration. See ModBlocks for the block stats.
    public static final RegistryObject<Item> PRISMIUM_LANTERN_ITEM = ITEMS.register("prismium_lantern",
            () -> new BlockItem(ModBlocks.PRISMIUM_LANTERN.get(), new Item.Properties()));

    // Prismium Cell (session 8): BlockItem for the mod's first block
    // entity/energy storage block. See ModBlocks.PRISMIUM_CELL. Session 11
    // switched this from a plain BlockItem to EnergyStorageBlockItem so
    // stored FE survives break+replace (via each block's loot table
    // copy_nbt function) and shows up in the item's tooltip - see
    // EnergyStorageBlockItem's javadoc.
    public static final RegistryObject<Item> PRISMIUM_CELL_ITEM = ITEMS.register("prismium_cell",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_CELL.get(), new Item.Properties(),
                    PrismiumCellBlockEntity.CAPACITY));

    // Prismium Generator (session 9): BlockItem for the mod's first
    // BlockEntityTicker / automatic energy-transfer block. See
    // ModBlocks.PRISMIUM_GENERATOR. Session 11: see PRISMIUM_CELL_ITEM
    // above, same EnergyStorageBlockItem persistence/tooltip treatment.
    public static final RegistryObject<Item> PRISMIUM_GENERATOR_ITEM = ITEMS.register("prismium_generator",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_GENERATOR.get(), new Item.Properties(),
                    PrismiumGeneratorBlockEntity.CAPACITY));

    // Prismium Cable (session 10): BlockItem for the mod's first relay
    // block / non-full-cube block. See ModBlocks.PRISMIUM_CABLE. Session
    // 11: see PRISMIUM_CELL_ITEM above, same EnergyStorageBlockItem
    // persistence/tooltip treatment.
    public static final RegistryObject<Item> PRISMIUM_CABLE_ITEM = ITEMS.register("prismium_cable",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_CABLE.get(), new Item.Properties(),
                    PrismiumCableBlockEntity.CAPACITY));

        // Prismium tool set (session 2): stats sit just above diamond, repaired
    // with Prismium Shard. See ModToolTiers for the tier definition.
    public static final RegistryObject<Item> PRISMIUM_PICKAXE = ITEMS.register("prismium_pickaxe",
            () -> new PickaxeItem(ModToolTiers.PRISMIUM, 1, -2.8f, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_AXE = ITEMS.register("prismium_axe",
            () -> new AxeItem(ModToolTiers.PRISMIUM, 6.0f, -3.0f, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_SHOVEL = ITEMS.register("prismium_shovel",
            () -> new ShovelItem(ModToolTiers.PRISMIUM, 1.5f, -3.0f, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_HOE = ITEMS.register("prismium_hoe",
            () -> new HoeItem(ModToolTiers.PRISMIUM, -2, -1.0f, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_SWORD = ITEMS.register("prismium_sword",
            () -> new SwordItem(ModToolTiers.PRISMIUM, 3, -2.4f, new Item.Properties()));

    // Prismium Warhammer (session 69, scheduled): the mod's first item
    // crafted from Prismium Ingot (see PRISMIUM_INGOT above - added
    // session 68 with no recipe using it until now). A plain SwordItem
    // like PRISMIUM_SWORD (no dedicated Item subclass), just with much
    // higher damage / much slower speed to read as a heavy two-handed
    // hammer rather than a reskinned sword. Its on-hit stagger gimmick
    // lives entirely in PrismiumWarhammerHandler, mirroring how
    // PRISMIUM_SWORD's Glowing gimmick lives in PrismiumSwordHandler.
    // Repaired with Prismium Shard (inherited from ModToolTiers.PRISMIUM,
    // same as every other Prismium tool) even though it is crafted from
    // Ingot - keeps repair consistent across the whole tool family rather
    // than introducing a second, Ingot-based repair path for just one
    // item.
    public static final RegistryObject<Item> PRISMIUM_WARHAMMER = ITEMS.register("prismium_warhammer",
            () -> new SwordItem(ModToolTiers.PRISMIUM, 8, -3.4f, new Item.Properties()));

    // Prismium armor set (session 3): defense sits at diamond/netherite
    // parity by design (no flat armor-value power creep), but with higher
    // durability, toughness, knockback resistance and enchantability. See
    // ModArmorMaterials for the full rationale. Session 4 added a full-set
    // Night Vision bonus - see event.ArmorSetBonusHandler.
    public static final RegistryObject<Item> PRISMIUM_HELMET = ITEMS.register("prismium_helmet",
            () -> new ArmorItem(ModArmorMaterials.PRISMIUM, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_CHESTPLATE = ITEMS.register("prismium_chestplate",
            () -> new ArmorItem(ModArmorMaterials.PRISMIUM, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_LEGGINGS = ITEMS.register("prismium_leggings",
            () -> new ArmorItem(ModArmorMaterials.PRISMIUM, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_BOOTS = ITEMS.register("prismium_boots",
            () -> new ArmorItem(ModArmorMaterials.PRISMIUM, ArmorItem.Type.BOOTS, new Item.Properties()));

    // Prismium Grappling Hook (session 7): the mod's first accessory-style
    // item, not a stat-stick tool. See PrismiumGrapplingHookItem for the
    // raycast-and-pull implementation and API notes.
    public static final RegistryObject<Item> PRISMIUM_GRAPPLING_HOOK = ITEMS.register("prismium_grappling_hook",
            () -> new PrismiumGrapplingHookItem(new Item.Properties().durability(250)));

    // Prismium Shield (session 28): the mod's first blocking-capable
    // gear item, and its first brand-new equipment slot since the Rift
    // Shard (session 14) - see PrismiumShieldItem for why this is a
    // plain Item overriding getUseAnimation/use rather than a subclass
    // of vanilla ShieldItem. durability(420) sits above vanilla's shield
    // (336), matching the "higher durability, no flat power creep"
    // philosophy already used by ModArmorMaterials/ModToolTiers.
    public static final RegistryObject<Item> PRISMIUM_SHIELD = ITEMS.register("prismium_shield",
            () -> new PrismiumShieldItem(new Item.Properties().durability(420)));

    // Prismium Bow (session 29): the mod's first ranged weapon and the
    // companion piece to the Shield (session 28) - see PrismiumBowItem
    // for why this extends vanilla BowItem directly (unlike Shield) and
    // for its innate-piercing gimmick. durability(460) sits above
    // vanilla's bow (384), matching the "higher durability, no flat
    // power creep" philosophy used across every other Prismium gear
    // item (tools/armor/shield).
    public static final RegistryObject<Item> PRISMIUM_BOW = ITEMS.register("prismium_bow",
            () -> new PrismiumBowItem(new Item.Properties().durability(460)));

    // Prismium Guardian Charm (session 30): the mod's first "cheat death"
    // item. stacksTo(1) mirrors vanilla's own Totem of Undying - see
    // PrismiumGuardianCharmItem / PrismiumGuardianCharmHandler for why
    // this item class carries no logic itself (all of it lives in the
    // LivingDeathEvent handler).
    public static final RegistryObject<Item> PRISMIUM_GUARDIAN_CHARM = ITEMS.register("prismium_guardian_charm",
            () -> new PrismiumGuardianCharmItem(new Item.Properties().stacksTo(1)));

    // Prismium Rift Shard (session 14): the mod's first way in/out of the
    // Prism Realm dimension. Not consumed on use (stacksTo(1), reusable
    // "key" item rather than a one-shot). See PrismiumRiftShardItem for
    // the teleport logic and known caveats.
    public static final RegistryObject<Item> PRISMIUM_RIFT_SHARD = ITEMS.register("prismium_rift_shard",
            () -> new PrismiumRiftShardItem(new Item.Properties().stacksTo(1)));

    // Prismium Locator (session 16): the mod's first detection item,
    // filling the other half of the "accessory items" roadmap wishlist
    // that the grappling hook (session 7) started. See
    // PrismiumLocatorItem for the block-scan implementation. stacksTo(1)
    // like the rift shard - this is a reusable tool, not a consumable.
    public static final RegistryObject<Item> PRISMIUM_LOCATOR = ITEMS.register("prismium_locator",
            () -> new PrismiumLocatorItem(new Item.Properties().stacksTo(1)));

    // Prismium Pulse Charm (scheduled session #63): the mod's fourth
    // detection accessory, this time for hostile mobs rather than ore -
    // see PrismiumPulseCharmItem. stacksTo(1) like the Locator/Rift
    // Shard/Guardian Charm - a reusable tool, not a consumable.
    public static final RegistryObject<Item> PRISMIUM_PULSE_CHARM = ITEMS.register("prismium_pulse_charm",
            () -> new PrismiumPulseCharmItem(new Item.Properties().stacksTo(1)));

    // Prismium Wraith spawn egg (session 12): uses Forge's ForgeSpawnEggItem
    // rather than vanilla SpawnEggItem because the latter needs the
    // EntityType eagerly at construction time, before ModEntities'
    // RegistryObject has resolved. Colors: dark violet base echoing the
    // "machine casing" dark palette used by Cell/Generator/Cable, with the
    // same cyan accent (PRISMIUM_ACCENT, see scripts/textures/common
    // palette notes in past sessions) as highlight spots, so the egg reads
    // as part of the Prismium family in the creative inventory.
    public static final RegistryObject<Item> PRISMIUM_WRAITH_SPAWN_EGG = ITEMS.register("prismium_wraith_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PRISMIUM_WRAITH, 0x2b1033, 0x39e6d6, new Item.Properties()));

    // Session 47: Prismium Deep Wraith spawn egg. Colors swapped to the
    // entity's own "waterlogged basalt + bioluminescent green" palette
    // (see scripts/textures/gen_prismium_deep_wraith.py) so it reads as a
    // distinct-but-related egg next to the land Wraith's in creative.
    public static final RegistryObject<Item> PRISMIUM_DEEP_WRAITH_SPAWN_EGG = ITEMS.register("prismium_deep_wraith_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PRISMIUM_DEEP_WRAITH, 0x1c3548, 0x7cffb8, new Item.Properties()));

    // Third mob (see PrismiumSentinelEntity's javadoc): ivory/gold egg
    // colors matching scripts/textures/gen_prismium_sentinel.py's new
    // FRAME_BASE/PRISMIUM_ACCENT palette, so this egg reads as visibly
    // distinct from the Wraith pair's in the creative inventory.
    public static final RegistryObject<Item> PRISMIUM_SENTINEL_SPAWN_EGG = ITEMS.register("prismium_sentinel_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PRISMIUM_SENTINEL, 0xb8ac94, 0xffd37c, new Item.Properties()));

    // Fourth mob (see PrismiumDrifterEntity's javadoc): dark violet base
    // (same family as the Wraith egg's 0x2b1033) with the mod's
    // established cyan PRISMIUM_ACCENT (0x39e6d6) as the spot color, so
    // it reads as part of the Prismium family while still being a
    // visibly distinct egg (spots, not base, carry the "family" color
    // this time) next to the other three in creative.
    public static final RegistryObject<Item> PRISMIUM_DRIFTER_SPAWN_EGG = ITEMS.register("prismium_drifter_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PRISMIUM_DRIFTER, 0x2b1033, 0x39e6d6, new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_BLOOM_ITEM = ITEMS.register("prismium_bloom",
            () -> new BlockItem(ModBlocks.PRISMIUM_BLOOM.get(), new Item.Properties()));

    // Prismium Spike (session 18): BlockItem for the mod's second Prism
    // Realm surface decoration - see ModBlocks.PRISMIUM_SPIKE.
    public static final RegistryObject<Item> PRISMIUM_SPIKE_ITEM = ITEMS.register("prismium_spike",
            () -> new BlockItem(ModBlocks.PRISMIUM_SPIKE.get(), new Item.Properties()));

    // Prism Lily (session 40): BlockItem for the mod's first Prism
    // Realm-exclusive surface decoration - see ModBlocks.PRISM_LILY.
    public static final RegistryObject<Item> PRISM_LILY_ITEM = ITEMS.register("prism_lily",
            () -> new BlockItem(ModBlocks.PRISM_LILY.get(), new Item.Properties()));

    // Prismium Soil (scheduled session #45): BlockItem for the mod's
    // first Prism Realm-exclusive ground block - see
    // ModBlocks.PRISMIUM_SOIL.
    public static final RegistryObject<Item> PRISMIUM_SOIL_ITEM = ITEMS.register("prismium_soil",
            () -> new BlockItem(ModBlocks.PRISMIUM_SOIL.get(), new Item.Properties()));

    // Prism Bramble (session 43): BlockItem for the mod's second Prism
    // Realm-exclusive surface decoration - see ModBlocks.PRISM_BRAMBLE.
    public static final RegistryObject<Item> PRISM_BRAMBLE_ITEM = ITEMS.register("prism_bramble",
            () -> new BlockItem(ModBlocks.PRISM_BRAMBLE.get(), new Item.Properties()));

    // Prism Vine (session 44): BlockItem for the mod's third Prism
    // Realm-exclusive surface decoration - see ModBlocks.PRISM_VINE.
    public static final RegistryObject<Item> PRISM_VINE_ITEM = ITEMS.register("prism_vine",
            () -> new BlockItem(ModBlocks.PRISM_VINE.get(), new Item.Properties()));

    // Prismium Pylon (session 19): BlockItem for the mod's first FE
    // consumer. See ModBlocks.PRISMIUM_PYLON. Same EnergyStorageBlockItem
    // persistence/tooltip treatment as Cell/Generator/Cable (session 11).
    public static final RegistryObject<Item> PRISMIUM_PYLON_ITEM = ITEMS.register("prismium_pylon",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_PYLON.get(), new Item.Properties(),
                    PrismiumPylonBlockEntity.CAPACITY));

    // Prismium Restorer (session 20): BlockItem for the mod's second FE
    // consumer. See ModBlocks.PRISMIUM_RESTORER. Same EnergyStorageBlockItem
    // persistence/tooltip treatment as every other Prismium Energy block.
    public static final RegistryObject<Item> PRISMIUM_RESTORER_ITEM = ITEMS.register("prismium_restorer",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_RESTORER.get(), new Item.Properties(),
                    PrismiumRestorerBlockEntity.CAPACITY));

    // Prismium Wardstone (session 21): BlockItem for the mod's third FE
    // consumer. See ModBlocks.PRISMIUM_WARDSTONE. Same EnergyStorageBlockItem
    // persistence/tooltip treatment as every other Prismium Energy block.
    public static final RegistryObject<Item> PRISMIUM_WARDSTONE_ITEM = ITEMS.register("prismium_wardstone",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_WARDSTONE.get(), new Item.Properties(),
                    PrismiumWardstoneBlockEntity.CAPACITY));

    // Prismium Pulverizer (session 67): BlockItem for the mod's sixth FE
    // consumer / first item-processing machine. See
    // ModBlocks.PRISMIUM_PULVERIZER. Same EnergyStorageBlockItem
    // persistence/tooltip treatment as every other Prismium Energy block
    // - note this only persists the "Energy" NBT key (see that class's
    // doc), not the input/output item slots, so items left in the
    // Pulverizer's slots when it is broken are lost, exactly the same
    // known limitation PrismiumGeneratorBlockEntity's fuel slot already
    // has (never fixed there either - see PROGRESS.md).
    public static final RegistryObject<Item> PRISMIUM_PULVERIZER_ITEM = ITEMS.register("prismium_pulverizer",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_PULVERIZER.get(), new Item.Properties(),
                    PrismiumPulverizerBlockEntity.CAPACITY));

    // Prismium Smelter (session 68): BlockItem for the mod's seventh FE
    // consumer / second item-processing machine. See
    // ModBlocks.PRISMIUM_SMELTER. Same EnergyStorageBlockItem
    // persistence/tooltip treatment as every other Prismium Energy block
    // - same known limitation as Pulverizer/Generator: only the "Energy"
    // NBT key persists across break/place, not the input/output slots.
    public static final RegistryObject<Item> PRISMIUM_SMELTER_ITEM = ITEMS.register("prismium_smelter",
            () -> new EnergyStorageBlockItem(ModBlocks.PRISMIUM_SMELTER.get(), new Item.Properties(),
                    PrismiumSmelterBlockEntity.CAPACITY));

    // Prismium Featherstone (session 31): the mod's first purely passive
    // accessory - see PrismiumFeatherstoneItem / PrismiumFeatherstoneHandler.
    // Deliberately left at the default stack size (not stacksTo(1)) - see
    // PrismiumFeatherstoneItem's javadoc for why this differs from the
    // Rift Shard/Locator/Guardian Charm pattern.
    public static final RegistryObject<Item> PRISMIUM_FEATHERSTONE = ITEMS.register("prismium_featherstone",
            () -> new PrismiumFeatherstoneItem(new Item.Properties()));

    // Prismium Emberguard (session 32): the mod's second purely passive
    // accessory - see PrismiumEmberguardItem / PrismiumEmberguardHandler.
    // Same "stacks normally, presence-only" treatment as Featherstone.
    public static final RegistryObject<Item> PRISMIUM_EMBERGUARD = ITEMS.register("prismium_emberguard",
            () -> new PrismiumEmberguardItem(new Item.Properties()));

    // Prismium Vitastone (session 33): the mod's third purely passive
    // accessory - see PrismiumVitastoneItem / PrismiumVitastoneHandler.
    // Same "stacks normally, presence-only" treatment as Featherstone/
    // Emberguard.
    public static final RegistryObject<Item> PRISMIUM_VITASTONE = ITEMS.register("prismium_vitastone",
            () -> new PrismiumVitastoneItem(new Item.Properties()));

    // Prismium Rift Anchor (session 48): the mod's first Rift Shard-
    // family derivative item - see PrismiumRiftAnchorItem for the full
    // design rationale (PROGRESS.md section 5 item 12(a)(i)). Default
    // stack size (not stacksTo(1)) since it is consumed on use, unlike
    // the reusable Rift Shard.
    public static final RegistryObject<Item> PRISMIUM_RIFT_ANCHOR = ITEMS.register("prismium_rift_anchor",
            () -> new PrismiumRiftAnchorItem(new Item.Properties()));

    // Prismium Chronoflame (scheduled session #49): BlockItem for
    // ModBlocks.PRISMIUM_CHRONOFLAME, using the custom
    // PrismiumChronoflameBlockItem subclass purely to add the "no drop on
    // break" usage tooltip - see that class's doc.
    public static final RegistryObject<Item> PRISMIUM_CHRONOFLAME_ITEM = ITEMS.register("prismium_chronoflame",
            () -> new PrismiumChronoflameBlockItem(ModBlocks.PRISMIUM_CHRONOFLAME.get(), new Item.Properties()));

    // Prismium Snare (session 64): BlockItem for ModBlocks.PRISMIUM_SNARE
    // - see that block's registration comment / PrismiumSnareBlock's
    // class doc for the mod's first gimmick/trap block.
    public static final RegistryObject<Item> PRISMIUM_SNARE_ITEM = ITEMS.register("prismium_snare",
            () -> new BlockItem(ModBlocks.PRISMIUM_SNARE.get(), new Item.Properties()));

    // Prismium Geyser (session 66): BlockItem for ModBlocks.PRISMIUM_GEYSER.
    public static final RegistryObject<Item> PRISMIUM_GEYSER_ITEM = ITEMS.register("prismium_geyser",
            () -> new BlockItem(ModBlocks.PRISMIUM_GEYSER.get(), new Item.Properties()));

    // Prismium Magnet Charm (session 65): the mod's fourth purely
    // passive accessory - see PrismiumMagnetCharmItem /
    // PrismiumMagnetCharmHandler. Same "stacks normally, presence-only"
    // treatment as Featherstone/Emberguard/Vitastone.
    public static final RegistryObject<Item> PRISMIUM_MAGNET_CHARM = ITEMS.register("prismium_magnet_charm",
            () -> new PrismiumMagnetCharmItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
