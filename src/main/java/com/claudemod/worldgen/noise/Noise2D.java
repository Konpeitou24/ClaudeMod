package com.claudemod.worldgen.noise;

/**
 * A deterministic, reusable source of two-dimensional coherent noise.
 *
 * <p>Unlike {@link java.util.Random}, the same (x, z) coordinate always
 * produces the same value for a given instance, and nearby coordinates
 * produce nearby values (the "coherent" part) - so the output looks like
 * smooth, organic terrain rather than static.
 *
 * <p>Intended for anywhere worldgen needs a "should this happen here?"
 * decision that looks natural instead of a hard cutoff: blending two
 * block types across a boundary, scattering a biome edge, varying a
 * feature's density across a chunk, and so on.
 */
@FunctionalInterface
public interface Noise2D {

    /**
     * @param x world X coordinate (can be fractional; scale it to control
     *          how large the noise "features" are - smaller frequency
     *          input values make bigger patches)
     * @param z world Z coordinate
     * @return a noise value, roughly in the range [-1, 1]
     */
    double sample(double x, double z);
}
