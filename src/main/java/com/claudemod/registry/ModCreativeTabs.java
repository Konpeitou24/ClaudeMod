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
                        output.accept(ModItems.CHISELED_PRISMIUM_BLOCK_ITEM.get());
                        output.accept(ModItems.PRISMIUM_BLOCK_SLAB_ITEM.get());
                        output.accept(ModItems.PRISMIUM_BLOCK_WALL_ITEM.get());
                        output.accept(ModItems.PRISMIUM_BLOCK_STAIRS_ITEM.get());
                        output.accept(ModItems.PRISMIUM_CORE_SLAB_ITEM.get());
                        output.accept(ModItems.PRISMIUM_CORE_WALL_ITEM.get());
                        output.accept(ModItems.PRISMIUM_CORE_STAIRS_ITEM.get());
                        output.accept(ModItems.CHISELED_PRISMIUM_CORE_ITEM.get());
                        output.accept(ModItems.PRISMIUM_SOIL_ITEM.get());
                        output.accept(ModItems.PRISMIUM_BLOOM_ITEM.get());
                        output.accept(ModItems.PRISMIUM_SPIKE_ITEM.get());
                        output.accept(ModItems.PRISM_LILY_ITEM.get());
                        output.accept(ModItems.PRISM_BRAMBLE_ITEM.get());
                        output.accept(ModItems.PRISM_VINE_ITEM.get());
                        output.accept(ModItems.PRISMIUM_LANTERN_ITEM.get());
                        output.accept(ModItems.PRISMIUM_CELL_ITEM.get());
                        output.accept(ModItems.PRISMIUM_GENERATOR_ITEM.get());
                        output.accept(ModItems.PRISMIUM_CABLE_ITEM.get());
                        output.accept(ModItems.PRISMIUM_PYLON_ITEM.get());
                        output.accept(ModItems.PRISMIUM_RESTORER_ITEM.get());
                        output.accept(ModItems.PRISMIUM_WARDSTONE_ITEM.get());
                        output.accept(ModItems.PRISMIUM_SWORD.get());
                        output.accept(ModItems.PRISMIUM_PICKAXE.get());
                        output.accept(ModItems.PRISMIUM_AXE.get());
                        output.accept(ModItems.PRISMIUM_SHOVEL.get());
                        output.accept(ModItems.PRISMIUM_HOE.get());
                        output.accept(ModItems.PRISMIUM_HELMET.get());
                        output.accept(ModItems.PRISMIUM_CHESTPLATE.get());
                        output.accept(ModItems.PRISMIUM_LEGGINGS.get());
                        output.accept(ModItems.PRISMIUM_BOOTS.get());
                        output.accept(ModItems.PRISMIUM_SHIELD.get());
                        output.accept(ModItems.PRISMIUM_BOW.get());
                        output.accept(ModItems.PRISMIUM_GRAPPLING_HOOK.get());
                        output.accept(ModItems.PRISMIUM_WRAITH_SPAWN_EGG.get());
                        output.accept(ModItems.PRISMIUM_RIFT_SHARD.get());
                        output.accept(ModItems.PRISMIUM_LOCATOR.get());
                        output.accept(ModItems.PRISMIUM_GUARDIAN_CHARM.get());
                        output.accept(ModItems.PRISMIUM_FEATHERSTONE.get());
                        output.accept(ModItems.PRISMIUM_EMBERGUARD.get());
                        output.accept(ModItems.PRISMIUM_VITASTONE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
