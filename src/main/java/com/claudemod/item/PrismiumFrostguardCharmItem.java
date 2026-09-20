package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-09-20): Prismium Frostguard Charm - the
 * mod's sixth purely passive "just carry it" accessory, following the
 * exact split {@link PrismiumFeatherstoneItem} (fall damage), {@link
 * PrismiumEmberguardItem} (fire/lava damage), {@link
 * PrismiumVitastoneItem} (heal amplification), {@link
 * PrismiumMagnetCharmItem} (item pickup) and {@link
 * PrismiumAegisCharmItem} (explosion damage) all established: this
 * class holds no gameplay logic at all, and the entire effect
 * (freeze/frostbite damage reduction) lives in {@link
 * com.claudemod.event.PrismiumFrostguardCharmHandler}'s {@code
 * LivingDamageEvent} listener. See that handler's javadoc for the
 * damage-type check and the reduction multiplier.
 *
 * <p>Concept: with Featherstone (fall), Emberguard (fire/lava) and
 * Aegis Charm (explosion) already covering the mod's most likely
 * environmental hazards (PROGRESS.md's 2026-09-19 note singled out
 * "poison/wither" and "freeze" as the remaining common damage types
 * for a sixth charm), this addresses freeze/frostbite damage - the
 * damage vanilla powder snow deals to an unprotected player over time.
 * Unlike fire (Fire Protection) and explosions (Blast Protection),
 * vanilla has no armor enchantment that reduces freeze damage itself
 * (leather boots only suppress the freezing status buildup, not
 * per-hit damage), so this charm is the first passive-charm damage
 * type with no existing vanilla stacking counter.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as
 * Featherstone/Emberguard/Vitastone/Magnet Charm/Aegis Charm - the
 * effect only cares about presence anywhere in inventory, not slot or
 * quantity, so it stacks like a raw material rather than a unique
 * trinket.
 */
public class PrismiumFrostguardCharmItem extends Item {

    public PrismiumFrostguardCharmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // Same passive-accessory hint pattern as Featherstone/Emberguard/Vitastone/Magnet Charm/Aegis Charm.
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
