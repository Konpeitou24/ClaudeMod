package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Scheduled session (2026-10-14): Prismium Aqualung Charm - the mod's
 * tenth purely passive "just carry it" accessory, following the exact
 * split {@link PrismiumFeatherstoneItem} (fall damage), {@link
 * PrismiumEmberguardItem} (fire/lava damage), {@link
 * PrismiumVitastoneItem} (heal amplification), {@link
 * PrismiumMagnetCharmItem} (item pickup), {@link
 * PrismiumAegisCharmItem} (explosion damage), {@link
 * PrismiumFrostguardCharmItem} (freeze damage), {@link
 * PrismiumAntivenomCharmItem} (poison/wither damage), {@link
 * PrismiumHasteCharmItem} (permanent Haste I) and {@link
 * PrismiumLuckCharmItem} (permanent Luck I) all established: this class
 * holds no gameplay logic at all, and the entire effect (passive Water
 * Breathing while carried) lives in {@link
 * com.claudemod.event.PrismiumAqualungCharmHandler}'s {@code
 * TickEvent.PlayerTickEvent} listener. See that handler's javadoc for
 * the effect application details.
 *
 * <p>Concept: the third charm in this family whose effect is not a
 * damage-type mitigator, following {@link PrismiumHasteCharmItem} and
 * {@link PrismiumLuckCharmItem}'s precedent. Permanent Water Breathing
 * ties back to this mod's "exploration itself should be fun" design
 * goal (PROGRESS.md section 5) from yet another angle: ocean
 * ruins/monuments, Prism Realm's own bodies of water, and drowned
 * fights all become easier to simply stay in, without needing to carry
 * a turtle helmet or watch an air meter.
 *
 * <p>Deliberately not {@code stacksTo(1)}: same reasoning as every
 * other charm in this family - the effect only cares about presence
 * anywhere in inventory, not slot or quantity, so it stacks like a raw
 * material rather than a unique trinket.
 */
public class PrismiumAqualungCharmItem extends Item {

    public PrismiumAqualungCharmItem(Item.Properties properties) {
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
