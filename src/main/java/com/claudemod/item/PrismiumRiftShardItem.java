package com.claudemod.item;

import com.claudemod.dimension.ModDimensions;
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
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

/**
 * Session 14: Prismium Rift Shard - the mod's first (and, for now, only)
 * way in and out of the Prism Realm dimension (see {@link ModDimensions}
 * and PROGRESS.md section 1, item 3).
 *
 * <p><b>Deliberate minimal-implementation choice</b>: a "real" portal
 * (multi-block frame detection, activation item, a custom
 * {@code ITeleporter} that finds/builds a landing pad) is the eventual
 * goal, but is a much larger, harder-to-verify surface area for a single
 * untested session. Instead this is a reusable, non-consumed item that
 * teleports the holder directly between the Overworld (or whichever
 * dimension they used it from) and a single fixed anchor point at
 * (0, ~surface, 0) in the Prism Realm - the same "reuse an existing,
 * lower-risk mechanism instead of building the fully-fledged version
 * first" choice this mod made for Prismium Wraith (reusing Zombie/
 * ZombieModel wholesale, see PROGRESS.md session 12) and the grappling
 * hook (no flying hook entity, see PrismiumGrapplingHookItem). A real
 * portal block/frame can replace or supplement this later without
 * removing the item - see PROGRESS.md for this session's write-up.
 *
 * <p>Round-trip position memory: right before leaving to the Prism Realm,
 * the player's current dimension + exact position/rotation is saved into
 * this player's {@link Player#getPersistentData()} tag (a general-purpose
 * per-player NBT compound that survives dimension changes and logout -
 * long-standing, stable Forge/vanilla API, not new to this session). Using
 * the shard again while inside the Prism Realm reads that tag back and
 * returns the player to the exact spot (and original dimension - e.g. the
 * Nether or End, if that's where they started) they left from. If no
 * saved position exists (e.g. player was placed in the realm by other
 * means), it falls back to the Overworld's shared spawn point.
 *
 * <p>API notes verified this session (web search):
 * <ul>
 *   <li>{@code ServerPlayer#teleportTo(ServerLevel, double, double, double,
 *   Set<RelativeMovement>, float, float)} is the 1.20.1 overload that
 *   teleports across dimensions directly (handles the dimension-change
 *   machinery internally) - confirmed against 1.20.1 mappings
 *   (mappings.dev) and cross-checked against a real Forge 1.20.1 tutorial
 *   mod's dimension-teleport code (Kaupenjoe's Forge-Tutorial-1.20.X,
 *   cloned and read directly this session) which uses the equivalent
 *   {@code Entity#changeDimension} + {@code ITeleporter} path - this
 *   item's simpler direct-{@code teleportTo} approach was chosen instead
 *   because it doesn't require a custom {@code ITeleporter}/portal block
 *   at all for a fixed anchor point.</li>
 *   <li>Dimension datapack JSON does not need any Java-side registration
 *   call to exist - see {@link ModDimensions}'s javadoc for the one
 *   caveat found (Forge issue #8552, fixed pre-1.20.1, about a possible
 *   first-world-creation restart requirement).</li>
 * </ul>
 *
 * <p><b>Unverified</b>: this entire item is untested beyond "the code
 * compiles" (no in-game playtest possible in this sandbox - see
 * PROGRESS.md). In particular: whether {@code server.getLevel(PRISM_REALM)}
 * actually resolves to a valid, generating level at runtime; whether the
 * fixed (0, ~surface, 0) anchor point ends up somewhere sane (not
 * inside solid terrain, not falling into a ravine); and whether the
 * persistent-data round trip survives a player logging out and back in
 * between uses.
 */
public class PrismiumRiftShardItem extends Item {

    private static final int COOLDOWN_TICKS = 100;
    private static final String RETURN_TAG_KEY = "claudemod_realm_return";
    private static final BlockPos REALM_ANCHOR = new BlockPos(0, 0, 0);

    public PrismiumRiftShardItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            // Actual teleport logic only runs server-side; on the client
            // just let the swing/use animation play.
            return InteractionResultHolder.success(stack);
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        MinecraftServer server = serverLevel.getServer();
        boolean inRealm = serverLevel.dimension() == ModDimensions.PRISM_REALM;

        boolean teleported;
        if (inRealm) {
            teleported = teleportBackFromRealm(server, serverPlayer);
        } else {
            teleported = teleportToRealm(server, serverPlayer);
        }

        if (!teleported) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(stack);
    }

    private boolean teleportToRealm(MinecraftServer server, ServerPlayer player) {
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

        int landingY = realmLevel.getHeight(Heightmap.Types.MOTION_TOP, REALM_ANCHOR.getX(), REALM_ANCHOR.getZ());
        double destX = REALM_ANCHOR.getX() + 0.5;
        double destZ = REALM_ANCHOR.getZ() + 0.5;

        player.teleportTo(realmLevel, destX, landingY, destZ, Set.of(), player.getYRot(), player.getXRot());
        realmLevel.playSound(null, destX, landingY, destZ,
                SoundEvents.END_PORTAL_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private boolean teleportBackFromRealm(MinecraftServer server, ServerPlayer player) {
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
            destY = overworld.getHeight(Heightmap.Types.MOTION_TOP, spawn.getX(), spawn.getZ());
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
                SoundEvents.END_PORTAL_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }
}
