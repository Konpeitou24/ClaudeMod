package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumDeepWraithEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumDeepWraithEntity}. Exact same pattern as
 * {@link PrismiumWraithRenderer} (vanilla ZombieModel geometry, custom
 * texture only) - see that class's javadoc for the rationale.
 */
public class PrismiumDeepWraithRenderer extends HumanoidMobRenderer<PrismiumDeepWraithEntity, ZombieModel<PrismiumDeepWraithEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_deep_wraith.png");

    public PrismiumDeepWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumDeepWraithEntity entity) {
        return TEXTURE;
    }
}
