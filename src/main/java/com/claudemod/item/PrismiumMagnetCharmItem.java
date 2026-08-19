package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Session 65 (scheduled): Prismium Magnet Charm - the mod's fourth purely
 * passive accessory, joining Featherstone (session 31, fall damage),
 * Emberguard (session 32, fire/lava damage) and Vitastone (session 33) in
 * the "just carry it anywhere in your inventory" family. Same split as
 * those three: this class carries no gameplay logic at all - the actual
 * effect (pulling nearby dropped {@code ItemEntity}s toward the carrying
 * player every tick) lives entirely in {@link
 * com.claudemod.event.PrismiumMagnetCharmHandler}'s {@code
 * TickEvent.PlayerTickEvent} listener, mirroring how Featherstone/
 * Emberguard hook a vanilla event instead of putting logic on the item.
 *
 * <p>Concept: this mod is mining/exploration-heavy (ore veins, worldgen
 * loot, mob drops) but had no item addressing the everyday friction of
 * walking back and forth to scoop up scattered drops - Locator (session
 * 16) finds ore, Pulse Charm (session 63) finds threats, but nothing
 * helped with actually collecting what's already on the ground. Deliberately
 * scoped to dropped items only (not experience orbs): vanilla {@code
 * ExperienceOrb} already homes in on the nearest player within 8 blocks on
 * its own (see the handler's javadoc), so adding orb-pulling here would
 * have been redundant with existing vanilla behavior rather than a new
 * capability.
 *
 * <p>Not {@code stacksTo(1)}: same reasoning as Featherstone/Emberguard/
 * Vitastone - the effect only cares about presence, not slot or quantity,
 * so it stacks like a raw material rather than a unique trinket.
 */
public class PrismiumMagnetCharmItem extends Item {

    public PrismiumMagnetCharmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
