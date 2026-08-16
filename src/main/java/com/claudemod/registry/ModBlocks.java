package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for every block added by ClaudeMod.
 *
 * Session 1 content: Prismium, the mod's first new resource. It's the seed
 * for the future "Prismium Energy" power system and the portal to the
 * upcoming Prism Realm dimension (see PROGRESS.md for the roadmap).
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ClaudeMod.MOD_ID);

    // Ore found in the overworld (mid-low depths), drops Prismium Shard.
    public static final RegistryObject<Block> PRISMIUM_ORE = BLOCKS.register("prismium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 3)));

    // Deepslate variant of the ore, found at lower depths.
    public static final RegistryObject<Block> DEEPSLATE_PRISMIUM_ORE = BLOCKS.register("deepslate_prismium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5f, 6.0f)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 3)));

    // Compressed storage block, crafted from 9 Prismium Shards.
    public static final RegistryObject<Block> PRISMIUM_BLOCK = BLOCKS.register("prismium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 6)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
