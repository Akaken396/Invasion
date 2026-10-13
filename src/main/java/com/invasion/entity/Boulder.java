package com.invasion.entity;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.ai.BlockStrength;
import com.invasion.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Boulder thrown by the Thrower. Hits entities harder the longer it flies, hits the
 * Nexus for 2 damage and blasts a small hole into anything else (obsidian may deflect it).
 */
public class Boulder extends ThrowableProjectile {
    public Boulder(EntityType<? extends Boulder> type, Level level) {
        super(type, level);
    }

    public Boulder(Level level, LivingEntity thrower) {
        super(ModEntities.BOULDER.get(), thrower, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        return 0.025F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        if (target == getOwner() || target instanceof InvasionMob) {
            return;
        }
        float damage = Math.min(14.0F, Math.max(tickCount / 20.0F, 1.0F) * 6.0F);
        if (target.hurt(damageSources().thrown(this, getOwner()), damage)) {
            playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 0.9F / (random.nextFloat() * 0.2F + 0.9F));
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        BlockState state = level().getBlockState(pos);
        if (level().getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
            nexus.damageNexus(2);
        } else if (!(BlockStrength.deflects(state) && random.nextBoolean())) {
            level().explode(this, getX(), getY(), getZ(), 0.5F, Level.ExplosionInteraction.MOB);
        }
        playSound(SoundEvents.GENERIC_EXPLODE, 1.0F, 0.9F / (random.nextFloat() * 0.2F + 0.9F));
        discard();
    }
}
