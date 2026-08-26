package com.claudemod.teleport;

import com.claudemod.dimension.ModDimensions;
import com.claudemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

import java.util.Set;

/**
 * Session 52: shared Overworld&lt;-&gt;Prism Realm teleport logic, pulled
 * out of {@link com.claudemod.item.PrismiumRiftShardItem} (session 14,
 * with session 47's landing-safety fix) so a second, more "real" way to
 * travel - {@link com.claudemod.block.PrismiumPortalBlock} - can call the
 * exact same, already-reasoned-about logic instead of copy-pasting it.
 * Behavior is unchanged from the Rift Shard's original implementation;
 * this is a pure extract-method refactor (verified by diffing the moved
 * bodies against the pre-refactor versions) plus visibility changes
 * (private instance methods -> public static methods taking the
 * {@link MinecraftServer}/{@link ServerPlayer} explicitly instead of
 * reading {@code this}).
 *
 * <p>See {@code PrismiumRiftShardItem}'s class javadoc for the original
 * design rationale (fixed anchor point, no custom {@code ITeleporter},
 * round-trip position saved in the player's persistent data under
 * {@link #RETURN_TAG_KEY}) - none of that changed here.
 *
 * <p><b>Session 53 addition - the one-way problem</b>: PROGRESS.md flagged
 * that a player who reaches the Prism Realm through
 * {@code PrismiumPortalBlock} (which consumes a Prismium Shard to ignite)
 * and has no spare shard left had no way back - {@code teleportBackFromRealm}
 * existed but nothing physical in the Realm could trigger it. {@link
 * #teleportToRealm} now calls {@link #ensureReturnPortal} every time,
 * which builds (once, idempotently) a second, fixed {@code
 * PrismiumPortalBlock} frame a few blocks from the landing spot - so
 * every arrival, regardless of which item/block sent the player there,
 * leaves behind a walk-through way back that costs nothing to use.
 */
public final class PrismiumTeleportHelper {

    private PrismiumTeleportHelper() {
    }

    private static final String RETURN_TAG_KEY = "claudemod_realm_return";
    private static final BlockPos REALM_ANCHOR = new BlockPos(0, 0, 0);
    private static final int REALM_FALLBACK_SURFACE_Y = 65;

    // Session 53: fixed offset (from REALM_ANCHOR, at the landing Y) for
    // the auto-built return portal - see class javadoc. Kept a few
    // blocks away from the anchor itself so it never overlaps the
    // landing spot a player is standing on when they first arrive.
    private static final int RETURN_PORTAL_X_OFFSET = 4;
    private static final int RETURN_PORTAL_RING_WIDTH = 4;
    private static final int RETURN_PORTAL_RING_HEIGHT = 5;

    /**
     * Teleports {@code player} from wherever they currently are into the
     * Prism Realm's fixed anchor point, first saving their current
     * dimension/position/rotation so {@link #teleportBackFromRealm} can
     * return them later. Returns {@code false} (no-op) if the Prism Realm
     * level isn't currently loaded on this server.
     */
    public static boolean teleportToRealm(MinecraftServer server, ServerPlayer player) {
        ServerLevel realmLevel = server.getLevel(ModDimensions.PRISM_REALM);
        if (realmLevel == null) {
            // Dimension not present yet - see ModDimensions' javadoc about
            // the "restart the server once after first world creation"
            // caveat. Fail quietly rather than throwing.
            return false;
        }

        CompoundTag save = new CompoundTag();
        save.putString("dim", player.level().dimension().location().toString());
        save.putDouble("x", player.getX());
        save.putDouble("y", player.getY());
        save.putDouble("z", player.getZ());
        save.putFloat("yaw", player.getYRot());
        save.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(RETURN_TAG_KEY, save);

        int landingY = findSafeRealmLanding(realmLevel, REALM_ANCHOR.getX(), REALM_ANCHOR.getZ());
        double destX = REALM_ANCHOR.getX() + 0.5;
        double destZ = REALM_ANCHOR.getZ() + 0.5;

        ensureReturnPortal(realmLevel, landingY);

        player.teleportTo(realmLevel, destX, landingY, destZ, Set.of(), player.getYRot(), player.getXRot());
        realmLevel.playSound(null, destX, landingY, destZ,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /**
     * Forces the destination chunk to fully generate before trusting its
     * heightmap, then clamps against anything at/below the world floor -
     * see {@code PrismiumRiftShardItem}'s session 47 javadoc (preserved
     * there) for the full reasoning. If the landing spot is liquid, a
     * 9x9 {@code prismium_soil} platform is carved at the water surface
     * so the player doesn't just start treading water.
     */
    private static int findSafeRealmLanding(ServerLevel realmLevel, int x, int z) {
        realmLevel.getChunk(x >> 4, z >> 4);

        int landingY = realmLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (landingY <= realmLevel.getMinBuildHeight()) {
            landingY = REALM_FALLBACK_SURFACE_Y;
        }

        BlockPos underfoot = new BlockPos(x, landingY - 1, z);
        FluidState fluid = realmLevel.getFluidState(underfoot);
        if (!fluid.isEmpty()) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos platformPos = underfoot.offset(dx, 0, dz);
                    realmLevel.setBlockAndUpdate(platformPos, ModBlocks.PRISMIUM_SOIL.get().defaultBlockState());
                }
            }
        }

        return landingY;
    }

    /**
     * Builds (once) a small, always-lit {@code PrismiumPortalBlock} frame
     * at a fixed spot near the Realm anchor, so a player who arrives with
     * no Prismium Shard on hand still has a free, physical way back - see
     * the class javadoc's "one-way problem" note. Idempotent: if the
     * frame's interior already has portal blocks in it (checked via a
     * single block read), this is a no-op, so repeated arrivals - the
     * common case - don't re-set the same blocks every time.
     *
     * <p>Reuses the exact ring dimensions and per-cell materials {@code
     * PrismiumPortalIgniteHandler} validates (4 wide x 5 tall outer ring,
     * 2x3 interior, top/bottom rows of {@code PRISMIUM_BLOCK} and
     * left/right columns of {@code PRISMIUM_BLOCK_WALL} - updated in the
     * direct-chat session on 2026-08-19 alongside that handler's own
     * recipe change, see its javadoc) so this auto-built frame looks
     * identical to one a player builds and ignites by hand, and remains a
     * legitimate, walk-through-both-ways {@code PrismiumPortalBlock} pair
     * once built - not a teleport pad or a special case.
     *
     * <p><b>Session 73 direct-chat fix - wrong-facing return portal</b>:
     * a screenshot showed the auto-built return portal looking like a
     * thin vertical pole instead of a gate. The frame geometry itself was
     * fine; the bug was that the frame's plane ({@code Direction.Axis.X},
     * spanning along X) was parallel to {@link #RETURN_PORTAL_X_OFFSET},
     * the very direction the landing spot is offset from it - so walking
     * straight toward the portal (the obvious thing to do, since it's
     * directly ahead) meant walking along its own width axis and seeing
     * it perfectly edge-on, all 4 width-blocks of each row visually
     * stacked behind one another. Fixed by building the frame in the
     * perpendicular plane instead (spanning Z, {@code Direction.Axis.Z})
     * so its thin, visible membrane faces back down the +X approach from
     * the landing spot, the same way a real doorway is oriented across
     * the hallway leading to it rather than along it. Unverified in an
     * actual client - reasoned from the same axis/getShape relationship
     * documented in {@link com.claudemod.block.PrismiumPortalBlock}, not
     * confirmed by walking up to it in a running game.
     */
    private static void ensureReturnPortal(ServerLevel realmLevel, int landingY) {
        BlockPos origin = new BlockPos(
                REALM_ANCHOR.getX() + RETURN_PORTAL_X_OFFSET, landingY,
                REALM_ANCHOR.getZ() - RETURN_PORTAL_RING_WIDTH / 2);

        BlockPos interiorProbe = origin.offset(0, 1, 1);
        if (realmLevel.getBlockState(interiorProbe).getBlock() == ModBlocks.PRISMIUM_PORTAL.get()) {
            // Already built on a previous arrival - nothing to do.
            return;
        }

        // Solid footing under the whole footprint first, in case the
        // ground here is water/void (this Realm is a flat "waterworld",
        // see PROGRESS.md session 47) - same material/reasoning as the
        // landing platform carved by findSafeRealmLanding. dx/dz swapped
        // relative to the pre-fix version to match the frame now
        // spanning Z instead of X (see javadoc above).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= RETURN_PORTAL_RING_WIDTH; dz++) {
                BlockPos floorPos = origin.offset(dx, -1, dz);
                realmLevel.setBlockAndUpdate(floorPos, ModBlocks.PRISMIUM_SOIL.get().defaultBlockState());
            }
        }

        for (int w = 0; w < RETURN_PORTAL_RING_WIDTH; w++) {
            for (int h = 0; h < RETURN_PORTAL_RING_HEIGHT; h++) {
                boolean isTopOrBottomRow = h == 0 || h == RETURN_PORTAL_RING_HEIGHT - 1;
                boolean isLeftOrRightColumn = w == 0 || w == RETURN_PORTAL_RING_WIDTH - 1;
                // w now walks along Z (was X pre-fix) - see javadoc above.
                BlockPos pos = origin.offset(0, h, w);
                if (isTopOrBottomRow) {
                    realmLevel.setBlockAndUpdate(pos, ModBlocks.PRISMIUM_BLOCK.get().defaultBlockState());
                } else if (isLeftOrRightColumn) {
                    realmLevel.setBlockAndUpdate(pos, ModBlocks.PRISMIUM_BLOCK_WALL.get().defaultBlockState());
                } else {
                    realmLevel.setBlockAndUpdate(pos,
                            ModBlocks.PRISMIUM_PORTAL.get().defaultBlockState()
                                    .setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z));
                }
            }
        }
    }

    /**
     * Teleports {@code player} out of the Prism Realm, back to whatever
     * dimension/position was saved by {@link #teleportToRealm}, or to the
     * Overworld's shared spawn if nothing was saved (e.g. the player was
     * placed in the realm by other means, like {@code /execute in}).
     */
    public static boolean teleportBackFromRealm(MinecraftServer server, ServerPlayer player) {
        CompoundTag saved = player.getPersistentData().getCompound(RETURN_TAG_KEY);

        ResourceKey<Level> destDim = Level.OVERWORLD;
        double destX;
        double destY;
        double destZ;
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (saved.contains("x")) {
            ResourceLocation dimLoc = ResourceLocation.tryParse(saved.getString("dim"));
            if (dimLoc != null) {
                destDim = ResourceKey.create(Registries.DIMENSION, dimLoc);
            }
            destX = saved.getDouble("x");
            destY = saved.getDouble("y");
            destZ = saved.getDouble("z");
            yaw = saved.getFloat("yaw");
            pitch = saved.getFloat("pitch");
        } else {
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return false;
            }
            BlockPos spawn = overworld.getSharedSpawnPos();
            destX = spawn.getX() + 0.5;
            destZ = spawn.getZ() + 0.5;
            overworld.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
            destY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        }

        ServerLevel destLevel = server.getLevel(destDim);
        if (destLevel == null) {
            // Saved dimension no longer exists (unlikely, but be defensive) -
            // fall back to the Overworld rather than failing outright.
            destLevel = server.getLevel(Level.OVERWORLD);
            if (destLevel == null) {
                return false;
            }
        }

        player.teleportTo(destLevel, destX, destY, destZ, Set.of(), yaw, pitch);
        destLevel.playSound(null, destX, destY, destZ,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
