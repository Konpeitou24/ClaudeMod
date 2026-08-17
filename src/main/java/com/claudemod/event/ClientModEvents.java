package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.client.screen.PrismiumCellScreen;
import com.claudemod.client.screen.PrismiumGeneratorScreen;
import com.claudemod.client.screen.PrismiumPylonScreen;
import com.claudemod.client.screen.PrismiumRestorerScreen;
import com.claudemod.client.screen.PrismiumWardstoneScreen;
import com.claudemod.entity.client.PrismiumWraithRenderer;
import com.claudemod.registry.ModEntities;
import com.claudemod.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
        });
    }
}
