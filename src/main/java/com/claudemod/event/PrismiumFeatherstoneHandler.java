package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Session 31: server-side {@link LivingFallEvent} listener implementing
 * {@link com.claudemod.item.PrismiumFeatherstoneItem}'s entire behavior -
 * same "the Item class itself carries no logic" split used by Guardian
 * Charm (session 30), because there is no vanilla per-item hook for
 * "reduces fall damage while merely carried" the way there is for
 * blocking (Shield) or arrow customization (Bow).
 *
 * <p><b>API confirmed this session</b> (Web search + 1.19.x Forge
 * javadoc mirrors, since the 1.20.x source itself was unreachable from
 * this sandbox - see PROGRESS.md): {@code LivingFallEvent(LivingEntity
 * entity, float distance, float damageMultiplier)} is {@code Cancelable}
 * and exposes {@code getDamageMultiplier()}/{@code
 * setDamageMultiplier(float)} (confirmed against a 1.19.2 mirror), and
 * {@code Inventory}'s {@code items}/{@code armor}/{@code offhand}
 * fields are {@code public final} (confirmed against a 1.19.3 mirror).
 * Neither class has changed shape between 1.19.x and 1.20.1 in any
 * changelog found, but as with every other "checked via a mirror of an
 * adjacent version" API in this mod, the final word is whichever way
 * the next CI build result comes back.
 *
 * <p><b>Design choice - multiply, don't cancel</b>: cancelling the event
 * (or calling {@code setDistance(0)}) would grant full fall-damage
 * immunity for as little as owning one stone anywhere in a 41-slot
 * inventory, forever, with no cooldown or consumption (unlike the
 * Guardian Charm, which is consumed on use). That reads as too strong
 * for a passive, always-on, non-equipped item, so this instead
 * multiplies whatever the existing {@code damageMultiplier} already is
 * by {@link #DAMAGE_MULTIPLIER} - a 75% reduction, not full negation -
 * and does so multiplicatively so it stacks reasonably with any other
 * mod's own fall-damage-reducing effects instead of clobbering them.
 *
 * <p><b>Design choice - presence, not equip slot</b>: unlike Guardian
 * Charm (which only checks the two hands, mirroring vanilla's totem
 * activation slots), this scans the player's entire inventory (main
 * inventory rows, armor slots, and offhand) via {@link
 * Inventory#items}/{@link Inventory#armor}/{@link Inventory#offhand} -
 * see {@link com.claudemod.item.PrismiumFeatherstoneItem}'s javadoc for
 * why a slot-agnostic "just carry it" design was chosen over adding a
 * new equip slot this mod has no framework for (no Curios-style API
 * dependency exists here).
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether the damage reduction actually
 * applies in practice (the multiplier math itself is trivial, but
 * whether {@code LivingFallEvent} still fires for players in 1.20.1
 * the same way the 1.19.2 javadoc describes has not been confirmed
 * beyond a successful CI compile); whether a flat 75% reduction is a
 * reasonable balance point for an always-on, unconsumed, unlimited-use
 * item (no comparison against vanilla Feather Falling enchantment
 * levels was done); and whether scanning three separate {@code
 * NonNullList<ItemStack>} fields every single fall is a meaningfully
 * different cost from a single hands-only scan (falls are much rarer
 * than every-tick logic elsewhere in this mod, so this was not
 * considered a performance risk, but it has never been measured).
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumFeatherstoneHandler {

    private static final float DAMAGE_MULTIPLIER = 0.25F;

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        if (event.getDistance() <= 0.0F) {
            return;
        }
        if (!hasFeatherstone(player)) {
            return;
        }

        event.setDamageMultiplier(event.getDamageMultiplier() * DAMAGE_MULTIPLIER);
    }

    private static boolean hasFeatherstone(Player player) {
        Inventory inventory = player.getInventory();
        return containsFeatherstone(inventory.items)
                || containsFeatherstone(inventory.armor)
                || containsFeatherstone(inventory.offhand);
    }

    private static boolean containsFeatherstone(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_FEATHERSTONE.get())) {
                return true;
            }
        }
        return false;
    }
}
