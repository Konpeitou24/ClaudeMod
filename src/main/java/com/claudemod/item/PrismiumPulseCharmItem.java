package com.claudemod.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Scheduled session #63: Prismium Pulse Charm - the mod's fourth purely
 * "detection" accessory, following {@link PrismiumLocatorItem} (session
 * 16, ore detection) but aimed at the mod's own growing hostile-mob
 * roster (Wraith/Deep Wraith/Sentinel, all {@code Monster} subclasses per
 * PROGRESS.md sessions 12/47/59) rather than blocks. Addresses the
 * long-standing "探索そのものが楽しくなるような装備" mod concept from a
 * new angle: previous sessions added mobs and hazards (Wraith, Sentinel,
 * Prismium Spike, Wardstone) but nothing lets a player sense them coming
 * before a surprise melee/ranged hit in a dark Prism Realm cave.
 *
 * <p>Right-click performs a one-off, server-side {@link
 * Level#getEntitiesOfClass(Class, AABB)} scan of a cube centered on the
 * player (see {@link #SEARCH_RADIUS}) for any {@link Monster} (vanilla or
 * this mod's own), then applies vanilla's {@link MobEffects#GLOWING} to
 * every one found for {@link #GLOW_DURATION_TICKS} - giving the player a
 * temporary see-through-walls outline on every nearby threat, similar in
 * spirit to the Locator's compass-direction readout but for "danger"
 * instead of "ore". Deliberately reuses {@link Level#getEntitiesOfClass}
 * (the same long-stable, version-unchanging entity-query API vanilla
 * itself uses everywhere, e.g. {@code Mob#findNearestValidTarget}) rather
 * than any newer/less-certain API, keeping this in the same
 * "well-established API, no new vocabulary" risk tier as the Locator's
 * own block-scan.
 *
 * <p><b>Design choice - {@code Monster}, not a hand-picked class list</b>:
 * {@code Monster} is the same base class {@code ModEntityEvents} already
 * relies on for this mod's own hostiles' spawn-rule registration (see
 * PROGRESS.md sessions 12/59, "{@code Monster::checkMonsterSpawnRules}"),
 * so reusing it here for detection keeps one consistent "what counts as
 * hostile" definition across the mod instead of introducing a second,
 * possibly-diverging one. This automatically covers every vanilla
 * hostile (zombies, skeletons, creepers, etc.) too, and just as
 * automatically excludes the mod's own non-hostile Prismium Drifter
 * (session 61, a {@code Squid}/{@code WaterAnimal} subclass, not a
 * {@code Monster}).
 *
 * <p><b>Design choice - Glowing, not a message</b>: the Locator reports a
 * single nearest match via action-bar text because a block position is a
 * single point; here there can be many simultaneous threats at different
 * positions, so a rendered outline (which vanilla's own Glowing status
 * effect already provides for free, through walls, without this mod
 * needing to write any new client-side rendering code) communicates
 * "here's everything and roughly where" far better than a wall of text
 * would. This also sidesteps needing a compass-style pointer UI, keeping
 * this item's implementation surface as small as Locator's own
 * deliberately low-risk choice (see that class's javadoc).
 *
 * <p>Not consumed and does not take durability damage - like the
 * Locator, only a cooldown (see {@link #COOLDOWN_TICKS}, matched to the
 * glow duration so the charm can't be re-used to "refresh" the outline
 * before it would have expired anyway) guards against spam.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, per
 * PROGRESS.md's standing note): whether {@link MobEffects#GLOWING}
 * applied this way actually renders the expected through-wall outline on
 * every affected entity; whether a 16-block search radius / 200-tick (10s)
 * glow duration / 200-tick cooldown are reasonable first-guess balance
 * numbers (no measurement behind any of the three, same caveat every
 * other accessory item in this mod carries); and whether scanning a
 * (2*16+1)^3 = 35,937-position-equivalent {@code AABB} (via the engine's
 * own entity-lookup, not a manual per-block loop the way Locator's ore
 * scan works) causes a noticeable hitch when used in an entity-dense
 * area - Locator's own javadoc flags the analogous concern for its
 * larger, hand-rolled block loop, but this class's entity query is a
 * single engine call so the actual cost profile is expected to be
 * different, just as unmeasured.
 */
public class PrismiumPulseCharmItem extends Item {

    private static final double SEARCH_RADIUS = 16.0D;
    private static final int GLOW_DURATION_TICKS = 200;
    private static final int COOLDOWN_TICKS = 200;

    public PrismiumPulseCharmItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            // Actual scan + effect application only runs server-side, same
            // split as PrismiumLocatorItem#use.
            return InteractionResultHolder.success(stack);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));

        AABB scanBox = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Monster> threats = level.getEntitiesOfClass(Monster.class, scanBox, LivingEntity::isAlive);

        if (threats.isEmpty()) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_pulse_charm.none"), true);
            return InteractionResultHolder.success(stack);
        }

        for (Monster threat : threats) {
            threat.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, true, false));
        }

        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 0.4F, 1.6F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    player.getX(), player.getY() + player.getBbHeight() / 2.0D, player.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        player.displayClientMessage(
                Component.translatable("message.claudemod.prismium_pulse_charm.found", threats.size()), true);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
