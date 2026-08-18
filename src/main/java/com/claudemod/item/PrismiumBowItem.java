package com.claudemod.item;

import com.claudemod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Session 29: Prismium Bow, the mod's first ranged weapon and the
 * companion piece to the Prismium Shield (session 28) - see PROGRESS.md
 * session 28 handoff item 6(a), which flagged a bow as the natural next
 * "new content" addition because extending vanilla's {@link BowItem}
 * reuses the entire draw/release/ammo-selection pipeline for free
 * (unlike {@link PrismiumShieldItem}, which deliberately avoided
 * extending {@code ShieldItem} to dodge its banner-layer render code -
 * {@code BowItem} carries no such baggage, so this class extends it
 * directly).
 *
 * <p><b>Gimmick</b>: every arrow fired gets {@code pierceLevel = 1} via
 * {@link #customArrow}, vanilla's documented, intended extension point
 * for exactly this kind of per-shot arrow customization (confirmed via
 * the 1.19.3 Forge javadoc, session 29 web search - the method is
 * {@code public AbstractArrow customArrow(AbstractArrow)}, called by
 * {@code BowItem#releaseUsing} right after the arrow entity is created
 * and before it is added to the level). This is a genuine mechanical
 * differentiator rather than a reskinned vanilla bow: the vanilla
 * Piercing enchantment can only be applied to crossbows (bows are not in
 * its applicable-items set), so "a bow whose arrows always pierce one
 * extra target" is not otherwise obtainable in vanilla survival. No new
 * event listener or NBT tagging was needed - the whole gimmick lives in
 * this one overridden hook.
 *
 * <p>Session 30 added a {@link #isValidRepairItem} override accepting
 * Prismium Shard, along with matching overrides on {@link
 * PrismiumGrapplingHookItem} and {@link PrismiumShieldItem} - resolving
 * the open question from PROGRESS.md session 28 §4-42 about unifying
 * Prismium gear repair across all three of the mod's non-Tier/
 * non-ArmorMaterial durability items.
 *
 * <p><b>Unverified</b> (no in-game client available in this sandbox, see
 * PROGRESS.md's standing note): whether the bow actually draws/fires
 * correctly, whether the pulling/pull item-model overrides render the
 * right frame at the right draw progress (see the model overrides list
 * in {@code prismium_bow.json} and the {@code ItemProperties}
 * registration in {@code ClientModEvents}), and above all whether
 * {@code setPierceLevel(1)} really lets a bow-fired arrow pass through
 * an extra target in practice (crossbows are the only vanilla precedent
 * and this mod has never fired a projectile through {@code customArrow}
 * before).
 */
public class PrismiumBowItem extends BowItem {

    public PrismiumBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        arrow.setPierceLevel((byte) 1);
        return arrow;
    }

    // Session 30: see class javadoc above - unifies repair material across
    // the mod's non-Tier/non-ArmorMaterial durability items.
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.PRISMIUM_SHARD.get());
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
