package com.claudemod.item;

import com.claudemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Session 16: Prismium Locator - the mod's second accessory-style item
 * (after the session 7 grappling hook) and the first "detection item" from
 * the long-standing roadmap wishlist (PROGRESS.md section 1, item 5:
 * "探索を楽しくするアクセサリ的アイテム(グラップリングフック、探知アイテム
 * など)" - the detection half had been untouched since session 1).
 *
 * <p>Right-click scans a cube of blocks centered on the player (see
 * {@link #SEARCH_RADIUS}) for the nearest Prismium Ore or Deepslate
 * Prismium Ore block, then reports its compass direction, rough vertical
 * position (above/below/same level) and straight-line distance via an
 * action-bar message - deliberately not a rendered compass-needle item
 * (that would need a custom {@code ItemPropertyFunction} + item model
 * predicate JSON, a much larger and harder-to-verify surface for a single
 * untested session, mirroring the "reuse a lower-risk mechanism" choices
 * already made for the grappling hook (no flying hook entity) and the
 * Prismium Rift Shard (no portal frame)). Not consumed and does not take
 * durability damage on use - unlike the grappling hook, nothing about
 * "detecting ore" should plausibly wear the item out, so instead only a
 * short cooldown (see {@link #COOLDOWN_TICKS}) guards against spamming the
 * block scan every tick.
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>The scan is a brute-force triple loop over a
 *   {@code (2*SEARCH_RADIUS+1)^3} cube (currently 41^3 = 68,921 positions
 *   at radius 20) calling {@code Level#getBlockState}. This is only ever
 *   run once per use (not per tick), so the cost is a one-off per
 *   right-click rather than a recurring tick burden - acceptable even
 *   though it is not spatially indexed the way a real "ore vein detector"
 *   mod might do it (e.g. via chunk-section palette scanning). If a
 *   future session wants a bigger radius, that indexed approach would be
 *   the place to start.</li>
 *   <li>Y bounds are clamped with {@code Level#getMinBuildHeight()} /
 *   {@code getMaxBuildHeight()} (stable {@code LevelHeightAccessor} API,
 *   present across many MC versions) to avoid querying outside the
 *   world's build limits near the top/bottom of the world.</li>
 *   <li>Compass direction is computed from the horizontal (X/Z) offset
 *   using a standard bearing formula (north = -Z, east = +X), bucketed
 *   into 8 directions. Vertical hint uses a small dead zone (|dy| &lt; 4)
 *   before calling it "above"/"below" rather than "same level", so minor
 *   height differences near the surface don't produce noisy readouts.</li>
 * </ul>
 *
 * <p><b>Unverified</b>: no in-game playtest yet (no Minecraft client
 * available in this sandbox - see PROGRESS.md). The search radius (20),
 * cooldown (60 ticks / 3s) and the above/below dead zone (4 blocks) are
 * first-guess balance numbers. Whether a single brute-force scan at this
 * radius causes a noticeable hitch on the server thread when used
 * repeatedly has not been measured.
 */
public class PrismiumLocatorItem extends Item {

    private static final int SEARCH_RADIUS = 20;
    private static final int COOLDOWN_TICKS = 60;
    private static final int VERTICAL_DEAD_ZONE = 4;

    public PrismiumLocatorItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            // Actual scan only runs server-side; client just plays the use
            // animation and waits for the action-bar message to arrive.
            return InteractionResultHolder.success(stack);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));

        BlockPos nearest = findNearestPrismiumOre(level, player.blockPosition());

        if (nearest == null) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
            player.displayClientMessage(
                    Component.translatable("message.claudemod.prismium_locator.none"), true);
            return InteractionResultHolder.success(stack);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8F, 1.2F);

        BlockPos origin = player.blockPosition();
        int dx = nearest.getX() - origin.getX();
        int dy = nearest.getY() - origin.getY();
        int dz = nearest.getZ() - origin.getZ();
        int distance = (int) Math.round(Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz));

        Component direction = compassDirection(dx, dz);
        Component vertical = verticalHint(dy);

        player.displayClientMessage(
                Component.translatable("message.claudemod.prismium_locator.found",
                        direction, distance, vertical), true);

        return InteractionResultHolder.success(stack);
    }

    private static BlockPos findNearestPrismiumOre(Level level, BlockPos origin) {
        Block oreBlock = ModBlocks.PRISMIUM_ORE.get();
        Block deepslateOreBlock = ModBlocks.DEEPSLATE_PRISMIUM_ORE.get();

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        long nearestDistSq = Long.MAX_VALUE;

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            int x = origin.getX() + dx;
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                int z = origin.getZ() + dz;
                for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                    int y = origin.getY() + dy;
                    if (y < minY || y > maxY) {
                        continue;
                    }
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(oreBlock) || state.is(deepslateOreBlock)) {
                        long distSq = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearest = cursor.immutable();
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private static Component compassDirection(int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return Component.translatable("direction.claudemod.here");
        }
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) {
            angle += 360;
        }
        String key;
        if (angle >= 337.5 || angle < 22.5) {
            key = "north";
        } else if (angle < 67.5) {
            key = "northeast";
        } else if (angle < 112.5) {
            key = "east";
        } else if (angle < 157.5) {
            key = "southeast";
        } else if (angle < 202.5) {
            key = "south";
        } else if (angle < 247.5) {
            key = "southwest";
        } else if (angle < 292.5) {
            key = "west";
        } else {
            key = "northwest";
        }
        return Component.translatable("direction.claudemod." + key);
    }

    private static Component verticalHint(int dy) {
        if (dy <= -VERTICAL_DEAD_ZONE) {
            return Component.translatable("direction.claudemod.below");
        }
        if (dy >= VERTICAL_DEAD_ZONE) {
            return Component.translatable("direction.claudemod.above");
        }
        return Component.translatable("direction.claudemod.level");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // GitHub issue #7 (scheduled session): same one-line gray hint
        // pattern as the other Prismium accessories/energy blocks.
        tooltip.add(Component.translatable(this.getDescriptionId() + ".usage")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
