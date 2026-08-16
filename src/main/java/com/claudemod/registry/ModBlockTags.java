package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Custom block tags used by ClaudeMod, in addition to the vanilla tags we
 * extend directly under {@code data/minecraft/tags/blocks/}.
 *
 * <p>{@link #NEEDS_PRISMIUM_TOOL} marks blocks that require at least the
 * Prismium tier to harvest - i.e. even a diamond tool is not enough.
 *
 * <p>{@link #INCORRECT_FOR_PRISMIUM_TOOL} is the Prismium tier's own "tools
 * of this tier fail on these blocks" tag, the same role vanilla tiers give
 * to tags like {@code BlockTags.INCORRECT_FOR_DIAMOND_TOOL}. It is
 * deliberately a brand-new tag (not a reuse of the vanilla diamond one):
 * DiggerItem#isCorrectToolForDrops's only way to make a level-3 tool fail on
 * a level-3-and-above block is that tool's own tier tag, so Prismium needs a
 * tag that diamond's tag does NOT share. We add our exclusive block(s) to
 * {@code minecraft:incorrect_for_diamond_tool} (locks diamond out) while
 * keeping them out of this tag (keeps Prismium tools working). See
 * data/claudemod/tags/blocks/ and data/minecraft/tags/blocks/ for the
 * data-driven wiring, and ModToolTiers for where this is consumed.
 *
 * <p>This mechanism is unverified against a real build (see PROGRESS.md) -
 * it matches the standard "tier above diamond" pattern from public Forge
 * modding references, but has not been compiled or play-tested yet.
 */
public final class ModBlockTags {

    public static final TagKey<Block> NEEDS_PRISMIUM_TOOL = tag("needs_prismium_tool");
    public static final TagKey<Block> INCORRECT_FOR_PRISMIUM_TOOL = tag("incorrect_for_prismium_tool");

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(ClaudeMod.MOD_ID, name));
    }

    private ModBlockTags() {
    }
}
