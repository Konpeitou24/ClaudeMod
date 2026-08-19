package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.client.ModKeyMappings;
import com.claudemod.client.render.PrismRealmEffects;
import com.claudemod.client.screen.PrismiumCellScreen;
import com.claudemod.client.screen.PrismiumGeneratorScreen;
import com.claudemod.client.screen.PrismiumPylonScreen;
import com.claudemod.client.screen.PrismiumRestorerScreen;
import com.claudemod.client.screen.PrismiumWardstoneScreen;
import com.claudemod.entity.client.PrismiumDeepWraithRenderer;
import com.claudemod.entity.client.PrismiumDrifterRenderer;
import com.claudemod.entity.client.PrismiumSentinelRenderer;
import com.claudemod.entity.client.PrismiumWraithRenderer;
import com.claudemod.registry.ModBlocks;
import com.claudemod.registry.ModEntities;
import com.claudemod.registry.ModItems;
import com.claudemod.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Mod-bus, client-only (Dist.CLIENT) event registrations. This is the mod's
 * first client-only listener class, needed because entity renderers must
 * only ever be registered on the physical client (registering them on a
 * dedicated server would try to load client-only rendering classes that
 * don't exist there and crash the server).
 *
 * Session 23 adds {@link #registerScreens}, the same reasoning applied to
 * {@code Screen}s: {@link MenuScreens#register} touches client-only
 * rendering classes (Screen/AbstractContainerScreen), so it must run here
 * rather than in {@link ClaudeMod}'s common constructor.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRISMIUM_WRAITH.get(), PrismiumWraithRenderer::new);
        // Session 47: Prismium Deep Wraith, same registration pattern.
        event.registerEntityRenderer(ModEntities.PRISMIUM_DEEP_WRAITH.get(), PrismiumDeepWraithRenderer::new);
        // Third mob (see PrismiumSentinelEntity's javadoc), same pattern.
        event.registerEntityRenderer(ModEntities.PRISMIUM_SENTINEL.get(), PrismiumSentinelRenderer::new);
        // Fourth mob, first non-combat/environmental entity (see
        // PrismiumDrifterEntity's javadoc) - same registration pattern.
        event.registerEntityRenderer(ModEntities.PRISMIUM_DRIFTER.get(), PrismiumDrifterRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // Session 60: this mod's first KeyMapping, see ModKeyMappings's
        // javadoc for why (Issue #7 follow-up). API shape (this event
        // fires on the mod bus, client logical side only, and simply
        // wants event.register(mapping)) confirmed this session against
        // Forge's own RegisterKeyMappingsEvent source (fetched via
        // WebSearch/github.com), matching the registerRenderers/
        // registerDimensionEffects pattern already used in this class.
        event.register(ModKeyMappings.SHOW_ITEM_DETAILS);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        // Session 50: give Prism Realm its own DimensionSpecialEffects
        // instead of reusing "minecraft:overworld" (see PrismRealmEffects
        // javadoc for the full investigation/citations). The
        // ResourceLocation key here must match the "effects" value in
        // data/claudemod/dimension_type/prism_realm_type.json exactly.
        event.register(new ResourceLocation(ClaudeMod.MOD_ID, "prism_realm"), new PrismRealmEffects());
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        // MenuScreens#register is not thread-safe (Forge docs, "Screens"
        // page, 1.20.x), so it must be deferred via enqueueWork rather
        // than called directly on this parallel-dispatched event.
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PRISMIUM_CELL_MENU.get(), PrismiumCellScreen::new);
            // Session 24: second screen registration, same call.
            MenuScreens.register(ModMenuTypes.PRISMIUM_GENERATOR_MENU.get(), PrismiumGeneratorScreen::new);
            // Session 25: third screen registration, same call.
            MenuScreens.register(ModMenuTypes.PRISMIUM_PYLON_MENU.get(), PrismiumPylonScreen::new);
            // Session 26: fourth screen registration, same call.
            MenuScreens.register(ModMenuTypes.PRISMIUM_RESTORER_MENU.get(), PrismiumRestorerScreen::new);
            // Session 27: fifth screen registration, same call - all five
            // energy blocks in the mod now have a GUI.
            MenuScreens.register(ModMenuTypes.PRISMIUM_WARDSTONE_MENU.get(), PrismiumWardstoneScreen::new);

            // Session 29: "pull"/"pulling" item-model property overrides
            // for Prismium Bow, mirroring vanilla's own registration for
            // Items.BOW (ItemProperties class, not automatically applied
            // to BowItem subclasses - each bow-like item must register
            // its own predicates). "pulling" flips to 1 while the player
            // is actively drawing this exact stack; "pull" reports draw
            // progress 0.0-1.0 over the item's use duration, which the
            // three predicate thresholds in prismium_bow.json's
            // "overrides" list (0, 0.65, 0.9) key off of to pick the
            // pulling_0/1/2 frame - the same threshold values vanilla's
            // bow.json uses, kept identical here since they are simply
            // draw-progress fractions, not vanilla-specific numbers.
            ItemProperties.register(ModItems.PRISMIUM_BOW.get(), new ResourceLocation("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null) {
                            return 0.0F;
                        } else {
                            return entity.getUseItem() != stack ? 0.0F
                                    : (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
                        }
                    });
            ItemProperties.register(ModItems.PRISMIUM_BOW.get(), new ResourceLocation("pulling"),
                    (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);

            // Session 47 / GitHub issue #6 follow-up: prismium_shield.json's
            // "overrides" list already switches to prismium_shield_blocking.json
            // when the "blocking" property equals 1 (added session 38, commit
            // 8d35154), but nothing ever registered an ItemPropertyFunction for
            // that property id on this item - "blocking" is not a built-in
            // generic predicate, vanilla only wires it up for Items.SHIELD
            // itself (ItemProperties handles this per-item, exactly like
            // "pull"/"pulling" above are per-item for bows, not automatic for
            // every BowItem subclass). Without this registration the override
            // could never fire. Mirrors LivingEntity#isBlocking's own check
            // (getUseItem().getUseAnimation() == UseAnim.BLOCK) so it flips to
            // 1 exactly while this exact stack is actively being used to block.
            ItemProperties.register(ModItems.PRISMIUM_SHIELD.get(), new ResourceLocation("blocking"),
                    (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);

            // Session 52: Prismium Portal (see PrismiumPortalBlock's class
            // doc) needs a translucent render type instead of the default
            // opaque cutout every other block in this mod uses so far -
            // otherwise its semi-transparent texture would render with
            // alpha treated as fully opaque/hard-cutout, looking wrong.
            // ItemBlockRenderTypes#setRenderLayer is the standard Forge
            // 1.20.1 mechanism for this (mirrors vanilla's own client init
            // call for Blocks.NETHER_PORTAL -> RenderType.translucent()),
            // and like MenuScreens#register above touches client-only
            // rendering state, so it is likewise deferred via enqueueWork
            // rather than called directly on this parallel-dispatched event.
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.PRISMIUM_PORTAL.get(), net.minecraft.client.renderer.RenderType.translucent());
        });
    }
}
