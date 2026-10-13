package com.invasion.entity;

import com.invasion.Config;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.Level;

/** A small fire-proof imp that climbs walls and sets its victims on fire. */
public class IMImp extends InvasionMonster {
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING = SynchedEntityData.defineId(IMImp.class, EntityDataSerializers.BOOLEAN);

    public IMImp(EntityType<? extends IMImp> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return createBaseAttributes().add(Attributes.ATTACK_DAMAGE, 3.0D).add(Attributes.MOVEMENT_SPEED, 0.36D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_CLIMBING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void applyStats(int tier, int flavour) {
        setStats(Config.HP_IMP.get(), 0.3D * 1.2D, 3);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            entityData.set(DATA_CLIMBING, horizontalCollision);
        }
    }

    @Override
    public boolean onClimbable() {
        return entityData.get(DATA_CLIMBING);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        target.setSecondsOnFire(3);
        return super.doHurtTarget(target);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }
}
