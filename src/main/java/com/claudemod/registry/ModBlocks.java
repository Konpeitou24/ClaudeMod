package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.block.PrismBrambleBlock;
import com.claudemod.block.PrismVineBlock;
import com.claudemod.block.PrismLilyBlock;
import com.claudemod.block.PrismiumBloomBlock;
import com.claudemod.block.PrismiumCableBlock;
import com.claudemod.block.PrismiumCellBlock;
import com.claudemod.block.PrismiumCompressorBlock;
import com.claudemod.block.PrismiumChronoflameBlock;
import com.claudemod.block.PrismiumGeneratorBlock;
import com.claudemod.block.PrismiumGeyserBlock;
import com.claudemod.block.PrismiumPortalBlock;
import com.claudemod.block.PrismiumPulverizerBlock;
import com.claudemod.block.PrismiumPylonBlock;
import com.claudemod.block.PrismiumRestorerBlock;
import com.claudemod.block.PrismiumSmelterBlock;
import com.claudemod.block.PrismiumSnareBlock;
import com.claudemod.block.PrismiumSpikeBlock;
import com.claudemod.block.PrismiumWardstoneBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
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

    // Session 47: Prismium Stone, the plain stone-equivalent fill block
    // for the Prism Realm's new flat chunk generator (see
    // data/claudemod/dimension/prism_realm.json). Directly requested by
    // the repo owner: ordinary overworld stone/dirt were still generating
    // in the dimension (the old generator reused minecraft:overworld noise
    // settings wholesale, see PROGRESS.md session 47), and ordinary
    // "recolor the ore" texture reuse was explicitly asked for. Texture
    // (scripts/textures/gen_prismium_stone.py) reuses prismium_ore.png's
    // exact grey stone-base palette (sampled directly from that file) with
    // only a handful of very sparse teal flecks - a "family resemblance"
    // to the ore without looking like an ore block itself, since this is
    // meant to be the mundane bulk fill material, not something to mine
    // for its own sake.
    public static final RegistryObject<Block> PRISMIUM_STONE = BLOCKS.register("prismium_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.STONE)));

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

    // Prism Lily (session 40): the mod's third surface decoration and
    // its first plant exclusive to the Prism Realm dimension (see
    // PrismLilyBlock for the full design rationale and PROGRESS.md
    // section 5 item 9). Uses MapColor.COLOR_PURPLE (new map color for
    // this mod's plant family, distinct from Bloom/Spike's
    // COLOR_CYAN) to reflect its violet/magenta palette, which is
    // deliberately different from Bloom/Spike's teal Prismium-crystal
    // look so it reads as native alien flora rather than a
    // palette-swapped duplicate. Dimmer light level (3) than Bloom (5)
    // or Spike (7) - a softer ambient glow rather than another bright
    // crystal light source, so the Realm doesn't end up with three
    // near-identical glowing props.
    public static final RegistryObject<Block> PRISM_LILY = BLOCKS.register("prism_lily",
            () -> new PrismLilyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 3)
                    .noOcclusion()));

    // Prism Bramble (session 43): the mod's second plant exclusive to
    // the Prism Realm dimension (see PrismBrambleBlock / PROGRESS.md
    // section 5 item 9(c)). Deliberately given a different silhouette
    // from Prism Lily (thorny three-pronged fan vs. rounded flower) so
    // the two exclusive plants read as different species at a glance,
    // not a palette-swapped duplicate. Reuses Lily's exact block
    // properties (MapColor.COLOR_PURPLE, same "family") except a
    // slightly dimmer light level (2 vs Lily's 3) - a bramble is
    // meant to read as a duller understory plant next to Lily's
    // brighter bloom, not a third near-identical light source.
    public static final RegistryObject<Block> PRISM_BRAMBLE = BLOCKS.register("prism_bramble",
            () -> new PrismBrambleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 2)
                    .noOcclusion()));

    // Prism Vine (session 44): the mod's third plant exclusive to the
    // Prism Realm dimension (see PrismVineBlock / PROGRESS.md section 5
    // item 9(c), which asked for a third plant with a different "growth
    // direction" than Lily/Bramble). Reuses the same violet "family"
    // properties as Lily/Bramble; dimmest light level of the three (1,
    // vs Bramble's 2 and Lily's 3) since a ground-hugging tangle in
    // shadow reads as the least luminous of the three by design.
    public static final RegistryObject<Block> PRISM_VINE = BLOCKS.register("prism_vine",
            () -> new PrismVineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 1)
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

    // Prismium Pulverizer (session 67): the mod's sixth GUI'd energy
    // block and first item-processing machine - see
    // PrismiumPulverizerBlockEntity for why. Same casing
    // strength/sound/mapColor family as every other machine block in
    // this mod; light level matches Generator's active-glow treatment
    // (8 while lit) rather than Wardstone/Pylon's steadier 10, since this
    // block's LIT state toggles far more often (once per operation, not
    // once per multi-second pulse) and a slightly dimmer glow reads
    // better for something flickering on/off that quickly.
    public static final RegistryObject<Block> PRISMIUM_PULVERIZER = BLOCKS.register("prismium_pulverizer",
            () -> new PrismiumPulverizerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 8 : 0)));

    // Prismium Smelter (session 68): the mod's seventh GUI'd energy
    // block and second item-processing machine - see
    // PrismiumSmelterBlockEntity for why (Ore -> Shard -> Ingot chain).
    // Same casing/strength/sound family as every other machine block;
    // light level matches Pulverizer's active-glow treatment (8 while
    // lit) since this block's LIT state toggles on the same per-
    // operation cadence, not a steadier multi-second pulse.
    public static final RegistryObject<Block> PRISMIUM_SMELTER = BLOCKS.register("prismium_smelter",
            () -> new PrismiumSmelterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 8 : 0)));

    // Prismium Compressor (session 70, scheduled): the mod's eighth
    // GUI'd energy block and third item-processing machine - see
    // PrismiumCompressorBlockEntity for why (Ingot -> Alloy Ingot, one
    // step past Smelter in the same chain). Same casing/strength/sound
    // family as every other machine block; light level matches
    // Pulverizer/Smelter's active-glow treatment (8 while lit) since
    // this block's LIT state toggles on the same per-operation cadence.
    public static final RegistryObject<Block> PRISMIUM_COMPRESSOR = BLOCKS.register("prismium_compressor",
            () -> new PrismiumCompressorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 8 : 0)));

    // Prismium Alloy Block (session 70, scheduled): a compact-storage
    // block for Prismium Alloy Ingot (see ModItems.PRISMIUM_ALLOY_INGOT),
    // the mod's second refined-material item and Compressor's product.
    // Same shape/role as PRISMIUM_BLOCK is to Prismium Shard (9 ingots
    // <-> 1 block, see recipes/prismium_alloy_block.json and
    // recipes/prismium_alloy_ingot_from_block.json) - gives Alloy Ingot
    // an immediate crafting-table use this same session, deliberately
    // avoiding the "refined material with no use yet" gap Prismium
    // Ingot itself had for one session (session 68) before Warhammer
    // (session 69) filled it. Same stats as PRISMIUM_BLOCK.
    public static final RegistryObject<Block> PRISMIUM_ALLOY_BLOCK = BLOCKS.register("prismium_alloy_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 6)));

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
    // BUG FIX (reported by user, addressed same-day as session 35):
    // placing two of these walls next to each other did not connect -
    // vanilla WallBlock#connectsTo() only treats a neighbor as
    // connectable if it's in the `minecraft:walls` block tag (or is a
    // full solid cube); a wall post's own hitbox isn't a full cube, so
    // without the tag two walls just sat as isolated posts. Session 34
    // never created data/minecraft/tags/blocks/walls.json for this mod,
    // so PRISMIUM_BLOCK_WALL was never added to it. Fixed by adding that
    // tag file with this block in it - still unverified in an actual
    // game render (this sandbox can't launch Minecraft), but the root
    // cause (confirmed by reading vanilla WallBlock's connectsTo logic)
    // matches the reported symptom exactly.
    public static final RegistryObject<Block> PRISMIUM_BLOCK_WALL = BLOCKS.register("prismium_block_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)));

    // Prismium Block Stairs (session 35): completes the basic building
    // trio started with the slab/wall in session 34 (§3AG-2 of
    // PROGRESS.md deliberately postponed this one - the 40-entry
    // facing/half/shape blockstate with its per-variant x/y rotations
    // was too easy to get subtly wrong from memory, and a wrong rotation
    // wouldn't fail the build, just look broken in-game with no way for
    // this sandbox to notice). This session sourced the exact vanilla
    // oak_stairs.json blockstate from two independently-fetched mirrors
    // of InventivetalentDev/minecraft-assets - github.com/edayot/
    // model_resolver's bundled datapack-tool fixture (oak_stairs.json),
    // and mcasset.cloud's acacia_stairs.json for 1.20.1-rc1 - which
    // matched exactly (same 40 rotation/uvlock values) before
    // transcribing prismium_block_stairs.json; see PROGRESS.md session
    // 35 for both URLs. Plain vanilla StairBlock, same low-risk
    // rationale as the slab/wall: no custom subclass, no event
    // listeners. Reuses Prismium Block's texture like the other two
    // building-variety blocks.
    public static final RegistryObject<Block> PRISMIUM_BLOCK_STAIRS = BLOCKS.register("prismium_block_stairs",
            () -> new StairBlock(() -> PRISMIUM_BLOCK.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.AMETHYST)));

    // Prismium Core building variety (session 36): extends the slab/wall/
    // stairs trio already proven out on Prismium Block (sessions 34-35,
    // PROGRESS.md handoff item 8-d) to Prismium Core. Same low-risk
    // rationale - plain vanilla SlabBlock/WallBlock/StairBlock, no custom
    // subclasses - but keeps Core's own stats (requiresCorrectToolForDrops,
    // strength 8.0/20.0) instead of Prismium Block's, and reuses Core's
    // own texture. Also added to needs_diamond_tool/incorrect_for_diamond_tool
    // (see data/minecraft/tags/blocks/) so the harvest-tier exclusivity
    // carries over to these variants too.
    public static final RegistryObject<Block> PRISMIUM_CORE_SLAB = BLOCKS.register("prismium_core_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(8.0f, 20.0f)
                    .sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> PRISMIUM_CORE_WALL = BLOCKS.register("prismium_core_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(8.0f, 20.0f)
                    .sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> PRISMIUM_CORE_STAIRS = BLOCKS.register("prismium_core_stairs",
            () -> new StairBlock(() -> PRISMIUM_CORE.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(8.0f, 20.0f)
                    .sound(SoundType.AMETHYST)));

    // Chiseled Prismium Core (session 37): the decorative-masonry
    // treatment already given to Prismium Block (session 34,
    // CHISELED_PRISMIUM_BLOCK above) extended to Prismium Core, so both
    // resource blocks in the mod now have a detail/panel variant. Same
    // stats as PRISMIUM_CORE (tool-gated behind the custom Prismium tier,
    // same hardness/resistance/sound/light) since it's the same material
    // just cut differently - only the texture differs. See
    // gen_prismium_chiseled_core.py for the art.
    public static final RegistryObject<Block> CHISELED_PRISMIUM_CORE = BLOCKS.register("chiseled_prismium_core",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(8.0f, 20.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 10)));

    // Prismium Soil (scheduled session #45): the mod's first Prism Realm
    // -exclusive GROUND block, addressing the long-flagged gap that the
    // dimension's terrain itself was still plain overworld grass/dirt
    // despite custom biome colors (session 39), boosted ore density
    // (session 41), and three exclusive plants (sessions 40/43/44). Not
    // placed by ordinary worldgen noise/surface rules (see
    // PrismiumSoilFeature for why that approach was judged too risky to
    // verify) - instead a decoration-step Feature repaints grass_block/
    // dirt/coarse_dirt to this block, restricted to claudemod:prism_realm
    // via the usual biome_modifier technique. Dark violet-indigo palette
    // sampled from the biome's own sky/fog colors (see
    // gen_prismium_soil.py), NOT the bright teal/magenta PRISMIUM_*
    // crystal palette, so mile after mile of it doesn't read as "a floor
    // made of gemstone" - only sparse embedded flecks tie it back to the
    // mod's crystal family. Plain dirt-like stats (soft, no tool
    // required), mineable/shovel like vanilla dirt.
    public static final RegistryObject<Block> PRISMIUM_SOIL = BLOCKS.register("prismium_soil",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.5f)
                    .sound(SoundType.GRAVEL)));

    // Prismium Chronoflame (scheduled session #49): PROGRESS.md section 5
    // item 12(a)(ii), a "campfire-like block that can freely change the
    // time, but doesn't drop an item when broken" per the repo owner's
    // request. See PrismiumChronoflameBlock's class doc for the
    // interaction/API details. noLootTable() is the literal mechanism
    // for "doesn't drop an item when broken" (confirmed a genuine no-arg
    // BlockBehaviour.Properties method via WebSearch this session, see
    // class doc) - deliberately no loot_tables/blocks JSON exists for
    // this block at all. Stats loosely modelled on PRISMIUM_LANTERN
    // (also always-lit, similar strength) since both are "small lit
    // fixture" blocks, with slightly higher blast resistance since this
    // one is meant to read as a more deliberate, semi-permanent shrine
    // piece rather than a portable light source - an arbitrary judgment
    // call, unplaytested (see PROGRESS.md).
    public static final RegistryObject<Block> PRISMIUM_CHRONOFLAME = BLOCKS.register("prismium_chronoflame",
            () -> new PrismiumChronoflameBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5f, 9.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 14)
                    .noLootTable()));

    // Session 52: Prismium Portal, the mod's first standing dimension
    // gateway (see PrismiumPortalBlock's class doc for the full design/
    // GitHub issue #9 writeup). No BlockItem is registered for this in
    // ModItems - like vanilla's nether_portal, it should never be
    // obtainable as an item, only ever placed by
    // PrismiumPortalIgniteHandler igniting a Prismium Core frame.
    // strength(-1.0F) + noLootTable() together make it unbreakable/
    // undroppable by any normal means, matching vanilla portal blocks.
    public static final RegistryObject<Block> PRISMIUM_PORTAL = BLOCKS.register("prismium_portal",
            () -> new PrismiumPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .strength(-1.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .noLootTable()));

    // Prismium Snare (session 64): the mod's first genuine gimmick/trap
    // block (see PrismiumSnareBlock's class doc for full design). Same
    // "family" properties as the other Prism Realm-exclusive flora
    // (Bramble/Lily/Vine) so it visually blends in with them at a
    // glance - noCollission()/instabreak()/AMETHYST_CLUSTER sound - the
    // camouflage is the point. lightLevel omitted (unlike Bramble/Lily/
    // Vine, which glow faintly) so it doesn't visually stand out as
    // "the one plant that's slightly brighter," which would defeat the
    // whole "blend in" premise.
    public static final RegistryObject<Block> PRISMIUM_SNARE = BLOCKS.register("prismium_snare",
            () -> new PrismiumSnareBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .noOcclusion()));

    // Prismium Geyser (session 66): the mod's second "gimmick" block
    // after Prismium Snare above, and the first with a positive/
    // traversal-boosting effect (see PrismiumGeyserBlock class doc for
    // the stepOn-vs-entityInside API rationale). A full, solid, walkable
    // cube (unlike Snare's no-collision cross-quad) since stepOn needs an
    // entity to actually be supported on top of the block. Strength/
    // sound modeled on Prismium Core (a "built into the ground" utility
    // block) rather than Lantern's hand-breakable stats, since a launch
    // pad that's too easy to break defeats a "found this out in the
    // world" discovery moment. Modest lightLevel (5) - noticeable but not
    // meant to compete with Lantern (15) or Core (10) as a light source,
    // this is a gimmick block first.
    public static final RegistryObject<Block> PRISMIUM_GEYSER = BLOCKS.register("prismium_geyser",
            () -> new PrismiumGeyserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .strength(5.0f, 8.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 5)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
