package com.claudemod.item;

import com.claudemod.dimension.ModDimensions;
import com.claudemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.material.FluidState;

import java.util.Set;

import javax.annotation.Nullable;

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

    // GitHub issues #7 ("no in-game explanation of items") and #9 ("no
    // way to reach the Prismium dimension") - session 38. This item was
    // always the mod's only way in/out of the Prism Realm (see class
    // doc above), but nothing in-game ever said so; a player would have
    // to find it in the creative inventory/JEI and guess. A one-line
    // tooltip is a much smaller fix than the "proper portal" the issue
    // asks for (see PROGRESS.md handoff for that larger, still-open
    // idea), but directly closes the "I have no idea how to get there"
    // gap for anyone who already has (or can craft) the item.
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(this.getDescriptionId() + ".usage")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
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

        int landingY = findSafeRealmLanding(realmLevel, REALM_ANCHOR.getX(), REALM_ANCHOR.getZ());
        double destX = REALM_ANCHOR.getX() + 0.5;
        double destZ = REALM_ANCHOR.getZ() + 0.5;

        player.teleportTo(realmLevel, destX, landingY, destZ, Set.of(), player.getYRot(), player.getXRot());
        realmLevel.playSound(null, destX, landingY, destZ,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /**
     * Session 47 fix (repo owner report, interactive session): the shard
     * used to land players inside solid terrain or below bedrock in the
     * Prism Realm. The dimension's chunk generator has separately been
     * rewritten this session to a flat "waterworld" (see
     * data/claudemod/dimension/prism_realm.json and PROGRESS.md) rather
     * than reusing overworld noise settings, which removes most of the
     * uncertainty this method used to have about what terrain shape it
     * might land on - but two defensive measures are added here on top of
     * that, since the exact previous failure mode was never confirmed:
     * <ol>
     *   <li>Force the destination chunk to fully generate before reading
     *   its heightmap ({@link ServerLevel#getChunk(int, int)} with no
     *   extra arguments defaults to requiring {@code ChunkStatus.FULL}).
     *   A heightmap read against a not-yet-generated/not-yet-loaded chunk
     *   is the most likely explanation for the old bug (an uninitialised
     *   or default heightmap value near the world's bottom would read as
     *   "solid ground" far below where any real terrain is).</li>
     *   <li>Clamp against anything at or below {@code minBuildHeight()}
     *   (the world floor / bedrock level) as clearly invalid, falling back
     *   to a known-safe constant tied to the current flat layout's water
     *   surface (see the dimension json's layer heights) rather than
     *   trusting a bogus value.</li>
     * </ol>
     * Per the user's own suggestion: if the landing column's surface turns
     * out to be liquid (true almost everywhere right now, since the flat
     * generator makes most of the dimension an open sea until biomes/land
     * are added in a future session), a 9x1x9 {@code prismium_soil}
     * platform is carved into the water's surface first so the player
     * always lands on solid ground rather than treading water.
     */
    private static final int REALM_FALLBACK_SURFACE_Y = 65;

    private int findSafeRealmLanding(ServerLevel realmLevel, int x, int z) {
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
            // Session 47: same "force the chunk to actually generate before
            // trusting its heightmap" defensive fix as findSafeRealmLanding.
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
