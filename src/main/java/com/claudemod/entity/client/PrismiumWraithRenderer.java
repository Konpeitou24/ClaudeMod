package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumWraithEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for {@link PrismiumWraithEntity}. This is the mod's first entity
 * renderer / first use of EntityRenderersEvent.RegisterRenderers.
 *
 * <p>Uses a plain {@code HumanoidModel} baked from the same
 * {@code ModelLayers.ZOMBIE} layer definition the entity used to render
 * with when it extended vanilla {@code Zombie} - same body geometry, only a
 * custom texture swapped in. Changed (2026-08-26) from the
 * Zombie-specific {@code ZombieModel<T extends Zombie>} to plain {@code
 * HumanoidModel<T extends LivingEntity>} because {@link
 * PrismiumWraithEntity} no longer extends {@code Zombie} (see that class's
 * javadoc) and so no longer satisfies {@code ZombieModel}'s type bound.
 * Known cosmetic trade-off: loses the "arms held forward" zombie shamble
 * walk animation ({@code AbstractZombieModel#setupAnim}'s pose), since that
 * pose lives in the very Zombie-bound class hierarchy this had to move
 * away from - flagged in PROGRESS.md as a nice-to-have follow-up rather
 * than a blocker.
 */
public class PrismiumWraithRenderer extends HumanoidMobRenderer<PrismiumWraithEntity, HumanoidModel<PrismiumWraithEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_wraith.png");

    public PrismiumWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumWraithEntity entity) {
        return TEXTURE;
    }
}
