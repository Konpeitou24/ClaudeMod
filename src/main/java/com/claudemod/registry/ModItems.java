package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.blockentity.PrismiumCableBlockEntity;
import com.claudemod.blockentity.PrismiumCellBlockEntity;
import com.claudemod.blockentity.PrismiumGeneratorBlockEntity;
import com.claudemod.blockentity.PrismiumPylonBlockEntity;
import com.claudemod.blockentity.PrismiumRestorerBlockEntity;
import com.claudemod.blockentity.PrismiumWardstoneBlockEntity;
import com.claudemod.item.EnergyStorageBlockItem;
import com.claudemod.item.ModArmorMaterials;
import com.claudemod.item.ModToolTiers;
import com.claudemod.item.PrismiumBowItem;
import com.claudemod.item.PrismiumEmberguardItem;
import com.claudemod.item.PrismiumFeatherstoneItem;
import com.claudemod.item.PrismiumGrapplingHookItem;
import com.claudemod.item.PrismiumGuardianCharmItem;
import com.claudemod.item.PrismiumLocatorItem;
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

    public static final RegistryObject<Item> PRISMIUM_ORE_ITEM = ITEMS.register("prismium_ore",
            () -> new BlockItem(ModBlocks.PRISMIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_PRISMIUM_ORE_ITEM = ITEMS.register("deepslate_prismium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_PRISMIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_BLOCK_ITEM = ITEMS.register("prismium_block",
            () -> new BlockItem(ModBlocks.PRISMIUM_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> PRISMIUM_CORE_ITEM = ITEMS.register("prismium_core",
            () -> new BlockItem(ModBlocks.PRISMIUM_CORE.get(), new Item.Properties()));

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

    public static final RegistryObject<Item> PRISMIUM_BLOOM_ITEM = ITEMS.register("prismium_bloom",
            () -> new BlockItem(ModBlocks.PRISMIUM_BLOOM.get(), new Item.Properties()));

    // Prismium Spike (session 18): BlockItem for the mod's second Prism
    // Realm surface decoration - see ModBlocks.PRISMIUM_SPIKE.
    public static final RegistryObject<Item> PRISMIUM_SPIKE_ITEM = ITEMS.register("prismium_spike",
            () -> new BlockItem(ModBlocks.PRISMIUM_SPIKE.get(), new Item.Properties()));

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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
