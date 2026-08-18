package com.claudemod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Prismium Rift Anchor (session 48): the mod's first Rift Shard-family
 * derivative item, addressing PROGRESS.md section 5 item 12(a)(i) - a
 * repo-owner request for "an item that lets you set a respawn point like
 * a bed, but not tied to a specific block". A vanilla bed only works in
 * the Overworld and a Respawn Anchor only works (without exploding) in
 * the Nether, so neither can anchor a respawn point in the Prism Realm
 * (a custom dimension whose {@code dimension_type} json - see
 * ModDimensions - does not necessarily set {@code bed_works}/
 * {@code respawn_anchor_works} for it, and was not designed around
 * either vanilla mechanic). This item sidesteps that entirely: it calls
 * {@link ServerPlayer#setRespawnPosition(net.minecraft.resources.ResourceKey,
 * net.minecraft.core.BlockPos, float, boolean, boolean)} directly with
 * {@code forced=true} (same flag Respawn Anchor uses), which sets a raw
 * coordinate spawn point that does not require any block to remain at
 * that location afterwards and works in any dimension - the player can
 * use this standing on open ground, on a boat, anywhere.
 *
 * <p>Single-use by design (consumed on successful use, {@link
 * ItemStack#shrink(int)}) rather than a reusable "key" item like {@link
 * PrismiumRiftShardItem} - setting a respawn point is a rarer, more
 * deliberate action than teleporting back and forth, and single-use
 * fits the "scroll/charge" framing better than an infinitely-reusable
 * tool. {@code sendMessage=true} is passed through to {@code
 * setRespawnPosition} so vanilla's own "Respawn point set" toast fires
 * automatically - no custom message needed.
 *
 * <p>API note (verified this session via WebSearch against the 1.19.3
 * Forge-mapped javadoc, the closest version mirror reachable from this
 * sandbox - see PROGRESS.md section 2-9 for why {@code api.github.com}/
 * Maven hosts are unreachable but general doc sites are not): {@code
 * ServerPlayer#setRespawnPosition} is a 5-argument method taking
 * {@code (ResourceKey<Level> dimension, BlockPos pos, float angle,
 * boolean forced, boolean sendMessage)} in 1.20.1 - unchanged from
 * 1.19.3's signature. {@code SoundEvents.RESPAWN_ANCHOR_SET_SPAWN}
 * (the sound vanilla's own RespawnAnchorBlock plays on a successful
 * charge/spawn-set) was cross-checked the same way.
 *
 * <p><b>Unverified</b>: like every item in this mod, untested beyond
 * "compiles against the API as documented" - no local build/game client
 * in this sandbox (see PROGRESS.md). In particular, whether {@code
 * forced=true} actually produces a safe landing spot on next respawn in
 * the Prism Realm's current flat waterworld terrain (mostly open water,
 * see PROGRESS.md section 5 item 11) has not been checked - vanilla's
 * own respawn-safety search may or may not find dry ground nearby.
 */
public class PrismiumRiftAnchorItem extends Item {

    public PrismiumRiftAnchorItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(this.getDescriptionId() + ".usage")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // Actual respawn-position logic only makes sense server-side;
            // let the client just play the use animation.
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        serverPlayer.setRespawnPosition(serverPlayer.level().dimension(), serverPlayer.blockPosition(),
                serverPlayer.getYRot(), true, true);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.awardStat(Stats.ITEM_USED.get(this));

        stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
