package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
