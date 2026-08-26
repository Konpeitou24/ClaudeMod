package com.claudemod.worldgen.noise;

/**
 * The three-dimensional counterpart to {@link Noise2D}: a deterministic,
 * reusable source of coherent noise over (x, y, z).
 *
 * <p>Use this over {@link Noise2D} whenever the decision needs to vary
 * with height too - for example scattering block-by-block within a
 * vertical band, rather than only across a flat horizontal boundary.
 */
@FunctionalInterface
public interface Noise3D {

    /**
     * @param x world X coordinate (fractional; see {@link Noise2D#sample})
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return a noise value, roughly in the range [-1, 1]
     */
    double sample(double x, double y, double z);
}
