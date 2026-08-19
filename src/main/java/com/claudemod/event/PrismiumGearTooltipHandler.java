package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.item.ModArmorMaterials;
import com.claudemod.item.TooltipUsageHelper;
import com.claudemod.registry.ModItems;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * GitHub issue #7 ("MODについて、ゲーム内で知ることができない" - none of the
 * mod's items explain themselves in-game, unlike CreateMod's more
 * approachable UX). Session 45 already added a one-line gray usage hint to
 * the six Prismium Energy blocks (via {@link com.claudemod.item.
 * EnergyStorageBlockItem#appendHoverText}) and to Rift Shard/Rift Anchor,
 * and this scheduled session extended the same pattern directly onto every
 * other custom {@code Item} subclass the mod has (Featherstone, Emberguard,
 * Vitastone, Guardian Charm, Grappling Hook, Locator, Shield, Bow - see
 * each of those classes' own {@code appendHoverText} override).
 *
 * <p>The nine items this class covers - the five Prismium tools
 * (Pickaxe/Axe/Shovel/Hoe/Sword) and four armor pieces
 * (Helmet/Chestplate/Leggings/Boots) - are different: they are plain
 * vanilla {@code PickaxeItem}/{@code AxeItem}/.../{@code ArmorItem}
 * instances constructed directly in {@link ModItems} rather than
 * dedicated subclasses (see that file), so there is no mod-owned
 * {@code appendHoverText} to override without adding nine new
 * near-empty subclasses purely to carry one tooltip line each - a lot
 * of registry churn for a purely cosmetic addition. A single
 * {@link ItemTooltipEvent} listener checking each stack's item against
 * the known set does the same job in one file instead.
 *
 * <p>API note: {@code ItemTooltipEvent} (confirmed via WebSearch this
 * session against Forge's own javadoc, which shows the same
 * {@code getItemStack()}/{@code getToolTip()} shape from 1.16.5 through
 * the 1.20.6 NeoForge fork with no breaking change in between - this
 * mod targets 1.20.1, safely inside that unbroken range) fires from
 * {@code ItemStack#getTooltipLines}, i.e. only where a client actually
 * renders a tooltip - it is never posted on a dedicated server, so no
 * client/server side-check is needed the way {@link ArmorSetBonusHandler}
 * needs one for its every-tick {@code TickEvent.PlayerTickEvent}. Lines
 * are appended after vanilla's own (durability, enchantments, etc.),
 * matching where the existing per-class {@code appendHoverText} hints
 * already place theirs (see e.g. {@code EnergyStorageBlockItem}).
 *
 * <p>The five tool gimmicks' exact numbers (bonus-drop chances, glow
 * chance) are read directly from {@link PrismiumMiningHandler} and
 * {@link PrismiumSwordHandler}'s own constants rather than re-guessed
 * here, so if a future session retunes those constants this class's
 * hint text also needs updating to match - noted in PROGRESS.md so it
 * is not forgotten.
 *
 * <p><b>Unverified</b>: no in-game playtest of tooltip rendering itself
 * (line wrapping, whether nine more translation keys collide with
 * anything) - see PROGRESS.md's standing note on this sandbox's lack of
 * a Minecraft client.
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumGearTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        boolean hasUsageHint = item == ModItems.PRISMIUM_PICKAXE.get()
                || item == ModItems.PRISMIUM_AXE.get()
                || item == ModItems.PRISMIUM_SHOVEL.get()
                || item == ModItems.PRISMIUM_HOE.get()
                || item == ModItems.PRISMIUM_SWORD.get()
                || item == ModItems.PRISMIUM_WARHAMMER.get() // session 69
                || (item instanceof ArmorItem armorItem && armorItem.getMaterial() == ModArmorMaterials.PRISMIUM);

        if (hasUsageHint) {
            // Session 60: delegates to TooltipUsageHelper (which appends
            // ".usage" itself) instead of building the translation key
            // and ChatFormatting.GRAY styling inline - see that class's
            // javadoc for why (Issue #7 "hold W for details" follow-up).
            event.getToolTip().add(TooltipUsageHelper.usageLine(item.getDescriptionId(event.getItemStack())));
        }
    }
}
