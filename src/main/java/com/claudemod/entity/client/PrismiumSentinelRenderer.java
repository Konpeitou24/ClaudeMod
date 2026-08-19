package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumSentinelEntity;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumSentinelEntity}. Same reuse-the-vanilla-model
 * approach as {@link PrismiumWraithRenderer}/{@link PrismiumDeepWraithRenderer}
 * (see PrismiumSentinelEntity's javadoc): wraps vanilla's own
 * {@link SkeletonModel} geometry (thin-limbed biped, {@link ModelLayers#SKELETON})
 * with only the texture swapped, rather than authoring new model geometry
 * blind in a sandbox that cannot render or verify it.
 */
public class PrismiumSentinelRenderer extends HumanoidMobRenderer<PrismiumSentinelEntity, SkeletonModel<PrismiumSentinelEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_sentinel.png");

    public PrismiumSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumSentinelEntity entity) {
        return TEXTURE;
    }
}
