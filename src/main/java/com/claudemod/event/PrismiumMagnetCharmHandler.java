package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Session 65 (scheduled): server-side {@code TickEvent.PlayerTickEvent}
 * listener implementing {@link
 * com.claudemod.item.PrismiumMagnetCharmItem}'s entire behavior - same
 * "the Item class itself carries no logic" split used by Featherstone/
 * Emberguard/Vitastone, except this hooks a per-tick event
 * ({@code TickEvent.PlayerTickEvent}) rather than a one-shot vanilla
 * event, because there is no existing "item was picked up" hook that
 * fires *before* pickup at a distance - the effect has to actively pull
 * candidate {@link ItemEntity}s closer every tick instead.
 *
 * <p><b>Pattern reused</b>: the {@code TickEvent.PlayerTickEvent} +
 * {@code Phase.END} + server-only ({@code player.level().isClientSide})
 * guard is copied verbatim from {@link ArmorSetBonusHandler} (session 5),
 * the only other per-tick listener in this codebase - both the phase
 * choice and the client-side skip (server-authoritative movement synced
 * to clients automatically, so running this client-side too would be
 * redundant double work) are established, previously-reasoned-through
 * decisions, not new judgment calls.
 *
 * <p><b>Velocity API reused</b>: {@code Entity#getDeltaMovement()} /
 * {@code #setDeltaMovement(Vec3)} paired with flipping the public {@code
 * hurtMarked} flag to request a client resync is the exact pattern
 * {@link com.claudemod.item.PrismiumGrapplingHookItem} (session 7)
 * already uses and documents as confirmed (that class's javadoc cites
 * vanilla's {@code FishingHook#pullEntity} as the source of the pattern).
 * This is the first time this codebase applies it to an entity other
 * than the player themselves, but the method pair is defined on the base
 * {@code Entity} class, not anything player-specific, so no new API
 * surface is actually being introduced.
 *
 * <p><b>Deliberately item-only, not experience orbs</b>: vanilla's
 * {@code ExperienceOrb#tick} already searches for (and homes in on) the
 * nearest player within an 8-block range entirely on its own, with no
 * mod involvement needed - see {@code net.minecraft.world.entity.ExperienceOrb}
 * (confirmed against a mirror of the class this session; the "orbs chase
 * you" behavior every player has seen since vanilla 1.0 is exactly this).
 * Reimplementing that here for orbs would only add redundant, possibly
 * conflicting velocity writes, so this handler only ever queries {@link
 * ItemEntity}, which has no equivalent built-in homing behavior.
 *
 * <p><b>Design choice - gentle acceleration, not teleport/instant
 * pickup</b>: {@link #PULL_ACCEL} is added to (not replacing) the item's
 * existing {@code deltaMovement} each tick, capped by {@link
 * #MAX_PULL_SPEED}, so items visibly drift/accelerate toward the player
 * across several ticks rather than snapping - this keeps physics-looking
 * behavior consistent with how vanilla items already move (e.g. water
 * currents, being pushed) rather than introducing a jarring teleport.
 * {@link #MIN_DISTANCE} stops applying the pull once vanilla's own
 * ~1-block pickup range would take over anyway, avoiding tug-of-war
 * jitter between "pull toward center" and "instant pickup" at point-blank
 * range.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether a 6-block radius / this specific
 * acceleration and speed cap feels good in practice (first-guess balance
 * numbers, same caveat as every other numeric constant in this mod);
 * whether pulling items through solid blocks (this does not raytrace for
 * line-of-sight, unlike the Grappling Hook, since dropped items are
 * expected to eventually funnel around obstacles via normal physics/
 * collision rather than needing a clear line first) produces any visibly
 * odd stuck-against-a-wall behavior; and the performance cost of an AABB
 * entity query every tick for every player carrying the charm - kept to a
 * modest 6-block radius specifically to bound this, but never measured
 * against a real server.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumMagnetCharmHandler {

    private static final double RADIUS = 6.0D;
    private static final double PULL_ACCEL = 0.12D;
    private static final double MAX_PULL_SPEED = 0.45D;
    private static final double MIN_DISTANCE = 0.6D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        if (!hasMagnetCharm(player)) {
            return;
        }

        Level level = player.level();
        AABB area = player.getBoundingBox().inflate(RADIUS);
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive);
        for (ItemEntity item : nearbyItems) {
            pullToward(item, player);
        }
    }

    private static void pullToward(ItemEntity item, Player player) {
        Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 toPlayer = target.subtract(item.position());
        double distance = toPlayer.length();
        if (distance < MIN_DISTANCE || distance > RADIUS || distance < 1.0E-4D) {
            return;
        }

        Vec3 direction = toPlayer.scale(1.0D / distance);
        Vec3 newVelocity = item.getDeltaMovement().add(direction.scale(PULL_ACCEL));
        if (newVelocity.length() > MAX_PULL_SPEED) {
            newVelocity = newVelocity.normalize().scale(MAX_PULL_SPEED);
        }
        item.setDeltaMovement(newVelocity);
        item.hurtMarked = true;
    }

    private static boolean hasMagnetCharm(Player player) {
        Inventory inventory = player.getInventory();
        return containsCharm(inventory.items)
                || containsCharm(inventory.armor)
                || containsCharm(inventory.offhand);
    }

    private static boolean containsCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_MAGNET_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
