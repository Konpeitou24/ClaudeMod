package com.claudemod.entity.client;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.PrismiumFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer for {@link PrismiumFamiliarEntity}. Same "borrow SquidModel
 * geometry wholesale, reskin only the texture" choice
 * {@link PrismiumWispRenderer}/{@link PrismiumDrifterRenderer} already
 * made (see those classes' javadoc for the full rationale) - the
 * Familiar's texture is its own recolor of Wisp's own texture (see
 * {@code gen_prismium_familiar.py}), not a freshly-guessed UV layout.
 *
 * <p><b>v0.61.0 addition - carried item render</b>: draws {@link
 * PrismiumFamiliarEntity#getCarriedItem()} floating just above the
 * model when non-empty (HANDOFF.md's follow-up (h), the on-model half of
 * v0.60.0's "pack familiar" feature). Every API used here was confirmed
 * against mappings.dev's 1.20.1 mojmap pages this session rather than
 * assumed:
 * <ul>
 *   <li>{@link EntityRendererProvider.Context#getItemRenderer()} exists
 *   with exactly this signature (returns {@link ItemRenderer}).</li>
 *   <li>{@link ItemRenderer#renderStatic(ItemStack, ItemDisplayContext,
 *   int, int, PoseStack, MultiBufferSource, net.minecraft.world.level.Level, int)}
 *   is the overload used - confirmed to exist with this exact 8-argument
 *   order (stack, display context, packed light, packed overlay, pose
 *   stack, buffer source, level, seed) on two independent 1.20.1 javadoc
 *   mirrors (mappings.dev and lexxie.dev) this session, distinct from the
 *   other {@code renderStatic} overload that additionally takes a
 *   {@code LivingEntity} (not needed here).</li>
 *   <li>{@link ItemDisplayContext#GROUND} and {@link
 *   OverlayTexture#NO_OVERLAY} are both confirmed-present constants
 *   (the latter a {@code public static final int}), not guessed names.</li>
 *   <li>{@code EntityRenderer#render(T, float, float, PoseStack,
 *   MultiBufferSource, int)} is the exact {@code render(...)} signature
 *   confirmed on {@code EntityRenderer} itself this session, so
 *   overriding it here with the matching generic type ({@code
 *   PrismiumFamiliarEntity}) is a legal, checked {@code @Override}
 *   rather than a guess.</li>
 * </ul>
 * Deliberately uses {@link ItemDisplayContext#GROUND} (a small, mostly
 * flat presentation) rather than {@code FIXED}/{@code HEAD} - this is a
 * light ambient flyer with no arms/hands in its borrowed
 * {@code SquidModel} geometry to hold anything "in", so a simple hovering
 * icon (the same visual idea vanilla uses for a dropped {@code ItemEntity}
 * or an armor stand's ground-slot display) reads more naturally than a
 * transform meant for a held-in-hand pose. The exact scale/height offset
 * below are an untested visual guess (this sandbox cannot render 3D, see
 * PROGRESS.md) and are the first things to tune once seen in-game.
 */
public class PrismiumFamiliarRenderer extends MobRenderer<PrismiumFamiliarEntity, SquidModel<PrismiumFamiliarEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClaudeMod.MOD_ID, "textures/entity/prismium_familiar.png");

    private final ItemRenderer itemRenderer;

    public PrismiumFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)), 0.4F);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(PrismiumFamiliarEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PrismiumFamiliarEntity entity, float entityYaw, float partialTicks,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        ItemStack carried = entity.getCarriedItem();
        if (carried.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.3125D, 0.0D);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        this.itemRenderer.renderStatic(carried, ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), 0);
        poseStack.popPose();
    }
}
