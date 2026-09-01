package com.claudemod.compat.jei;

import com.claudemod.ClaudeMod;
import com.claudemod.blockentity.AbstractPrismiumMachineBlockEntity;
import com.claudemod.blockentity.PrismiumCompressorBlockEntity;
import com.claudemod.blockentity.PrismiumPulverizerBlockEntity;
import com.claudemod.blockentity.PrismiumSmelterBlockEntity;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /** Every subclass of {@code AbstractPrismiumMachineBlockEntity} shares
     * the same {@code PROCESS_TIME_TICKS}/{@code ENERGY_PER_TICK}
     * constants (see that class - no machine currently overrides either),
     * so the FE cost shown on every category's recipe pages is this one
     * shared number rather than something computed per machine. */
    private static final int ENERGY_COST_FE = AbstractPrismiumMachineBlockEntity.PROCESS_TIME_TICKS
            * AbstractPrismiumMachineBlockEntity.ENERGY_PER_TICK;

    public static final RecipeType<MachineJeiRecipe> PULVERIZING_TYPE =
            RecipeType.create(ClaudeMod.MOD_ID, "pulverizing", MachineJeiRecipe.class);
    public static final RecipeType<MachineJeiRecipe> SMELTING_TYPE =
            RecipeType.create(ClaudeMod.MOD_ID, "smelting", MachineJeiRecipe.class);
    public static final RecipeType<MachineJeiRecipe> COMPRESSING_TYPE =
            RecipeType.create(ClaudeMod.MOD_ID, "compressing", MachineJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    /**
     * GitHub issue #21's main request: show this mod's own machine
     * conversions (previously invisible to JEI - see {@link
     * MachineJeiRecipe}'s javadoc for why) as real recipe pages. One
     * {@link MachineRecipeCategory} instance per machine, each carrying its
     * own {@link RecipeType} and using the machine block itself as the
     * category's tab icon (via {@code guiHelper.createDrawableItemStack} -
     * confirmed against JEI's 1.20.1 source this session, see {@link
     * MachineRecipeCategory}'s javadoc).
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new MachineRecipeCategory(PULVERIZING_TYPE,
                        Component.translatable("block.claudemod.prismium_pulverizer"),
                        new ItemStack(ModBlocks.PRISMIUM_PULVERIZER.get()), guiHelper),
                new MachineRecipeCategory(SMELTING_TYPE,
                        Component.translatable("block.claudemod.prismium_smelter"),
                        new ItemStack(ModBlocks.PRISMIUM_SMELTER.get()), guiHelper),
                new MachineRecipeCategory(COMPRESSING_TYPE,
                        Component.translatable("block.claudemod.prismium_compressor"),
                        new ItemStack(ModBlocks.PRISMIUM_COMPRESSOR.get()), guiHelper)
        );
    }

    /**
     * Builds each machine's {@link MachineJeiRecipe} list directly from its
     * own {@code jeiRecipes()} accessor (see that method's javadoc on each
     * of the three block entity classes) rather than hand-copying the
     * input/output pairs here, so this can never silently drift out of
     * sync with the machines' actual hardcoded conversion tables.
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PULVERIZING_TYPE, toJeiRecipes(PrismiumPulverizerBlockEntity.jeiRecipes()));
        registration.addRecipes(SMELTING_TYPE, toJeiRecipes(PrismiumSmelterBlockEntity.jeiRecipes()));
        registration.addRecipes(COMPRESSING_TYPE, toJeiRecipes(PrismiumCompressorBlockEntity.jeiRecipes()));

        // Second half of issue #21's own follow-up comment: "プリズミウムの
        // 鉱石が出やすい高さなどを、プリズミウムの欠片の入手方法(JEI)で
        // 表示するようにできたら幸いです" (show the ore's likely spawn
        // height in JEI's "how to obtain" info for Prismium Shard). The
        // height figures in the lang entry below were read directly from
        // this session's actual worldgen JSON - both
        // data/claudemod/worldgen/placed_feature/prismium_ore_placed.json
        // and its Prism Realm counterpart share the same trapezoid
        // height_range (min above_bottom 0 to max absolute 40, with a
        // plateau of 16), and both dimensions share min_y -64 (see
        // data/claudemod/dimension_type/prism_realm_type.json and vanilla
        // overworld). Per Minecraft's own documented trapezoid-height-
        // provider math, the plateau's flat peak band is centered in the
        // middle of the full min/max range, which works out here to
        // Y=-20 through Y=-4 - not guessed or approximated from memory.
        registration.addItemStackInfo(new ItemStack(ModItems.PRISMIUM_SHARD.get()),
                Component.translatable("jei.claudemod.prismium_shard.ore_info"));
    }

    private static List<MachineJeiRecipe> toJeiRecipes(Map<Item, ItemStack> recipes) {
        List<MachineJeiRecipe> result = new ArrayList<>();
        for (Map.Entry<Item, ItemStack> entry : recipes.entrySet()) {
            result.add(new MachineJeiRecipe(new ItemStack(entry.getKey()), entry.getValue(), ENERGY_COST_FE));
        }
        return result;
    }

    /**
     * Lets players see, from the machine block itself, which of this
     * mod's recipe categories it can craft - the same "click the furnace
     * to see what it makes" convenience vanilla's own furnace has.
     */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PRISMIUM_PULVERIZER.get()), PULVERIZING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PRISMIUM_SMELTER.get()), SMELTING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PRISMIUM_COMPRESSOR.get()), COMPRESSING_TYPE);
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
