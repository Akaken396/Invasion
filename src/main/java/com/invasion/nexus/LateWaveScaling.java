package com.invasion.nexus;

import com.invasion.Config;
import com.invasion.compat.IronsSpellsCompat;
import com.invasion.entity.IMSkeleton;
import com.invasion.entity.InvasionMonster;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Late waves (50+ by default): invaders wear armour, carry better weapons, get
 * more health and damage, and some of them cast Iron's Spells 'n Spellbooks spells.
 */
public final class LateWaveScaling {
    private static final UUID HEALTH_ID = UUID.fromString("2d6e1f7a-4b3c-4e59-9a1d-8c7b6e5f4a31");
    private static final UUID DAMAGE_ID = UUID.fromString("7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    private static final Item[][] ARMOUR = {
            {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
            {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
            {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}};
    private static final Item[] SWORDS = {Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD};
    private static final Item[] AXES = {Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static boolean isLate(int wave) {
        return wave >= Config.LATE_WAVE_START.get();
    }

    public static void apply(Mob mob, int wave, RandomSource random) {
        if (!isLate(wave)) {
            return;
        }
        int over = wave - Config.LATE_WAVE_START.get();
        multiply(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "Late wave health",
                Config.LATE_HEALTH_BONUS.get() + Config.LATE_HEALTH_PER_WAVE.get() * over);
        multiply(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, "Late wave damage",
                Config.LATE_DAMAGE_BONUS.get() + Config.LATE_DAMAGE_PER_WAVE.get() * over);
        mob.setHealth(mob.getMaxHealth());

        int tier = over < 20 ? 0 : over < 40 ? 1 : 2;
        if (wearsArmour(mob)) {
            float chance = Math.min(1.0F, 0.6F + over * 0.02F);
            for (int i = 0; i < SLOTS.length; i++) {
                if (mob.getItemBySlot(SLOTS[i]).isEmpty() && random.nextFloat() < chance) {
                    mob.setItemSlot(SLOTS[i], new ItemStack(ARMOUR[tier][i]));
                    mob.setDropChance(SLOTS[i], 0.0F);
                }
            }
            if (!(mob instanceof IMSkeleton) && !(mob instanceof Evoker)) {
                ItemStack held = mob.getMainHandItem();
                if (held.isEmpty() || held.is(Items.WOODEN_SWORD) || held.is(Items.STONE_SWORD) || held.is(Items.IRON_SWORD)
                        || held.is(Items.IRON_AXE) || held.is(Items.GOLDEN_SWORD)) {
                    Item weapon = mob instanceof AbstractIllager ? AXES[tier] : SWORDS[tier];
                    mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                }
            }
        }
        if (random.nextDouble() < Config.SPELL_CASTER_CHANCE.get() && IronsSpellsCompat.isLoaded()) {
            IronsSpellsCompat.makeCaster(mob);
        }
    }

    private static boolean wearsArmour(Mob mob) {
        return mob instanceof InvasionMonster && !(mob instanceof com.invasion.entity.IMThrower) && !(mob instanceof com.invasion.entity.IMImp)
                || mob instanceof IMSkeleton || mob instanceof AbstractIllager;
    }

    private static void multiply(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null || amount <= 0.0D) {
            return;
        }
        attribute.removeModifier(id);
        attribute.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private LateWaveScaling() {
    }
}
