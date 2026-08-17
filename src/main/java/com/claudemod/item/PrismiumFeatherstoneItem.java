package com.claudemod.item;

import net.minecraft.world.item.Item;

/**
 * Session 31: Prismium Featherstone - the mod's first purely passive
 * accessory. Every other Prismium item so far either occupies a vanilla
 * equipment slot (tools/armor/Shield/Bow) or requires a conscious
 * action to use (Grappling Hook/Locator/Rift Shard, or Guardian Charm's
 * held-in-hand check). This one does neither: simply owning one
 * anywhere in inventory is enough - see {@link
 * com.claudemod.event.PrismiumFeatherstoneHandler}'s {@code
 * LivingFallEvent} listener for the actual effect (fall damage
 * reduction, not full negation - see that class's javadoc for the
 * exact multiplier and reasoning).
 *
 * <p>Deliberately not stacksTo(1): unlike Rift Shard/Locator/Guardian
 * Charm (each a distinct "key"/"tool"/"reusable totem" concept where
 * owning more than one is meaningless, or for the Charm would be
 * meaningfully different), this item's own effect already only cares
 * about *presence*, not equip slot or quantity, so nothing is lost by
 * letting it stack normally like a raw material. This also keeps it
 * visually/mechanically closer to "a smooth stone you just happen to be
 * carrying" than to a unique magic trinket.
 */
public class PrismiumFeatherstoneItem extends Item {

    public PrismiumFeatherstoneItem(Item.Properties properties) {
        super(properties);
    }
}
