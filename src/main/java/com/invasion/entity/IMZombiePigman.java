package com.invasion.entity;

import com.invasion.Config;
import com.invasion.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Zombie pigman invader: T1 and T2 fighters with golden swords, T3 brute that charges. Fire immune. */
public class IMZombiePigman extends InvasionMonster {
    private int chargeCooldown;

    public IMZombiePigman(EntityType<? extends IMZombiePigman> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void applyStats(int tier, int flavour) {
        setBase(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
        switch (tier) {
            case 2 -> setStats(Config.HP_ZOMBIE_PIGMAN_T2.get(), 0.35D * 1.2D, 12);
            case 3 -> {
                setStats(Config.HP_ZOMBIE_PIGMAN_T3.get(), 0.2D * 1.2D, 18);
                setBase(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
            }
            default -> setStats(Config.HP_ZOMBIE_PIGMAN_T1.get(), 0.25D * 1.2D, 8);
        }
        setItemSlot(EquipmentSlot.MAINHAND, tier >= 3 ? ItemStack.EMPTY : new ItemStack(Items.GOLDEN_SWORD));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected int chooseTexture(int tier, int flavour) {
        return tier >= 3 ? 2 : 0;
    }

    @Override
    public float getScaleAmount() {
        return switch (getTier()) {
            case 2 -> 1.12F;
            case 3 -> 1.21F;
            default -> 1.0F;
        };
    }

    public boolean isBrute() {
        return getTier() >= 3;
    }

    @Override
    public boolean canDig() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isBrute()) {
            LivingEntity target = getTarget();
            if (chargeCooldown > 0) {
                chargeCooldown--;
            }
            setSprinting(target != null && chargeCooldown <= 0 && distanceToSqr(target) < 64.0D && distanceToSqr(target) > 9.0D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isBrute() && isSprinting()) {
            boolean hit = target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + 3.0F);
            if (hit && target instanceof LivingEntity living) {
                living.knockback(2.0D, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
                level().playSound(null, target.blockPosition(), SoundEvents.GENERIC_BIG_FALL, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            setSprinting(false);
            chargeCooldown = 100;
            return hit;
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!isBrute()) {
            super.knockback(strength, x, z);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextFloat() < 0.35F) {
            spawnAtLocation(Items.GOLD_NUGGET);
        }
        if (getTier() < 3 && random.nextFloat() < 0.1F) {
            spawnAtLocation(Items.GOLDEN_SWORD);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isBrute()) {
            return random.nextInt(3) == 0 ? ModSounds.BIG_ZOMBIE.get() : null;
        }
        return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
    }
}
