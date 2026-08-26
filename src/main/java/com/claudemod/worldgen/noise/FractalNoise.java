package com.claudemod.worldgen.noise;

/**
 * Combines several octaves of a base {@link Noise2D}/{@link Noise3D}
 * source into fractal ("fractal Brownian motion" / fBm) noise.
 *
 * <p>A single {@link PerlinNoise} sample tends to look too smooth and
 * uniform for scattering a block-by-block transition - it produces one
 * broad wave, not the patchy, uneven look natural terrain has. Stacking
 * several octaves at increasing frequency and decreasing amplitude adds
 * fine detail on top of the broad shape, which is what actually reads
 * as "organic"/"まばら" (scattered) rather than a single smooth curve.
 *
 * <p>Reusable and independent of any particular block or feature - wrap
 * any {@link Noise3D} (typically a {@link PerlinNoise}) to get a richer
 * noise field, whether that's for this mod's Prismium Stone/Deepstone
 * boundary (Issue #23) or a future use such as scattering a biome edge.
 */
public final class FractalNoise implements Noise2D, Noise3D {

    private final Noise3D base;
    private final int octaves;
    private final double persistence;
    private final double lacunarity;

    /**
     * @param base        the underlying noise source to layer (usually a {@link PerlinNoise})
     * @param octaves     how many layers to stack (more = finer detail, more expensive); at least 1
     * @param persistence how much each successive octave's amplitude shrinks by (typically ~0.5)
     * @param lacunarity  how much each successive octave's frequency grows by (typically ~2.0)
     */
    public FractalNoise(Noise3D base, int octaves, double persistence, double lacunarity) {
        this.base = base;
        this.octaves = Math.max(1, octaves);
        this.persistence = persistence;
        this.lacunarity = lacunarity;
    }

    @Override
    public double sample(double x, double z) {
        return sample(x, 0.0, z);
    }

    @Override
    public double sample(double x, double y, double z) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double amplitudeSum = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += base.sample(x * frequency, y * frequency, z * frequency) * amplitude;
            amplitudeSum += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        // Normalize so the combined output stays roughly in [-1, 1]
        // regardless of how many octaves were stacked.
        return amplitudeSum == 0.0 ? 0.0 : total / amplitudeSum;
    }
}
