package com.invasion.item;

import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Changes the spawn radius of an inactive Nexus. Right click to increase, sneak + right click to decrease. */
public class NexusAdjusterItem extends DescribedItem {
    public NexusAdjusterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof NexusBlockEntity nexus)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Player player = context.getPlayer();
            if (nexus.isActive()) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.invasion.adjuster.active").withStyle(ChatFormatting.RED), true);
                }
            } else {
                boolean decrease = player != null && player.isShiftKeyDown();
                int radius = nexus.adjustSpawnRadius(decrease ? -NexusBlockEntity.RADIUS_STEP : NexusBlockEntity.RADIUS_STEP);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.invasion.adjuster.radius", radius), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
