package com.claudemod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Session 7: Prismium Grappling Hook - the mod's first accessory-style item
 * (roadmap PROGRESS.md section 1, item 5 - "探索を楽しくするアクセサリ的
 * アイテム(グラップリングフック等)" had been on the wishlist since session 1
 * and untouched until now).
 *
 * <p>Right-click raycasts along the player's look vector; if it hits a
 * block within range, the player's velocity is set directly toward the hit
 * point, pulling them there. Unlike a fishing rod there is no separate
 * flying hook entity - the raycast and pull both happen instantly inside
 * {@link #use}, which keeps this a pure {@code Item} (no new Entity/render
 * layer/network payload to get wrong). This mirrors the *approach* vanilla
 * uses in {@code FishingHook#pullEntity} (directly call
 * {@code setDeltaMovement} then flag {@code hurtMarked}) without reusing
 * any of the fishing-specific entity machinery itself.
 *
 * <p>API notes verified this session (web search, since this is the mod's
 * first use of all three):
 * <ul>
 *   <li>{@code Level#clip(ClipContext)} + {@code ClipContext(Vec3, Vec3,
 *   ClipContext.Block, ClipContext.Fluid, Entity)} - standard block
 *   raytrace, safe to call on both logical sides.</li>
 *   <li>{@code Entity#hurtMarked} - public boolean flag vanilla uses (e.g.
 *   fishing rod reel-in, knockback) to mark that this entity's velocity
 *   changed server-side and needs syncing to tracked clients.</li>
 *   <li>{@code ItemStack#hurtAndBreak(int, LivingEntity, Consumer
 *   <LivingEntity>)} paired with {@code LivingEntity#broadcastBreakEvent
 *   (InteractionHand)} - confirmed against vanilla's shield-breaking code
 *   path, which uses this exact hand-based overload (as opposed to the
 *   {@code EquipmentSlot} overload used by armor/mainhand-tool breakage in
 *   {@code DiggerItem}).</li>
 * </ul>
 *
 * <p><b>Unverified</b>: no in-game playtest yet (no Minecraft client
 * available in this sandbox - see PROGRESS.md). Pull strength, cooldown
 * length and max reach are first-guess balance numbers and may feel too
 * strong/weak/floaty in practice; likely candidates to tune once someone
 * can actually play with it.
 */
public class PrismiumGrapplingHookItem extends Item {

    private static final double MAX_REACH = 24.0D;
    private static final double MIN_PULL_DISTANCE = 1.5D;
    private static final double PULL_SPEED = 1.35D;
    private static final int COOLDOWN_TICKS = 25;

    public PrismiumGrapplingHookItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.x * MAX_REACH, lookVec.y * MAX_REACH, lookVec.z * MAX_REACH);

        ClipContext clipContext = new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = level.clip(clipContext);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 target = hit.getLocation();
        Vec3 toTarget = target.subtract(player.position());
        double distance = toTarget.length();

        if (distance < MIN_PULL_DISTANCE) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 motion = toTarget.normalize().scale(PULL_SPEED);
        player.setDeltaMovement(motion);
        player.hurtMarked = true;

        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                    SoundSource.PLAYERS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
