package com.claudemod.client.render;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Session 50: custom sky/cloud effects for the Prism Realm dimension.
 *
 * Background (see PROGRESS.md 00000/§3AW-6 for the earlier investigation):
 * the dimension_type JSON previously pointed {@code effects} at
 * {@code "minecraft:overworld"}, which made Prism Realm draw the exact same
 * white clouds at height 192 as the Overworld - visually wrong for a
 * separate dimension, and something the mod's user explicitly flagged as
 * looking unnatural.
 *
 * API confirmed this session via the Forge 1.19.3 javadoc mirror
 * (nekoyue.github.io/ForgeJavaDocs-NG, class layout unchanged through
 * 1.20.1): {@link DimensionSpecialEffects}'s protected constructor is
 * {@code (float cloudLevel, boolean hasGround, SkyType skyType,
 * boolean forceBrightLightmap, boolean constantAmbientLight)}, and
 * {@link DimensionSpecialEffects#getCloudHeight()} just returns that
 * {@code cloudLevel} field verbatim - the renderer is what decides what a
 * NaN height means. That vanilla's own Nether/End effects pass
 * {@code Float.NaN} as this argument specifically to suppress cloud
 * rendering (rather than e.g. drawing them at height NaN) is corroborated
 * by a Forge Forums example doing exactly this
 * (super(Float.NaN, true, SkyType.NONE, false, true)) and by a Sodium
 * issue titled "Float.NaN in clouds_height of the DimensionEffects" - two
 * independent sources landing on the same fact, which is the bar this mod
 * holds itself to before hard-coding a "magic" value (see PROGRESS.md
 * section 3G).
 *
 * Everything else about this class deliberately mirrors
 * {@code DimensionSpecialEffects.OverworldEffects} (round sky, ground
 * present, normal ambient light, standard fog) rather than Nether/End,
 * since Prism Realm has a sky, a horizon, and a lit surface just like the
 * Overworld - only the clouds should differ. The exact vanilla constants
 * used inside {@code OverworldEffects#getBrightnessDependentFogColor}
 * could not be confirmed from a primary source this session (the
 * decompiled method body isn't published on the javadoc mirrors we have
 * access to, and a targeted web search for the literal constants turned up
 * nothing usable). Rather than guess at unverified magic numbers, this
 * class uses a simpler, honestly-approximate formula (linear brightness
 * scaling of the incoming fog color) for that one method. This is a purely
 * cosmetic detail (it only affects how strongly fog darkens during
 * thunderstorms/night), so the risk from the approximation being slightly
 * "off" from vanilla's exact curve is low - unlike the cloud fix itself,
 * which is the actual point of this class.
 *
 * UNVERIFIED (client-only rendering code, no local build/game client in
 * this sandbox - see PROGRESS.md's recurring note about this category of
 * risk): whether clouds actually disappear in-game, and whether the fog
 * color approximation looks reasonable. Next session should check this
 * visually if a build/game client ever becomes available, or ask the user.
 */
public class PrismRealmEffects extends DimensionSpecialEffects {

    public PrismRealmEffects() {
        super(Float.NaN, true, DimensionSpecialEffects.SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        // Deliberate simplification, see class javadoc: linear brightness
        // scaling rather than vanilla's exact (unconfirmed) curve.
        return fogColor.multiply(brightness, brightness, brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
