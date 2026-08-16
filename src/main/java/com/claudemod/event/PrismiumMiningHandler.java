package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Block-break-triggered gimmicks for the Prismium tool set.
 *
 * <p>Session 5 introduced the pattern here (originally Pickaxe-only, see
 * the revision history below for the {@code HarvestDropsEvent} ->
 * {@code BlockEvent.BreakEvent} lesson learned that session). Session 6
 * reuses the same proven, low-risk pattern - subscribe to
 * {@link BlockEvent.BreakEvent}, check the tool and the broken block, and
 * on success spawn a bonus {@link ItemEntity} independent of the block's
 * own loot table - to give the Axe and Shovel their first gimmicks too, so
 * every "digging/breaking" tool in the set (Pickaxe, Axe, Shovel) now has
 * one. The Hoe (an interact-based gimmick, see
 * {@link PrismiumHoeHandler}) and Sword (an on-hit gimmick, see
 * {@link PrismiumSwordHandler}) needed different event types entirely and
 * so live in their own classes; see PROGRESS.md session 6 notes for why
 * every tool now has exactly one gimmick apiece.
 *
 * <p><b>Revision history (session 5, kept for context)</b>: the first
 * draft of the Pickaxe gimmick subscribed to
 * {@code BlockEvent.HarvestDropsEvent} and mutated {@code event.getDrops()}.
 * That draft passed local review but <i>failed the real GitHub Actions
 * build</i> (Run 11) - {@code HarvestDropsEvent} was replaced by
 * {@code BlockEvent.GenerateLootEvent}/{@code DropLootEvent} around
 * Minecraft 1.15 and no longer exists in Forge 1.20.1. The fix (and the
 * pattern every gimmick below now follows) was to hook
 * {@link BlockEvent.BreakEvent} instead and spawn bonus items as
 * independent {@link ItemEntity}s, with zero dependency on the block's own
 * loot-table event pipeline.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumMiningHandler {

    // Pickaxe (session 5): mining Prismium Ore with the Prismium Pickaxe
    // has a 1-in-4 chance of an extra Prismium Shard.
    private static final float BONUS_SHARD_CHANCE = 0.25f;

    // Axe (session 6): felling any log block with the Prismium Axe has a
    // 1-in-5 chance of an extra copy of that exact log (a "lumberjack"
    // perk - encourages using the axe on trees rather than just as a combat
    // stat stick).
    private static final float BONUS_LOG_CHANCE = 0.20f;

    // Shovel (session 6): digging Gravel with the Prismium Shovel has a
    // 50/50 chance of an extra Flint, on top of whatever the block's normal
    // loot table already rolls (a "prospector" perk - flavor text: the
    // shovel is good at sifting gravel for flint). Deliberately not 100%:
    // a guaranteed bonus felt too strong for a resource this common.
    private static final float BONUS_FLINT_CHANCE = 0.5f;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        BlockState state = event.getState();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.getItem() == ModItems.PRISMIUM_PICKAXE.get()) {
            boolean isPrismiumOre = state.is(ModBlocks.PRISMIUM_ORE.get())
                    || state.is(ModBlocks.DEEPSLATE_PRISMIUM_ORE.get());
            if (isPrismiumOre && rollSuccess(BONUS_SHARD_CHANCE)) {
                spawnBonus(level, event.getPos(), new ItemStack(ModItems.PRISMIUM_SHARD.get(), 1));
            }
            return;
        }

        if (heldItem.getItem() == ModItems.PRISMIUM_AXE.get()) {
            if (state.is(BlockTags.LOGS) && rollSuccess(BONUS_LOG_CHANCE)) {
                // The block itself, as an item, is the matching log/stem
                // item (e.g. minecraft:oak_log -> minecraft:oak_log item).
                spawnBonus(level, event.getPos(), new ItemStack(state.getBlock(), 1));
            }
            return;
        }

        if (heldItem.getItem() == ModItems.PRISMIUM_SHOVEL.get()) {
            if (state.is(Blocks.GRAVEL) && rollSuccess(BONUS_FLINT_CHANCE)) {
                spawnBonus(level, event.getPos(), new ItemStack(Items.FLINT, 1));
            }
        }
    }

    private static boolean rollSuccess(float chance) {
        return ThreadLocalRandom.current().nextFloat() < chance;
    }

    private static void spawnBonus(Level level, net.minecraft.core.BlockPos pos, ItemStack stack) {
        ItemEntity bonusEntity = new ItemEntity(level,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                stack);
        level.addFreshEntity(bonusEntity);
    }
}
