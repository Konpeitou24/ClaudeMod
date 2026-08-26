package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct-chat session (2026-08-19): a follow-up to the same day's
 * Prismium Portal frame-recipe change (see
 * {@link PrismiumPortalIgniteHandler}'s javadoc). After seeing the new
 * thin/animated portal in-game, the repo owner reported that breaking
 * part of the frame did not deactivate the portal - unlike vanilla's
 * nether portal, which fizzles out (the interior portal blocks vanish)
 * once its frame is broken. This class fixes that: whenever a
 * {@code PRISMIUM_BLOCK} or {@code PRISMIUM_BLOCK_WALL} is broken, it
 * checks every candidate frame the broken block could have belonged to
 * and, for any that still has an intact 2x3 interior of active
 * {@code PRISMIUM_PORTAL} blocks, re-validates the full ring; if the
 * ring is no longer valid (because of the block that just broke), the
 * whole interior is cleared back to air.
 *
 * <p><b>Why {@code BlockEvent.BreakEvent} instead of overriding {@code
 * neighborChanged} on {@code PrismiumPortalBlock}</b>: {@code
 * neighborChanged} only fires on blocks that are orthogonally adjacent
 * to the position that changed. The 4-wide x 5-tall frame ring has four
 * corner cells that are only *diagonally* adjacent to the 2x3 interior -
 * breaking a corner would never notify any portal block via {@code
 * neighborChanged}, silently leaving the bug half-fixed. Listening for
 * the break directly on the frame material itself, and searching
 * outward from there (the same brute-force "every candidate ring
 * placement" approach {@link PrismiumPortalIgniteHandler} already uses
 * to search inward from a clicked block), covers corners too without
 * needing to reproduce vanilla's more general portal-shape search.
 *
 * <p><b>Event timing note</b>: {@code BlockEvent.BreakEvent} fires
 * *before* the broken block is actually removed from the level, so
 * {@code level.getBlockState(brokenPos)} during this handler still
 * reports the old (about-to-be-removed) block. {@link #tryCollapse}
 * accounts for this by treating the broken position as failing the ring
 * check unconditionally, rather than re-reading its (stale) world state.
 *
 * <p><b>Known gaps, not handled by this class (deliberately left as a
 * simplification, matching this mod's general "small mob/no vanilla-
 * shape-search" approach to this feature)</b>: only *breaking* a frame
 * block (a player-initiated break, via {@code BlockEvent.BreakEvent})
 * triggers re-validation. Frame material destroyed by an explosion,
 * piston, fluid flow, or {@code /setblock} will not trigger this check
 * and could leave a portal running with a broken frame - a real gap,
 * left for a future session if it turns out to matter in practice.
 * <b>Unverified</b>, like the rest of this feature: no in-game
 * confirmation that breaking a frame block actually clears the portal,
 * or that legitimate frame edits (e.g. replacing one Prismium Block with
 * another, which is a break+place in quick succession) don't
 * accidentally clear an otherwise-still-valid portal in the brief window
 * between the two.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumPortalFrameBreakHandler {

    /** Must match {@link PrismiumPortalIgniteHandler}'s ring dimensions. */
    private static final int RING_WIDTH = 4;
    private static final int RING_HEIGHT = 5;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Block brokenBlock = event.getState().getBlock();
        if (brokenBlock != ModBlocks.PRISMIUM_BLOCK.get() && brokenBlock != ModBlocks.PRISMIUM_BLOCK_WALL.get()) {
            return;
        }
        BlockPos brokenPos = event.getPos();

        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            for (int widthOffset = 0; widthOffset < RING_WIDTH; widthOffset++) {
                for (int heightOffset = 0; heightOffset < RING_HEIGHT; heightOffset++) {
                    boolean isTopOrBottomRow = heightOffset == 0 || heightOffset == RING_HEIGHT - 1;
                    boolean isLeftOrRightColumn = widthOffset == 0 || widthOffset == RING_WIDTH - 1;
                    if (!isTopOrBottomRow && !isLeftOrRightColumn) {
                        // This candidate treats brokenPos as an interior
                        // cell, but brokenPos is frame material, not a
                        // portal block - not a valid candidate.
                        continue;
                    }
                    BlockPos origin = brokenPos
                            .relative(axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH, widthOffset)
                            .below(heightOffset);
                    tryCollapse(level, origin, axis, brokenPos);
                }
            }
        }
    }

    /**
     * Checks one candidate frame (given by its ring origin/axis). If its
     * interior is currently a full 2x3 block of active
     * {@code PRISMIUM_PORTAL} blocks (i.e. this really is an existing,
     * lit portal - not just an empty or half-built frame) and its ring is
     * no longer valid given the just-broken block, clears the interior
     * back to air. A no-op for candidates that aren't an active portal at
     * all, so most of the up-to-40 candidates tried per break bail out
     * immediately on the first interior cell check.
     */
    private static void tryCollapse(ServerLevel level, BlockPos origin, Direction.Axis axis, BlockPos brokenPos) {
        List<BlockPos> interior = new ArrayList<>();
        for (int w = 1; w < RING_WIDTH - 1; w++) {
            for (int h = 1; h < RING_HEIGHT - 1; h++) {
                BlockPos pos = offset(origin, axis, w, h);
                if (level.getBlockState(pos).getBlock() != ModBlocks.PRISMIUM_PORTAL.get()) {
                    return;
                }
                interior.add(pos);
            }
        }

        for (int w = 0; w < RING_WIDTH; w++) {
            for (int h = 0; h < RING_HEIGHT; h++) {
                boolean isTopOrBottomRow = h == 0 || h == RING_HEIGHT - 1;
                boolean isLeftOrRightColumn = w == 0 || w == RING_WIDTH - 1;
                if (!isTopOrBottomRow && !isLeftOrRightColumn) {
                    continue;
                }
                BlockPos pos = offset(origin, axis, w, h);
                Block required = isTopOrBottomRow
                        ? ModBlocks.PRISMIUM_BLOCK.get()
                        : ModBlocks.PRISMIUM_BLOCK_WALL.get();
                // See class javadoc's "event timing note": brokenPos's
                // world state is stale (not yet removed) at this point,
                // so it's checked by position rather than by reading it.
                boolean thisCellFailed = pos.equals(brokenPos) || level.getBlockState(pos).getBlock() != required;
                if (thisCellFailed) {
                    for (BlockPos interiorPos : interior) {
                        level.removeBlock(interiorPos, false);
                    }
                    // Direct-chat session (2026-08-26): the collapse used
                    // to be silent - added an original "fizzle" sound
                    // here (see ModSounds' class javadoc) to match the
                    // ignite sound played on the way up. Played once per
                    // collapse (not per removed block) from roughly the
                    // interior's center. Follow-up the same session:
                    // layered with vanilla's AMETHYST_BLOCK_RESONATE at a
                    // lowered pitch (the "settling down" counterpart to
                    // AMETHYST_BLOCK_CHIME on ignite, see
                    // PrismiumPortalIgniteHandler) for real musical/tonal
                    // texture a pure synth can't easily match - see that
                    // class's updated javadoc for the repo owner's
                    // reasoning. Pitch randomized per play on both.
                    BlockPos soundPos = interior.get(interior.size() / 2);
                    RandomSource random = level.getRandom();
                    level.playSound(null, soundPos, ModSounds.PRISMIUM_PORTAL_FIZZLE.get(),
                            SoundSource.BLOCKS, 1.0F, 0.95F + random.nextFloat() * 0.1F);
                    level.playSound(null, soundPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS,
                            0.7F, 0.6F + random.nextFloat() * 0.15F);
                    return;
                }
            }
        }
    }

    private static BlockPos offset(BlockPos origin, Direction.Axis axis, int w, int h) {
        BlockPos horizontal = axis == Direction.Axis.X ? origin.east(w) : origin.south(w);
        return horizontal.above(h);
    }
}
