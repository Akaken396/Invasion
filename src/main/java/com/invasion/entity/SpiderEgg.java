package com.invasion.entity;

import com.invasion.nexus.wave.EntityConstruct;
import com.invasion.nexus.wave.IMEntityType;
import com.invasion.registry.ModEntities;
import com.invasion.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Egg laid by a mother spider. Hatches into baby spiders after a few seconds unless destroyed. */
public class SpiderEgg extends Entity {
    private static final int HATCH_TIME = 125;
    private static final EntityDataAccessor<Boolean> DATA_HATCHED = SynchedEntityData.defineId(SpiderEgg.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private BlockPos nexusPos;
    private int contents = 6;
    private int ticks;
    private float health = 12.0F;

    public SpiderEgg(EntityType<? extends SpiderEgg> type, Level level) {
        super(type, level);
    }

    public void setup(@Nullable BlockPos nexusPos, int contents) {
        this.nexusPos = nexusPos;
        this.contents = contents;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_HATCHED, false);
    }

    public boolean isHatched() {
        return entityData.get(DATA_HATCHED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.6D, 0.98D, 0.6D));
        if (level().isClientSide) {
            return;
        }
        ticks++;
        if (!isHatched() && ticks > HATCH_TIME) {
            hatch();
        } else if (isHatched() && ticks > HATCH_TIME + 40) {
            discard();
        }
    }

    private void hatch() {
        entityData.set(DATA_HATCHED, true);
        level().playSound(null, blockPosition(), ModSounds.EGG_HATCH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        for (int i = 0; i < contents; i++) {
            IMSpider baby = ModEntities.SPIDER.get().create(level());
            if (baby == null) {
                continue;
            }
            baby.moveTo(getX() + (random.nextDouble() - 0.5D) * 0.5D, getY(), getZ() + (random.nextDouble() - 0.5D) * 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            baby.getNexusLink().setNexus(nexusPos);
            baby.applyConstruct(new EntityConstruct(IMEntityType.SPIDER, 1, 0, 1));
            if (nexusPos != null) {
                baby.setPersistenceRequired();
            }
            level().addFreshEntity(baby);
        }
    }

    @Override
    public boolean isPickable() {
        return !isHatched();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isHatched() || level().isClientSide) {
            return false;
        }
        health -= amount;
        if (health <= 0.0F) {
            discard();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        nexusPos = tag.contains("InvasionNexus") ? NbtUtils.readBlockPos(tag.getCompound("InvasionNexus")) : null;
        contents = tag.getInt("Contents");
        ticks = tag.getInt("Ticks");
        entityData.set(DATA_HATCHED, tag.getBoolean("Hatched"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (nexusPos != null) {
            tag.put("InvasionNexus", NbtUtils.writeBlockPos(nexusPos));
        }
        tag.putInt("Contents", contents);
        tag.putInt("Ticks", ticks);
        tag.putBoolean("Hatched", isHatched());
    }
}
