package com.claudemod.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Optional;

/**
 * Direct-chat session (following scheduled session #80, v0.30.0) - soft-dependency bridge to
 * CuriosAPI, added in response to GitHub issue #18 ("チャーム類を
 * CuriosAPIに対応したスロットを付けてほしいです... 存在する場合のみ、
 * という限定を付けてもらって構いません" - CuriosAPI support for charm
 * items, optional/soft dependency only).
 *
 * <p><b>Why this class exists instead of calling {@link CuriosApi}
 * directly from the event handlers</b>: this class is the ONLY file in
 * the mod that imports {@code top.theillusivec4.curios.*} types. Every
 * call site (see {@link com.claudemod.event.PrismiumMagnetCharmHandler},
 * {@link com.claudemod.event.PrismiumFeatherstoneHandler}, {@link
 * com.claudemod.event.PrismiumEmberguardHandler}, {@link
 * com.claudemod.event.PrismiumVitastoneHandler}, {@link
 * com.claudemod.event.PrismiumGuardianCharmHandler}, {@link
 * CuriosSetupEvents}) is required to guard every reference to this class
 * behind {@code net.minecraftforge.fml.ModList.get().isLoaded("curios")}
 * first. The JVM only resolves/loads a class's own referenced types when
 * that class is actually linked, which happens on first active use (the
 * first time a static method on it is actually invoked) - so as long as
 * nothing calls into {@code CuriosCompat} when Curios isn't loaded, this
 * class (and therefore the {@code curios-forge} jar it references) is
 * never touched, and the mod loads and runs completely normally with
 * Curios absent. This is the standard "lazy class loading" soft-
 * dependency pattern used broadly across the Forge modding ecosystem.
 *
 * <p><b>Two separate integration styles used here</b>:
 * <ul>
 *   <li>Featherstone/Emberguard/Vitastone/Magnet Charm only need a
 *   presence check ("is item X equipped in any curio slot right now") -
 *   {@link #isEquippedInCurioSlot(LivingEntity, Item)} answers that
 *   directly via {@link top.theillusivec4.curios.api.type.capability.
 *   ICuriosItemHandler#isEquipped(Item)}, no capability needed, since
 *   these items have no equip-specific behaviour to begin with (see
 *   each item's own javadoc).</li>
 *   <li>Guardian Charm (added in a direct-chat session after v0.30.0) is consumed
 *   (shrunk by 1) on save, so its handler needs the actual live
 *   {@link ItemStack} reference sitting in the curio slot, not just a
 *   boolean - {@link #findEquippedCurioStack(LivingEntity, Item)}
 *   returns that via {@code ICuriosItemHandler#findFirstCurio(Item)}.</li>
 * </ul>
 *
 * <p><b>Right-click-to-equip (added in a direct-chat session after v0.30.0)</b>: merely tagging an
 * item into {@code data/curios/tags/items/charm.json} does NOT make
 * Curios's own right-click-to-equip feature work for it - confirmed this
 * session by reading Curios's actual {@code CuriosEventHandler
 * #curioRightClick} listener (1.20.x branch): it only proceeds if
 * {@code CuriosApi.getCurio(stack)} resolves a real {@code ICurio}
 * capability on the clicked stack, and even then {@code
 * ICurio#canEquipFromUse}/{@code ICurioItem#canEquipFromUse} both
 * default to {@code false} (via the deprecated {@code
 * canRightClickEquip()}/{@code canRightClickEquip(ItemStack)} they
 * delegate to). So an item with no capability attached at all is
 * invisible to that listener no matter what tags it has. {@link
 * #enableRightClickEquip(Item)} attaches a minimal {@link ICurioItem}
 * (via {@link CuriosApi#registerCurio(Item, ICurioItem)}, which per its
 * own javadoc is additive and does not disturb any {@code ICurio}
 * capability an item might separately get from {@code
 * Item#initCapabilities} - not used anywhere in this mod, so there is no
 * conflict) that overrides only {@code canEquipFromUse} to {@code true},
 * leaving every other behaviour (tick/equip/unequip/attribute
 * modifiers/etc.) at Curios's own defaults. This is called for the same
 * five items as the presence/consume checks above, and deliberately NOT
 * for {@link com.claudemod.item.PrismiumPulseCharmItem} (its right click
 * is already its own active ability - letting Curios intercept that
 * click to equip it instead would silently break that ability the
 * moment Curios is installed, so Pulse Charm stays untagged and
 * uninvolved with Curios entirely).
 *
 * <p><b>Slot type reused, not invented</b>: items are tagged into
 * Curios's own built-in {@code charm} slot type (see
 * {@code data/curios/tags/items/charm.json}) rather than a
 * ClaudeMod-specific slot type. {@code charm} is one of Curios's
 * documented "Frequently Used Slots" (wiki: "miscellaneous items",
 * already used by many other mods, e.g. Botania/Artifacts/Cyclic).
 *
 * <p><b>Correction (direct chat, after v0.30.1 shipped)</b>: v0.30.0's
 * original javadoc here incorrectly claimed that tagging items into
 * {@code charm} plus that slot type's default size of 1 (confirmed via
 * {@code SlotType.Builder#build()}) was sufficient for players to
 * actually have a usable {@code charm} slot. This was wrong, and
 * こんぺいとう氏 caught it by actually testing with only Curios
 * installed: a registered slot type is NOT automatically granted to any
 * entity. Confirmed this session against Curios's actual {@code
 * CuriosEntityManager} source (1.20.x branch, reads {@code
 * data/(namespace)/curios/entities/*.json}) and the current dev docs
 * ("Entity Slot Types" page: "Registered slot types will all be
 * available for use but will not appear in-game until they are added to
 * one or more entities."): a slot type must be separately granted to an
 * entity type via that datapack file. ClaudeMod now ships {@code
 * data/claudemod/curios/entities/player.json} granting {@code charm} to
 * {@code player}, which is what actually gives players the slot - the
 * size-1 default only matters once a slot is granted at all.
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, and Curios
 * itself cannot be downloaded here to test against - see PROGRESS.md
 * environment constraints): whether any of the above actually behaves as
 * documented against a real running Curios instance - in particular,
 * whether right-click-to-equip actually triggers from the hotbar as
 * expected, whether the swap-with-occupied-slot path (see {@code
 * CuriosEventHandler#curioRightClick}) behaves sensibly for these
 * stackable (not {@code stacksTo(1)}) items, and whether {@link
 * ItemStack#shrink(int)} on the reference returned by {@link
 * #findEquippedCurioStack} actually mutates the real backing curio slot
 * (expected, since Curios's own internal listeners - e.g. its mending
 * handler - mutate {@code IDynamicStackHandler} stacks the same way, but
 * not directly confirmed against a live instance).
 */
public final class CuriosCompat {

    /**
     * Shared {@link ICurioItem} instance used by {@link
     * #enableRightClickEquip(Item)}. Every default method is left as-is
     * except {@code canEquipFromUse}, which both {@code ICurio} and
     * {@code ICurioItem} default to {@code false} for (Curios does not
     * enable right-click-equip unless a curio explicitly opts in).
     */
    private static final ICurioItem RIGHT_CLICK_EQUIP_CURIO = new ICurioItem() {
        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }
    };

    private CuriosCompat() {
    }

    /**
     * Checks whether the given item is currently equipped in any of the
     * entity's Curios slots. Callers MUST guard every call to this method
     * behind {@code net.minecraftforge.fml.ModList.get().isLoaded("curios")}
     * - see this class's javadoc for why.
     */
    public static boolean isEquippedInCurioSlot(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(item))
                .orElse(false);
    }

    /**
     * Returns the live {@link ItemStack} reference for the given item if
     * it is currently equipped in one of the entity's Curios slots, so
     * callers can mutate it in place (e.g. {@link ItemStack#shrink(int)}
     * to consume it) the same way they would a stack returned by {@link
     * LivingEntity#getItemInHand}. Callers MUST guard every call to this
     * method behind {@code
     * net.minecraftforge.fml.ModList.get().isLoaded("curios")} - see this
     * class's javadoc for why.
     */
    public static Optional<ItemStack> findEquippedCurioStack(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .resolve()
                .flatMap(handler -> handler.findFirstCurio(item))
                .map(result -> result.stack());
    }

    /**
     * Attaches a minimal {@link ICurioItem} to the given item so that
     * Curios's own right-click-to-equip feature (see {@code
     * CuriosEventHandler#curioRightClick}) recognizes it - without this,
     * merely tagging the item into a curio slot type is not enough (see
     * this class's javadoc). Intended to be called once per applicable
     * item during {@code FMLCommonSetupEvent} - see {@link
     * CuriosSetupEvents}. Callers MUST guard every call to this method
     * behind {@code net.minecraftforge.fml.ModList.get().isLoaded("curios")}
     * - see this class's javadoc for why.
     */
    public static void enableRightClickEquip(Item item) {
        CuriosApi.registerCurio(item, RIGHT_CLICK_EQUIP_CURIO);
    }
}
