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
        // Let the Nexus Adjuster / Material Probe act on the Nexus instead of opening the interface.
        if (player.getItemInHand(hand).getItem() instanceof com.invasion.item.NexusAdjusterItem) {
            return InteractionResult.PASS;
        }
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
        if (!state.getValue(ACTIVE)) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            double y1 = pos.getY() + random.nextFloat();
            double y2 = (random.nextFloat() - 0.5D) * 0.5D;
            int direction = random.nextInt(2) * 2 - 1;
            double x1;
            double x2;
            double z1;
            double z2;
            if (random.nextBoolean()) {
                z1 = pos.getZ() + 0.5D + 0.25D * direction;
                z2 = random.nextFloat() * 2.0F * direction;
                x1 = pos.getX() + random.nextFloat();
                x2 = (random.nextFloat() - 0.5D) * 0.5D;
            } else {
                x1 = pos.getX() + 0.5D + 0.25D * direction;
                x2 = random.nextFloat() * 2.0F * direction;
                z1 = pos.getZ() + random.nextFloat();
                z2 = (random.nextFloat() - 0.5D) * 0.5D;
            }
            level.addParticle(ParticleTypes.PORTAL, x1, y1, z1, x2, y2, z2);
        }
    }
}
