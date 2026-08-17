package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import com.claudemod.menu.PrismiumCellMenu;
import com.claudemod.menu.PrismiumGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for every {@link MenuType} added by ClaudeMod.
 *
 * Session 23 adds the mod's first GUI (see PROGRESS.md section 5, "GUIの
 * 導入" - the last big open item after the energy pillar's storage/
 * generation/transport/consumption blocks were all in place, session 22).
 * Prismium Cell was chosen as the first block to get a GUI because it is
 * the simplest energy block in the mod (a passive buffer with no tick,
 * see {@link com.claudemod.blockentity.PrismiumCellBlockEntity}) - a
 * minimal "just show the energy bar" screen establishes the
 * Menu/MenuType/Screen pattern without also having to reason about
 * per-tick state changes while writing it.
 *
 * {@link IForgeMenuType#create} is Forge's helper for building a
 * {@link MenuType} from an {@code (windowId, inventory, extraData) ->
 * menu} factory without hand-writing an {@code IContainerFactory}
 * anonymous class - confirmed as the standard 1.20.1 pattern against the
 * version-pinned docs.minecraftforge.net/en/1.20.1/gui/menus/ page while
 * implementing this (not the generic "1.20.x"/"latest" docs branches,
 * which for some related APIs - see
 * {@link com.claudemod.block.PrismiumCellBlock#use} - document a
 * 1.20.2+-only replacement that does not exist in this mod's pinned
 * Forge version, 47.4.0).
 */
public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ClaudeMod.MOD_ID);

    public static final RegistryObject<MenuType<PrismiumCellMenu>> PRISMIUM_CELL_MENU =
            MENU_TYPES.register("prismium_cell", () -> IForgeMenuType.create((windowId, inv, extraData) -> {
                BlockPos pos = extraData.readBlockPos();
                return new PrismiumCellMenu(windowId, inv, pos);
            }));

    /** Session 24: the mod's second GUI, see {@link PrismiumGeneratorMenu}
     * for why Generator was picked next and what's different from Cell's
     * menu. */
    public static final RegistryObject<MenuType<PrismiumGeneratorMenu>> PRISMIUM_GENERATOR_MENU =
            MENU_TYPES.register("prismium_generator", () -> IForgeMenuType.create((windowId, inv, extraData) -> {
                BlockPos pos = extraData.readBlockPos();
                return new PrismiumGeneratorMenu(windowId, inv, pos);
            }));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
