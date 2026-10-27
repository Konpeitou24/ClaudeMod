package com.claudemod.block;

import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Thin public wrapper around vanilla's {@code PressurePlateBlock}
 * (net.minecraft.world.level.block.PressurePlateBlock), for the same
 * reason as {@link PrismiumButtonBlock}: its own constructor is
 * {@code protected} in 1.20.1. Confirmed via the same two independent,
 * version-pinned mirrors before writing this file: mappings.dev's
 * Mojmap-named 1.20.1 page
 * (https://mappings.dev/1.20.1/net/minecraft/world/level/block/PressurePlateBlock.html)
 * lists exactly one constructor, {@code PressurePlateBlock(
 * PressurePlateBlock.Sensitivity type, BlockBehaviour.Properties settings,
 * BlockSetType blockSetType)} (protected, no public subclass listed), and
 * the Yarn-mapped mirror
 * (https://maven.fabricmc.net/docs/yarn-1.20.1+build.1/net/minecraft/block/PressurePlateBlock.html)
 * shows the identical three-parameter shape (Yarn names the first
 * parameter type {@code ActivationRule} instead of {@code Sensitivity} -
 * same enum, different mapping set, confirmed by both mirrors agreeing on
 * the constant names {@code EVERYTHING}/{@code MOBS}). No {@code @Override}
 * is added here, only a constructor forwarding to {@code super(...)}.
 *
 * <p>Every Prismium pressure plate registered from this class uses
 * {@link BlockSetType#STONE} (same stone/crystal/metal rationale as
 * {@link PrismiumButtonBlock}) and {@code Sensitivity.EVERYTHING}, matching
 * vanilla's own stone_pressure_plate: any entity stepping on the plate
 * triggers it (unlike the wooden weighted plates, which this mod does not
 * use).
 */
public class PrismiumPressurePlateBlock extends PressurePlateBlock {
    public PrismiumPressurePlateBlock(BlockBehaviour.Properties properties) {
        super(PressurePlateBlock.Sensitivity.EVERYTHING, properties, BlockSetType.STONE);
    }
}
