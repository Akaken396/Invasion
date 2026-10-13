package com.invasion.item;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.ai.BlockDigger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Shows how resistant a block is against invaders, or the status of a Nexus. */
public class ProbeItem extends DescribedItem {
    public ProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
            player.sendSystemMessage(nexus.describe());
            return InteractionResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        if (BlockDigger.canDig(level, pos, state)) {
            int seconds = Math.max(1, BlockDigger.digTicks(state, level, pos, 1.0F) / 20);
            player.displayClientMessage(Component.translatable("message.invasion.probe.diggable",
                    state.getBlock().getName(), seconds).withStyle(ChatFormatting.YELLOW), true);
        } else {
            player.displayClientMessage(Component.translatable("message.invasion.probe.immune",
                    state.getBlock().getName()).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.SUCCESS;
    }
}
