package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-10-10): Prismium Haste Charm - the mod's
 * eighth purely passive "just carry it" accessory, following the exact
 * split {@link PrismiumFeatherstoneItem} (fall damage), {@link
 * PrismiumEmberguardItem} (fire/lava damage), {@link
 * PrismiumVitastoneItem} (heal amplification), {@link
 * PrismiumMagnetCharmItem} (item pickup), {@link
 * PrismiumAegisCharmItem} (explosion damage), {@link
 * PrismiumFrostguardCharmItem} (freeze damage) and {@link
 * PrismiumAntivenomCharmItem} (poison/wither damage) all established:
 * this class holds no gameplay logic at all, and the entire effect
 * (passive Haste I while carried) lives in {@link
 * com.claudemod.event.PrismiumHasteCharmHandler}'s {@code
 * TickEvent.PlayerTickEvent} listener. See that handler's javadoc for
 * the effect application details.
 *
 * <p>Concept: HANDOFF.md's 2026-10-09 note (option (r)) pointed out
 * that every environmental-damage-reduction angle for this charm
 * family (fall/fire/explosion/freeze/poison-wither) was already
 * covered, and suggested that the family's next entry, if any, should
 * be a non-damage-reduction effect. This is the first charm in the
 * family that is not a damage mitigator at all: it is a miner's/
 * explorer's convenience item, granting a permanent Haste I buff so
 * digging through Prism Realm stone or building a base goes a little
 * faster, tying back to this mod's "exploration itself should be fun"
 * design goal (PROGRESS.md section 5).
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as every
 * other charm in this family - the effect only cares about presence
 * anywhere in inventory, not slot or quantity, so it stacks like a raw
 * material rather than a unique trinket.
 */
public class PrismiumHasteCharmItem extends Item {

    public PrismiumHasteCharmItem(Item.Properties properties) {
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
