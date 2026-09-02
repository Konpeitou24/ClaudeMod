package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumWispEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumWispEntity}. Reuses vanilla's
 * {@code SquidModel} geometry wholesale, the exact same choice
 * {@link PrismiumDrifterRenderer} made (see that class's javadoc and
 * {@link PrismiumWispEntity}'s own javadoc for the full rationale) -
 * only the texture differs, a recolor of Drifter's own texture rather
 * than a freshly-guessed UV layout.
 *
 * <p>Extends the generic {@code MobRenderer<T, M>} base directly, same
 * choice as {@link PrismiumDrifterRenderer}/{@link PrismiumCrawlerRenderer}
 * for the same reason: the lowest-risk option versus extending vanilla's
 * own {@code SquidRenderer} (unconfirmed whether it is generic or
 * hardcoded to {@code Squid}).
 */
public class PrismiumWispRenderer extends MobRenderer<PrismiumWispEntity, SquidModel<PrismiumWispEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_wisp.png");

    public PrismiumWispRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumWispEntity entity) {
        return TEXTURE;
    }
}
