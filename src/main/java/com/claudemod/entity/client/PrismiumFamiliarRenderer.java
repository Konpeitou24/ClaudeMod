package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumFamiliarEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumFamiliarEntity}. Same "borrow SquidModel
 * geometry wholesale, reskin only the texture" choice
 * {@link PrismiumWispRenderer}/{@link PrismiumDrifterRenderer} already
 * made (see those classes' javadoc for the full rationale) - the
 * Familiar's texture is its own recolor of Wisp's own texture (see
 * {@code gen_prismium_familiar.py}), not a freshly-guessed UV layout.
 */
public class PrismiumFamiliarRenderer extends MobRenderer<PrismiumFamiliarEntity, SquidModel<PrismiumFamiliarEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_familiar.png");

    public PrismiumFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumFamiliarEntity entity) {
        return TEXTURE;
    }
}
