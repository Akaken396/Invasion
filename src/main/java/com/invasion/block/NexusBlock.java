package com.invasion.block;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The Nexus. Insert a catalyst through its interface to start an invasion,
 * then keep it alive while waves of invaders try to destroy it.
 */
public class NexusBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public NexusBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NexusBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.NEXUS.get(), NexusBlockEntity::serverTick);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
            NetworkHooks.openScreen(serverPlayer, nexus, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // The Nexus cannot be broken while an invasion is running.
        if (state.getValue(ACTIVE)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
                nexus.onBroken();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int count = state.getValue(ACTIVE) ? 6 : 1;
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 1.6D;
            double y = pos.getY() + random.nextDouble() * 1.4D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 1.6D;
            level.addParticle(ParticleTypes.PORTAL, x, y, z,
                    (pos.getX() + 0.5D - x) * 0.5D, (pos.getY() + 0.5D - y) * 0.5D, (pos.getZ() + 0.5D - z) * 0.5D);
        }
    }
}
