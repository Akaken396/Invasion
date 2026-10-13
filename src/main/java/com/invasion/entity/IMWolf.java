package com.invasion.entity;

import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A tamed wolf bound to a Nexus with a Strange Bone. It is tougher, hits invaders
 * twice as hard, heals itself when biting and comes back at the Nexus when it dies
 * during an invasion.
 */
public class IMWolf extends Wolf {
    @Nullable
    private BlockPos nexusPos;

    public IMWolf(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH, 25.0D).add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    public void bindToNexus(BlockPos pos) {
        this.nexusPos = pos.immutable();
    }

    @Override
    public void setTame(boolean tame) {
        super.setTame(tame);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(25.0D);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float damage = isTame() ? 4.0F : 2.0F;
        if (target instanceof Enemy) {
            damage *= 2.0F;
        }
        boolean hit = target.hurt(damageSources().mobAttack(this), damage);
        if (hit) {
            heal(4.0F);
            doEnchantDamageEffects(this, target);
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && nexusPos != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getBlockEntity(nexusPos) instanceof NexusBlockEntity nexus && nexus.isActive()) {
            // Respawn next to the Nexus instead of dying.
            IMWolf copy = (IMWolf) getType().create(serverLevel);
            if (copy != null) {
                CompoundTag tag = saveWithoutId(new CompoundTag());
                tag.remove("UUID");
                copy.load(tag);
                copy.setHealth(copy.getMaxHealth());
                copy.clearFire();
                copy.setTarget(null);
                copy.moveTo(nexusPos.getX() + 0.5D, nexusPos.getY() + 1.0D, nexusPos.getZ() + 0.5D, getYRot(), 0.0F);
                serverLevel.addFreshEntity(copy);
            }
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (nexusPos != null) {
            tag.put("InvasionNexus", NbtUtils.writeBlockPos(nexusPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nexusPos = tag.contains("InvasionNexus") ? NbtUtils.readBlockPos(tag.getCompound("InvasionNexus")) : null;
    }
}
