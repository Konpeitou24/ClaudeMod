package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import com.claudemod.compat.curios.CuriosCompat;
import net.minecraftforge.fml.ModList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Scheduled session (2026-09-19): server-side {@link LivingDamageEvent}
 * listener implementing {@link
 * com.claudemod.item.PrismiumAegisCharmItem}'s entire behavior - the
 * fifth "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler} (session 31, fall), {@link
 * PrismiumEmberguardHandler} (session 32, fire/lava) and {@link
 * PrismiumVitastoneHandler} (session 33, healing), reusing the exact
 * same overall shape (multiply, don't cancel; scan the whole
 * inventory, not just hands; emit a small particle/sound cue when the
 * reduction actually fires) rather than inventing a new pattern.
 *
 * <p><b>Why {@code LivingDamageEvent} and not a dedicated explosion
 * event</b>: same reasoning as {@link PrismiumEmberguardHandler} - Forge
 * has no bespoke "pre-explosion-damage" event comparable to {@code
 * LivingFallEvent}; explosion damage is ordinary {@code
 * LivingEntity#hurt} damage carrying a blast-flavored {@link
 * DamageSource}, so the well-established "reduce {@code getAmount()}
 * on {@code LivingDamageEvent}, don't cancel it" pattern applies here
 * too.
 *
 * <p><b>Damage-type check</b>: uses {@link DamageTypeTags#IS_EXPLOSION}
 * (confirmed to exist on 1.20.1's {@code DamageTypeTags} this session
 * via {@code mappings.dev/1.20.1/net/minecraft/tags/DamageTypeTags.html}
 * before use, per the mod's standing "unconfirmed Java API" rule -
 * same verification method already used for {@code IS_FIRE} in
 * Emberguard). Vanilla tags {@code explosion} and {@code
 * player_explosion} (TNT, creepers, beds/respawn anchors out of
 * dimension, the Wither, etc.) into this tag, so the charm reacts to
 * essentially every blast source in the base game, plus any future
 * ClaudeMod hazard (Prism Realm dungeon bosses, TODO9) that reuses a
 * vanilla explosion damage source rather than inventing a bespoke one.
 *
 * <p><b>Design choice - 50%, matching Emberguard's fire reduction, not
 * Featherstone's 75%</b>: like fire, explosion damage already has an
 * existing vanilla counter (Blast Protection enchantment) that a
 * passive charm would stack on top of, whereas fall damage's only
 * vanilla counters (Feather Falling, boots-only) are narrower - so
 * this was tuned to the same, more conservative reduction Emberguard
 * uses rather than Featherstone's stronger one. A judgment call, not a
 * measurement; may need revisiting once actual play feedback exists.
 *
 * <p><b>Feedback sound choice</b>: {@link SoundEvents#AMETHYST_BLOCK_HIT}
 * (confirmed already in use elsewhere in this mod - {@code
 * PrismiumCrawlerEntity}/{@code PrismiumWispEntity}'s hurt sounds -
 * rather than guessing at a shield-specific sound event that has not
 * been confirmed to exist on 1.20.1, per the mod's standing
 * "unconfirmed Java API" rule). Its resonant crystal "clink" doubles
 * as a plausible "impact absorbed by a ward" cue and keeps this charm
 * sonically in the same family as the mod's other Prismium crystal
 * content.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether a flat 50% reduction feels
 * balanced in practice, especially stacked with Blast Protection
 * armor; whether the {@link ParticleTypes#CRIT} + {@code
 * AMETHYST_BLOCK_HIT} feedback cue actually looks/sounds sensible
 * timed against the vanilla explosion sound/particles that will also
 * be playing at the same moment.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumAegisCharmHandler {

    private static final float DAMAGE_MULTIPLIER = 0.5F;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        if (event.getAmount() <= 0.0F) {
            return;
        }

        DamageSource source = event.getSource();
        if (!source.is(DamageTypeTags.IS_EXPLOSION)) {
            return;
        }
        if (!hasAegisCharm(player)) {
            return;
        }

        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
        playFeedback(player);
    }

    private static void playFeedback(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + player.getBbHeight() / 2.0D, player.getZ(),
                    10, 0.4D, 0.4D, 0.4D, 0.02D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.PLAYERS, 0.5F, 0.7F);
        }
    }

    private static boolean hasAegisCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsAegisCharm(inventory.items)
                || containsAegisCharm(inventory.armor)
                || containsAegisCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as Featherstone/Emberguard/
        // Vitastone/Magnet Charm - see CuriosCompat's javadoc for why
        // the ModList guard must live here rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_AEGIS_CHARM.get());
    }

    private static boolean containsAegisCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_AEGIS_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
