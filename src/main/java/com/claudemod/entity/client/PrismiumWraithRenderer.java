package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumWraithEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumWraithEntity}. Reuses vanilla's ZombieModel
 * geometry wholesale (see PrismiumWraithEntity's javadoc for the rationale)
 * and only swaps in a custom texture. This is the mod's first entity
 * renderer / first use of EntityRenderersEvent.RegisterRenderers.
 */
public class PrismiumWraithRenderer extends HumanoidMobRenderer<PrismiumWraithEntity, ZombieModel<PrismiumWraithEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_wraith.png");

    public PrismiumWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumWraithEntity entity) {
        return TEXTURE;
    }
}
