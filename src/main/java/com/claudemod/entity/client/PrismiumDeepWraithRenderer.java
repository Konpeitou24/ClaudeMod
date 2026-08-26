package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumDeepWraithEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumDeepWraithEntity}. Exact same pattern as
 * {@link PrismiumWraithRenderer} (same {@code ModelLayers.ZOMBIE} body
 * geometry via plain {@code HumanoidModel}, custom texture only) - see
 * that class's javadoc for the full rationale and the "why HumanoidModel
 * instead of ZombieModel" trade-off.
 */
public class PrismiumDeepWraithRenderer extends HumanoidMobRenderer<PrismiumDeepWraithEntity, HumanoidModel<PrismiumDeepWraithEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_deep_wraith.png");

    public PrismiumDeepWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumDeepWraithEntity entity) {
        return TEXTURE;
    }
}
