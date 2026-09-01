package com.claudemod.network;

import com.claudemod.ClaudeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * This mod's first custom network channel (session, scheduled - issue #17
 * follow-up, see {@link com.claudemod.client.overlay.FeatherstoneReductionOverlay}
 * for the feature this exists to support). Every previous client-facing
 * effect in this repo (tooltips, action-bar text via {@code
 * Player#displayClientMessage}, GUI menu sync via vanilla's own {@code
 * ContainerData}/{@code ItemStackHandler} machinery) rode on an existing
 * vanilla or Forge-menu mechanism, so this is the first time a bespoke
 * server-to-client payload is actually needed: a HUD overlay that must
 * react to a purely server-side calculation ({@code LivingFallEvent}'s
 * damage multiplier) with no vanilla packet already carrying that
 * information to the client.
 *
 * <p><b>API confirmed this session</b> against Forge's own 1.20.x-branch
 * documentation (docs.minecraftforge.net/en/1.20.x/networking/simpleimpl/,
 * reachable from this sandbox): {@link NetworkRegistry#newSimpleChannel}
 * takes a channel name plus three version-negotiation callbacks, and this
 * mod does not need to support being loaded by only one side (it has never
 * been sided-optional), so per the docs' own "Getting Started" example the
 * simplest exact-match predicate pair ({@code PROTOCOL_VERSION::equals} for
 * both) is used rather than {@link NetworkRegistry#acceptMissingOr}, which
 * exists for mods that must stay compatible with the other side lacking
 * this channel entirely.
 *
 * <p>Registration happens eagerly in this class's static initializer (the
 * same "just reference the class" pattern the docs use), and {@link
 * #init()} exists purely so {@code ClaudeMod}'s constructor has an explicit,
 * visible call site - mirroring this mod's existing convention of listing
 * every subsystem it wires up in one place - even though calling it only
 * forces the class to load and does no additional work itself.
 */
public final class ClaudeModNetwork {

    private ClaudeModNetwork() {
    }

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ClaudeMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    static {
        CHANNEL.registerMessage(nextId++,
                FeatherstoneReductionMessage.class,
                FeatherstoneReductionMessage::encode,
                FeatherstoneReductionMessage::decode,
                FeatherstoneReductionMessage::handle);
    }

    /** Forces this class (and therefore the static block above) to load
     * from {@code ClaudeMod}'s constructor. See class javadoc. */
    public static void init() {
        // Intentionally empty.
    }
}
