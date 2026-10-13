package com.invasion.entity;

import com.invasion.Config;
import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.ai.BlockDigger;
import com.invasion.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Boulder thrown by the Thrower. Hurts entities, damages the Nexus and smashes soft blocks. */
public class Boulder extends ThrowableItemProjectile {
    private static final float MAX_SMASH_HARDNESS = 3.0F;

    public Boulder(EntityType<? extends Boulder> type, Level level) {
        super(type, level);
    }

    public Boulder(Level level, LivingEntity thrower) {
        super(ModEntities.BOULDER.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected float getGravity() {
        return 0.04F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (target instanceof InvasionMob) {
            return;
        }
        target.hurt(damageSources().thrown(this, getOwner()), 6.0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos hit = result.getBlockPos();
        if (level.getBlockEntity(hit) instanceof NexusBlockEntity nexus) {
            nexus.damageNexus(3);
            return;
        }
        if (!Config.MOBS_DESTROY_BLOCKS.get()) {
            return;
        }
        for (BlockPos pos : BlockPos.betweenClosed(hit.offset(-1, 0, -1), hit.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (level.random.nextFloat() < 0.6F && BlockDigger.canDig(level, pos, state)
                    && state.getDestroySpeed(level, pos) <= MAX_SMASH_HARDNESS) {
                level.destroyBlock(pos, Config.DROP_DESTROYED_BLOCKS.get(), getOwner());
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                    getX(), getY(), getZ(), 20, 0.3D, 0.3D, 0.3D, 0.1D);
            discard();
        }
    }
}
