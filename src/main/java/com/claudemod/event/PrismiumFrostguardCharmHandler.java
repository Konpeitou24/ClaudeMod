package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import com.claudemod.compat.curios.CuriosCompat;
import net.minecraftforge.fml.ModList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Scheduled session (2026-09-20): server-side {@link LivingDamageEvent}
 * listener implementing {@link
 * com.claudemod.item.PrismiumFrostguardCharmItem}'s entire behavior -
 * the sixth "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler} (fall), {@link
 * PrismiumEmberguardHandler} (fire/lava), {@link
 * PrismiumVitastoneHandler} (healing) and {@link
 * PrismiumAegisCharmHandler} (explosion), reusing the exact same
 * overall shape (multiply, don't cancel; scan the whole inventory, not
 * just hands; emit a small particle/sound cue when the reduction
 * actually fires) rather than inventing a new pattern.
 *
 * <p><b>Damage-type check</b>: uses {@code
 * source.is(DamageTypes.FREEZE)} rather than a {@code DamageTypeTags}
 * bundle tag, because - unlike {@code IS_EXPLOSION}/{@code IS_FIRE} -
 * 1.20.1 has no vanilla {@code DamageTypeTags} entry that bundles
 * freeze-flavored damage types together (there is exactly one:
 * {@code minecraft:freeze}). Both {@code DamageTypes#FREEZE} (a {@code
 * ResourceKey<DamageType>} field) and {@code DamageSource#is(ResourceKey
 * <DamageType>)} were confirmed to exist on 1.20.1 this session via
 * {@code mappings.dev/1.20.1/net/minecraft/world/damagesource/DamageTypes.html}
 * and {@code .../DamageSource.html} before use, per the mod's standing
 * "unconfirmed Java API" rule.
 *
 * <p><b>Design choice - 50%, same as Emberguard/Aegis, not
 * Featherstone's 75%</b>: freeze damage has no existing vanilla
 * enchantment counter to stack on top of (unlike Fire/Blast
 * Protection), which on its own might argue for a stronger reduction
 * like Featherstone's. However, freeze damage only occurs after
 * prolonged unprotected exposure to powder snow and is already
 * comparatively rare/avoidable (leather boots prevent the freezing
 * status entirely), so this was tuned to the same conservative 50%
 * the mod's other two "already covered elsewhere" charms use rather
 * than treating it as a high-priority hazard like fall damage. A
 * judgment call, not a measurement; may need revisiting once actual
 * play feedback exists.
 *
 * <p><b>Feedback sound choice</b>: {@link SoundEvents#AMETHYST_BLOCK_HIT}
 * (confirmed already in use elsewhere in this mod, including {@link
 * PrismiumAegisCharmHandler}) rather than guessing at a frost/ice
 * specific sound event that has not been confirmed to exist on
 * 1.20.1, per the mod's standing "unconfirmed Java API" rule. Uses
 * {@link ParticleTypes#SNOWFLAKE} instead of Aegis Charm's {@code
 * CRIT} for the visual cue, since {@code SNOWFLAKE} is already
 * confirmed to exist vanilla (used by powder snow/frost itself) and
 * fits the frost theme better than a generic hit spark.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether a flat 50% reduction feels
 * balanced in practice; whether the {@code SNOWFLAKE} + {@code
 * AMETHYST_BLOCK_HIT} feedback cue actually looks/sounds sensible
 * against vanilla's own freeze damage flash.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumFrostguardCharmHandler {

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
        if (!source.is(DamageTypes.FREEZE)) {
            return;
        }
        if (!hasFrostguardCharm(player)) {
            return;
        }

        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
        playFeedback(player);
    }

    private static void playFeedback(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX(), player.getY() + player.getBbHeight() / 2.0D, player.getZ(),
                    10, 0.4D, 0.4D, 0.4D, 0.02D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.PLAYERS, 0.5F, 1.3F);
        }
    }

    private static boolean hasFrostguardCharm(Player player) {
        Inventory inventory = player.getInventory();
        if (containsFrostguardCharm(inventory.items)
                || containsFrostguardCharm(inventory.armor)
                || containsFrostguardCharm(inventory.offhand)) {
            return true;
        }
        // Same Curios charm-slot accounting as Featherstone/Emberguard/
        // Vitastone/Magnet Charm/Aegis Charm - see CuriosCompat's javadoc
        // for why the ModList guard must live here rather than inside
        // CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_FROSTGUARD_CHARM.get());
    }

    private static boolean containsFrostguardCharm(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_FROSTGUARD_CHARM.get())) {
                return true;
            }
        }
        return false;
    }
}
