package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModItems;
import com.claudemod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Session 52: ignition logic for {@link com.claudemod.block.PrismiumPortalBlock}
 * (see that class's javadoc for the overall feature/GitHub issue #9
 * context). Right-clicking a frame ring block ({@code PRISMIUM_BLOCK} or
 * {@code PRISMIUM_BLOCK_WALL}, see below) while holding a Prismium Shard
 * searches the immediate neighborhood for a valid hollow frame - interior
 * exactly 2 blocks wide by 3 blocks tall - in either horizontal
 * orientation, and if found, consumes one shard and fills the interior
 * with portal blocks.
 *
 * <p><b>Direct-chat session update (2026-08-19)</b>: the repo owner
 * reported directly (not via a scheduled session) that (a) the portal
 * block rendered as a full solid cube instead of a thin membrane like
 * vanilla's nether portal, and (b) requested the frame recipe be changed
 * to a specific mixed-material shape instead of uniform
 * {@code PRISMIUM_CORE}: the top row and bottom row (4 blocks each,
 * corners included) built from plain {@code PRISMIUM_BLOCK}, and the
 * left and right columns (3 blocks each, corners excluded since those
 * belong to the top/bottom rows) built from {@code PRISMIUM_BLOCK_WALL}.
 * That shape happens to fit the existing 4-wide x 5-tall outer ring
 * exactly (see {@link #RING_WIDTH}/{@link #RING_HEIGHT}), so only the
 * per-cell material check in {@link #tryFrame} and the ignition-click
 * block check below needed to change - the search geometry itself is
 * untouched. Per the repo owner's explicit choice, this *replaces* the
 * old uniform-{@code PRISMIUM_CORE} recipe rather than existing
 * alongside it: a ring built entirely out of {@code PRISMIUM_CORE} no
 * longer ignites. (Item (a), the thin/animated rendering, was handled
 * entirely in {@code prismium_portal.json} and the new animated
 * {@code prismium_portal.png}/{@code .png.mcmeta} - no Java changes were
 * needed for that part; see {@link com.claudemod.block.PrismiumPortalBlock}
 * for the particle-shape follow-up.) <b>Unverified</b>, like the rest of
 * this feature: no in-game confirmation that the mixed-material ring
 * validates correctly or that the animated texture actually plays
 * smoothly client-side.
 *
 * <p>Follows the same {@code @Mod.EventBusSubscriber} +
 * {@code PlayerInteractEvent.RightClickBlock} pattern already established
 * by {@link PrismiumHoeHandler} (see that class's javadoc for why the
 * hand check and the {@code ServerLevel}-only guard exist - identical
 * reasoning applies here: avoid double-firing on main+off hand, and world
 * edits are server-authoritative).
 *
 * <p><b>Frame search strategy</b>: rather than reproducing vanilla's
 * generic {@code PortalShape} (which supports variable-size frames and is
 * a substantial, hard-to-verify-from-this-sandbox piece of code), this
 * checks a small, fixed number of candidate frame placements - every
 * position where the clicked block could plausibly be part of a 4-wide x
 * 5-tall ring (the outer bounds of a 2x3 interior), now with a per-cell
 * material check instead of a single uniform block type (see the
 * direct-chat session update above) - and accepts the first one that
 * validates. This is deliberately less general than
 * vanilla (frame size is fixed, not "any size 2x3 or larger") but far
 * simpler to reason about without a running client to test against.
 *
 * <p><b>Unverified</b>: no in-game confirmation that {@code
 * PlayerInteractEvent.RightClickBlock} actually fires with the clicked
 * block already resolved to {@code PRISMIUM_BLOCK}/{@code
 * PRISMIUM_BLOCK_WALL} in every case (e.g. whether it also fires for the
 * chiseled/slab/stairs variants of Prismium Block in a way that could
 * confuse players expecting the frame to accept those too - it
 * deliberately does not, only the plain block and the plain wall count)
 * or that the brute-force search below finds every rotation a player
 * might reasonably attempt (only the two axis-aligned orientations are
 * checked; a frame built diagonally, which vanilla doesn't support
 * either, is not expected to work).
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumPortalIgniteHandler {

    /** Interior width (blocks), matching vanilla's minimum Nether portal frame. */
    private static final int INTERIOR_WIDTH = 2;
    /** Interior height (blocks), matching vanilla's minimum Nether portal frame. */
    private static final int INTERIOR_HEIGHT = 3;
    /** Outer ring width = interior + 1 block of frame on each side. */
    private static final int RING_WIDTH = INTERIOR_WIDTH + 2;
    /** Outer ring height = interior + 1 block of frame on each side. */
    private static final int RING_HEIGHT = INTERIOR_HEIGHT + 2;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos clickedPos = event.getPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        net.minecraft.world.level.block.Block clickedBlock = clickedState.getBlock();
        if (clickedBlock != ModBlocks.PRISMIUM_BLOCK.get()
                && clickedBlock != ModBlocks.PRISMIUM_BLOCK_WALL.get()) {
            return;
        }
        ItemStack heldStack = event.getItemStack();
        if (heldStack.getItem() != ModItems.PRISMIUM_SHARD.get()) {
            return;
        }

        FrameMatch match = findFrame(serverLevel, clickedPos);
        if (match == null) {
            return;
        }

        Player player = event.getEntity();
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }

        for (BlockPos interiorPos : match.interiorPositions()) {
            serverLevel.setBlock(interiorPos,
                    ModBlocks.PRISMIUM_PORTAL.get().defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_AXIS, match.axis()),
                    3);
        }

        // Direct-chat session (2026-08-26): was SoundEvents.PORTAL_TRIGGER
        // (a reused vanilla sound) until the repo owner asked for an
        // original ignite sound - see ModSounds' class javadoc for how
        // it was synthesized/self-reviewed.
        serverLevel.playSound(null, clickedPos, ModSounds.PRISMIUM_PORTAL_IGNITE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }

    private record FrameMatch(java.util.List<BlockPos> interiorPositions, Direction.Axis axis) {
    }

    /**
     * Searches every candidate ring placement in which {@code clickedPos}
     * could be one of the ring cells, for both horizontal axes, and
     * returns the first valid match, or {@code null} if none of the
     * candidates validate.
     */
    private static FrameMatch findFrame(ServerLevel level, BlockPos clickedPos) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            // "along" is the axis the ring's width runs parallel to;
            // "across" is always vertical (Y) for the ring's height.
            for (int widthOffset = 0; widthOffset < RING_WIDTH; widthOffset++) {
                for (int heightOffset = 0; heightOffset < RING_HEIGHT; heightOffset++) {
                    FrameMatch match = tryFrame(level, clickedPos, axis, widthOffset, heightOffset);
                    if (match != null) {
                        return match;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Attempts one specific candidate frame: the ring's bottom-left-most
     * corner (in the given axis) is placed so that {@code clickedPos}
     * lands {@code widthOffset}/{@code heightOffset} cells in from that
     * corner. Returns a populated {@link FrameMatch} if every ring cell
     * matches the required material for its position ({@code
     * PRISMIUM_BLOCK} on the top/bottom rows, {@code PRISMIUM_BLOCK_WALL}
     * on the left/right columns) and every interior cell is air, else
     * {@code null}.
     */
    private static FrameMatch tryFrame(ServerLevel level, BlockPos clickedPos, Direction.Axis axis,
                                        int widthOffset, int heightOffset) {
        BlockPos origin = clickedPos.relative(axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH, widthOffset)
                .below(heightOffset);

        for (int w = 0; w < RING_WIDTH; w++) {
            for (int h = 0; h < RING_HEIGHT; h++) {
                boolean isTopOrBottomRow = h == 0 || h == RING_HEIGHT - 1;
                boolean isLeftOrRightColumn = w == 0 || w == RING_WIDTH - 1;
                if (!isTopOrBottomRow && !isLeftOrRightColumn) {
                    continue;
                }
                BlockPos pos = offset(origin, axis, w, h);
                net.minecraft.world.level.block.Block ringBlock = level.getBlockState(pos).getBlock();
                // Corners belong to the top/bottom row (4-wide, corners
                // included), matching the repo owner's description of
                // "top 4 / bottom 4" Prismium Blocks; the left/right
                // columns are the remaining 3 non-corner cells each,
                // built from Prismium Block Wall.
                net.minecraft.world.level.block.Block required = isTopOrBottomRow
                        ? ModBlocks.PRISMIUM_BLOCK.get()
                        : ModBlocks.PRISMIUM_BLOCK_WALL.get();
                if (ringBlock != required) {
                    return null;
                }
            }
        }

        java.util.List<BlockPos> interior = new java.util.ArrayList<>();
        for (int w = 1; w < RING_WIDTH - 1; w++) {
            for (int h = 1; h < RING_HEIGHT - 1; h++) {
                BlockPos pos = offset(origin, axis, w, h);
                if (!level.getBlockState(pos).isAir()) {
                    return null;
                }
                interior.add(pos);
            }
        }

        return new FrameMatch(interior, axis);
    }

    private static BlockPos offset(BlockPos origin, Direction.Axis axis, int w, int h) {
        BlockPos horizontal = axis == Direction.Axis.X ? origin.east(w) : origin.south(w);
        return horizontal.above(h);
    }
}
