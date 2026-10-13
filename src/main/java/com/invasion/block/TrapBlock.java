package com.invasion.block;

import com.invasion.entity.InvasionMob;
import com.invasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin plate that is triggered by hostile mobs stepping on it.
 * Rift traps deal heavy magic damage and slow, flame traps set the area on fire.
 * After triggering, a trap turns back into an empty trap that can be re-armed by crafting.
 */
public class TrapBlock extends Block {
    public enum Type { EMPTY, RIFT, FLAME }

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);

    private final Type type;

    public TrapBlock(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (type == Type.EMPTY || level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (!(entity instanceof InvasionMob) && !(entity instanceof Enemy)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        if (type == Type.RIFT) {
            living.hurt(level.damageSources().magic(), 14.0F);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    40, 0.6D, 0.6D, 0.6D, 0.1D);
            level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.6F);
        } else {
            living.setSecondsOnFire(8);
            living.hurt(level.damageSources().inFire(), 6.0F);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos firePos = pos.relative(dir);
                if (level.isEmptyBlock(firePos) && BaseFireBlock.canBePlacedAt(level, firePos, dir)) {
                    level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                }
            }
            serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D,
                    30, 0.5D, 0.3D, 0.5D, 0.05D);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        level.setBlockAndUpdate(pos, ModBlocks.EMPTY_TRAP.get().defaultBlockState());
    }
}
