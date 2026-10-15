package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-10-15): Prismium Leap Charm - the mod's
 * eleventh purely passive "just carry it" accessory, following the
 * exact split {@link PrismiumFeatherstoneItem} (fall damage), {@link
 * PrismiumEmberguardItem} (fire/lava damage), {@link
 * PrismiumVitastoneItem} (heal amplification), {@link
 * PrismiumMagnetCharmItem} (item pickup), {@link
 * PrismiumAegisCharmItem} (explosion damage), {@link
 * PrismiumFrostguardCharmItem} (freeze damage), {@link
 * PrismiumAntivenomCharmItem} (poison/wither damage), {@link
 * PrismiumHasteCharmItem} (permanent Haste I), {@link
 * PrismiumLuckCharmItem} (permanent Luck I) and {@link
 * PrismiumAqualungCharmItem} (permanent Water Breathing) all
 * established: this class holds no gameplay logic at all, and the
 * entire effect (passive Jump Boost while carried) lives in {@link
 * com.claudemod.event.PrismiumLeapCharmHandler}'s {@code
 * TickEvent.PlayerTickEvent} listener. See that handler's javadoc for
 * the effect application details.
 *
 * <p>Concept: the fourth charm in this family whose effect is not a
 * damage-type mitigator, following {@link PrismiumHasteCharmItem},
 * {@link PrismiumLuckCharmItem} and {@link PrismiumAqualungCharmItem}'s
 * precedent. Permanent Jump Boost ties back to this mod's "exploration
 * itself should be fun" design goal (PROGRESS.md section 5): traversing
 * Prism Realm's uneven land/dungeon terrain (and vanilla terrain alike)
 * becomes noticeably easier without needing to drink or re-apply a
 * potion.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as every
 * other charm in this family - the effect only cares about presence
 * anywhere in inventory, not slot or quantity, so it stacks like a raw
 * material rather than a unique trinket.
 */
public class PrismiumLeapCharmItem extends Item {

    public PrismiumLeapCharmItem(Item.Properties properties) {
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
