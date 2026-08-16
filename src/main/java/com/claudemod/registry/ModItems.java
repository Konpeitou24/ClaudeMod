package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.item.ModToolTiers;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
