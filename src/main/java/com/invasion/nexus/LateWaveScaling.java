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
    private static final UUID STEP_HEALTH_ID = UUID.fromString("3c8f2a91-6d4e-4b7a-a1c5-0e9d8f7b6a52");
    private static final UUID STEP_DAMAGE_ID = UUID.fromString("9b2e7d41-1a6c-4f3e-8d5b-7c4a2e1f9d63");
    private static final UUID STEP_SPEED_ID = UUID.fromString("5e1d9c72-8b3a-4d6f-9e2c-1a7b5d3f8e94");

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

    public static boolean isSpellWave(int wave) {
        int n = Config.SPELL_WAVE_INTERVAL.get();
        return n > 0 && wave > 0 && wave % n == 0;
    }

    public static boolean isGravityWave(int wave) {
        int n = Config.GRAVITY_WAVE_INTERVAL.get();
        return n > 0 && wave > 0 && wave % n == 0;
    }

    public static boolean isForeignWave(int wave) {
        int n = Config.FOREIGN_WAVE_INTERVAL.get();
        return n > 0 && wave > 0 && wave % n == 0;
    }

    /** Power steps from wave 100 on (every 5 waves by default). */
    public static int powerSteps(int wave) {
        int start = Config.POWER_STEP_START.get();
        return wave < start ? 0 : (wave - start) / Config.POWER_STEP_INTERVAL.get() + 1;
    }

    public static boolean isPowerStepWave(int wave) {
        int start = Config.POWER_STEP_START.get();
        return wave >= start && (wave - start) % Config.POWER_STEP_INTERVAL.get() == 0;
    }

    public static void apply(Mob mob, int wave, RandomSource random) {
        if (wave <= 0) {
            return;
        }
        boolean late = isLate(wave);
        if (late) {
            applyLate(mob, wave, random);
        }
        applyPowerSteps(mob, wave);
        if (random.nextDouble() < (late ? Config.WARD_CHANCE_LATE.get() : Config.WARD_CHANCE.get())) {
            com.invasion.compat.MagicWard.ward(mob);
        }
        boolean caster = isSpellWave(wave) || (late && random.nextDouble() < Config.SPELL_CASTER_CHANCE.get());
        if (caster && IronsSpellsCompat.isLoaded()) {
            IronsSpellsCompat.makeCaster(mob);
        }
    }

    private static void applyPowerSteps(Mob mob, int wave) {
        int steps = powerSteps(wave);
        if (steps <= 0) {
            return;
        }
        multiply(mob.getAttribute(Attributes.MAX_HEALTH), STEP_HEALTH_ID, "Power step health", Config.POWER_STEP_HEALTH.get() * steps);
        multiply(mob.getAttribute(Attributes.ATTACK_DAMAGE), STEP_DAMAGE_ID, "Power step damage", Config.POWER_STEP_DAMAGE.get() * steps);
        multiply(mob.getAttribute(Attributes.MOVEMENT_SPEED), STEP_SPEED_ID, "Power step speed",
                Math.min(0.5D, Config.POWER_STEP_SPEED.get() * steps));
        mob.setHealth(mob.getMaxHealth());
        if (steps >= 4) {
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                    net.minecraft.world.effect.MobEffectInstance.INFINITE_DURATION, Math.min(2, steps / 4 - 1), false, false));
        }
    }

    private static void applyLate(Mob mob, int wave, RandomSource random) {
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
