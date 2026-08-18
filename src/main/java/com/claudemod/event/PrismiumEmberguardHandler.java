package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
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
 * Session 32: server-side {@link LivingDamageEvent} listener implementing
 * {@link com.claudemod.item.PrismiumEmberguardItem}'s entire behavior -
 * the second "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler} (session 31), reusing the same overall
 * shape (multiply, don't cancel; scan the whole inventory, not just
 * hands; emit a small particle/sound cue when the reduction actually
 * fires) rather than inventing a new pattern.
 *
 * <p><b>Why {@code LivingDamageEvent} and not {@code
 * LivingAttackEvent}/{@code LivingFallEvent}</b>: fire/lava damage
 * (unlike fall damage) has no dedicated Forge event of its own - it is
 * ordinary {@code LivingEntity#hurt} damage with a fire-flavored {@link
 * DamageSource}. {@code LivingDamageEvent} fires after invulnerability/
 * armor/enchantment reduction has already been applied but before the
 * final amount is subtracted from health, and exposes a plain {@code
 * getAmount()}/{@code setAmount(float)} pair - the same "reduce the
 * float, don't cancel the event" shape Featherstone uses on {@code
 * LivingFallEvent}. This is a well-established pattern for custom
 * resistance items across the wider Forge modding ecosystem, though as
 * with every event in this mod, it has only been confirmed against
 * javadoc/decompiled-source mirrors, not a live server (see
 * PROGRESS.md's standing note and the "Unverified" paragraph below).
 *
 * <p><b>Damage-type check</b>: uses {@link
 * DamageTypeTags#IS_FIRE}, which vanilla applies to the {@code
 * in_fire}, {@code on_fire}, {@code lava}, and {@code hot_floor}
 * (magma block) damage types - so this covers both "standing in
 * fire/lava" and "still burning after leaving it," matching what a
 * player would intuitively expect an item named "Emberguard" to guard
 * against. Explosions, freezing, and every other damage type are left
 * untouched.
 *
 * <p><b>Design choice - 50%, not Featherstone's 75%</b>: fire damage
 * over time (burning ticks) can already be fully cleared with a single
 * water bucket or Fire Resistance potion, so a passive item stacking on
 * top of those existing vanilla counters was deliberately tuned to a
 * smaller flat reduction than Featherstone's fall-damage multiplier
 * (which has no equally cheap vanilla counter) - this is a judgment
 * call, not derived from any measurement, and may need revisiting once
 * actual play feedback exists (see PROGRESS.md).
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether {@code LivingDamageEvent} still
 * has this exact shape in 1.20.1 Forge (checked against the same class
 * of source/javadoc mirror as every other "confirmed" API in this mod,
 * not the actual 1.20.1 source directly); whether reducing {@code
 * getAmount()} here actually results in the expected lower health loss
 * once armor toughness/other mods' own damage listeners are also in the
 * chain (ordering between competing {@code LivingDamageEvent}
 * subscribers across mods is not something this sandbox can test);
 * whether a flat 50% reduction feels balanced in practice; and whether
 * the {@link ParticleTypes#SMALL_FLAME} + {@link
 * SoundEvents#GENERIC_EXTINGUISH_FIRE} feedback cue (chosen to read as
 * "the fire fizzled/was dampened" rather than the Featherstone's soft
 * "cushioned landing" cue) actually looks/sounds sensible timed against
 * the vanilla burning-damage-tick sound/particles that will also be
 * playing at the same moment.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumEmberguardHandler {

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
        if (!source.is(DamageTypeTags.IS_FIRE)) {
            return;
        }
        if (!hasEmberguard(player)) {
            return;
        }

        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
        playFeedback(player);
    }

    private static void playFeedback(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                    player.getX(), player.getY() + player.getBbHeight() / 2.0D, player.getZ(),
                    6, 0.3D, 0.3D, 0.3D, 0.01D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.PLAYERS, 0.4F, 1.2F);
        }
    }

    private static boolean hasEmberguard(Player player) {
        Inventory inventory = player.getInventory();
        return containsEmberguard(inventory.items)
                || containsEmberguard(inventory.armor)
                || containsEmberguard(inventory.offhand);
    }

    private static boolean containsEmberguard(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_EMBERGUARD.get())) {
                return true;
            }
        }
        return false;
    }
}
