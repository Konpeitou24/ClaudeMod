package com.claudemod.compat.jei;

import com.claudemod.ClaudeMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * 2026-08-31 direct-chat session (こんぺいとう氏: 「ＪＥＩの修正は混乱を招く
 * からちゃんと修正してくれ」) - this session's earlier scheduled-task pass
 * had made {@link com.claudemod.client.overlay.ItemDetailsOverlay}'s W-hold
 * panel fall back to showing the player's *held* item on any non-container
 * Screen (including JEI's own recipe-view popup), since that overlay
 * previously required {@code AbstractContainerScreen#getSlotUnderMouse()}
 * and JEI's screens aren't one. The repo owner correctly called that
 * fallback out as actively misleading (it shows unrelated info while
 * browsing JEI, not the thing the player is actually looking at) rather
 * than a real fix. This class is the proper fix: a real JEI plugin that
 * lets {@link com.claudemod.compat.jei.JeiCompat} ask JEI itself what
 * ingredient is under the mouse, on any of JEI's own screens, via
 * {@code IJeiRuntime#getScreenHelper()#getClickableIngredientUnderMouse}
 * (confirmed this session against JEI's own {@code 1.20.1} branch source,
 * fetched via {@code mcp__workspace__web_fetch} against
 * {@code raw.githubusercontent.com/mezz/JustEnoughItems/1.20.1/...} - see
 * {@link JeiCompat}'s class doc for the full API citation).
 *
 * <p><b>Soft-dependency design, mirrors {@code CuriosCompat}/{@code
 * CuriosSetupEvents} exactly</b> (see those classes' javadoc for the same
 * reasoning applied to Curios): this class and {@link JeiCompat} are the
 * only two files in this mod that import any {@code mezz.jei.*} type.
 * Nothing else in the mod references either class directly except
 * {@code ItemDetailsOverlay}, which only ever calls into {@link JeiCompat}
 * after confirming {@code ModList.get().isLoaded("jei")} - so a player
 * without JEI installed never triggers classloading of this file or
 * {@link JeiCompat} at all. This class itself is even more strongly
 * isolated than that pattern requires: it is never referenced anywhere in
 * this mod's own code - Forge's {@code @JeiPlugin} annotation scanning
 * (done by JEI itself, not by this mod) is the only thing that ever
 * discovers and instantiates it, and that scanning only happens at all
 * when JEI's own mod is present and loaded. mods.toml declares the
 * {@code jei} dependency as {@code mandatory = false, side = "CLIENT"} -
 * see that file's comment.
 *
 * <p><b>Unverified in-game</b> (no Minecraft client in this sandbox, see
 * PROGRESS.md standing note): whether JEI actually discovers and loads
 * this plugin correctly, whether {@code onRuntimeAvailable}/{@code
 * onRuntimeUnavailable} fire at the expected times (e.g. world
 * join/leave), and whether the {@code jei_version} pinned in
 * gradle.properties (the latest 1.20.1 release as of this session) is
 * compatible with whatever JEI version the repo owner actually has
 * installed - JEI's own wiki states the API is "very stable" across a
 * major version line, but this has not been tested against an older
 * 15.x JEI install specifically.
 */
@JeiPlugin
public class ClaudeModJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = new ResourceLocation(ClaudeMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiCompat.setRuntime(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiCompat.setRuntime(null);
    }
}
