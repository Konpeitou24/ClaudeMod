package com.claudemod.registry;

import com.claudemod.ClaudeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * ClaudeMod's own Creative inventory tab. All items added by the mod are
 * exposed here (in addition to relevant vanilla tabs where useful).
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ClaudeMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CLAUDEMOD_TAB = CREATIVE_MODE_TABS.register("claudemod_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(ModItems.PRISMIUM_SHARD.get()))
                    .title(Component.translatable("itemGroup.claudemod.claudemod_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.PRISMIUM_SHARD.get());
                        output.accept(ModItems.PRISMIUM_ORE_ITEM.get());
                        output.accept(ModItems.DEEPSLATE_PRISMIUM_ORE_ITEM.get());
                        output.accept(ModItems.PRISMIUM_BLOCK_ITEM.get());
                        output.accept(ModItems.PRISMIUM_CORE_ITEM.get());
                        output.accept(ModItems.PRISMIUM_LANTERN_ITEM.get());
                        output.accept(ModItems.PRISMIUM_CELL_ITEM.get());
                        output.accept(ModItems.PRISMIUM_GENERATOR_ITEM.get());
                        output.accept(ModItems.PRISMIUM_SWORD.get());
                        output.accept(ModItems.PRISMIUM_PICKAXE.get());
                        output.accept(ModItems.PRISMIUM_AXE.get());
                        output.accept(ModItems.PRISMIUM_SHOVEL.get());
                        output.accept(ModItems.PRISMIUM_HOE.get());
                        output.accept(ModItems.PRISMIUM_HELMET.get());
                        output.accept(ModItems.PRISMIUM_CHESTPLATE.get());
                        output.accept(ModItems.PRISMIUM_LEGGINGS.get());
                        output.accept(ModItems.PRISMIUM_BOOTS.get());
                        output.accept(ModItems.PRISMIUM_GRAPPLING_HOOK.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
