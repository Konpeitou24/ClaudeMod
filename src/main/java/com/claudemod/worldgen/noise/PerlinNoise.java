package com.claudemod.worldgen.noise;

import java.util.Random;

/**
 * Self-contained 3D gradient ("Perlin-style") noise generator.
 *
 * <p>This is our own implementation of the classic improved-Perlin-noise
 * algorithm (permutation table + fade curve + gradient dot products) -
 * it does not use or depend on Minecraft's internal
 * {@code net.minecraft.world.level.levelgen.synth.PerlinNoise}, so it
 * has no dependency on Mojang-mapped internals that can shift between
 * game versions, and its exact behaviour is fully under this mod's
 * control.
 *
 * <p>Originally written to answer GitHub Issue #23 ("ノイズを自作して、
 * まばらに切り替わるようにしてください" - make our own noise so the
 * Prismium Stone / Deepstone boundary switches over more scattered
 * instead of a hard flat line), but deliberately kept generic: it takes
 * no dependency on any specific block, dimension, or feature, so it can
 * be reused anywhere else in the mod that wants organic-looking
 * variation - e.g. scattering a biome edge, varying ore/feature density,
 * or any other "avoid a hard cutoff" need.
 *
 * <p>Two instances built from different seeds never line up, so
 * unrelated systems can each hold their own {@code PerlinNoise} (see
 * {@link FractalNoise}) without interfering with one another.
 */
public final class PerlinNoise implements Noise2D, Noise3D {

    private static final int TABLE_SIZE = 256;
    private static final int TABLE_MASK = TABLE_SIZE - 1;

    /** Permutation table, duplicated so lookups never need to wrap manually. */
    private final int[] permutation = new int[TABLE_SIZE * 2];

    public PerlinNoise(long seed) {
        int[] base = new int[TABLE_SIZE];
        for (int i = 0; i < TABLE_SIZE; i++) {
            base[i] = i;
        }

        Random random = new Random(seed);
        for (int i = TABLE_SIZE - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            int temp = base[i];
            base[i] = base[swapIndex];
            base[swapIndex] = temp;
        }

        for (int i = 0; i < TABLE_SIZE * 2; i++) {
            permutation[i] = base[i & TABLE_MASK];
        }
    }

    /** 2D sample, implemented as a slice through the 3D field at y = 0. */
    @Override
    public double sample(double x, double z) {
        return sample(x, 0.0, z);
    }

    @Override
    public double sample(double x, double y, double z) {
        int cellX = fastFloor(x) & TABLE_MASK;
        int cellY = fastFloor(y) & TABLE_MASK;
        int cellZ = fastFloor(z) & TABLE_MASK;

        double fracX = x - Math.floor(x);
        double fracY = y - Math.floor(y);
        double fracZ = z - Math.floor(z);

        double fadeX = fade(fracX);
        double fadeY = fade(fracY);
        double fadeZ = fade(fracZ);

        int a = permutation[cellX] + cellY;
        int aa = permutation[a] + cellZ;
        int ab = permutation[a + 1] + cellZ;
        int b = permutation[cellX + 1] + cellY;
        int ba = permutation[b] + cellZ;
        int bb = permutation[b + 1] + cellZ;

        double x1 = lerp(fadeX,
                grad(permutation[aa], fracX, fracY, fracZ),
                grad(permutation[ba], fracX - 1, fracY, fracZ));
        double x2 = lerp(fadeX,
                grad(permutation[ab], fracX, fracY - 1, fracZ),
                grad(permutation[bb], fracX - 1, fracY - 1, fracZ));
        double y1 = lerp(fadeY, x1, x2);

        double x3 = lerp(fadeX,
                grad(permutation[aa + 1], fracX, fracY, fracZ - 1),
                grad(permutation[ba + 1], fracX - 1, fracY, fracZ - 1));
        double x4 = lerp(fadeX,
                grad(permutation[ab + 1], fracX, fracY - 1, fracZ - 1),
                grad(permutation[bb + 1], fracX - 1, fracY - 1, fracZ - 1));
        double y2 = lerp(fadeY, x3, x4);

        return lerp(fadeZ, y1, y2);
    }

    private static int fastFloor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    /** Ken Perlin's improved fade curve: 6t^5 - 15t^4 + 10t^3. */
    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /** Dot product of a pseudo-random gradient direction (from the hash) and (x, y, z). */
    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
