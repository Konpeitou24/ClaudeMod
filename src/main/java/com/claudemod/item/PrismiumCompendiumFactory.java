package com.claudemod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Builds the pre-filled {@link ItemStack} for Prismium Compendium
 * (scheduled session, GitHub issue #7 follow-up). See
 * {@code EnergyStorageBlockItem}'s class doc for the original half of
 * issue #7 (per-block usage tooltips, session 38) - that doc explicitly
 * flagged "a full in-game guide book/manual... is a much bigger feature
 * than fits in one session" as the still-missing piece this class exists
 * to close.
 *
 * <p><b>Why a plain vanilla {@code WrittenBookItem} instead of a custom
 * item + custom GUI</b>: a written book already gives free, correct
 * page-turning, word-wrap, and (since pages are stored as serialized text
 * {@link Component}s, not raw strings) automatic client-side
 * localization, all with zero new screen/rendering code. Every other
 * "explain this to the player" surface in this mod
 * ({@code TooltipUsageHelper}, action-bar messages) already leans on
 * vanilla text/localization primitives rather than inventing new UI, so
 * this follows the same instinct at a larger scale.
 *
 * <p><b>NBT format confirmed against 1.20.1</b> (WebSearch this session,
 * cross-checked the still-current minecraft.wiki "Item data" section -
 * which as of 1.20.5+ documents a newer "components"-based format not
 * applicable here - against {@code WrittenBookItem}'s own 1.20.1 mojmap
 * field constants via mappings.dev, since this mod targets 1.20.1
 * specifically): the item's tag compound uses bare top-level keys
 * {@code author} (String), {@code pages} (ListTag of StringTag, each
 * either plain text or - as used here - a JSON-serialized
 * {@link Component}, matching {@code WrittenBookItem.TAG_PAGES}), and
 * {@code resolved} (boolean; set {@code true} here since this book's
 * pages contain no entity-selector/score placeholders that would ever
 * need server-side resolution - see {@code WrittenBookItem.TAG_RESOLVED}).
 * Deliberately does *not* set a {@code title} tag: leaving it unset (per
 * the wiki's own note, an empty title tag "does not override the base
 * item name") lets the item's ordinary translated display name
 * ({@code item.claudemod.prismium_compendium}, see en_us.json/ja_jp.json)
 * keep working normally in both locales, instead of baking in one
 * hardcoded, unlocalized title string the way a real signed book would.
 *
 * <p>Each page is a single {@link Component#translatable} pointing at a
 * {@code book.claudemod.compendium.pageN} lang key, so - unlike a title,
 * which the format only supports as a raw string - the book's actual
 * body text *does* re-localize per viewer, exactly like every tooltip
 * and action-bar message elsewhere in this mod.
 *
 * <p><b>Unverified in-game</b> (no Minecraft client in this sandbox, see
 * PROGRESS.md's standing note): whether each page's chosen line lengths
 * actually fit the book GUI's rendering area without overflowing past the
 * bottom of the page at either locale's font metrics - Japanese full-
 * width glyphs in particular render wider per character than the Latin
 * alphabet, so page text here was kept deliberately short (roughly
 * 90-130 Japanese characters per page) as a first-guess safety margin,
 * not a measured one.
 */
public final class PrismiumCompendiumFactory {

    private PrismiumCompendiumFactory() {
    }

    /** In-universe "author" shown in the item's tooltip - this mod's own
     * name, since the book is written about ClaudeMod's own systems
     * rather than by any particular player character. */
    private static final String AUTHOR = "ClaudeMod";

    /** Lang keys are book.claudemod.compendium.page1 .. PAGE_COUNT,
     * covering (in order): intro, Prismium resource line, energy system
     * overview, generator/cell usage, cable networks, the three
     * processing machines, equipment overview, the passive/active charm
     * family, Prism Realm access, hostile mobs, and misc utility items -
     * see PROGRESS.md section 5 for the same grouping applied to the
     * mod's own roadmap. */
    private static final int PAGE_COUNT = 11;

    public static ItemStack createStack() {
        ItemStack stack = new ItemStack(com.claudemod.registry.ModItems.PRISMIUM_COMPENDIUM.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("author", AUTHOR);
        tag.putBoolean("resolved", true);

        ListTag pages = new ListTag();
        for (int i = 1; i <= PAGE_COUNT; i++) {
            Component page = Component.translatable("book.claudemod.compendium.page" + i);
            pages.add(StringTag.valueOf(Component.Serializer.toJson(page)));
        }
        tag.put("pages", pages);

        return stack;
    }
}
