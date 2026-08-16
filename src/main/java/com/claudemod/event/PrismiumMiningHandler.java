package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * {@value #BONUS_SHARD_CHANCE}-in-1 chance to drop one extra Prismium
 * Shard on top of whatever the block's own loot table already rolled
 * (including any Fortune bonus - this handler runs after loot table drops
 * are computed, so it stacks additively rather than replacing anything).
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>Uses {@link net.minecraftforge.event.level.BlockEvent.HarvestDropsEvent},
 *   confirmed to live under the {@code net.minecraftforge.event.level}
 *   package (not the older {@code net.minecraftforge.event.world}) for
 *   Forge 1.20.x by checking the import list of MinecraftForge's own
 *   {@code ForgeEventFactory.java} on the {@code 1.20.x} branch during this
 *   session.</li>
 *   <li>Deliberately uses {@link ThreadLocalRandom} instead of a
 *   Minecraft-specific {@code RandomSource} to keep this event handler's
 *   API surface entirely within plain Java, since the exact accessor for a
 *   world-seeded random from inside this event was not worth the extra
 *   verification risk for a cosmetic drop-chance roll (i.e. this roll is
 *   not deterministic/seed-based, unlike vanilla loot tables).</li>
 *   <li>Guards on {@code harvester.level().isClientSide} the same way
 *   {@link ArmorSetBonusHandler} now does, for consistency, even though
 *   drop resolution is expected to be server-authoritative already.</li>
 * </ul>
 *
 * <p><b>Unverified</b>: {@link BlockEvent.HarvestDropsEvent#getState()},
 * {@code #getHarvester()} and {@code #getDrops()} were cross-checked against
 * publicly documented usage examples of this event (the shape has been
 * stable across many Forge versions), but this exact handler has not been
 * compiled or playtested in this sandbox. If it turns out
 * {@code getHarvester()} is null more often than expected (e.g. certain
 * automated mining tools from other mods), the bonus would simply never
 * trigger for those cases rather than error - low risk either way.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumMiningHandler {

    // 1 in 4 chance per ore block mined with the Prismium Pickaxe.
    private static final float BONUS_SHARD_CHANCE = 0.25f;

    @SubscribeEvent
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        Player harvester = event.getHarvester();
        if (harvester == null || harvester.level().isClientSide) {
            return;
        }
        boolean isPrismiumOre = event.getState().is(ModBlocks.PRISMIUM_ORE.get())
                || event.getState().is(ModBlocks.DEEPSLATE_PRISMIUM_ORE.get());
        if (!isPrismiumOre) {
            return;
        }
        if (harvester.getMainHandItem().getItem() != ModItems.PRISMIUM_PICKAXE.get()) {
            return;
        }
        if (ThreadLocalRandom.current().nextFloat() < BONUS_SHARD_CHANCE) {
            event.getDrops().add(new ItemStack(ModItems.PRISMIUM_SHARD.get(), 1));
        }
    }
}
