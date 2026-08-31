package com.claudemod.event;

import com.claudemod.ClaudeMod;
import com.claudemod.item.PrismiumCompendiumFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Scheduled session, GitHub issue #7 follow-up: gives every player one
 * Prismium Compendium (see {@link PrismiumCompendiumFactory}) the first
 * time they ever log into a world with this mod installed, so the "how do
 * I use any of this?" answer is placed directly in the player's hands
 * instead of relying on them to already know a guide book exists (a
 * creative-tab-only item would still leave a fresh survival player with
 * nothing - see {@code ModCreativeTabs}, which also lists the compendium,
 * but that alone does not help a survival player who never opens creative
 * inventory).
 *
 * <p><b>API confirmed this session</b> (WebSearch against Forge's own
 * 1.20.1-branch source, since - unlike vanilla classes - Forge's own
 * event classes are not covered by the mojmap mirror this mod otherwise
 * uses): {@code PlayerEvent.PlayerLoggedInEvent} is a plain, uncancelable
 * event exposing only the inherited {@code getEntity()}, fired on
 * {@code MinecraftForge.EVENT_BUS} - matching every other Forge event
 * this mod already listens to via {@code @Mod.EventBusSubscriber}'s
 * default bus. Guarded by {@code !level.isClientSide} and an
 * {@code instanceof ServerPlayer} check (same defensive pattern
 * {@code PrismiumChronoflameBlock#use} already uses for
 * {@code ServerLevel}) since this event's {@code getEntity()} returns the
 * shared {@code Player} supertype and this handler must only ever run
 * once, server-authoritatively, not once per side.
 *
 * <p><b>One-time only</b>: tracked via a boolean flag in
 * {@link Player#getPersistentData()} (same general-purpose per-player NBT
 * mechanism {@code PrismiumTeleportHelper}'s Prism Realm return-point
 * memory already uses), so a player who already has - or has since lost -
 * their starting compendium is never handed a second one automatically;
 * they can still get another from the creative inventory (single-player/
 * OP) or a future crafting recipe (not added this session, see
 * PROGRESS.md).
 *
 * <p>If the player's inventory happens to be full at the exact moment
 * they first log in, {@link net.minecraft.world.entity.player.Inventory#add}
 * drops the leftover stack at the player's feet instead of silently
 * discarding it (standard vanilla behavior for that method - see e.g.
 * how vanilla's own starting-inventory/loot-table give-item paths already
 * rely on the same fallback), so this can never quietly eat the book.
 *
 * <p><b>Unverified in-game</b> (no Minecraft client in this sandbox, see
 * PROGRESS.md standing note): whether this event fires early enough in
 * the login sequence that adding an item here is safe (no other mod in
 * this pack was audited for a conflicting assumption about inventory
 * state at this exact event).
 */
@Mod.EventBusSubscriber(modid = ClaudeMod.MOD_ID)
public class PrismiumCompendiumHandler {

    private static final String GIVEN_FLAG_KEY = "claudemod_given_compendium";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        CompoundTag persisted = player.getPersistentData();
        if (persisted.getBoolean(GIVEN_FLAG_KEY)) {
            return;
        }
        persisted.putBoolean(GIVEN_FLAG_KEY, true);

        ItemStack compendium = PrismiumCompendiumFactory.createStack();
        if (!player.getInventory().add(compendium)) {
            player.drop(compendium, false);
        }
    }
}
