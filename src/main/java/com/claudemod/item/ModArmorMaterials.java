package com.claudemod.item;

import com.claudemod.ClaudeMod;
import com.claudemod.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Custom armor material(s) added by ClaudeMod (session 3).
 *
 * PRISMIUM follows the same "just above diamond, not a raw copy of
 * netherite" philosophy already used for {@link ModToolTiers}: defense
 * values are kept identical to diamond/netherite (no flat armor-value
 * power creep), but durability, toughness, knockback resistance and
 * enchantability are pushed a notch higher, and repair uses Prismium
 * Shard. Base durability array {11,16,16,13} (helmet/chestplate/leggings/
 * boots) matches every vanilla material - only the multiplier differs.
 *
 * Implementation verified against Forge's 1.20.X ArmorMaterial interface
 * shape (getDurabilityForType/getDefenseForType keyed by ArmorItem.Type,
 * ordinal order HELMET,CHESTPLATE,LEGGINGS,BOOTS) - see PROGRESS.md for
 * the source cross-checked during this session. Still not compiled in
 * this sandbox (no Forge/Mojang maven access here); first real build
 * happens on GitHub Actions after push.
 */
public enum ModArmorMaterials implements ArmorMaterial {

    PRISMIUM("prismium", 40, new int[]{3, 8, 6, 3}, 14,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 3.5f, 0.1f,
            () -> Ingredient.of(ModItems.PRISMIUM_SHARD.get()));

    private static final int[] BASE_DURABILITY = {11, 16, 16, 13};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] protectionAmounts, int enchantmentValue,
                       SoundEvent equipSound, float toughness, float knockbackResistance,
                       Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionAmounts = protectionAmounts;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.protectionAmounts[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return ClaudeMod.MOD_ID + ":" + this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
