package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Session 33: Prismium Vitastone - the mod's third purely passive
 * "just carry it" accessory, following the exact split Featherstone
 * (session 31) and Emberguard (session 32) established: this class
 * holds no logic at all, and the entire effect (amplifying incoming
 * healing) lives in {@link com.claudemod.event.PrismiumVitastoneHandler}'s
 * {@code LivingHealEvent} listener. See that handler's javadoc for the
 * amplification multiplier and the reasoning behind it.
 *
 * <p>Like Featherstone/Emberguard, deliberately not {@code
 * stacksTo(1)}: the effect only cares about presence anywhere in the
 * player's inventory, not about which single copy is "the" active
 * one, so nothing is lost by letting it stack like a raw material
 * rather than a unique key item (Rift Shard/Locator/Guardian Charm's
 * category).
 */
public class PrismiumVitastoneItem extends Item {

    public PrismiumVitastoneItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // GitHub issue #7 (scheduled session): same passive-accessory hint pattern as Featherstone/Emberguard.
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
