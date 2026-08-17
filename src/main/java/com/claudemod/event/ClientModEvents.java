package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.entity.client.PrismiumWraithRenderer;
import com.claudemod.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mod-bus, client-only (Dist.CLIENT) event registrations. This is the mod's
 * first client-only listener class, needed because entity renderers must
 * only ever be registered on the physical client (registering them on a
 * dedicated server would try to load client-only rendering classes that
 * don't exist there and crash the server).
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRISMIUM_WRAITH.get(), PrismiumWraithRenderer::new);
    }
}
