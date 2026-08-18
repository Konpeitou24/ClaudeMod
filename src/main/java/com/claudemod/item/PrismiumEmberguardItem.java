package com.claudemod.item;

import net.minecraft.world.item.Item;

/**
 * Session 32: Prismium Emberguard - the mod's second purely passive
 * "just carry it" accessory, following the exact split {@link
 * PrismiumFeatherstoneItem} (session 31) established: this class holds
 * no logic at all, and the entire effect (fire/lava damage reduction)
 * lives in {@link com.claudemod.event.PrismiumEmberguardHandler}'s
 * {@code LivingDamageEvent} listener. See that handler's javadoc for
 * the damage-type check and the reduction multiplier.
 *
 * <p>Like Featherstone, deliberately not {@code stacksTo(1)}: the
 * effect only cares about presence anywhere in the player's inventory,
 * not about which single copy is "the" active one, so nothing is lost
 * by letting it stack like a raw material rather than a unique key
 * item (Rift Shard/Locator/Guardian Charm's category).
 */
public class PrismiumEmberguardItem extends Item {

    public PrismiumEmberguardItem(Item.Properties properties) {
        super(properties);
    }
}
