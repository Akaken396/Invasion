package com.invasion.entity;

import com.invasion.Config;
import com.invasion.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Invading zombie, with the tiers and flavours of the original mod:
 * <ul>
 *   <li>T1: plain digger (flavour 0) or wooden-sword fighter (flavour 1)</li>
 *   <li>T2: armoured digger (0), iron-sword fighter (1), tar zombie (2), zombie pigman (3)</li>
 *   <li>T3: zombie brute, a huge charger that cannot be knocked back</li>
 * </ul>
 */
public class IMZombie extends InvasionMonster {
    @Nullable
    private Item itemDrop;
    private float dropChance;
    private int flammability = 3;
    private int chargeCooldown;

    public IMZombie(EntityType<? extends IMZombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 4.0D).add(Attributes.MOVEMENT_SPEED, 0.228D);
    }

    @Override
    protected void applyStats(int tier, int flavour) {
        itemDrop = null;
        dropChance = 0.0F;
        flammability = 3;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setBase(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
        if (tier >= 3) {
            setStats(Config.HP_ZOMBIE_T3.get(), 0.17D * 1.2D, 18);
            setBase(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
            flammability = 4;
        } else if (tier == 2) {
            switch (flavour) {
                case 1 -> {
                    setStats(Config.HP_ZOMBIE_T2.get(), 0.228D, 10);
                    hold(Items.IRON_SWORD);
                    itemDrop = Items.IRON_SWORD;
                    dropChance = 0.25F;
                }
                case 2 -> {
                    setStats(Config.HP_ZOMBIE_T2.get(), 0.228D, 5);
                    flammability = 30;
                }
                case 3 -> {
                    setStats(Config.HP_ZOMBIE_T2.get(), 0.3D, 8);
                    hold(Items.GOLDEN_SWORD);
                }
                default -> {
                    setStats(Config.HP_ZOMBIE_T2.get(), 0.228D, 7);
                    setBase(Attributes.ARMOR, 6.0D);
                    itemDrop = Items.IRON_CHESTPLATE;
                    dropChance = 0.25F;
                    flammability = 4;
                }
            }
        } else if (flavour == 1) {
            setStats(Config.HP_ZOMBIE_T1.get(), 0.228D, 6);
            hold(Items.WOODEN_SWORD);
            itemDrop = Items.WOODEN_SWORD;
            dropChance = 0.2F;
        } else {
            setStats(Config.HP_ZOMBIE_T1.get(), 0.228D, 4);
        }
    }

    private void hold(Item item) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected int chooseTexture(int tier, int flavour) {
        if (tier >= 3) {
            return 6;
        }
        if (tier == 2) {
            if (flavour == 2) {
                return 5;
            }
            if (flavour == 3) {
                return 3;
            }
            return random.nextBoolean() ? 2 : 4;
        }
        return random.nextBoolean() ? 0 : 1;
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

    public boolean isTarZombie() {
        return getTier() == 2 && getFlavour() == 2;
    }

    @Override
    public boolean canDig() {
        // Sword fighters (flavour 1) don't dig, like the original "destructiveness 0".
        return getFlavour() != 1;
    }

    @Override
    public boolean fireImmune() {
        return (getTier() == 2 && getFlavour() == 3) || super.fireImmune();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && flammability >= 20 && isOnFire() && isAlive()) {
            doFireball();
        }
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
            // Charge: extra damage and a big knock-back.
            float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + 3.0F;
            boolean hit = target.hurt(damageSources().mobAttack(this), damage);
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
    public boolean isPushable() {
        return !isBrute() && super.isPushable();
    }

    /** Tar zombies burst into flames when set on fire. */
    private void doFireball() {
        BlockPos center = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = level().getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) {
                level().setBlockAndUpdate(pos.immutable(), BaseFireBlock.getState(level(), pos));
            }
        }
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(1.5D))) {
            entity.setSecondsOnFire(8);
        }
        level().playSound(null, center, ModSounds.FIREBALL.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        hurt(damageSources().onFire(), 500.0F);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextFloat() < 0.35F) {
            spawnAtLocation(Items.ROTTEN_FLESH);
        }
        if (itemDrop != null && random.nextFloat() < dropChance) {
            spawnAtLocation(itemDrop);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (isBrute()) {
            return random.nextInt(3) == 0 ? ModSounds.BIG_ZOMBIE.get() : null;
        }
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Restore non-synced per-variant fields without resetting health.
        float health = getHealth();
        applyStats(getTier(), getFlavour());
        setHealth(health);
    }
}
