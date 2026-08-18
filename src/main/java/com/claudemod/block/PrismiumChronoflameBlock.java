package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Prismium Chronoflame (session 49, scheduled): PROGRESS.md section 5 item
 * 12(a)(ii) - the second of the two Rift Shard-family follow-up items the
 * repo owner requested (the first, a respawn-point item, shipped session
 * 48 as {@link PrismiumRiftAnchorItem}). Requested feature: "a
 * campfire-like block that can freely change the time, but doesn't drop
 * an item when broken".
 *
 * <p>Deliberately kept as a plain {@code Block} (no {@link
 * com.claudemod.blockentity}, no GUI, no custom VoxelShape/model beyond
 * {@code cube_all}) rather than mirroring the energy machines' {@code
 * BaseEntityBlock} pattern - this block carries no persistent state at
 * all (it doesn't remember anything about itself, it just nudges the
 * *level's* day-time counter on interact), so a block entity would be
 * pure unused overhead. This is the same "smallest slice that still does
 * the job" reasoning {@link PrismiumSpikeBlock}'s class doc lays out for
 * reusing the plain-{@code Block} shape instead of inventing new API
 * surface for a single session's feature.
 *
 * <p><b>Time control</b>: right-click steps the current level's day-time
 * counter forward by {@link #TIME_STEP} (6000 ticks = a quarter Minecraft
 * day = 6 in-game hours, e.g. sunrise -> noon); shift+right-click steps
 * it backward by the same amount. Implemented via {@link
 * ServerLevel#setDayTime(long)} - verified this session (WebSearch) to
 * exist as a stable, long-standing vanilla API: a public void method
 * taking a single {@code long} tick argument, documented identically
 * across the 1.18.2 Forge-mapped javadoc mirror and CraftTweaker's own
 * ServerLevel API docs (which expose it as a settable {@code dayTime}
 * property backed by the same underlying call) - two independent sources
 * cross-checked rather than assuming from memory, per this mod's
 * "verify, don't guess" API practice (see PROGRESS.md section 3G). Only
 * {@code ServerLevel} (not the shared {@code Level} superclass) exposes a
 * *setter* for day-time, hence the {@code instanceof ServerLevel} check
 * below - {@code use()} always runs server-side already (the {@code
 * level.isClientSide} branch returns before this point), so the cast is
 * expected to always succeed in practice; the {@code instanceof} is a
 * defensive fallback rather than a code path expected to be hit.
 *
 * <p>Result is clamped to a minimum of 0 so that repeatedly rewinding
 * within the first few minutes of a fresh world/dimension's existence
 * cannot drive the counter negative (day-time also feeds moon-phase
 * calculations elsewhere in vanilla, which were not audited here for
 * negative-input behaviour - clamping avoids relying on that being safe).
 *
 * <p><b>No drops</b>: {@code Properties.noLootTable()} (confirmed
 * this session via the 1.20.1 Forge-mapped javadoc mirror to be a
 * genuine no-arg {@code BlockBehaviour.Properties} method, not
 * invented) means this block has no loot table file at all and simply
 * never drops anything when broken - matches the repo owner's request
 * literally ("破壊時にアイテムとしてドロップしない"). Once placed, a
 * Chronoflame is a one-way commitment; there is no way to reclaim it as
 * an item. This is an intentional, slightly unusual design (most blocks
 * in this mod drop themselves) so it is called out explicitly in both
 * lang usage tooltips (see {@code
 * PrismiumChronoflameBlockItem#appendHoverText}) and PROGRESS.md.
 *
 * <p><b>Unverified</b> (no local build/game client in this sandbox, see
 * PROGRESS.md): whether {@code setDayTime} on the *current* level
 * actually only affects that dimension's own clock (expected/intended -
 * each {@code ServerLevel} instance keeps its own day-time in its own
 * {@code LevelData}) or has any unexpected cross-dimension effect;
 * whether repeated large jumps in day-time cause any visible mob-spawn
 * or lighting-update hiccups; the in-game feel/usefulness of a fixed
 * 6-hour step size (chosen as "a quarter of a day, an easy mental model"
 * rather than anything play-tested).
 *
 * <p><b>Session 50 addition - per-player cooldown</b>: PROGRESS.md's
 * "discussion points" section flagged that this block, as shipped in
 * session 49, has no rate limit at all - a player can hold right-click
 * and spam {@code use()} every tick, making day/night effectively
 * togglable at will with no cost beyond the original craft. This did not
 * change the crafting cost (still a modest clock + 2 glowstone dust + 4
 * Prismium Shard - the repo owner has not weighed in on whether the cost
 * itself is a problem), but adds a {@value #COOLDOWN_TICKS}-tick
 * (5 second) per-player cooldown as the smallest change that removes the
 * "hold click, get instant full day/night control" degenerate case while
 * leaving the core mechanic and its cost untouched. Tracked in a static
 * {@link WeakHashMap} keyed by player {@link UUID} (server-side only,
 * never persisted) rather than a block-entity field, consistent with
 * this class's existing "the block itself holds no state" design - the
 * cooldown belongs to the *player*, not to any particular Chronoflame
 * block, matching how e.g. ender pearl cooldown is per-player rather
 * than per-item-stack.
 */
public class PrismiumChronoflameBlock extends Block {

    /** A quarter of a full 24000-tick Minecraft day, i.e. 6 in-game
     * hours per click - see class doc for why this size was chosen. */
    private static final long TIME_STEP = 6000L;

    /** 5 real-time seconds at 20 ticks/second - see class doc, session 50
     * addition. */
    private static final long COOLDOWN_TICKS = 100L;

    /** Player UUID -> the serverLevel game-time (not day-time; {@link
     * ServerLevel#getGameTime()} always advances monotonically, unlike
     * day-time which this very block can rewind) at which that player's
     * cooldown expires. WeakHashMap so entries for players who log off
     * for good don't accumulate forever; server-side only, rebuilt (empty)
     * on every server restart, which is fine since a cooldown surviving a
     * restart was never a requirement. */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new WeakHashMap<>();

    public PrismiumChronoflameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            // Defensive fallback only - see class doc, this branch is not
            // expected to actually be reached.
            return InteractionResult.PASS;
        }

        // GitHub issue #16 ("意図せずクリックして時間が進んでしまう" -
        // walking past this block or aiming at something behind it, e.g.
        // a door, could accidentally trigger a time change): now
        // requires holding a Clock - the same item this block is crafted
        // from, see class doc's recipe note - in the interacting hand.
        // An empty hand or unrelated item does nothing (PASS, so any
        // other interaction the player actually meant to perform, e.g. a
        // door one block behind this one, still goes through normally).
        // This does not change the block's hitbox/interaction range
        // (never the actual problem), but it does mean a stray click only
        // matters if the player happens to be holding a clock at that
        // exact moment, which is far less likely than an idle empty hand.
        if (!player.getItemInHand(hand).is(Items.CLOCK)) {
            return InteractionResult.PASS;
        }

        long gameTime = serverLevel.getGameTime();
        Long cooldownUntil = COOLDOWN_UNTIL.get(player.getUUID());
        if (cooldownUntil != null && gameTime < cooldownUntil) {
            // GitHub issue #16 ("クールダウンがわかりずらい"): this used
            // to swallow the interaction with zero feedback (see session
            // 50's original comment, kept here for history: "no sound, no
            // message, no day-time change"). Now reports the remaining
            // wait via the action bar (the same transient overlay line
            // vanilla uses for e.g. "Can't sleep now") so a player who
            // just used it understands why the second click did nothing
            // instead of assuming the block is broken or missed the
            // click entirely.
            long remainingTicks = cooldownUntil - gameTime;
            int remainingSeconds = (int) Math.max(1, (remainingTicks + 19) / 20);
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_chronoflame.cooldown", remainingSeconds),
                    true);
            return InteractionResult.CONSUME;
        }
        COOLDOWN_UNTIL.put(player.getUUID(), gameTime + COOLDOWN_TICKS);

        boolean rewind = player.isShiftKeyDown();
        long current = serverLevel.getDayTime();
        long next = rewind ? current - TIME_STEP : current + TIME_STEP;
        if (next < 0) {
            next = 0;
        }
        serverLevel.setDayTime(next);

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS,
                0.6f, rewind ? 0.7f : 1.3f);
        player.displayClientMessage(
                Component.translatable(rewind
                        ? "message.claudemod.prismium_chronoflame.rewind"
                        : "message.claudemod.prismium_chronoflame.advance"),
                true);

        return InteractionResult.CONSUME;
    }
}
