package com.claudemod.network;

import com.claudemod.client.overlay.FeatherstoneReductionOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-client payload for issue #17's "HP表示に何か工夫を入れて"
 * (add some improvement to the HP display) request - see {@link
 * com.claudemod.event.PrismiumFeatherstoneHandler#announceReduction} for
 * where this is sent, and {@link FeatherstoneReductionOverlay} for what the
 * client does with it. Carries only the fixed reduction percentage (always
 * {@code 75} today, but sent as data rather than hardcoded on the client so
 * a future session can vary it - e.g. by charm tier - without touching this
 * packet's shape).
 *
 * <p>Encode/decode via {@code writeVarInt}/{@code readVarInt} rather than a
 * plain byte or int, matching {@code FriendlyByteBuf}'s own documented
 * preference for variable-length encoding of small non-negative numbers
 * (confirmed against Forge's SimpleImpl docs example, which encodes its own
 * demo payload the same way).
 */
public final class FeatherstoneReductionMessage {

    private final int reductionPercent;

    public FeatherstoneReductionMessage(int reductionPercent) {
        this.reductionPercent = reductionPercent;
    }

    public static void encode(FeatherstoneReductionMessage message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.reductionPercent);
    }

    public static FeatherstoneReductionMessage decode(FriendlyByteBuf buf) {
        return new FeatherstoneReductionMessage(buf.readVarInt());
    }

    /**
     * Runs on the network thread (per SimpleImpl's own warning that packet
     * handlers are not on the main thread by default), so the actual HUD
     * state update is deferred via {@code enqueueWork} and, since it
     * touches client-only classes ({@code GuiGraphics}/{@code Minecraft}
     * transitively via {@link FeatherstoneReductionOverlay}), wrapped in
     * {@link DistExecutor#unsafeRunWhenOn} exactly as the Forge docs'
     * "Handling Packets" section shows for server-to-client messages - this
     * guards against ever loading {@link FeatherstoneReductionOverlay}'s
     * client-only call graph on a dedicated server.
     */
    public static void handle(FeatherstoneReductionMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FeatherstoneReductionOverlay.trigger(message.reductionPercent)));
        ctx.get().setPacketHandled(true);
    }
}
