package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumDrifterEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumDrifterEntity}. Reuses vanilla's
 * {@code SquidModel} geometry wholesale (see PrismiumDrifterEntity's
 * javadoc for the rationale) and only swaps in a custom texture.
 *
 * Extends the generic {@code MobRenderer<T, M>} base directly rather than
 * vanilla's own {@code SquidRenderer} - the same choice
 * PrismiumWraithRenderer made (extending {@code HumanoidMobRenderer}
 * rather than the Zombie-specific {@code ZombieRenderer}), for the same
 * reason: it is the lowest-risk option when this sandbox cannot confirm
 * whether {@code SquidRenderer} is written as a generic
 * {@code <T extends Squid>} class or hardcoded to the vanilla
 * {@code Squid} type. The one thing this trades away is vanilla Squid's
 * extra "tumble while swimming/drifting" rotation animation
 * ({@code SquidRenderer#setupRotations}); the entity still swims and
 * moves correctly via inherited {@code Squid} AI, it just orients more
 * simply. <b>Improvement idea for a future session</b>: if this sandbox
 * ever gets a way to confirm {@code SquidRenderer}'s exact class
 * signature, switching to extend it directly would restore that
 * animation for free.
 */
public class PrismiumDrifterRenderer extends MobRenderer<PrismiumDrifterEntity, SquidModel<PrismiumDrifterEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_drifter.png");

    public PrismiumDrifterRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumDrifterEntity entity) {
        return TEXTURE;
    }
}
