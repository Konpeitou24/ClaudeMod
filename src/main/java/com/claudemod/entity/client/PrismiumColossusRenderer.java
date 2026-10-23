package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumColossusEntity;
import com.claudemod.registry.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumColossusEntity} (ClaudeMod's first boss-tier
 * mob, scheduled session 2026-10-17). Reuses the exact same
 * {@code HumanoidModel} baked from the vanilla {@code ModelLayers.ZOMBIE}
 * layer that {@link PrismiumWraithRenderer} / {@link
 * PrismiumDeepWraithRenderer} already use successfully - see this mod's
 * repeated "reuse an already-baked vanilla layer rather than author a new
 * model class this sandbox cannot render/verify" rule (PrismiumWraithEntity
 * / PrismiumSentinelEntity javadocs). The boss's "giant" read is carried by
 * its hitbox size ({@code ModEntities.PRISMIUM_COLOSSUS}'s
 * {@code .sized(1.2F, 2.7F)}) and its texture/stats, not by a scaled-up
 * render - the humanoid body model itself stays the normal Steve-proportion
 * geometry (same accepted model/hitbox mismatch precedent as
 * {@link PrismiumWispRenderer}'s javadoc).
 *
 * <p><b>Since the scheduled 2026-10-23 session</b>, this single renderer is
 * also registered (see {@code ClientModEvents}) for {@code
 * ModEntities.PRISMIUM_MOUNTAIN_COLOSSUS} - a second {@code EntityType} that
 * reuses this exact same {@link PrismiumColossusEntity} class (TODO53's
 * "各バイオームに固有のボス" follow-up). Rather than writing a second
 * near-duplicate renderer class, {@link #getTextureLocation} simply checks
 * which {@code EntityType} the entity instance actually is and picks one of
 * two textures accordingly - both {@code EntityType.Builder}-built types
 * share the same model/shadow radius, so nothing else about this renderer
 * needs to differ. This mirrors the "parameterize rather than duplicate"
 * approach {@code PrismiumDungeonFeature}/{@code PrismiumVaultFeature}
 * already used for their material palettes in v0.83.0.
 */
public class PrismiumColossusRenderer extends HumanoidMobRenderer<PrismiumColossusEntity, HumanoidModel<PrismiumColossusEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_colossus.png");

    private static final ResourceLocation MOUNTAIN_TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_mountain_colossus.png");

    public PrismiumColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumColossusEntity entity) {
        if (entity.getType() == ModEntities.PRISMIUM_MOUNTAIN_COLOSSUS.get()) {
            return MOUNTAIN_TEXTURE;
        }
        return TEXTURE;
    }
}
