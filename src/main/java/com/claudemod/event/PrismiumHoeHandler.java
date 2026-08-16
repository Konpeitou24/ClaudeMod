package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Session 6: the Hoe's gimmick, the fourth of the five Prismium tools to
 * get one (see {@link PrismiumMiningHandler} for the Pickaxe/Axe/Shovel
 * gimmicks and {@link PrismiumSwordHandler} for the Sword's).
 *
 * <p>Right-clicking any {@link BonemealableBlock} (crops, saplings, etc.
 * - the same interface vanilla Bone Meal itself checks) with the Prismium
 * Hoe in hand attempts to grow it exactly like using a Bone Meal item,
 * without consuming one. This is a deliberately conservative design: it
 * reuses {@code isValidBonemealTarget}/{@code isBonemealSuccess}/
 * {@code performBonemeal} verbatim, so the odds of a successful growth
 * tick per use are identical to vanilla Bone Meal's own odds (no new
 * balance surface to invent) - the "gimmick" is purely not needing to
 * carry/craft Bone Meal, which fits the Hoe's existing
 * PROGRESS.md-suggested theme ("growth speed bonus"). No cooldown or NBT
 * state is tracked; spamming right-click is equivalent to spamming Bone
 * Meal by hand, which vanilla already allows.
 *
 * <p><b>API verification (this session, via Forge/Mojang mappings, not
 * guessed)</b>: {@link BonemealableBlock#isValidBonemealTarget} takes
 * {@code (LevelReader, BlockPos, BlockState)} - 3 args, no trailing
 * boolean (older 1.18-era docs floating around search results show a
 * different, outdated signature; cross-checked against 1.20.2 mappings to
 * be sure, since PROGRESS.md section 4-8 specifically warns that
 * long-standing APIs tend to surface stale versioned docs in search
 * results). {@code isBonemealSuccess(Level, RandomSource, BlockPos,
 * BlockState)} and {@code performBonemeal(ServerLevel, RandomSource,
 * BlockPos, BlockState)} were confirmed the same way.
 *
 * <p><b>Unverified</b>: like every other gimmick in this mod, this has
 * not been playtested in a running game (no Minecraft client in this
 * sandbox). In particular, whether {@code PlayerInteractEvent.RightClickBlock}
 * fires once or twice per real-world right-click (main hand + off hand)
 * in a way that could double-trigger this is not confirmed; gating on
 * {@code event.getHand() == InteractionHand.MAIN_HAND} below is meant to
 * guard against that but the guard itself is unverified in-game.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumHoeHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != ModItems.PRISMIUM_HOE.get()) {
            return;
        }
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            // Bone-meal-style growth is a server-authoritative world edit;
            // let the client-side firing of this event no-op here.
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) {
            return;
        }
        if (!bonemealable.isValidBonemealTarget(level, pos, state)) {
            return;
        }
        RandomSource random = level.getRandom();
        if (bonemealable.isBonemealSuccess(serverLevel, random, pos, state)) {
            bonemealable.performBonemeal(serverLevel, random, pos, state);
            event.setCanceled(true);
        }
    }
}
