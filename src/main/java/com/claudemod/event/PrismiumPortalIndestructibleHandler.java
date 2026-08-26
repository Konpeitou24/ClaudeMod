package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModBlocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Direct-chat follow-up to GitHub issue #20 (screenshot provided
 * 2026-08-26 showing a correctly-built, correctly-oriented gate - so the
 * "generates vertically" point from the original report is not
 * reproduced by this screenshot and remains unexplained/unconfirmed):
 * the repo owner confirmed that a Creative-mode player can punch the
 * portal's interior ({@link com.claudemod.block.PrismiumPortalBlock})
 * away entirely.
 *
 * <p><b>Root cause (verified via WebSearch, not just code reading)</b>:
 * {@code strength(-1.0f)} on {@code ModBlocks.PRISMIUM_PORTAL} (matching
 * vanilla's own {@code NetherPortalBlock}) does <em>not</em>, by itself,
 * protect a block from Creative-mode punching. Negative hardness only
 * prevents a block's mined-damage from ever accumulating in
 * <em>Survival</em> (so it can never be timed-mined) - it is a commonly
 * held misconception (one this class's author briefly held too) that it
 * also blocks Creative's "instant destroy on left-click". In vanilla,
 * Creative mode destroys instantly whatever block the crosshair is
 * actually targeting, regardless of hardness, for any block the player
 * can target at all - this is exactly how bedrock, barrier, and end
 * portal frame blocks can genuinely be punched away in Creative (a fact
 * this class's author double-checked instead of assuming). What actually
 * protects vanilla's {@code NetherPortalBlock} from ever being punched,
 * in *either* game mode, is that it never overrides
 * {@link net.minecraft.world.level.block.Block#getShape}, so its pick/
 * selection shape stays empty just like its (also empty) collision shape
 * - the crosshair can never lock onto it in the first place, so no
 * break action is ever sent for it at all.
 *
 * <p>This mod's {@code PrismiumPortalBlock} cannot use that same trick:
 * an earlier direct-chat session deliberately gave it a non-empty
 * {@code getShape} (a thin box matching the visual membrane) specifically
 * so the block-highlight outline wouldn't feel "too big" - see that
 * class's javadoc. That fix made the block targetable, which as a side
 * effect made it punchable in Creative. Rather than reverting that
 * earlier, explicitly-requested fix, this class closes the gap the
 * proper Forge way: cancel {@link BlockEvent.BreakEvent} outright
 * whenever the broken block is the portal itself, in every game mode
 * (this event fires from server-side break handling before vanilla
 * actually removes the block, in Creative as well as Survival - it is
 * the standard mechanism other mods use for "this block cannot be broken
 * by punching it, full stop" and does not rely on hardness at all).
 * Survival was never actually able to complete a break here (matching
 * the original report: "long-press shows a mining animation but nothing
 * happens" - correctly explained by negative hardness after all, since
 * that part *is* about Survival's damage-accumulation math), so this
 * only changes observable behavior for Creative.
 *
 * <p>Frame material ({@code PRISMIUM_BLOCK}/{@code PRISMIUM_BLOCK_WALL})
 * is intentionally untouched here and remains breakable as normal in
 * both game modes - only the portal's own interior fill block is
 * protected. See {@link PrismiumPortalFrameBreakHandler} for the
 * separate, still-active logic that collapses the portal when its frame
 * is broken.
 *
 * <p><b>Unverified</b>, like the rest of this feature: no in-game
 * confirmation that {@code BlockEvent.BreakEvent} actually fires (and is
 * actually honored) for a Creative-mode instant-destroy in this Forge
 * version - the reasoning above is grounded in how the event is
 * documented/used elsewhere, not a confirmed root-cause from a debugger.
 * If Creative punching still removes the portal after this fix, the
 * more invasive fallback (reported to work for other mods in the same
 * situation) would be overriding
 * {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#onDestroyedByPlayer}
 * on {@code PrismiumPortalBlock} itself to unconditionally return
 * {@code false} - left as the next thing to try if this event-based
 * approach turns out not to be enough.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumPortalIndestructibleHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() == ModBlocks.PRISMIUM_PORTAL.get()) {
            event.setCanceled(true);
        }
    }
}
