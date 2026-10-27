package com.claudemod.block;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Thin public wrapper around vanilla's {@code ButtonBlock}
 * (net.minecraft.world.level.block.ButtonBlock), needed only because that
 * class's own constructor is {@code protected} in 1.20.1 - confirmed before
 * writing this file by cross-checking two independent, version-pinned
 * documentation mirrors (per this mod's "unverified Java API" rule in
 * PROGRESS.md): mappings.dev's Mojmap-named 1.20.1 page
 * (https://mappings.dev/1.20.1/net/minecraft/world/level/block/ButtonBlock.html)
 * lists exactly one constructor, {@code protected ButtonBlock(
 * BlockBehaviour$Properties settings, BlockSetType blockSetType,
 * int pressTicks, boolean wooden)}, and the equivalent Yarn-mapped mirror
 * (https://maven.fabricmc.net/docs/yarn-1.20.1+build.1/net/minecraft/block/ButtonBlock.html)
 * shows the identical four-parameter shape under Yarn's own names. Neither
 * mirror lists any "Direct Known Subclasses" - an initial guess that
 * vanilla exposes public WoodButtonBlock/StoneButtonBlock subclasses (the
 * way some very old Yarn builds' class list made it look) was checked and
 * turned out to be wrong for this version, so this wrapper - a constructor
 * that only calls {@code super(...)}, no {@code @Override} of anything -
 * is the correct low-risk way to reuse vanilla's button behaviour, in the
 * same "reuse the vanilla class, add nothing new" spirit as every other
 * building-variant block in this mod (Slab/Wall/Stairs/Fence/FenceGate/
 * TrapDoorBlock/DoorBlock all have public constructors and needed no
 * wrapper at all; Button/PressurePlate are simply the first two vanilla
 * building-variant classes this mod has used whose constructor happens to
 * be protected instead of public).
 *
 * <p>Every Prismium button registered from this class uses
 * {@link BlockSetType#STONE} (this mod's button materials are all
 * stone/crystal/metal, never wood, matching vanilla's own stone_button
 * choice of block set type) and {@code wooden=false}, so - like vanilla's
 * stone button and unlike its wooden ones - these cannot be pressed by
 * arrows. {@code pressTicks=20} matches vanilla stone button's own
 * stay-pressed duration.
 */
public class PrismiumButtonBlock extends ButtonBlock {
    public PrismiumButtonBlock(BlockBehaviour.Properties properties) {
        super(properties, BlockSetType.STONE, 20, false);
    }
}
