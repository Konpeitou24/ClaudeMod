package com.claudemod.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Session (scheduled, 2026-08-31 direct-chat feedback item TODO3 - see
 * PROGRESS.md "2. TODO" #3): fixes the "プリズミウム・コンペンディウムが
 * 右クリックで開けない" bug reported in-game against v0.33.1.
 *
 * <p><b>Root cause (confirmed via WebSearch this session, a well-known
 * Forge modding gotcha):</b> {@link ModItems#PRISMIUM_COMPENDIUM} used to
 * be registered as a bare {@code new WrittenBookItem(...)}. Vanilla's
 * {@code WrittenBookItem#use} defers the actual "open the book GUI" work
 * to {@code Player#openItemGui}, whose client-side override
 * ({@code LocalPlayer#openItemGui}) decides whether to open
 * {@code BookViewScreen} by checking {@code stack.is(Items.WRITTEN_BOOK)} -
 * an *identity* check against vanilla's own registered
 * {@code minecraft:written_book} item, not an {@code instanceof
 * WrittenBookItem} type check. Because this mod's compendium is a
 * different, separately-registered item that merely happens to *extend*
 * {@code WrittenBookItem} for its NBT/page-format convenience (see
 * {@link PrismiumCompendiumFactory}'s class doc), that identity check
 * always failed, so {@code openItemGui} silently did nothing at all -
 * matching the reported symptom exactly ("そもそも右クリックで開く基本動作
 * をしていない").
 *
 * <p><b>Fix</b>: stop relying on {@code Player#openItemGui}'s vanilla
 * item-identity gate entirely. This subclass overrides {@link #use} to
 * open {@code BookViewScreen} directly (client-side only, mirroring
 * exactly what {@code LocalPlayer#openItemGui} would have done for a real
 * {@code minecraft:written_book}) regardless of this item's own identity.
 * {@link #openBookScreen} is annotated {@link OnlyIn}(Dist.CLIENT) and
 * only ever invoked from behind {@code level.isClientSide()}, so a
 * dedicated server (which never takes that branch) never needs to resolve
 * {@code Minecraft}/{@code BookViewScreen} - same pattern already used
 * elsewhere in this mod for client-only work reached from common code
 * (see {@code ClientModEvents}'s class doc for the equivalent reasoning
 * applied to renderer/screen *registration* rather than a per-item {@code
 * use} override).
 *
 * <p>{@code BookViewScreen}/{@code BookViewScreen.WrittenBookAccess}'s
 * shape ({@code public BookViewScreen(BookViewScreen.BookAccess)},
 * {@code public WrittenBookAccess(ItemStack)}) confirmed this session via
 * WebFetch against Forge's own 1.18.2 javadoc mirror (1.20.1's own is not
 * hosted there, but this exact GUI class predates 1.18 and was not touched
 * by the "components" NBT rewrite that only landed in 1.20.5+ - see
 * {@link PrismiumCompendiumFactory}'s own class doc, which already
 * verified that boundary for this mod's NBT format).
 *
 * <p><b>Unverified in-game</b> (no Minecraft client in this sandbox, see
 * PROGRESS.md standing note): that the book actually renders/paginates
 * correctly once opened this way - only the "does right-click open
 * anything at all" symptom is addressed here, since that was the entire
 * reported failure (the book never got far enough to test page rendering).
 */
public class PrismiumCompendiumItem extends WrittenBookItem {

    public PrismiumCompendiumItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            openBookScreen(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private static void openBookScreen(ItemStack stack) {
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(stack)));
    }
}
