package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.item.ModArmorMaterials;
import com.claudemod.item.ModToolTiers;
import com.claudemod.item.PrismiumGrapplingHookItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
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
    // entity/energy storage block. See ModBlocks.PRISMIUM_CELL.
    public static final RegistryObject<Item> PRISMIUM_CELL_ITEM = ITEMS.register("prismium_cell",
            () -> new BlockItem(ModBlocks.PRISMIUM_CELL.get(), new Item.Properties()));

    // Prismium Generator (session 9): BlockItem for the mod's first
    // BlockEntityTicker / automatic energy-transfer block. See
    // ModBlocks.PRISMIUM_GENERATOR.
    public static final RegistryObject<Item> PRISMIUM_GENERATOR_ITEM = ITEMS.register("prismium_generator",
            () -> new BlockItem(ModBlocks.PRISMIUM_GENERATOR.get(), new Item.Properties()));

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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
