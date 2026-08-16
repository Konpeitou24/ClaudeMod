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
 * Session 6 (revised): the Hoe's gimmick, the fourth of the five Prismium
 * tools to get one (see {@link PrismiumMiningHandler} for the
 * Pickaxe/Axe/Shovel gimmicks and {@link PrismiumSwordHandler} for the
 * Sword's).
 *
 * <p>Right-clicking any {@link BonemealableBlock} (crops, saplings, etc.
 * - the same interface vanilla Bone Meal itself checks) with the Prismium
 * Hoe in hand attempts to grow it exactly like using a Bone Meal item,
 * without consuming one. No cooldown or NBT state is tracked; spamming
 * right-click is equivalent to spamming Bone Meal by hand, which vanilla
 * already allows.
 *
 * <p><b>Revision (this session, after a real CI build failure)</b>: the
 * first version of this handler also called
 * {@code BonemealableBlock#isValidBonemealTarget} as a pre-check before
 * rolling {@code isBonemealSuccess}. That push broke the real GitHub
 * Actions build (see PROGRESS.md session 6 notes). Root cause could not be
 * pinned down with certainty from this sandbox (no local Forge/Gradle
 * classpath available to reproduce the exact javac error - see
 * PROGRESS.md section 2-1), but {@code isValidBonemealTarget} was the
 * newest and least-proven API surface in this push - its exact arg count
 * changed between Minecraft versions (a trailing {@code boolean} was
 * removed at some point in the 1.19.x -> 1.20.x range), unlike
 * {@code isBonemealSuccess}/{@code performBonemeal} whose
 * {@code (Level/ServerLevel, RandomSource, BlockPos, BlockState)} shape
 * was confirmed identical across every version's docs checked (1.18.2
 * through 1.20.2). To de-risk this without being able to compile-check
 * locally, this revision drops the {@code isValidBonemealTarget} call
 * entirely and relies only on the two stable methods. Practical effect:
 * the Hoe will occasionally attempt {@code performBonemeal} on a target
 * that would have failed the pre-check too (e.g. a crop already at max
 * growth stage); vanilla growable blocks are expected to no-op safely in
 * that case since bonemeal spam against a fully-grown crop is already
 * normal, always-safe player behavior. This is a deliberate
 * safety-over-completeness tradeoff pending the next session confirming
 * (via CI) whether this was really the fix, or whether the real cause was
 * elsewhere in this push (see PROGRESS.md "next steps" for what to check
 * if Run 17 is still red).
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
        RandomSource random = level.getRandom();
        if (bonemealable.isBonemealSuccess(serverLevel, random, pos, state)) {
            bonemealable.performBonemeal(serverLevel, random, pos, state);
            event.setCanceled(true);
        }
    }
}
