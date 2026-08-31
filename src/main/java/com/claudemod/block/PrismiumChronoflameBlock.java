package com.claudemod.block;

import com.claudemod.menu.PrismiumChronoflameMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

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
 * com.claudemod.blockentity}, no custom VoxelShape/model beyond
 * {@code cube_all}) rather than mirroring the energy machines' {@code
 * BaseEntityBlock} pattern - this block carries no persistent *world*
 * state (it doesn't remember anything about itself, it just nudges the
 * *level's* day-time counter on interact), so a block entity would be
 * pure unused overhead. This is the same "smallest slice that still does
 * the job" reasoning {@link PrismiumSpikeBlock}'s class doc lays out for
 * reusing the plain-{@code Block} shape instead of inventing new API
 * surface for a single session's feature. Having a GUI (see below) does
 * not change this - {@link SimpleMenuProvider} (vanilla's own
 * CraftingTableBlock/LoomBlock use exactly this class for the same
 * reason: a block that wants a menu but has nothing of its own worth
 * persisting in a BlockEntity) lets a plain {@code Block} open a menu by
 * capturing {@code level}/{@code pos} in a closure at {@code use()} time
 * instead.
 *
 * <p><b>Time control</b>: the GUI's two buttons step the current level's
 * day-time counter forward or backward by {@link #TIME_STEP} (6000 ticks
 * = a quarter Minecraft day = 6 in-game hours, e.g. sunrise -> noon).
 * Implemented via {@link ServerLevel#setDayTime(long)} - verified this
 * session (WebSearch) to exist as a stable, long-standing vanilla API: a
 * public void method taking a single {@code long} tick argument,
 * documented identically across the 1.18.2 Forge-mapped javadoc mirror
 * and CraftTweaker's own ServerLevel API docs (which expose it as a
 * settable {@code dayTime} property backed by the same underlying call) -
 * two independent sources cross-checked rather than assuming from memory,
 * per this mod's "verify, don't guess" API practice (see PROGRESS.md
 * section 3G). Only {@code ServerLevel} (not the shared {@code Level}
 * superclass) exposes a *setter* for day-time, hence the
 * {@code instanceof ServerLevel} check in {@link #tryActivate} -
 * {@link PrismiumChronoflameMenu#clickMenuButton} always runs
 * server-side already, so the cast is expected to always succeed in
 * practice; the {@code instanceof} is a defensive fallback rather than a
 * code path expected to be hit.
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
 * <p><b>Session (scheduled, direct feedback from こんぺいとう氏) - GUI
 * replaces the Clock-in-hand requirement</b>: GitHub issue #16 ("意図せず
 * クリックして時間が進んでしまう") was originally fixed (session 50, kept
 * in git history) by requiring the player to hold a vanilla Clock in the
 * interacting hand before a click did anything - this successfully
 * stopped accidental triggers, but こんぺいとう氏 reported directly in
 * chat that hunting for a clock every time to use the block at all made
 * it "煩雑([tedious])" to the point of being useless in practice, and
 * asked for a real UI instead. This session removes the Clock
 * requirement entirely and replaces it with a proper GUI (see
 * {@link PrismiumChronoflameMenu} / {@code PrismiumChronoflameScreen}):
 * right-clicking with *any* item (or empty hand) now always opens the
 * screen, and only an explicit click on one of its two buttons actually
 * changes the time. This solves both problems at once - no item needs to
 * be held to use the block, and a stray/accidental right-click (e.g.
 * while walking past, or aiming at a door behind it) can no longer change
 * the time by itself, since opening a screen is inert until a button
 * inside it is deliberately clicked.
 *
 * <p><b>Unverified</b> (no local build/game client in this sandbox, see
 * PROGRESS.md): whether {@code setDayTime} on the *current* level
 * actually only affects that dimension's own clock (expected/intended -
 * each {@code ServerLevel} instance keeps its own day-time in its own
 * {@code LevelData}) or has any unexpected cross-dimension effect;
 * whether repeated large jumps in day-time cause any visible mob-spawn
 * or lighting-update hiccups; the in-game feel/usefulness of a fixed
 * 6-hour step size; and (new this session) whether the GUI itself opens
 * and its buttons respond correctly in-game.
 *
 * <p><b>Per-player cooldown</b>: PROGRESS.md's "discussion points"
 * section (session 50) flagged that this block, as originally shipped in
 * session 49, had no rate limit at all. A {@value #COOLDOWN_TICKS}-tick
 * (5 second) per-player cooldown remains in place after moving to a GUI
 * (now enforced when a *button* is clicked, not when the screen is
 * opened - opening the screen itself is always free) as the smallest
 * change that removes the "spam-click, get instant full day/night
 * control" degenerate case while leaving the core mechanic and its cost
 * untouched. Tracked in a static {@link WeakHashMap} keyed by player
 * {@link UUID} (server-side only, never persisted) rather than a
 * block-entity field, consistent with this class's existing "the block
 * itself holds no state" design - the cooldown belongs to the *player*,
 * not to any particular Chronoflame block, matching how e.g. ender pearl
 * cooldown is per-player rather than per-item-stack.
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
        // Scheduled session, direct feedback: no more Items.CLOCK check
        // here - see class doc's "GUI replaces the Clock-in-hand
        // requirement" section. Any right-click (any held item, or none)
        // opens the screen; the cooldown/actual time change only happens
        // if the player then clicks a button inside it.
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (windowId, inv, menuPlayer) -> new PrismiumChronoflameMenu(windowId, inv,
                                    createCooldownContainerData(menuPlayer),
                                    ContainerLevelAccess.create(level, pos)),
                            Component.translatable("block.claudemod.prismium_chronoflame")),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Single-slot {@link ContainerData} (index 0 = remaining cooldown
     * ticks for {@code player}, floored at 0) handed to the *server-side*
     * {@link PrismiumChronoflameMenu} instance so
     * {@code PrismiumChronoflameScreen} can gray out its buttons and show
     * a countdown while on cooldown, instead of a click silently doing
     * nothing (the exact "分かりづらい" complaint GitHub issue #16
     * originally raised about the cooldown, now addressed visually
     * instead of only via an action-bar message). {@code set()} is a
     * deliberate no-op, matching every other {@code ContainerData} in
     * this mod (see e.g. {@code PrismiumCellBlockEntity}'s field doc) -
     * the *client-side* menu constructor
     * ({@link PrismiumChronoflameMenu#PrismiumChronoflameMenu(int,
     * net.minecraft.world.entity.player.Inventory, BlockPos)}) must never
     * reuse this real instance, only a fresh {@code SimpleContainerData},
     * for the exact reason documented at length on
     * {@code PrismiumCellMenu#resolveData} (the v0.31.2 frozen-GUI bug).
     */
    private static ContainerData createCooldownContainerData(Player player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? getRemainingCooldownTicks(player) : 0;
            }

            @Override
            public void set(int index, int value) {
                // Deliberately a no-op - see method doc.
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    /** Ticks remaining on {@code player}'s cooldown, floored at 0 and
     * capped to stay comfortably inside the short range a
     * {@code ContainerData}/{@code DataSlot} can actually sync (see
     * {@code PrismiumCellMenu}'s class doc on that limit) - this value is
     * at most {@link #COOLDOWN_TICKS} (100) so no explicit cap is needed
     * here, unlike e.g. the Generator's burn-time gauge. */
    private static int getRemainingCooldownTicks(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        Long cooldownUntil = COOLDOWN_UNTIL.get(player.getUUID());
        if (cooldownUntil == null) {
            return 0;
        }
        long remaining = cooldownUntil - serverLevel.getGameTime();
        return (int) Math.max(0, remaining);
    }

    /**
     * Actually changes the time, called only from
     * {@link PrismiumChronoflameMenu#clickMenuButton} - i.e. only when a
     * player has explicitly clicked one of the GUI's two buttons, never
     * merely from opening the screen (see class doc). Enforces the same
     * per-player cooldown the old direct-right-click implementation did,
     * with the same action-bar feedback (remaining-seconds message on
     * cooldown, advance/rewind confirmation message and chime sound on
     * success) - only the trigger changed, not the underlying behavior or
     * its pacing.
     */
    public static void tryActivate(Level level, BlockPos pos, Player player, boolean rewind) {
        if (!(level instanceof ServerLevel serverLevel)) {
            // Defensive fallback only - clickMenuButton always runs
            // server-side already, see class doc.
            return;
        }

        long gameTime = serverLevel.getGameTime();
        Long cooldownUntil = COOLDOWN_UNTIL.get(player.getUUID());
        if (cooldownUntil != null && gameTime < cooldownUntil) {
            long remainingTicks = cooldownUntil - gameTime;
            int remainingSeconds = (int) Math.max(1, (remainingTicks + 19) / 20);
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_chronoflame.cooldown", remainingSeconds),
                    true);
            return;
        }
        COOLDOWN_UNTIL.put(player.getUUID(), gameTime + COOLDOWN_TICKS);

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
    }
}
