package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-10-18): Prismium Colossus Sigil Charm - the
 * mod's thirteenth purely passive "just carry it" accessory, following
 * the exact split every other charm in this family established: this
 * class holds no gameplay logic at all, and the entire effect
 * (permanent Strength I while carried) lives in {@link
 * com.claudemod.event.PrismiumColossusSigilCharmHandler}'s {@code
 * TickEvent.PlayerTickEvent} listener. See that handler's javadoc for
 * the effect application details.
 *
 * <p>Concept: HANDOFF.md's 2026-10-17 note (option (x)) flagged that
 * Prismium Colossus Core ({@link com.claudemod.registry.ModItems#
 * PRISMIUM_COLOSSUS_CORE}, dropped by this mod's first boss, Prismium
 * Colossus - v0.77.0 entity / v0.78.0 dungeon) had no crafting use at
 * all since it was introduced; this charm gives it one. It also answers
 * {@link com.claudemod.item.PrismiumSatiationCharmItem}'s javadoc note
 * that the family's "permanently apply an unused vanilla buff" angle
 * had run out of untouched effects by exploring a genuinely new
 * direction - combat offense (Strength) - rather than another
 * environmental mitigator or utility buff.
 *
 * <p>Deliberately gated behind a boss-drop ingredient (unlike every
 * earlier charm in this family, which only need overworld-obtainable
 * materials): this is meant to read as an actual trophy reward for
 * having beaten the mod's first boss, not another early-game pebble
 * charm, so {@link com.claudemod.registry.ModItems#
 * PRISMIUM_COLOSSUS_SIGIL_CHARM}'s recipe spends a single Colossus
 * Core (a boss-only drop) rather than being reproducible purely from
 * mined/farmed materials.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as every
 * other charm in this family - the effect only cares about presence
 * anywhere in inventory, not slot or quantity, so it stacks like a raw
 * material rather than a unique trinket (even though the recipe cost
 * makes crafting more than one a rare event in practice).
 */
public class PrismiumColossusSigilCharmItem extends Item {

    public PrismiumColossusSigilCharmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // Same passive-accessory hint pattern as every other charm in this family.
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
