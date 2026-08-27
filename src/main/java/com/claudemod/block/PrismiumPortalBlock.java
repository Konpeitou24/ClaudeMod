package com.claudemod.block;

import com.claudemod.dimension.ModDimensions;
import com.claudemod.teleport.PrismiumTeleportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Session 52: Prismium Portal, the mod's first standing, walk-through
 * dimension gateway - a direct answer to GitHub issue #9
 * ("プリズミウムディメンションへ行く手段" / "a means to reach the
 * Prismium dimension"), which had until now only been answered with a
 * tooltip pointing at {@link com.claudemod.item.PrismiumRiftShardItem}
 * (session 38, see that item's class doc). The repo owner's own proposal
 * for this feature (PROGRESS.md §5 old item 9(j): "Prismium Coreの枠に
 * Prismiumを投げ込む" - throw Prismium into a Prismium Core frame) is
 * implemented essentially literally: build a hollow rectangular frame
 * (interior 2 wide x 3 tall, identical proportions to vanilla's minimum
 * Nether portal frame, chosen so the shape is already familiar to any
 * Minecraft player) and right-click one of the frame blocks while
 * holding a Prismium Shard - see
 * {@link com.claudemod.event.PrismiumPortalIgniteHandler} for the frame
 * detection/ignition logic that places this block. The frame's material
 * originally had to be a uniform ring of {@code PRISMIUM_CORE}; as of the
 * direct-chat session on 2026-08-19 it was replaced, per the repo owner's
 * explicit request, with a mixed-material recipe (top/bottom rows of
 * {@code PRISMIUM_BLOCK}, left/right columns of
 * {@code PRISMIUM_BLOCK_WALL}) - see that handler class's javadoc for the
 * full rationale.
 *
 * <p>Unlike vanilla's {@code NetherPortalBlock}, this class does not
 * extend that vanilla class (it's not public API meant for subclassing,
 * and copying vanilla's exact rendering/frame-search internals would add
 * a lot of unverifiable surface area). Instead it's a plain {@link Block}
 * with:
 * <ul>
 *   <li>{@link #AXIS} (reusing {@code BlockStateProperties.HORIZONTAL_AXIS},
 *   restricted to X/Z, the same property vanilla's nether portal uses) so
 *   the two frame orientations get a visually distinct block model: as of
 *   the direct-chat session on 2026-08-19 (see
 *   {@link com.claudemod.event.PrismiumPortalIgniteHandler}'s javadoc for
 *   the accompanying frame-recipe change), {@code
 *   assets/claudemod/models/block/prismium_portal.json} is a thin,
 *   2-pixel-deep membrane with only north/south faces (matching vanilla's
 *   nether portal silhouette, replacing the original full-cube {@code
 *   cube_all} model) and its blockstate applies a 90-degree Y rotation
 *   for {@code axis=z} so the thin side faces the right way in both
 *   orientations. The texture itself
 *   ({@code assets/claudemod/textures/block/prismium_portal.png}) is now
 *   an 8-frame animation strip with a matching {@code .png.mcmeta}
 *   (interpolated, 10 ticks/frame - slowed down from an initial 2 after
 *   the repo owner reported the faster version was flickery/eye-straining)
 *   instead of a single static frame - each
 *   frame is the previous one rolled diagonally by 2 pixels so it loops
 *   seamlessly and keeps the exact original color palette.</li>
 *   <li>No collision ({@link #getCollisionShape} returns empty) so players
 *   can walk straight through, like vanilla portals.</li>
 *   <li>Indestructible by normal means ({@code strength(-1.0F)}, no loot
 *   table) - same as vanilla nether/end portal blocks - since it's never
 *   obtainable as an item (no {@code BlockItem} registered for it, see
 *   {@code ModItems}) and letting it be silently minable would be a
 *   confusing dead end (a hole in a portal that just... stays a hole).</li>
 *   <li>{@link #entityInside} performs the actual teleport, reusing
 *   {@link PrismiumTeleportHelper} (the same logic
 *   {@code PrismiumRiftShardItem} uses) rather than duplicating it. Only
 *   {@link ServerPlayer} entities are teleported in this first version -
 *   items, mobs, projectiles etc. simply pass through visually without
 *   triggering anything, a known simplification (vanilla's nether portal
 *   teleports any entity) left for a future session if it turns out to
 *   matter.</li>
 *   <li>Re-entry protection via {@link Entity#isOnPortalCooldown()} /
 *   {@link Entity#setPortalCooldown()} - the same stock vanilla
 *   mechanism nether portals rely on to stop a player who just arrived
 *   from immediately bouncing back through a portal on the other side -
 *   rather than inventing a new persistent-data cooldown tag.</li>
 * </ul>
 *
 * <p><b>Deliberately no gradual "stand in it for N ticks" delay</b>: unlike
 * vanilla's nether portal (which accumulates portal time over several
 * ticks before actually teleporting), this teleports on the very first
 * tick an entity's hitbox intersects the block. Simpler to reason about
 * and test from this sandbox (no timer state to track/desync), at the
 * cost of a slightly less "weighty" feel than vanilla - a candidate for a
 * future polish pass if it feels too abrupt in actual play.
 *
 * <p><b>Unverified</b>: like everything else in this mod, this has not
 * been seen rendered or walked through in an actual running game client.
 * In particular: whether the translucent render type registered in
 * {@link com.claudemod.event.ClientModEvents} actually applies (no
 * in-game screenshot possible from this sandbox), and whether
 * {@link #entityInside} fires reliably for a player walking through at
 * normal speed (vs. sprinting/elytra-flying through too fast for a
 * single tick of overlap to register - a real risk this session could not
 * rule out).
 *
 * <p><b>Direct-chat session follow-up (2026-08-19, same day as the thin
 * model/animation/recipe changes above)</b>: after seeing the new thin
 * model in-game (screenshot provided), the repo owner reported two more
 * bugs: (1) breaking part of the frame did not deactivate the portal
 * (unlike vanilla's nether portal, which fizzles when its frame is
 * broken) - fixed by {@link com.claudemod.event.PrismiumPortalFrameBreakHandler},
 * a new listener on {@code BlockEvent.BreakEvent} (see that class's
 * javadoc for why a break-event listener was used instead of {@code
 * neighborChanged}, and why {@code neighborChanged} alone would have
 * missed the frame's corner blocks); and (2) the block's selection/pick
 * outline was still a full block-sized cube even though collision was
 * already disabled, making the portal feel much "bigger" to aim/interact
 * with than its thin visual - fixed by overriding {@link #getShape} below
 * to return the same thin box as the render model instead of the
 * default full cube. Both fixes are <b>unverified</b> in an actual
 * client/server, same as everything else in this class.
 */
public class PrismiumPortalBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public PrismiumPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                         BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * Direct-chat session follow-up (2026-08-19): the selection/pick
     * outline (used for the block-highlight box and for ray tracing what
     * the player is looking at) defaults to a full block-sized cube
     * unless overridden - {@link #getCollisionShape} only affects
     * whether entities physically collide with/walk through the block,
     * it does not affect this. Left at the default, the portal felt much
     * "bigger" to aim at / interact with than its thin visual model. This
     * mirrors the same thin box used by
     * {@code assets/claudemod/models/block/prismium_portal.json} (thin
     * along Z for {@code axis=x}, thin along X for {@code axis=z} - see
     * that model/this class's top javadoc for why the axes work out this
     * way).
     */
    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X
                ? Block.box(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D)
                : Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);
    }

    /**
     * GitHub issue #20 (2026-08-19, "触れていなくても1ブロック前を通過し
     * ただけでディメンションに飛ばされるときがある" - teleported just by
     * passing near the gate without touching it): {@link #entityInside} is
     * invoked by the engine for any entity whose bounding box intersects
     * this block's full 1x1x1 cell, independent of {@link #getCollisionShape}
     * (empty here) or {@link #getShape} (the thin visual box below) - the
     * same mechanism vanilla relies on for lava/cactus/powder-snow/its own
     * nether portal. Since this block's model is only a 2px-deep membrane
     * (1/8 of the cell), a player could previously trigger teleportation by
     * clipping the mostly-empty remainder of the full cell without ever
     * visually touching the glowing membrane, which matches the report
     * closely. Fixed by requiring the entity's actual bounding box to
     * overlap {@link #getShape}'s thin box (the same box already used for
     * the selection/pick outline) before teleporting, so the trigger volume
     * matches what the player can actually see. <b>Unverified</b> in an
     * actual client/server like the rest of this class - a plausible
     * explanation grounded in how {@code entityInside} is documented/used
     * elsewhere in vanilla, not a confirmed root-cause from a debugger or
     * log.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }
        VoxelShape thinShape = getShape(state, level, pos, CollisionContext.empty());
        AABB thinBox = thinShape.bounds().move(pos);
        if (!entity.getBoundingBox().intersects(thinBox)) {
            return;
        }
        if (entity.isOnPortalCooldown()) {
            entity.setPortalCooldown();
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();
        boolean inRealm = serverLevel.dimension() == ModDimensions.PRISM_REALM;

        boolean teleported = inRealm
                ? PrismiumTeleportHelper.teleportBackFromRealm(server, player)
                : PrismiumTeleportHelper.teleportToRealm(server, player);

        if (teleported) {
            entity.setPortalCooldown();
        }
    }

    /**
     * Ambient swirl particles, purely cosmetic (client-side only). Loosely
     * modeled on vanilla's nether portal ambiance but much sparser (one
     * particle attempt per tick instead of several).
     *
     * <p>Direct-chat session update (2026-08-19): now that the block
     * model is a thin membrane instead of a full cube (see this class's
     * top javadoc), particles are spawned hugging the model's thin
     * dimension (the axis picked here mirrors {@link #AXIS} - see
     * {@code prismium_portal.json}, which is thin along Z for
     * {@code axis=x} and gets rotated 90 degrees for {@code axis=z}, so
     * "thin along Z before rotation" is exactly right for both) rather
     * than spread across the full block volume, so the particles
     * visually hug the plane instead of drifting away from it inside
     * empty space that no longer has a model there. <b>Unverified</b>:
     * no in-game confirmation this reads correctly for both axis values.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        Direction.Axis axis = state.getValue(AXIS);
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        double thinJitter = (random.nextDouble() - 0.5D) * 0.125D;
        if (axis == Direction.Axis.X) {
            // Model is thin along Z before the blockstate's rotation.
            z = pos.getZ() + 0.5D + thinJitter;
        } else {
            x = pos.getX() + 0.5D + thinJitter;
        }
        level.addParticle(ParticleTypes.PORTAL, x, y, z,
                (random.nextDouble() - 0.5D) * 0.5D, -random.nextDouble(), (random.nextDouble() - 0.5D) * 0.5D);
    }

    /**
     * Direct-chat session (2026-08-27): the repo owner confirmed the
     * Creative-punch fix ({@link com.claudemod.event.PrismiumPortalIndestructibleHandler})
     * works, but reported a second, previously-unnoticed way to destroy
     * the portal: simply pouring water onto/into it makes it vanish, with
     * no drop and (per the same report) no sound.
     *
     * <p><b>Root cause (verified via WebSearch against {@code
     * BlockBehaviour}'s 1.20.x method list, not just code reading)</b>:
     * {@link net.minecraft.world.level.block.state.BlockBehaviour#canBeReplaced(BlockState, Fluid)}
     * is the method flowing fluid consults before it overwrites whatever
     * block is currently in its path (the same mechanism that lets
     * water/lava silently sweep away torches, saplings, tall grass, etc).
     * The inherited default resolves to {@code state.canBeReplaced() ||
     * !state.isSolid()} - and {@code isSolid()} is derived from this
     * block's collision shape, which {@link #getCollisionShape} already
     * returns empty (so players can walk through). That combination made
     * this block look "replaceable" to fluid code by default, exactly
     * like an un-collidable decoration - nothing before this fix ever
     * told the engine "no, not even fluid may overwrite this." Vanilla's
     * own {@code NetherPortalBlock} and {@code EndPortalBlock} hit the
     * same default and both explicitly override this method to
     * {@code false}; this mirrors that.
     *
     * <p>This also explains the "no sound" half of the report without a
     * separate bug: fluid overwriting a block is a direct
     * {@code setBlock} from the fluid-spread code, not a
     * {@code BlockEvent.BreakEvent} - so {@link
     * com.claudemod.event.PrismiumPortalFrameBreakHandler}'s collapse
     * sound (which only plays for a player-initiated frame-material
     * break) was never in the loop for this path, and neither was any
     * other break/place sound. With this override, fluid can no longer
     * remove the block at all, so there is nothing left to be silent
     * about.
     *
     * <p><b>Unverified</b>, like the rest of this class: no in-game
     * confirmation that water actually leaves the portal alone after
     * this change (this sandbox cannot run a client), though the
     * reasoning is grounded in vanilla's own portal blocks hitting and
     * fixing the identical default.
     */
    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }
}
