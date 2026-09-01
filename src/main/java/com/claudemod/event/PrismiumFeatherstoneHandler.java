package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import com.claudemod.compat.curios.CuriosCompat;
import com.claudemod.network.ClaudeModNetwork;
import com.claudemod.network.FeatherstoneReductionMessage;
import net.minecraftforge.fml.ModList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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

        // Issue #17 follow-up (2026-08-31): the previous implementation fired
        // the particle/sound cue and the action-bar message on *every* landing,
        // including ordinary jumps (LivingFallEvent fires for any fall distance
        // > 0, not just damaging ones). The reporter explicitly asked for this
        // to only activate "when damage is actually taken" - so first compute
        // whether this fall would deal any damage at all using the same
        // formula LivingEntity#calculateFallDamage uses server-side
        // (Mth.ceil((distance - safeFallDistance) * damageMultiplier)), using
        // the *pre-reduction* multiplier/distance. Only when that is > 0 do we
        // apply the reduction and show feedback; harmless short falls now stay
        // silent, matching the report.
        float preReductionMultiplier = event.getDamageMultiplier();
        // LivingEntity#calculateFallDamage hardcodes a 3.0-block safe fall
        // distance before any damage formula applies (confirmed via 1.20.1
        // mappings - no public getSafeFallDistance() accessor exists in this
        // version, so the vanilla constant is duplicated here rather than
        // called).
        final float SAFE_FALL_DISTANCE = 3.0F;
        int wouldBeDamage = Mth.ceil((event.getDistance() - SAFE_FALL_DISTANCE) * preReductionMultiplier);
        if (wouldBeDamage <= 0) {
            return;
        }

        event.setDamageMultiplier(preReductionMultiplier * DAMAGE_MULTIPLIER);
        playFeedback(player);
        announceReduction(player);
    }

    /**
     * Session 32 addition: a subtle particle/sound cue at the moment the
     * reduction actually applies, so the effect isn't purely invisible
     * arithmetic (see PROGRESS.md session 31 handoff - "no visual
     * feedback" was flagged as a concern shared by every passive item in
     * this "just carry it" family). Reuses {@link
     * PrismiumGuardianCharmHandler}'s established pattern (server-side
     * {@code ServerLevel#sendParticles}/{@code #playSound} pair, guarded
     * behind an {@code instanceof ServerLevel} check) rather than
     * inventing a new one. Amethyst's chime sound was picked over a
     * generic "poof" because it reads as a soft crystalline cue that
     * fits Prismium's crystal-shard theme, and {@link
     * ParticleTypes#CLOUD} because a light puff under the player's feet
     * reads as "cushioned landing" without implying anything as dramatic
     * as the Guardian Charm's totem burst.
     */
    private static void playFeedback(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.1D, player.getZ(),
                    8, 0.3D, 0.05D, 0.3D, 0.02D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    /**
     * GitHub issue #17 ("羽石の効果がわかりずらい" - when Featherstone
     * triggers, it's not intuitively clear how much effect just fired;
     * please improve part of the UI). {@link #playFeedback} already gives
     * a particle/sound cue that *something* happened, but per the report
     * that still leaves the actual magnitude invisible - a player cannot
     * tell a 75% reduction from, say, a 10% one just from a puff of cloud
     * particles.
     *
     * <p>Session update (this session, same issue's still-open request:
     * "HP表示に何か工夫を入れた上" - add some improvement to the HP
     * display): this previously sent a plain vanilla action-bar line via
     * {@code Player#displayClientMessage(component, true)}. That satisfied
     * "show the number" but not "improve the HP display" specifically, and
     * risked reading as the same kind of generic chat/status notification
     * the reporter's very first comment on this issue already said was
     * unwelcome. This now instead sends {@link FeatherstoneReductionMessage}
     * to the triggering player over {@link ClaudeModNetwork#CHANNEL} - see
     * {@link com.claudemod.client.overlay.FeatherstoneReductionOverlay} for
     * the small HUD panel this drives, anchored directly next to the
     * player's own health/armor stack instead of the shared action-bar
     * slot. Deliberately a static percentage rather than a computed
     * before/after damage number: {@link net.minecraft.world.entity.LivingEntity#causeFallDamage}
     * derives the actual health delta from this event's multiplier
     * *after* this listener returns, taking armor/enchantments/other mods'
     * own {@code LivingFallEvent} listeners into account, so this
     * handler cannot know the final applied damage at the point it runs -
     * sending the guaranteed, always-true "75%" figure is honest, whereas
     * guessing a final HP number here could easily be wrong.
     *
     * <p>Only sent when {@code player} is actually a {@link ServerPlayer}
     * (true for every real client connection; a defensive guard against
     * fake/bot players some other mod might feed through the same {@link
     * LivingFallEvent} path, which cannot receive packets at all) - the
     * fall-damage reduction itself and {@link #playFeedback}'s
     * particle/sound cue still apply unconditionally either way, so a fake
     * player is never denied the actual gameplay effect, only this
     * client-only visual.
     */
    private static void announceReduction(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int reductionPercent = Math.round((1.0F - DAMAGE_MULTIPLIER) * 100.0F);
        ClaudeModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new FeatherstoneReductionMessage(reductionPercent));
    }

    private static boolean hasFeatherstone(Player player) {
        Inventory inventory = player.getInventory();
        if (containsFeatherstone(inventory.items)
                || containsFeatherstone(inventory.armor)
                || containsFeatherstone(inventory.offhand)) {
            return true;
        }
        // Session #80 (scheduled, issue #18): also count the charm as
        // "carried" if it is equipped in a Curios accessory slot, when
        // Curios is installed - see CuriosCompat's javadoc for why the
        // ModList guard must live here rather than inside CuriosCompat.
        return ModList.get().isLoaded("curios")
                && CuriosCompat.isEquippedInCurioSlot(player, ModItems.PRISMIUM_FEATHERSTONE.get());
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
