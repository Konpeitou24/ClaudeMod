package com.claudemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Block for Prismium Geyser (session 66): the mod's second "gimmick"
 * block after Prismium Snare (session 64, {@link PrismiumSnareBlock}),
 * and the first with a *positive* effect - where Snare punishes
 * carelessness with Slowness/Poison, Geyser rewards walking onto it with
 * a traversal boost, launching whatever steps on it upward. This fills
 * part of the roadmap's still-open "探索そのものが楽しくなる...ギミック"
 * goal (PROGRESS.md §1 item 6) with something exploration-positive rather
 * than another hazard, after two sessions in a row (Snare, Magnet Charm)
 * of either hazards or passive accessories.
 *
 * <p><b>API choice</b>: deliberately uses {@link #stepOn}, NOT
 * {@code entityInside} (which {@link PrismiumSnareBlock} uses). The two
 * are not interchangeable: {@code entityInside} only fires when an
 * entity's hitbox actually overlaps the block's own collision volume,
 * which is why Snare has {@code noCollission()}-style near-zero geometry
 * (an entity standing "on top of" a normal full block never enters its
 * shape). {@code stepOn} is the opposite - vanilla's hook for "an entity
 * is supported by/standing on top of this block," used by
 * {@code SlimeBlock} (bounce), {@code HoneyBlock} (slow), and
 * {@code MagmaBlock} (damage) - exactly the shape needed for a full,
 * solid, walkable launch pad. Both the {@code stepOn} signature
 * ({@code void stepOn(Level, BlockPos, BlockState, Entity)}) and
 * {@link #animateTick} below were confirmed against the official 1.20.1
 * Mojang mappings published at mappings.dev (session 66 web search,
 * class {@code net.minecraft.world.level.block.Block}) rather than
 * assumed from memory or an older-version javadoc, since this is the
 * mod's first time overriding either method.
 *
 * <p><b>Launch logic</b>: mirrors {@code SlimeBlock}'s own bounce-gating
 * idea (skip the effect while the entity is sneaking, matching
 * {@code Entity#isShiftKeyDown()} the way vanilla's
 * {@code Entity#isSuppressingBounce()} does for slime blocks) plus a
 * "only when not already moving upward fast" guard
 * ({@code motion.y <= 0.1}) so an entity bouncing repeatedly while
 * standing still doesn't get an ever-escalating velocity stack - each
 * bounce fully consumes the upward window before the next one can apply,
 * the same natural self-limiting shape vanilla slime block trampolines
 * have. {@code hurtMarked = true} forces the velocity change to sync to
 * clients for non-player entities, the same pattern
 * {@code PrismiumMagnetCharmHandler} (session 65) established for
 * pushing {@code ItemEntity} velocity - reused here for the general
 * {@code Entity} case (players, mobs, items alike can all stepOn a
 * block).
 *
 * <p>Sound ({@code SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE}) and
 * particle ({@code ParticleTypes.BUBBLE_COLUMN_UP}) were picked
 * specifically for their vanilla "upward force" association (bubble
 * columns above soul sand) rather than something generic, and confirmed
 * to exist in 1.20.1 via web search (session 66) before use, following
 * this codebase's standing practice of verifying unfamiliar constants
 * rather than guessing. The server-side {@code sendParticles} + null-player
 * {@code playSound} combo copies {@link PrismiumSnareBlock#entityInside}'s
 * already-proven pattern verbatim.
 *
 * <p><b>Unverified</b> (see PROGRESS.md session 66): whether
 * {@code LAUNCH_VELOCITY} (1.4) feels fun rather than disorienting or
 * dangerous (fall damage on landing elsewhere is NOT mitigated - a
 * deliberate first-cut choice, see class doc below in PROGRESS.md), the
 * exact bounce-gating feel in practice, whether {@code stepOn} actually
 * fires reliably for a full solid block the way it's believed to based
 * on the vanilla precedents above, and whether {@code animateTick}'s
 * ambient bubble particle is visually noticeable/appropriate.
 */
public class PrismiumGeyserBlock extends Block {

    /** Upward velocity (blocks/tick) applied on trigger. Roughly in the
     * same ballpark as a vanilla slime block's bounce-back for a
     * moderate fall, chosen as a starting point for "noticeable but not
     * absurd" - entirely untuned by actual play, see class doc. */
    private static final double LAUNCH_VELOCITY = 1.4D;

    public PrismiumGeyserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isShiftKeyDown()) {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y <= 0.1D) {
                entity.setDeltaMovement(motion.x, LAUNCH_VELOCITY, motion.z);
                entity.hurtMarked = true;
                entity.fallDistance = 0.0F;

                if (level instanceof ServerLevel serverLevel) {
                    level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                    serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                            pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            14, 0.3D, 0.4D, 0.3D, 0.05D);
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    /** Client-only ambient flavor (see class doc): an occasional bubble
     * rising off the top face even when nothing is standing on it, so it
     * reads as "active" rather than an inert decorative block. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                    pos.getX() + 0.3D + random.nextDouble() * 0.4D,
                    pos.getY() + 1.02D,
                    pos.getZ() + 0.3D + random.nextDouble() * 0.4D,
                    0.0D, 0.04D, 0.0D);
        }
    }
}
