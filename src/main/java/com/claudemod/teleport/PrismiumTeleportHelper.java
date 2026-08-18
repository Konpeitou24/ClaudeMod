package com.claudemod.teleport;

import com.claudemod.dimension.ModDimensions;
import com.claudemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
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
 */
public final class PrismiumTeleportHelper {

    private PrismiumTeleportHelper() {
    }

    private static final String RETURN_TAG_KEY = "claudemod_realm_return";
    private static final BlockPos REALM_ANCHOR = new BlockPos(0, 0, 0);
    private static final int REALM_FALLBACK_SURFACE_Y = 65;

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
