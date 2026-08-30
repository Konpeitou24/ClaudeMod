package com.claudemod.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Session #80 (scheduled) - soft-dependency bridge to CuriosAPI, added in
 * response to GitHub issue #18 ("チャーム類をCuriosAPIに対応したスロット
 * を付けてほしいです... 存在する場合のみ、という限定を付けてもらって
 * 構いません" - CuriosAPI support for charm items, optional/soft
 * dependency only).
 *
 * <p><b>Why this class exists instead of calling {@link CuriosApi}
 * directly from the event handlers</b>: this class is the ONLY file in
 * the mod that imports {@code top.theillusivec4.curios.*} types. Every
 * call site (see {@link com.claudemod.event.PrismiumMagnetCharmHandler},
 * {@link com.claudemod.event.PrismiumFeatherstoneHandler}, {@link
 * com.claudemod.event.PrismiumEmberguardHandler}, {@link
 * com.claudemod.event.PrismiumVitastoneHandler}) is required to guard
 * every reference to this class behind {@code
 * net.minecraftforge.fml.ModList.get().isLoaded("curios")} first. The
 * JVM only resolves/loads a class's own referenced types when that
 * class is actually linked, which happens on first active use (the
 * first time a static method on it is actually invoked) - so as long as
 * nothing calls into {@code CuriosCompat} when Curios isn't loaded, this
 * class (and therefore the {@code curios-forge} jar it references) is
 * never touched, and the mod loads and runs completely normally with
 * Curios absent. This is the standard "lazy class loading" soft-
 * dependency pattern used broadly across the Forge modding ecosystem
 * (the alternative - reflection for every single call - would be far
 * more error-prone for what is, here, a single one-line boolean check).
 *
 * <p><b>Why only a presence check, no {@code ICurio}/{@code ICurioItem}
 * capability</b>: none of the four charm items this integrates with
 * (Featherstone/Emberguard/Vitastone/Magnet Charm) have any equip-
 * specific behaviour to begin with - per their own javadocs, they were
 * deliberately designed as "just carry it anywhere in your inventory"
 * items with no vanilla equipment slot, and their effects are entirely
 * driven by each item's own {@code TickEvent}/vanilla-event listener
 * class polling "is this item present somewhere". Extending that
 * "somewhere" to also include Curios slots only requires answering
 * "is item X currently equipped in any of the wearer's curio slots",
 * which {@link top.theillusivec4.curios.api.type.capability.
 * ICuriosItemHandler#isEquipped(Item)} already answers directly - there
 * is no need to attach a capability provider via {@code
 * AttachCapabilitiesEvent<ItemStack>} or implement {@code ICurioItem}
 * on the item classes themselves (which would also reintroduce a hard
 * compile-time reference to Curios types on the item classes, breaking
 * the soft-dependency contract). If a future session wants slot-only
 * effects (bonuses that ONLY apply while worn in Curios, not from plain
 * inventory) or curio-specific tooltips/render layers, that would need
 * the capability-provider approach instead - deliberately not attempted
 * this session, see PROGRESS.md.
 *
 * <p><b>Slot type reused, not invented</b>: items are tagged into
 * Curios's own built-in {@code charm} slot type (see
 * {@code data/curios/tags/items/charm.json}) rather than a
 * ClaudeMod-specific slot type. {@code charm} is one of Curios's
 * documented "Frequently Used Slots" (wiki: "miscellaneous items",
 * already used by many other mods, e.g. Botania/Artifacts/Cyclic), and
 * - confirmed this session by reading Curios's own {@code SlotType} /
 * {@code CuriosSlotManager} source (1.20.x branch) - a slot type with no
 * explicit {@code size} in any contributing mod's datapack JSON defaults
 * to size 1 in {@code SlotType.Builder#build()}. Curios's own
 * {@code data/curios/curios/slots/charm.json} does not set a size, so
 * players get exactly one usable {@code charm} slot out of the box with
 * only Curios + ClaudeMod installed, no third mod required, and no
 * separate slot-size datapack file needed on ClaudeMod's side either.
 *
 * <p><b>Not integrated this session (deliberately, see PROGRESS.md)</b>:
 * {@link com.claudemod.item.PrismiumGuardianCharmItem} (explicitly
 * documented as "must be held in hand" - its entire death-save behaviour
 * keys off vanilla's {@code LivingDeathEvent} without a concept of
 * curio slots, and Curios does not forward "held in hand" semantics to
 * equipped curios) and {@link com.claudemod.item.PrismiumPulseCharmItem}
 * (an actively right-clicked item, not a passive-presence one - Curios
 * items are not commonly right-clicked in place the way held items are).
 *
 * <p><b>Unverified</b> (no in-game client in this sandbox, and Curios
 * itself cannot be downloaded here to test against - see PROGRESS.md
 * environment constraints): whether {@code isEquipped(Item)} behaves as
 * documented against an actual running Curios instance; whether the
 * {@code charm} slot actually appears with size 1 in a real Curios GUI
 * for a fresh player with only these two mods installed; and the tag
 * file's effect on Curios's own slot-validity checks (i.e. whether the
 * items becomes drag-and-droppable into the charm slot in practice).
 */
public final class CuriosCompat {

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
}
