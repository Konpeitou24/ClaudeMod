package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumCrawlerEntity;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumCrawlerEntity}. Reuses vanilla's
 * {@code SilverfishModel} geometry wholesale (see that entity's javadoc
 * for the rationale, and PROGRESS.md for the mappings.dev verification
 * that the class is generic over {@code <T extends Entity>} rather than
 * hardcoded to vanilla {@code Silverfish}) and only swaps in a custom
 * texture.
 *
 * <p>Extends the generic {@code MobRenderer<T, M>} base directly, same
 * choice as {@link PrismiumDrifterRenderer} (and for the same reason:
 * lowest-risk option when this sandbox has no way to confirm whether
 * vanilla's own {@code SilverfishRenderer} is a generic
 * {@code <T extends Silverfish>} class or something narrower) rather than
 * extending any vanilla Silverfish-specific renderer class.
 */
public class PrismiumCrawlerRenderer extends MobRenderer<PrismiumCrawlerEntity, SilverfishModel<PrismiumCrawlerEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_crawler.png");

    public PrismiumCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumCrawlerEntity entity) {
        return TEXTURE;
    }
}
