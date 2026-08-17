package com.claudemod.item;

import com.claudemod.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Session 28: Prismium Shield, the mod's first blocking-capable gear and
 * its first entirely new equipment slot item since the Prismium Rift
 * Shard (session 14) - see PROGRESS.md session 27 handoff item 5(c),
 * "新規コンテンツ(ブロック・アイテム・MOB等)の追加を再開する" (five
 * sessions in a row, #23-27, had been GUI-only work).
 *
 * <p>Deliberately does <b>not</b> extend vanilla's {@link
 * net.minecraft.world.item.ShieldItem}. That class hard-codes an
 * in-hand 3D model built from banner-pattern layers baked onto
 * {@code textures/entity/shield_base_nopattern.png}/{@code
 * shield_base.png} via a dedicated {@code BlockEntityWithoutLevelRenderer}
 * - reproducing that correctly would require a custom ISTER and a very
 * specific texture layout, an unrelated and much larger yak-shave than
 * "add a working shield". Web search (session 28, query "Forge 1.20.1
 * custom shield item UseAnim.BLOCK getUseDuration example not extending
 * ShieldItem") confirmed this is a well-known modding shortcut: any
 * plain {@link Item} that overrides {@link #getUseAnimation} to return
 * {@link UseAnim#BLOCK} and starts using itself in {@link #use} gets
 * vanilla's full blocking behavior for free (see
 * {@code LivingEntity#isBlocking}, which only checks
 * {@code getUseItem().getUseAnimation() == UseAnim.BLOCK} - it does not
 * care whether the item is actually a {@code ShieldItem}), including
 * axe-disable and shield-bash knockback, at the cost of rendering as a
 * flat 2D icon in-hand instead of a 3D banner-capable model - an
 * acceptable trade for a first pass (same "function over full 3D
 * polish" call already made for {@link PrismiumGrapplingHookItem} and
 * {@link PrismiumLocatorItem}, neither of which has a custom in-hand
 * model either).
 *
 * <p>{@code use()} mirrors vanilla {@code ShieldItem#use}'s own body
 * (confirmed against the 1.16.5/1.18.2 ShieldItem javadocs found during
 * the same search - the method shape has been stable across those
 * versions and this mod's other {@code Item#use} overrides already
 * establish the 1.20.1 {@code (Level, Player, InteractionHand) ->
 * InteractionResultHolder<ItemStack>} signature works, see
 * {@link PrismiumGrapplingHookItem#use}): just call
 * {@code player.startUsingItem(hand)} and report success/consume so the
 * client starts playing the raise-shield arm animation immediately.
 *
 * <p><b>Unverified</b>: no in-game playtest yet (no Minecraft client
 * available in this sandbox, see PROGRESS.md's standing note on this).
 * In particular the flat-icon in-hand rendering (no 3D model/ISTER) is
 * untested - it should render fine as a generic held-item sprite like
 * any other non-block item, but has not been visually confirmed. If a
 * future session wants a true 3D in-hand shield look, that would mean
 * either extending {@code ShieldItem} after all and supplying the
 * banner-layout texture it expects, or writing a custom
 * {@code BlockEntityWithoutLevelRenderer}/ISTER - out of scope here.
 */
public class PrismiumShieldItem extends Item {

    public PrismiumShieldItem(Item.Properties properties) {
        super(properties);
    }

    // Session 30: accept Prismium Shard as a repair material, closing the
    // gap called out in this class's own javadoc (and PROGRESS.md session
    // 28 §4-42) - see PrismiumGrapplingHookItem#isValidRepairItem for the
    // same change applied across all three of the mod's non-Tier/
    // non-ArmorMaterial durability items in this session.
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.PRISMIUM_SHARD.get());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // Same 72000-tick (effectively "until released") duration vanilla's
        // own ShieldItem uses.
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
