package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import com.claudemod.compat.curios.CuriosCompat;
import net.minecraftforge.fml.ModList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Session 33: server-side {@link LivingHealEvent} listener implementing
 * {@link com.claudemod.item.PrismiumVitastoneItem}'s entire behavior -
 * the third "just carry it" passive accessory after {@link
 * PrismiumFeatherstoneHandler} (session 31, fall damage) and {@link
 * PrismiumEmberguardHandler} (session 32, fire/lava damage), reusing
 * the same overall shape (multiply, don't cancel; scan the whole
 * inventory, not just hands; emit a small particle/sound cue when the
 * effect actually fires) rather than inventing a new pattern - see
 * PROGRESS.md session 32 handoff, which specifically suggested a third
 * example of this pattern on a not-yet-used event.
 *
 * <p><b>Why {@code LivingHealEvent}</b>: fired via {@code
 * ForgeEventFactory.onLivingHeal(LivingEntity, float)} whenever {@code
 * LivingEntity#heal(float)} runs - this covers natural regeneration,
 * the Regeneration status effect, golden apples/enchanted golden
 * apples, potions of Healing, totems of undying, and this mod's own
 * Prismium Restorer/Guardian Charm where applicable, all in one place,
 * with the same plain {@code getAmount()}/{@code setAmount(float)}
 * pair Featherstone/Emberguard already use on their respective events.
 * <b>Confidence note</b>: unlike Featherstone/Emberguard (whose event
 * shapes were checked against general/older javadoc mirrors), this
 * signature was fetched and read directly from a javadoc mirror built
 * specifically for Forge 1.20.1
 * (lexxie.dev/forge/1.20.1/net/minecraftforge/event/entity/living/LivingHealEvent.html),
 * so the version-match confidence here is higher than usual for this
 * mod - see PROGRESS.md's session 25/32 notes about old-version
 * javadoc contamination being a recurring research risk.
 *
 * <p><b>Design choice - a flat 20% amplification, deliberately
 * conservative</b>: unlike Featherstone (no cheap vanilla counter to
 * fall damage) or Emberguard (fire damage already has cheap vanilla
 * counters, so a moderate 50% reduction was safe to stack on top),
 * amplifying <em>healing</em> uniformly is a fundamentally different
 * and riskier kind of passive bonus: it compounds with every existing
 * healing source at once, including burst sources like Instant Health
 * II splash potions or a stacked Regeneration effect, rather than
 * mitigating a single damage type. A 20% multiplier was chosen as a
 * deliberately smaller number than either existing passive item's
 * effect specifically because of this compounding risk - this is a
 * judgment call with no measurement behind it (see PROGRESS.md), and
 * is the single number in this file most likely to need retuning
 * (probably downward) once real play feedback exists.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether amplifying {@code
 * LivingHealEvent} uniformly across every healing source actually
 * feels reasonable in practice, or whether it makes burst-healing
 * items (Instant Health potions especially) noticeably too strong;
 * whether the {@link ParticleTypes#HEART} + {@link
 * SoundEvents#EXPERIENCE_ORB_PICKUP} feedback cue (chosen to read as
 * "you gained something" rather than reusing Featherstone/Emberguard's
 * more muted cues) is legible/pleasant timed against whatever
 * vanilla heal feedback (hearts flashing, totem particles, etc.) is
 * already playing at the same moment; and whether firing particles/
 * sound on every single {@code heal()} call (which, unlike a single
 * fall or a single burn tick, can happen very frequently under
 * natural regeneration or a long Regeneration effect) turns out to be
 * spammy rather than a nice occasional cue - this is a new risk this
 * item introduces that Featherstone/Emberguard's rarer trigger
 * conditions did not have to consider, and is worth revisiting first
 * if play feedback calls it out.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumVitastoneHandler {

    private static final float HEAL_MULTIPLIER = 1.2F;

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
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
        if (!hasVitastone(player)) {
            return;
        }

        event.setAmount(event.getAmount() * HEAL_MULTIPLIER);
        playFeedback(player);
    }

    private static void playFeedback(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + player.getBbHeight() / 2.0D, player.getZ(),
                    3, 0.3D, 0.3D, 0.3D, 0.02D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.3F, 1.6F);
        }
    }

    private static boolean hasVitastone(Player player) {
        Inventory inventory = player.getInventory();
        if (containsVitastone(inventory.items)
                || containsVitastone(inventory.armor)
                || containsVitastone(inventory.offhand)) {
            return true;
        }
        // Session #80 (scheduled, issue #18): also count the charm as
        // "carried" if it is equipped in a Curios accessory slot, when
        // Curios is installed - see CuriosCompat's javadoc for why the
        // ModList guard must live here rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_VITASTONE.get());
    }

    private static boolean containsVitastone(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.is(ModItems.PRISMIUM_VITASTONE.get())) {
                return true;
            }
        }
        return false;
    }
}
