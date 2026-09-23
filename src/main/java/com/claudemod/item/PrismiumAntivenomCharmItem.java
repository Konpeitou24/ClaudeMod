package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-09-23): Prismium Antivenom Charm - the
 * mod's seventh purely passive "just carry it" accessory, following
 * the exact split {@link PrismiumFeatherstoneItem} (fall damage),
 * {@link PrismiumEmberguardItem} (fire/lava damage), {@link
 * PrismiumVitastoneItem} (heal amplification), {@link
 * PrismiumMagnetCharmItem} (item pickup), {@link
 * PrismiumAegisCharmItem} (explosion damage) and {@link
 * PrismiumFrostguardCharmItem} (freeze damage) all established: this
 * class holds no gameplay logic at all, and the entire effect
 * (poison/wither damage reduction) lives in {@link
 * com.claudemod.event.PrismiumAntivenomCharmHandler}'s {@code
 * LivingDamageEvent} listener. See that handler's javadoc for the
 * damage-type check and the reduction multiplier.
 *
 * <p>Concept: PROGRESS.md's 2026-09-19/20 notes singled out
 * "poison/wither" as the one remaining common environmental damage
 * type not yet covered once Featherstone (fall), Emberguard
 * (fire/lava), Aegis Charm (explosion) and Frostguard Charm (freeze)
 * were in place. This closes that gap, completing the mod's coverage
 * of the main vanilla "you're taking damage over time from a status
 * effect or hazard, not from an attack" categories.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as every
 * other passive charm in this family - the effect only cares about
 * presence anywhere in inventory, not slot or quantity, so it stacks
 * like a raw material rather than a unique trinket.
 */
public class PrismiumAntivenomCharmItem extends Item {

    public PrismiumAntivenomCharmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // Same passive-accessory hint pattern as every other charm in
        // this family (Featherstone/Emberguard/Vitastone/Magnet Charm/
        // Aegis Charm/Frostguard Charm).
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
