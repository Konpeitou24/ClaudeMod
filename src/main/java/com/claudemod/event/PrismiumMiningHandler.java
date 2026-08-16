package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Session 5: the first tool-specific gimmick for the Prismium tool set.
 * PROGRESS.md (session 4, section 5 "議論したい論点") flagged that all five
 * Prismium tools were still pure stat upgrades over diamond with no unique
 * ability, unlike the armor set which got a set bonus in session 4. This is
 * a first, deliberately small step for the Pickaxe specifically: mining
 * Prismium Ore or Deepslate Prismium Ore with a Prismium Pickaxe has a
 * {@value #BONUS_SHARD_CHANCE}-in-1 chance to spawn one extra Prismium
 * Shard alongside the block's normal loot-table drops.
 *
 * <p><b>Revision history / a build-failure lesson learned this session</b>:
 * the first draft of this handler subscribed to
 * {@code BlockEvent.HarvestDropsEvent} and mutated {@code event.getDrops()}.
 * That draft passed local review but <i>failed the real GitHub Actions
 * build</i> (Run 11) - the first confirmed real compile failure caught via
 * the CI pipeline described in PROGRESS.md section 2-4. Follow-up research
 * this same session strongly suggests {@code HarvestDropsEvent} was
 * replaced by {@code BlockEvent.GenerateLootEvent} /
 * {@code BlockEvent.DropLootEvent} starting around Minecraft 1.15 (Forge PR
 * #5871, "Replace HarvestDropsEvent with GenerateLootEvent and
 * DropLootEvent"), so it likely no longer exists as a class in Forge
 * 1.20.1/47.4.0, which would explain a compile error. Rather than gamble on
 * unverified exact signatures for the newer loot-event pair (which carry a
 * {@code LootContext} and are non-trivial to construct/inspect correctly),
 * this rewrite sidesteps the whole loot-table event pipeline: it hooks
 * {@link BlockEvent.BreakEvent} instead (fired server-side when a player
 * breaks a block - confirmed to still live under
 * {@code net.minecraftforge.event.level.BlockEvent} in 1.20.1-era javadocs
 * found this session) and spawns the bonus shard as an independent
 * {@link ItemEntity} in the world, with zero dependency on however the
 * block's own loot table/drops event pipeline is currently shaped. This is
 * a strictly smaller, more conservative API surface, at the cost of the
 * bonus shard not being affected by things like Fortune (acceptable for a
 * "lucky pickaxe" flavor mechanic).
 *
 * <p><b>Still unverified</b> (this rewrite has not itself been confirmed by
 * a green CI run as of writing): {@link BlockEvent.BreakEvent#getPlayer()},
 * {@code #getState()} and {@code #getPos()} (inherited from
 * {@link BlockEvent}), plus the {@link ItemEntity} 5-arg
 * (level, x, y, z, stack) constructor and {@link Level#addFreshEntity}.
 * These are long-standing, widely-used APIs so the risk is low, but
 * PROGRESS.md should record whether Run 12 (or whatever this push becomes)
 * actually goes green, since that is the only real confirmation available
 * in this sandbox.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumMiningHandler {

    // 1 in 4 chance per ore block mined with the Prismium Pickaxe.
    private static final float BONUS_SHARD_CHANCE = 0.25f;

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
        boolean isPrismiumOre = event.getState().is(ModBlocks.PRISMIUM_ORE.get())
                || event.getState().is(ModBlocks.DEEPSLATE_PRISMIUM_ORE.get());
        if (!isPrismiumOre) {
            return;
        }
        if (player.getMainHandItem().getItem() != ModItems.PRISMIUM_PICKAXE.get()) {
            return;
        }
        if (ThreadLocalRandom.current().nextFloat() >= BONUS_SHARD_CHANCE) {
            return;
        }
        ItemStack bonus = new ItemStack(ModItems.PRISMIUM_SHARD.get(), 1);
        ItemEntity bonusEntity = new ItemEntity(level,
                event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5,
                bonus);
        level.addFreshEntity(bonusEntity);
    }
}
