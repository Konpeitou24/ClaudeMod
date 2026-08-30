package com.claudemod.compat.curios;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Direct-chat session (after v0.30.0) - mod-bus setup listener that enables Curios's
 * right-click-to-equip behaviour (see {@link CuriosCompat#enableRightClickEquip})
 * for every ClaudeMod item that is tagged into a Curios slot AND has no
 * conflicting active right-click use of its own.
 *
 * <p>This class deliberately does NOT import any {@code
 * top.theillusivec4.curios.*} type - it only ever calls into {@link
 * CuriosCompat} (the sole file that does) after confirming {@link
 * ModList#isLoaded(String)} is true, preserving the lazy-classload
 * soft-dependency guarantee described in {@link CuriosCompat}'s javadoc.
 *
 * <p>Runs during {@code FMLCommonSetupEvent} (wrapped in {@code
 * event.enqueueWork(...)}, the conventional way to defer cross-mod
 * registration calls to after parallel mod construction finishes) since
 * this is a one-time, order-independent registration rather than
 * something that needs to happen earlier during registry population.
 *
 * <p>Deliberately excludes {@link
 * com.claudemod.item.PrismiumPulseCharmItem} - see {@link
 * CuriosCompat}'s javadoc for why right-click-to-equip and an item's own
 * active right-click ability cannot coexist.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CuriosSetupEvents {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        event.enqueueWork(() -> {
            CuriosCompat.enableRightClickEquip(ModItems.PRISMIUM_FEATHERSTONE.get());
            CuriosCompat.enableRightClickEquip(ModItems.PRISMIUM_EMBERGUARD.get());
            CuriosCompat.enableRightClickEquip(ModItems.PRISMIUM_VITASTONE.get());
            CuriosCompat.enableRightClickEquip(ModItems.PRISMIUM_MAGNET_CHARM.get());
            CuriosCompat.enableRightClickEquip(ModItems.PRISMIUM_GUARDIAN_CHARM.get());
        });
    }
}
