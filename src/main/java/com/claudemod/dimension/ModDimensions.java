package com.claudemod.dimension;

import com.claudemod.ClaudeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Session 14: the mod's first step into the "Prism Realm" dimension, which
 * had been sitting untouched in the roadmap since session 1 (PROGRESS.md
 * section 1, item 3 - the last of the mod's four content pillars to see
 * any work at all).
 *
 * <p>This class only holds the {@link ResourceKey} constant used to look
 * the dimension up at runtime (e.g. {@code server.getLevel(PRISM_REALM)}).
 * The dimension itself is defined entirely as data - see
 * {@code data/claudemod/dimension_type/prism_realm_type.json} and
 * {@code data/claudemod/dimension/prism_realm.json} - following the same
 * "hand-written JSON, no datagen" convention the rest of the mod uses (see
 * PROGRESS.md section 4, item 2). No Java-side registration call is
 * needed for a datapack-defined dimension to exist; Forge loads
 * dimension/dimension_type JSON from a mod's bundled resources the same
 * way it loads any other data pack file.
 *
 * <p><b>Known caveat (verified via web search this session, not yet
 * confirmed against this mod's own gameplay):</b> historically (Forge
 * issue #8552, filed against 1.18.2, fixed via PR #8555) mod-provided
 * dimension JSON was missing from a *brand new* world's generated
 * level.dat on the very first server start, and only appeared after
 * restarting the server once (existing world, no deletion needed). The
 * fix predates this mod's 1.20.1 Forge version, but this class's own
 * behavior can't be exercised in this sandbox (no client/server can run
 * here - see PROGRESS.md's environment constraints), so if
 * {@code server.getLevel(PRISM_REALM)} unexpectedly returns {@code null}
 * on a freshly created world, restarting the server once is the first
 * thing to try before assuming the JSON itself is broken.
 */
public final class ModDimensions {

    public static final ResourceKey<Level> PRISM_REALM = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation(ClaudeMod.MOD_ID, "prism_realm"));

    private ModDimensions() {
    }
}
