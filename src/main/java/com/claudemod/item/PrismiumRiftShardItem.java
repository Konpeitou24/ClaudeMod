package com.claudemod.item;

import com.claudemod.dimension.ModDimensions;
import com.claudemod.teleport.PrismiumTeleportHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Session 14: Prismium Rift Shard - the mod's first (and, for a long time,
 * only) way in and out of the Prism Realm dimension (see
 * {@link ModDimensions} and PROGRESS.md section 1, item 3).
 *
 * <p><b>Deliberate minimal-implementation choice</b>: a "real" portal
 * (multi-block frame detection, activation item, a custom block that
 * teleports on contact) was flagged from session 14 onward as the
 * eventual goal, but was judged too large a surface area for a single
 * untested session at the time. Instead this item is a reusable,
 * non-consumed item that teleports the holder directly between the
 * Overworld (or whichever dimension they used it from) and a single
 * fixed anchor point at (0, ~surface, 0) in the Prism Realm.
 *
 * <p><b>Session 52 update</b>: the "real portal" alternative now exists -
 * see {@link com.claudemod.block.PrismiumPortalBlock}, ignited by
 * right-clicking a Prismium Core frame with a Prismium Shard (see
 * {@link com.claudemod.event.PrismiumPortalIgniteHandler}) - directly
 * answering GitHub issue #9's request for "a way to reach the Prismium
 * dimension" that doesn't require already owning/crafting this
 * particular item first. This item is kept exactly as-is rather than
 * removed: it's still useful as a portable, no-frame-required backup
 * (e.g. for returning home from deep underground), and the two travel
 * methods now share their actual teleport mechanics via
 * {@link PrismiumTeleportHelper} instead of each having their own copy.
 *
 * <p>Round-trip position memory: right before leaving to the Prism Realm,
 * the player's current dimension + exact position/rotation is saved into
 * this player's {@link Player#getPersistentData()} tag (a general-purpose
 * per-player NBT compound that survives dimension changes and logout).
 * Using the shard again while inside the Prism Realm reads that tag back
 * and returns the player to the exact spot (and original dimension) they
 * left from. If no saved position exists (e.g. player was placed in the
 * realm by other means), it falls back to the Overworld's shared spawn
 * point. See {@link PrismiumTeleportHelper} for the implementation.
 *
 * <p><b>Unverified</b>: this entire item is untested beyond "the code
 * compiles" (no in-game playtest possible in this sandbox - see
 * PROGRESS.md). In particular: whether {@code server.getLevel(PRISM_REALM)}
 * actually resolves to a valid, generating level at runtime; whether the
 * fixed (0, ~surface, 0) anchor point ends up somewhere sane; and whether
 * the persistent-data round trip survives a player logging out and back
 * in between uses.
 */
public class PrismiumRiftShardItem extends Item {

    private static final int COOLDOWN_TICKS = 100;

    public PrismiumRiftShardItem(Item.Properties properties) {
        super(properties);
    }

    // GitHub issues #7 ("no in-game explanation of items") and #9 ("no
    // way to reach the Prismium dimension") - session 38. This item was
    // always the mod's only way in/out of the Prism Realm (see class
    // doc above), but nothing in-game ever said so; a player would have
    // to find it in the creative inventory/JEI and guess. A one-line
    // tooltip is a much smaller fix than the "proper portal" the issue
    // asked for - session 52 added that portal too (see class doc), but
    // this tooltip is kept since the item itself still needs explaining.
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

        boolean teleported = inRealm
                ? PrismiumTeleportHelper.teleportBackFromRealm(server, serverPlayer)
                : PrismiumTeleportHelper.teleportToRealm(server, serverPlayer);

        if (!teleported) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(stack);
    }
}
