package com.claudemod.item;

import com.claudemod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Session 28: Prismium Shield, the mod's first blocking-capable gear and
 * its first entirely new equipment slot item since the Prismium Rift
 * Shard (session 14) - see PROGRESS.md session 27 handoff item 5(c),
 * "新規コンテンツ(ブロック・アイテム・MOB等)の追加を再開する" (five
 * sessions in a row, #23-27, had been GUI-only work).
 *
 * <p>Deliberately does <b>not</b> extend vanilla's {@link
 * net.minecraft.world.item.ShieldItem}. Web search (session 28, query
 * "Forge 1.20.1 custom shield item UseAnim.BLOCK getUseDuration example
 * not extending ShieldItem") confirmed this is a well-known modding
 * shortcut: any plain {@link Item} that overrides {@link
 * #getUseAnimation} to return {@link UseAnim#BLOCK} and starts using
 * itself in {@link #use} gets vanilla's full blocking behavior for free
 * (see {@code LivingEntity#isBlocking}, which only checks {@code
 * getUseItem().getUseAnimation() == UseAnim.BLOCK} - it does not care
 * whether the item is actually a {@code ShieldItem}), including
 * axe-disable and shield-bash knockback, without needing to reproduce
 * vanilla ShieldItem's banner-pattern machinery at all.
 *
 * <p><b>Session 38 update (GitHub issue #6, "盾を手に持ってもアイテムの
 * まま")</b>: the in-hand look this class originally shipped with (a
 * flat 2D icon, since the item model at the time was a plain {@code
 * minecraft:item/generated}) turned out to look wrong enough in real
 * play that the repo owner filed a bug. Investigation found vanilla's
 * own shield does <i>not</i> actually need a {@code
 * BlockEntityWithoutLevelRenderer}/ISTER as this class originally
 * assumed - it gets its 3D look from an ordinary "elements"-based item
 * model JSON (box geometry + UVs, the same mechanism block models use),
 * which is plain data and requires no Java rendering code at all.
 * {@code models/item/prismium_shield.json} was rebuilt on that basis
 * (two boxes: a body panel + a center boss, see that file and its
 * {@code scripts/textures/gen_prismium_shield_base.py} texture
 * generator), and now renders as a real 3D held shape instead of a
 * flat sprite. {@code getUseAnimation}/{@code getUseDuration}/{@code
 * use} below are unaffected by any of this - they only drive the
 * blocking <i>behavior</i>, not the model.
 *
 * <p><b>Session 47 follow-up</b>: the session-38 model file already had
 * an {@code "overrides": [{"predicate": {"blocking": 1}, ...}]} entry
 * pointing at {@code prismium_shield_blocking.json}, but no Java code
 * had ever registered an {@code ItemPropertyFunction} for the
 * "blocking" property id on this item (unlike vanilla, which only wires
 * that id up for {@code Items.SHIELD} specifically - every item needs
 * its own registration, see {@code ClientModEvents#registerScreens},
 * same pattern already used there for Prismium Bow's "pull"/"pulling").
 * That gap is now closed, but {@code prismium_shield_blocking.json} is
 * currently byte-for-byte the same geometry as the resting model, so
 * blocking still looks visually identical to idle - giving the two
 * states genuinely different poses/transforms remains open for a
 * future session.
 *
 * <p>{@code use()} mirrors vanilla {@code ShieldItem#use}'s own body
 * (confirmed against the 1.16.5/1.18.2 ShieldItem javadocs found during
 * the session-28 search - the method shape has been stable across those
 * versions and this mod's other {@code Item#use} overrides already
 * establish the 1.20.1 {@code (Level, Player, InteractionHand) ->
 * InteractionResultHolder<ItemStack>} signature works, see
 * {@link PrismiumGrapplingHookItem#use}): just call
 * {@code player.startUsingItem(hand)} and report success/consume so the
 * client starts playing the raise-shield arm animation immediately.
 *
 * <p><b>Unverified</b>: no in-game playtest yet (no Minecraft client
 * available in this sandbox, see PROGRESS.md's standing note on this).
 * The 3D model's orientation/scale/thickness across first/third
 * person and GUI, and whether the "blocking" override actually flips
 * on screen now that it is registered, have not been visually
 * confirmed.
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
