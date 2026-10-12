package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-10-12): Prismium Luck Charm - the mod's
 * ninth purely passive "just carry it" accessory, following the exact
 * split {@link PrismiumFeatherstoneItem} (fall damage), {@link
 * PrismiumEmberguardItem} (fire/lava damage), {@link
 * PrismiumVitastoneItem} (heal amplification), {@link
 * PrismiumMagnetCharmItem} (item pickup), {@link
 * PrismiumAegisCharmItem} (explosion damage), {@link
 * PrismiumFrostguardCharmItem} (freeze damage), {@link
 * PrismiumAntivenomCharmItem} (poison/wither damage) and {@link
 * PrismiumHasteCharmItem} (permanent Haste I) all established: this
 * class holds no gameplay logic at all, and the entire effect (passive
 * Luck I while carried) lives in {@link
 * com.claudemod.event.PrismiumLuckCharmHandler}'s {@code
 * TickEvent.PlayerTickEvent} listener. See that handler's javadoc for
 * the effect application details.
 *
 * <p>Concept: the second charm in this family whose effect is not a
 * damage-type mitigator, following {@link PrismiumHasteCharmItem}'s
 * precedent (see that item's javadoc for why the family moved past
 * damage-reduction-only effects at v0.72.0). A permanent Luck I buff
 * ties back to this mod's "exploration itself should be fun" design
 * goal (PROGRESS.md section 5) from a different angle than Haste's
 * mining-speed convenience: slightly better fishing/looting/bartering
 * odds reward exploring Prism Realm and fighting its mobs for loot,
 * without touching mining at all.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as every
 * other charm in this family - the effect only cares about presence
 * anywhere in inventory, not slot or quantity, so it stacks like a raw
 * material rather than a unique trinket.
 */
public class PrismiumLuckCharmItem extends Item {

    public PrismiumLuckCharmItem(Item.Properties properties) {
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
