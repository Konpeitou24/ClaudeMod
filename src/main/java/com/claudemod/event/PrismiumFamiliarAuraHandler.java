package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumFamiliarEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Scheduled session, 2026-10-01: the Prismium Familiar's third function
 * (HANDOFF.md's option (d), "使い魔的MOB案の第三の機能"), following the
 * v0.60.0 "pack familiar" carrying feature and the v0.61.0 render/death-drop
 * follow-up. Where those two gave the Familiar a utility role, this gives it
 * a passive companion-support role: while a player's own tamed Familiar is
 * nearby and actively accompanying them (not sitting), the player slowly
 * regenerates health, as a small ongoing reward for keeping the Familiar
 * close rather than leaving it parked at home.
 *
 * <p><b>Deliberately an event-bus handler, not a new {@code Mob} override</b>:
 * every other Familiar behaviour change so far ({@code mobInteract},
 * {@code addAdditionalSaveData}/{@code readAdditionalSaveData},
 * {@code dropCustomDeathLoot}) required adding a new override to {@link
 * PrismiumFamiliarEntity} and verifying it against mappings.dev/lexxie.dev
 * first (see that class's javadoc, and PROGRESS.md's "未確認のJava APIは
 * 必ず出典を確認してから使う" rule motivated by the v0.37.0/v0.60.0/v0.61.0
 * build failures). This feature instead reuses the exact {@code
 * TickEvent.PlayerTickEvent} + {@code Phase.END} + server-only ({@code
 * player.level().isClientSide}) listener shape already proven to compile
 * twice in this codebase ({@link ArmorSetBonusHandler}, session 4/5;
 * {@link PrismiumMagnetCharmHandler}, session 65) - no new {@code @Override}
 * is introduced at all, so this feature carries essentially none of the
 * "does this method actually exist with this signature" risk that caused
 * three separate build failures in this mod's history.
 *
 * <p><b>APIs used, each already proven elsewhere in this codebase rather
 * than freshly assumed</b>:
 * <ul>
 *   <li>{@code Level#getEntitiesOfClass(Class, AABB, Predicate)} - used
 *   verbatim by {@link PrismiumMagnetCharmHandler} for its own radius
 *   search.</li>
 *   <li>{@code TamableAnimal#isTame()}/{@code #isOwnedBy(Player)}/{@code
 *   #isOrderedToSit()} - all confirmed present (see {@link
 *   PrismiumFamiliarEntity}'s own class javadoc, which cites mappings.dev
 *   for exactly these three) and already called from this mod's own
 *   {@code mobInteract} implementation.</li>
 *   <li>{@code Player#addEffect(MobEffectInstance)} with {@code
 *   MobEffects.REGENERATION} - the same "re-apply every tick with a short
 *   buffer duration so a missed tick never visibly runs out" pattern
 *   {@link ArmorSetBonusHandler} already uses for Night Vision/Water
 *   Breathing, just with a different vanilla effect constant (a plain
 *   enum-like constant on {@code MobEffects}, not a method whose signature
 *   could be wrong).</li>
 *   <li>{@code Level#getGameTime()} - used to throttle the particle
 *   feedback to once per second instead of every tick, the exact
 *   {@code level.getGameTime() % N == 0} idiom already used by {@link
 *   com.claudemod.blockentity.PrismiumGeneratorBlockEntity} (session
 *   history, see that class).</li>
 *   <li>{@code ServerLevel#sendParticles} with {@code ParticleTypes.HEART}
 *   - the exact method already used by {@link PrismiumFamiliarEntity
 *   #playCarryFeedback}, and {@code HEART} itself is already used
 *   elsewhere in this codebase (grepped this session to confirm, rather
 *   than assumed).</li>
 * </ul>
 *
 * <p><b>Design choices</b>: the aura only fires while the Familiar is
 * un-sat (i.e. actively following/flying around, {@code
 * !isOrderedToSit()}) rather than whenever it merely exists somewhere
 * nearby, so a player can't just drop a sitting Familiar at their base and
 * get the buff for free everywhere else - they have to actually bring it
 * along. Regeneration I only, refreshed every tick it is in range with a
 * 7-second buffer (140 ticks, comfortably longer than the once-per-tick
 * refresh so a brief lag spike or the Familiar momentarily drifting out of
 * range doesn't cause a visible on/off flicker) - deliberately modest so a
 * tamed companion feels like a nice bonus rather than trivializing combat
 * or survival healing. The 6-block radius matches {@link
 * PrismiumMagnetCharmHandler}'s own radius choice for the same "close
 * companion, not a battlefield-wide effect" reasoning.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per PROGRESS.md's
 * standing note): whether Regeneration I at this refresh rate feels
 * balanced in actual play, whether 6 blocks is a comfortable "stay with
 * your familiar" range for a flying companion that uses {@code
 * WaterAvoidingRandomFlyingGoal} (it may wander in and out of range more
 * than a ground-bound Wolf would), and whether the once-per-second heart
 * particle is a clear enough signal that the aura is active without being
 * distracting.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumFamiliarAuraHandler {

    private static final double RADIUS = 6.0D;
    private static final int EFFECT_DURATION_TICKS = 140;
    private static final int EFFECT_AMPLIFIER = 0;
    private static final int PARTICLE_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(RADIUS);
        List<PrismiumFamiliarEntity> companions = level.getEntitiesOfClass(
                PrismiumFamiliarEntity.class, area,
                familiar -> familiar.isAlive()
                        && familiar.isTame()
                        && familiar.isOwnedBy(player)
                        && !familiar.isOrderedToSit());
        if (companions.isEmpty()) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER,
                true, false, false));

        if (level.getGameTime() % PARTICLE_INTERVAL_TICKS == 0 && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    1, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }
}
