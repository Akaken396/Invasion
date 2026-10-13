package com.invasion.entity;

import com.invasion.registry.ModItems;
import com.invasion.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A trap placed on the ground (like the original EntityIMTrap). It arms after 3
 * seconds and fires at the first living thing stepping on it:
 * the rift trap deals 38 magic damage (12 to players) plus 8 around it and stuns,
 * the flame trap sets the area on fire. A spent trap is picked up by walking over it;
 * an armed trap can be retrieved with the Material Probe.
 */
public class TrapEntity extends Entity {
    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_RIFT = 1;
    public static final int TYPE_FLAME = 2;
    private static final int ARM_TIME = 60;

    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EMPTY = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.BOOLEAN);

    private int ticks;

    public TrapEntity(EntityType<? extends TrapEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_TYPE, TYPE_EMPTY);
        entityData.define(DATA_EMPTY, false);
    }

    public void setTrapType(int type) {
        entityData.set(DATA_TYPE, type);
    }

    public int getTrapType() {
        return entityData.get(DATA_TYPE);
    }

    public boolean isEmpty() {
        return entityData.get(DATA_EMPTY);
    }

    public static Item itemFor(int type) {
        return switch (type) {
            case TYPE_RIFT -> ModItems.RIFT_TRAP.get();
            case TYPE_FLAME -> ModItems.FLAME_TRAP.get();
            default -> ModItems.EMPTY_TRAP.get();
        };
    }

    public boolean isValidPlacement() {
        BlockPos below = BlockPos.containing(getX(), getY() - 0.5D, getZ());
        return level().getBlockState(below).isFaceSturdy(level(), below, Direction.UP)
                && level().getEntitiesOfClass(TrapEntity.class, getBoundingBox()).size() < 2;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (level().isClientSide) {
            return;
        }
        if (ticks % 20 == 0 && !isValidPlacement()) {
            spawnAtLocation(new ItemStack(ModItems.EMPTY_TRAP.get()));
            discard();
            return;
        }
        if (isEmpty() || ticks < ARM_TIME) {
            return;
        }
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox())) {
            if (entity.isAlive() && !entity.isSpectator()) {
                trigger(entity);
                return;
            }
        }
    }

    private void trigger(LivingEntity victim) {
        ServerLevel level = (ServerLevel) level();
        if (getTrapType() == TYPE_RIFT) {
            victim.hurt(damageSources().magic(), victim instanceof Player ? 12.0F : 38.0F);
            for (Entity entity : level.getEntities(this, getBoundingBox().inflate(1.9D, 1.0D, 1.9D))) {
                entity.hurt(damageSources().magic(), 8.0F);
                if (entity instanceof InvasionMob && entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10));
                }
            }
            level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 2.0D, getZ(), 300, 3.0D, 0.5D, 3.0D, 1.0D);
            level.playSound(null, blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.5F, random.nextFloat() * 0.25F + 0.55F);
        } else if (getTrapType() == TYPE_FLAME) {
            level.playSound(null, blockPosition(), ModSounds.FIREBALL.get(), SoundSource.BLOCKS, 1.5F, 1.15F / (random.nextFloat() * 0.3F + 1.0F));
            BlockPos center = blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.canBeReplaced()) {
                    level.setBlockAndUpdate(pos.immutable(), BaseFireBlock.getState(level, pos));
                }
            }
            for (Entity entity : level.getEntities(this, getBoundingBox().inflate(1.1D))) {
                entity.setSecondsOnFire(8);
                entity.hurt(damageSources().onFire(), 8.0F);
            }
        } else {
            victim.hurt(damageSources().generic(), 4.0F);
        }
        entityData.set(DATA_EMPTY, true);
        ticks = 0;
    }

    @Override
    public void playerTouch(Player player) {
        if (!level().isClientSide && isEmpty() && ticks > 30 && player.getInventory().add(new ItemStack(ModItems.EMPTY_TRAP.get()))) {
            playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.take(this, 1);
            discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (isEmpty() || !player.getItemInHand(hand).is(ModItems.MATERIAL_PROBE.get())) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(itemFor(getTrapType())));
            item.setPickUpDelay(5);
            level().addFreshEntity(item);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_TYPE, tag.getInt("Type"));
        entityData.set(DATA_EMPTY, tag.getBoolean("IsEmpty"));
        ticks = ARM_TIME;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Type", getTrapType());
        tag.putBoolean("IsEmpty", isEmpty());
    }
}
