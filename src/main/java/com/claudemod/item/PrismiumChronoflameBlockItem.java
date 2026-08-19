package com.claudemod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Session 49: {@link BlockItem} subclass for Prismium Chronoflame (see
 * {@link com.claudemod.block.PrismiumChronoflameBlock}), following the
 * exact same "custom BlockItem purely to add a tooltip line" pattern
 * {@link EnergyStorageBlockItem} (session 11) and {@link
 * PrismiumRiftAnchorItem}'s {@code appendHoverText} (session 48)
 * established, rather than inventing a new mechanism. The tooltip exists
 * specifically to surface this block's one unusual, easy-to-miss
 * behaviour up front (no drop on break) before a player commits to
 * placing it, since nothing about a plain block silhouette in the
 * inventory would otherwise hint at that.
 */
public class PrismiumChronoflameBlockItem extends BlockItem {

    public PrismiumChronoflameBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
