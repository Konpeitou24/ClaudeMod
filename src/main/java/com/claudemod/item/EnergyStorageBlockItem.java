package com.claudemod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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
 * Session 11: {@link BlockItem} subclass for the mod's three Prismium
 * Energy machines (Cell, Generator, Cable). Two things bundled together
 * here, both addressing the same gap flagged repeatedly in PROGRESS.md
 * since session 8 ("known issue #13/#15/#18: breaking one of these blocks
 * throws away its stored FE, since nothing copied the block entity's NBT
 * onto the dropped item"):
 *
 * <ol>
 *   <li>Persistence itself is *not* implemented in this class - it is
 *   handled by each block's loot table (see
 *   {@code data/claudemod/loot_tables/blocks/prismium_{cell,generator,cable}.json})
 *   via the vanilla {@code minecraft:copy_nbt} loot function, which copies
 *   the block entity's "Energy" (and, for the Generator, "BurnTime") NBT
 *   keys onto the dropped item's "BlockEntityTag" compound. Vanilla's own
 *   {@link BlockItem#updateCustomBlockEntityTag} then applies that
 *   "BlockEntityTag" back onto the new block entity automatically the
 *   next time the item is placed - this is the same mechanism vanilla
 *   uses for shulker boxes, so no placement-side code was needed here.</li>
 *   <li>This class only adds the *visible* half: a tooltip line so a
 *   player holding a charged Cell/Generator/Cable in their inventory can
 *   actually see "yes, it kept its charge" without needing to place it
 *   down and right-click. Without this, the fix from item 1 would be
 *   real but silent and hard to verify by eye.</li>
 * </ol>
 *
 * <p>API note (this mod's first use, quick self-check rather than a full
 * WebSearch since {@code ItemStack#getTagElement(String)} and
 * {@code Item#appendHoverText(ItemStack, Level, List, TooltipFlag)} are
 * long-stable vanilla APIs referenced directly in {@code BlockItem}'s own
 * {@code updateCustomBlockEntityTag}/{@code getTooltipImage} neighbors):
 * {@code appendHoverText} takes a bare {@code Level} parameter in 1.20.1
 * (this only changes to {@code Item.TooltipContext} in 1.20.6+, outside
 * this mod's target version).
 */
public class EnergyStorageBlockItem extends BlockItem {

    private final int maxEnergy;

    public EnergyStorageBlockItem(Block block, Item.Properties properties, int maxEnergy) {
        super(block, properties);
        this.maxEnergy = maxEnergy;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // GitHub issue #7 (session 38): "no explanation anywhere for how
        // the mod's items/energy blocks are meant to be used". A full
        // in-game guide book/manual (closer to what the report actually
        // asks for, comparing to Create's approach) is a much bigger
        // feature than fits in one session - see PROGRESS.md handoff.
        // As an immediate, low-risk improvement this adds one always-
        // shown, gray usage-hint line per energy block, sourced from a
        // "<block translation key>.usage" lang entry (see en_us.json/
        // ja_jp.json) so every one of this class's six users (Cell,
        // Generator, Cable, Pylon, Restorer, Wardstone) gets a hint for
        // free without needing six separate Item subclasses.
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId(stack)));
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null || !blockEntityTag.contains("Energy")) {
            return;
        }
        int energy = blockEntityTag.getInt("Energy");
        tooltip.add(Component.translatable("tooltip.claudemod.energy_storage", energy, maxEnergy)
                .withStyle(ChatFormatting.AQUA));
        if (blockEntityTag.contains("BurnTime")) {
            int burnTicks = blockEntityTag.getInt("BurnTime");
            if (burnTicks > 0) {
                tooltip.add(Component.translatable("tooltip.claudemod.burn_time", burnTicks / 20)
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
