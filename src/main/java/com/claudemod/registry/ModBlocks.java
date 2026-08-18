package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.block.PrismiumBloomBlock;
import com.claudemod.block.PrismiumCableBlock;
import com.claudemod.block.PrismiumCellBlock;
import com.claudemod.block.PrismiumGeneratorBlock;
import com.claudemod.block.PrismiumPylonBlock;
import com.claudemod.block.PrismiumRestorerBlock;
import com.claudemod.block.PrismiumSpikeBlock;
import com.claudemod.block.PrismiumWardstoneBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

    // Prismium Generator (session 9): burns Prismium Shards over time to
    // fill a small internal buffer with FE, then automatically pushes it
    // into any adjacent block exposing the Forge Energy capability (e.g.
    // a Prismium Cell placed next to it). The mod's first
    // BlockEntityTicker and first genuine block-to-block automatic
    // energy transfer - closes the loop Prismium Cell opened in session
    // 8. LIT (reused vanilla property, same one furnace/campfire use)
    // drives both the model swap and the light level: 8 while burning, 0
    // while idle. See PrismiumGeneratorBlock / PrismiumGeneratorBlockEntity
    // and PROGRESS.md for the full design rationale and what's unverified.
    public static final RegistryObject<Block> PRISMIUM_GENERATOR = BLOCKS.register("prismium_generator",
            () -> new PrismiumGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 8 : 0)));

    // Prismium Cable (session 10): a relay block for the Prismium Energy
    // pillar. Carries FE between a source (e.g. Prismium Generator) and a
    // sink (e.g. Prismium Cell) that aren't directly touching - the gap
    // the session 9 handoff notes flagged as the next obvious step. The
    // mod's first non-full-cube block; light strength/sound values kept
    // in line with the rest of the "machine" family (Cell/Generator)
    // rather than the tool-gated resource blocks. See
    // PrismiumCableBlock / PrismiumCableBlockEntity for the shape and
    // relay logic, and PROGRESS.md for what's unverified.
    public static final RegistryObject<Block> PRISMIUM_CABLE = BLOCKS.register("prismium_cable",
            () -> new PrismiumCableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 4)
                    .noOcclusion()));

    // Prismium Bloom (session 17): the mod's first surface decoration for
    // the Prism Realm dimension - see PrismiumBloomBlock for the full
    // rationale. noCollission()/instabreak() so it behaves like a vanilla
    // flower (walk through it, pop it in one hit); noOcclusion() since it's
    // a cross-quad, not a full cube, and would otherwise cull neighbouring
    // faces incorrectly. AMETHYST_CLUSTER sound (first use in this mod,
    // cross-checked against Forge 1.18.2 javadocs before use per the
    // session 15 "verify new symbols individually" lesson, PROGRESS.md
    // section 3N-3) reads as "crystal plant" rather than the flatter
    // AMETHYST sound already used across the mineral-family blocks.
    public static final RegistryObject<Block> PRISMIUM_BLOOM = BLOCKS.register("prismium_bloom",
            () -> new PrismiumBloomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 5)
                    .noOcclusion()));

    // Prismium Spike (session 18): the mod's second surface decoration,
    // a tall narrow crystal shard sibling to Prismium Bloom (session 17) -
    // see PrismiumSpikeBlock for the full design rationale. Same
    // noCollission()/instabreak()/noOcclusion() treatment as Bloom so it
    // behaves like a walk-through, one-hit-pop decorative prop rather
    // than a mineable block. Slightly brighter light level (7 vs Bloom's
    // 5) to read as a distinct "glowing crystal" accent rather than a
    // palette-swapped duplicate.
    public static final RegistryObject<Block> PRISMIUM_SPIKE = BLOCKS.register("prismium_spike",
            () -> new PrismiumSpikeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 7)
                    .noOcclusion()));

    // Prismium Pylon (session 19): the mod's first FE *consumer*, closing
    // the loop the Cell/Generator/Cable trio (sessions 8-10) opened but
    // never finished - see PrismiumPylonBlock / PrismiumPylonBlockEntity
    // for the full design rationale. Same "machine" family stats as
    // Cell/Generator (strength, AMETHYST sound); LIT drives light level
    // exactly like Generator (0 idle, here 9 while radiating - slightly
    // brighter than Generator's 8 to read as a more overtly "magical"
    // effect than a burning fuel source).
    public static final RegistryObject<Block> PRISMIUM_PYLON = BLOCKS.register("prismium_pylon",
            () -> new PrismiumPylonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 9 : 0)));

    // Prismium Restorer (session 20): the mod's second FE *consumer*,
    // after Prismium Pylon (session 19). See PrismiumRestorerBlock /
    // PrismiumRestorerBlockEntity for the repair-on-right-click design.
    // No LIT property here - like Prismium Cell (session 8), this block
    // has no BlockEntityTicker and thus no "idle vs active" state to
    // render, just a single static texture.
    public static final RegistryObject<Block> PRISMIUM_RESTORER = BLOCKS.register("prismium_restorer",
            () -> new PrismiumRestorerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)));

    // Prismium Wardstone (session 21): the mod's third FE *consumer*,
    // after Prismium Pylon (session 19, buffs players) and Prismium
    // Restorer (session 20, repairs items). Where those two are helpful
    // to the player directly, Wardstone is a defensive "area ward" that
    // weakens/slows nearby hostile mobs - see PrismiumWardstoneBlock /
    // PrismiumWardstoneBlockEntity for the full design rationale. Same
    // "machine" family stats as Cell/Generator/Pylon; LIT drives light
    // level exactly like Pylon/Generator (0 idle, here 10 while warding -
    // brighter than Pylon's 9 to read as an even more overt magical
    // effect, matching the "wider radius, defensive" framing).
    public static final RegistryObject<Block> PRISMIUM_WARDSTONE = BLOCKS.register("prismium_wardstone",
            () -> new PrismiumWardstoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0)));

    // Chiseled Prismium Block (session 34): the mod's first purely
    // decorative masonry variant - no new mechanics, just a second look
    // for Prismium Block so builders/explorers have a detail block to
    // break up flat surfaces, matching vanilla's
    // stone_bricks/chiseled_stone_bricks pairing. Same stats as
    // PRISMIUM_BLOCK (tool-gated, same hardness/resistance/sound/light)
    // since it's the same material, just cut differently - only the
    // texture differs. See gen_prismium_chiseled_block.py for the art.
    public static final RegistryObject<Block> CHISELED_PRISMIUM_BLOCK = BLOCKS.register("chiseled_prismium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 6)));

    // Prismium Block Slab (session 34): the mod's first SlabBlock, giving
    // builders half-height Prismium Block pieces for stairs/roofing/detail
    // work - the mod's roadmap has emphasized "exploration should be fun"
    // since its concept doc, but building variety for what players *do*
    // with what they find has been untouched since session 1. Plain
    // vanilla SlabBlock (no custom subclass needed, unlike every
    // block-entity-bearing block in this file) so bottom/top/double
    // placement and drop-count-on-break are entirely vanilla behavior -
    // deliberately the lowest-risk new block type added so far. Reuses
    // Prismium Block's own texture (see models/block/prismium_block_slab*
    // .json) rather than a new one, matching vanilla's own
    // slab-reuses-parent-texture convention (e.g. oak_slab/oak_planks).
    public static final RegistryObject<Block> PRISMIUM_BLOCK_SLAB = BLOCKS.register("prismium_block_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)));

    // Prismium Block Wall (session 34): the mod's first WallBlock,
    // pairing with the slab above to round out basic building variety.
    // Plain vanilla WallBlock, same low-risk rationale as the slab. Also
    // reuses Prismium Block's texture (see
    // models/block/prismium_block_wall_*.json).
    public static final RegistryObject<Block> PRISMIUM_BLOCK_WALL = BLOCKS.register("prismium_block_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
