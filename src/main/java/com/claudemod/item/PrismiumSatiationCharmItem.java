package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-10-16): Prismium Satiation Charm - the mod's
 * twelfth purely passive "just carry it" accessory, following the exact
 * split {@link PrismiumFeatherstoneItem} (fall damage), {@link
 * PrismiumEmberguardItem} (fire/lava damage), {@link
 * PrismiumVitastoneItem} (heal amplification), {@link
 * PrismiumMagnetCharmItem} (item pickup), {@link PrismiumAegisCharmItem}
 * (explosion damage), {@link PrismiumFrostguardCharmItem} (freeze
 * damage), {@link PrismiumAntivenomCharmItem} (poison/wither damage),
 * {@link PrismiumHasteCharmItem} (permanent Haste I), {@link
 * PrismiumLuckCharmItem} (permanent Luck I), {@link
 * PrismiumAqualungCharmItem} (permanent Water Breathing) and {@link
 * PrismiumLeapCharmItem} (permanent Jump Boost I) all established: this
 * class holds no gameplay logic at all, and the entire effect (passive
 * Saturation I while carried) lives in {@link
 * com.claudemod.event.PrismiumSatiationCharmHandler}'s {@code
 * TickEvent.PlayerTickEvent} listener. See that handler's javadoc for
 * the effect application details.
 *
 * <p>Concept: the fifth charm in this family whose effect is not a
 * damage-type mitigator, following {@link PrismiumHasteCharmItem},
 * {@link PrismiumLuckCharmItem}, {@link PrismiumAqualungCharmItem} and
 * {@link PrismiumLeapCharmItem}'s precedent, and (per PROGRESS.md's
 * 2026-10-15 note) the last standing-effect idea identified before this
 * family would need to look beyond "an unused vanilla MobEffect applied
 * permanently" for its next member. Permanent Saturation I keeps the
 * hunger gauge topped up without needing to stop and eat, which ties
 * back to this mod's "exploration itself should be fun" design goal
 * (PROGRESS.md section 5) the same way Leap Charm's permanent Jump
 * Boost does: long expeditions into Prism Realm or vanilla terrain no
 * longer need to be interrupted to manage food.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as every other
 * charm in this family - the effect only cares about presence anywhere
 * in inventory, not slot or quantity, so it stacks like a raw material
 * rather than a unique trinket.
 */
public class PrismiumSatiationCharmItem extends Item {

    public PrismiumSatiationCharmItem(Item.Properties properties) {
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
