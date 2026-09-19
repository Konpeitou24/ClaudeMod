package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-09-19): Prismium Aegis Charm - the mod's
 * fifth purely passive "just carry it" accessory, following the exact
 * split {@link PrismiumFeatherstoneItem} (session 31, fall damage),
 * {@link PrismiumEmberguardItem} (session 32, fire/lava damage), {@link
 * PrismiumVitastoneItem} (session 33, heal amplification) and {@link
 * PrismiumMagnetCharmItem} (session 65, item pickup) all established:
 * this class holds no gameplay logic at all, and the entire effect
 * (explosion damage reduction) lives in {@link
 * com.claudemod.event.PrismiumAegisCharmHandler}'s {@code
 * LivingDamageEvent} listener. See that handler's javadoc for the
 * damage-type check and the reduction multiplier.
 *
 * <p>Concept: the mod's passive-charm family already covers fall
 * (Featherstone), fire/lava (Emberguard) and drowning is left to
 * vanilla water breathing/the armor set bonus - but nothing addressed
 * explosion damage, despite the mod's own Prismium Wardstone (a
 * defensive structure block) and future Prism Realm dungeon bosses
 * (PROGRESS.md TODO9) both being likely sources of blast damage. This
 * rounds out the "environmental damage type" trio (fall/fire/blast)
 * the mod's own hazards are most likely to deal.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as
 * Featherstone/Emberguard/Vitastone/Magnet Charm - the effect only
 * cares about presence anywhere in inventory, not slot or quantity, so
 * it stacks like a raw material rather than a unique trinket.
 */
public class PrismiumAegisCharmItem extends Item {

    public PrismiumAegisCharmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // Same passive-accessory hint pattern as Featherstone/Emberguard/Vitastone/Magnet Charm.
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
