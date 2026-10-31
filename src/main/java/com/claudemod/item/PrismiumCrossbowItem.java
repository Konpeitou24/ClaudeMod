package com.claudemod.item;

import com.claudemod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Scheduled session (2026-10-31): Prismium Crossbow, the mod's second
 * ranged weapon after Prismium Bow (see {@link PrismiumBowItem}) and the
 * answer to the long-standing HANDOFF.md option (m) "新しい装備カテゴリ
 * の検討(例: トライデント、クロスボウ)". A trident was explicitly flagged
 * in that same note as risky ("投擲エンティティへの新規{@code @Override}
 * が必要になりやすく...慎重に") because a custom trident would need a new
 * thrown-entity subclass with unverified overrides - exactly the pattern
 * that caused every past build accident in this repo (see PROGRESS.md
 * §1, "未確認のJava APIは必ず出典を確認してから使う"). A crossbow needs
 * none of that: vanilla's own {@link CrossbowItem} already has a public
 * constructor taking only {@code Item.Properties} (confirmed directly
 * against the 1.20.1 Mojang mappings via {@code mappings.dev} this
 * session - no abstract/unimplemented hooks, unlike the uncertainty that
 * bit {@code PrismiumLanternBlock} in session/v0.37.0), so this class can
 * extend it exactly the same low-risk way {@link PrismiumBowItem} extends
 * {@code BowItem}: reuse the entire load/draw/release/ammo-selection
 * pipeline (arrows <em>or</em> firework rockets) for free.
 *
 * <p>Unlike {@code PrismiumBowItem}, this class adds <b>no</b> mechanical
 * gimmick (no override of any shoot/fire hook): {@code CrossbowItem}
 * does not expose a documented per-shot customization point analogous to
 * {@code BowItem#customArrow} (the 1.20.1 mappings.dev page for this
 * class lists no such method), and past sessions have repeatedly been
 * burned by guessing at hooks that "should" exist (see PROGRESS.md §1,
 * the {@code canPlace}/{@code getEntitiesAround} incidents). Rather than
 * guess, this item is a reskin with only the two overrides every other
 * non-Tier/non-ArmorMaterial Prismium gear item already uses safely
 * ({@link #isValidRepairItem}, mirroring {@link PrismiumBowItem}/{@link
 * PrismiumShieldItem}/{@code PrismiumGrapplingHookItem}; and {@link
 * #appendHoverText}, the standard one-line "hold W for details" hint -
 * see {@link TooltipUsageHelper}). durability(560) follows this mod's
 * "higher durability than the vanilla equivalent, no flat power creep"
 * philosophy already used for every other Prismium gear item: vanilla's
 * crossbow caps at 465 uses, so 560 is roughly the same +20% step
 * {@code PrismiumBowItem} (460 vs. vanilla's 384) and {@code
 * PrismiumShieldItem} (420 vs. vanilla's 336) already use.
 *
 * <p>The item-model "pulling"/"pull"/"charged"/"firework" predicate
 * registrations this item needs (vanilla only wires these up for its own
 * {@code Items.CROSSBOW} instance - not generically for every {@code
 * CrossbowItem} subclass, exactly the same per-item gotcha already
 * documented for {@code PrismiumBowItem}'s "pull"/"pulling" and {@code
 * PrismiumShieldItem}'s "blocking" in {@code ClientModEvents}) live in
 * {@code ClientModEvents#registerScreens}, mirroring vanilla's own
 * registration logic for {@code Items.CROSSBOW} (formula confirmed
 * against {@link CrossbowItem}'s confirmed-public static helpers {@code
 * isCharged}/{@code getChargeDuration}/{@code containsChargedProjectile}
 * rather than guessed from scratch).
 *
 * <p><b>Unverified</b> (same standing sandbox limitation as every other
 * piece of equipment in this mod): whether the crossbow actually
 * loads/fires correctly, whether the four item-model override states
 * (standby/pulling/arrow/firework) render the right frame at the right
 * moment, and whether the generic {@code minecraft:item/generated}
 * diagonal-icon convention this mod's other handheld items use (see
 * {@code gen_prismium_bow.py}'s session-36 history) still looks correct
 * for a crossbow's different silhouette - this is flagged in
 * PROGRESS.md §3 as a new, qualitatively different visual risk from the
 * bow (no custom {@code display} block was copied from vanilla's own
 * crossbow model, deliberately, to avoid combining an unverified
 * geometry override with this mod's own texture convention - see the
 * class-level note in {@code gen_prismium_crossbow.py}).
 */
public class PrismiumCrossbowItem extends CrossbowItem {

    public PrismiumCrossbowItem(Item.Properties properties) {
        super(properties);
    }

    // Mirrors PrismiumBowItem/PrismiumShieldItem/PrismiumGrapplingHookItem -
    // unifies repair material across the mod's non-Tier/non-ArmorMaterial
    // durability items.
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.PRISMIUM_SHARD.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(TooltipUsageHelper.usageLine(this.getDescriptionId()));
    }
}
