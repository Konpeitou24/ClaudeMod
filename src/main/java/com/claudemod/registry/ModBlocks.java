package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.block.PrismiumCellBlock;
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

    // Condensed Prismium Core (session 3): the mod's first material that
    // sits *above* diamond in the harvest hierarchy - a diamond pickaxe
    // breaks it but drops nothing, only a Prismium tool harvests it. See
    // ModBlockTags for how that exclusivity is wired, and PROGRESS.md for
    // why it's flagged unverified. Crafted from Prismium Blocks + an
    // Amethyst Shard; foreshadows Prism Realm portal material.
    public static final RegistryObject<Block> PRISMIUM_CORE = BLOCKS.register("prismium_core",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(8.0f, 20.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 10)));

    // Prismium Lantern (session 4): the mod's first purely-utility
    // exploration block, not gated behind a tool tier. Deliberately modeled
    // on vanilla Lantern's own stats (hardness/resistance 3.5, breakable by
    // hand but efficient with a pickaxe via the mineable/pickaxe tag - see
    // data/minecraft/tags/blocks/mineable/pickaxe.json) rather than reusing
    // Prismium Block/Core's tool-gated strength values, since the point of
    // this block is to be crafted in bulk and placed liberally to light up
    // dungeons/caves during exploration. Max light level (15) distinguishes
    // it from Prismium Block (6) and Prismium Core (10). No
    // requiresCorrectToolForDrops(): like vanilla Lantern, it always drops
    // itself regardless of what (if anything) broke it.
    public static final RegistryObject<Block> PRISMIUM_LANTERN = BLOCKS.register("prismium_lantern",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5f, 3.5f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15)));

    // Prismium Cell (session 8): the mod's first block entity, and the
    // opening move of the long-dormant "Prismium Energy" roadmap pillar
    // (see PROGRESS.md section 1, item 2 - untouched since session 1).
    // Stores Forge Energy (FE) via a capability, no GUI yet. See
    // com.claudemod.block.PrismiumCellBlock and
    // com.claudemod.blockentity.PrismiumCellBlockEntity for the
    // implementation, and PROGRESS.md for what's still unverified.
    public static final RegistryObject<Block> PRISMIUM_CELL = BLOCKS.register("prismium_cell",
            () -> new PrismiumCellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 5)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
