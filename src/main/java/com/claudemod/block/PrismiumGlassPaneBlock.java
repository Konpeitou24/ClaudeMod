package com.claudemod.block;

import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Thin public wrapper around vanilla's {@code IronBarsBlock}
 * (net.minecraft.world.level.block.IronBarsBlock, extends CrossCollisionBlock) -
 * the single Mojmap class vanilla uses for BOTH iron bars and every glass
 * pane/stained glass pane variant. Needed for the same reason as
 * {@link PrismiumButtonBlock}/{@link PrismiumPressurePlateBlock}: its own
 * constructor is {@code protected} in 1.20.1, confirmed before writing this
 * file via two independent, version-pinned documentation mirrors (per this
 * mod's "unverified Java API" rule in PROGRESS.md): mappings.dev's
 * Mojmap-named 1.20.1 page
 * (https://mappings.dev/1.20.1/net/minecraft/world/level/block/IronBarsBlock.html)
 * lists exactly one constructor, {@code protected IronBarsBlock(
 * BlockBehaviour$Properties settings)}, extending {@code CrossCollisionBlock};
 * the Yarn-mapped mirror (which names this same class {@code PaneBlock})
 * shows the identical single-parameter shape, {@code protected PaneBlock(
 * AbstractBlock.Settings settings)}. Neither mirror lists a public
 * subclass, so - same "reuse the vanilla class, add nothing new" spirit as
 * every other building-variant block in this mod, and the exact same
 * pattern as {@link PrismiumButtonBlock}/{@link PrismiumPressurePlateBlock}
 * - this wrapper is only a constructor forwarding to {@code super(...)},
 * with NO {@code @Override} of anything. Connection logic (which neighbor
 * panes/solid faces it links to), the six-way shape, and WATERLOGGED
 * handling (it implements {@code SimpleWaterloggedBlock}) are all inherited
 * unchanged from {@code CrossCollisionBlock}/{@code IronBarsBlock} - see
 * {@code blockstates/prismium_glass_pane.json} (multipart, copied from
 * vanilla's own {@code glass_pane.json} with only the model namespace
 * swapped) for the client-side connection rendering this produces.
 *
 * <p>This is the mod's first "connects to its neighbors" transparent block
 * (the cable block, {@link PrismiumCableBlock}, also connects to six
 * neighbors but is a fully custom {@code BaseEntityBlock} with its own
 * hand-written shape/model logic, not a reuse of a vanilla connecting
 * block) - reusing {@code IronBarsBlock} directly here, rather than
 * hand-rolling a second custom multipart block, keeps the compile risk at
 * the same low level as every other vanilla-class-reuse block in this file.
 */
public class PrismiumGlassPaneBlock extends IronBarsBlock {
    public PrismiumGlassPaneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
