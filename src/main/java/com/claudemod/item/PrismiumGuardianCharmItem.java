package com.claudemod.item;

import net.minecraft.world.item.Item;

/**
 * Session 30: Prismium Guardian Charm - the mod's first "cheat death"
 * safety-net item, addressing PROGRESS.md's recurring "プレイテストの手段
 * が無い問題" discussion from the opposite direction: since this sandbox
 * can never playtest survivability, giving the *player* a forgiving second
 * chance against the mod's own untested new hazards (Wraith, Wardstone's
 * effect radius, Prism Realm hazards, etc.) is a reasonable hedge.
 *
 * <p>This class itself is deliberately empty of logic - unlike {@link
 * PrismiumShieldItem} (which overrides {@code getUseAnimation}/{@code use}
 * to plug into vanilla's per-tick blocking check) or {@link
 * PrismiumBowItem} (which overrides {@code customArrow}), there is no
 * vanilla per-item hook for "prevents death while held" the way there is
 * for blocking or arrow customization. Vanilla's own Totem of Undying is
 * handled by a hard-coded {@code stack.is(Items.TOTEM_OF_UNDYING)} check
 * inside {@code LivingEntity#checkTotemDeathProtection} (confirmed this
 * session by cloning the actual MinecraftForge 1.20.1 source and reading
 * {@code patches/minecraft/net/minecraft/world/entity/LivingEntity.java.patch}
 * directly, rather than guessing - see PROGRESS.md session 30 notes) -
 * that check cannot be redirected to a different item, and Forge's own
 * {@code LivingUseTotemEvent} only lets a mod *cancel* an in-progress
 * vanilla-totem save, not designate a new totem item. So this charm's
 * entire behavior lives in {@link com.claudemod.event.
 * PrismiumGuardianCharmHandler}, a {@code LivingDeathEvent} listener that
 * fires independently of (and later than) vanilla's own totem check -
 * meaning a player holding both a vanilla Totem of Undying and this charm
 * would have the vanilla totem save them first, this charm only stepping
 * in on a *second* otherwise-fatal hit once the totem is spent.
 *
 * <p>stacksTo(1), matching vanilla's own Totem of Undying stacking
 * behavior and the mod's other "reusable key/tool" items (Rift Shard,
 * Locator) rather than a bulk-craftable consumable - though unlike those
 * two this one *is* consumed (shrunk by 1) on activation, same as the
 * vanilla item it parallels.
 */
public class PrismiumGuardianCharmItem extends Item {

    public PrismiumGuardianCharmItem(Item.Properties properties) {
        super(properties);
    }
}
