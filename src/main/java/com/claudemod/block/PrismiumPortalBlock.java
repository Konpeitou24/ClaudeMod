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
 *   (interpolated, 2 ticks/frame) instead of a single static frame - each
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

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
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

    @Override
    public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }
}
