package com.invasion.item;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.ai.BlockStrength;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Nexus Adjuster / Material Probe. Both change the spawn radius of an inactive
 * Nexus (32-128 in steps of 8, sneak to decrease). The Material Probe also shows how
 * strong a block is against invaders and can pick up armed traps.
 */
public class NexusAdjusterItem extends DescribedItem {
    private final boolean probe;

    public NexusAdjusterItem(Properties properties, boolean probe) {
        super(properties);
        this.probe = probe;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
            if (!level.isClientSide && player != null) {
                int radius = nexus.getSpawnRadius();
                if (player.isShiftKeyDown()) {
                    radius -= 8;
                    if (radius < 32) {
                        radius = 128;
                    }
                } else {
                    radius += 8;
                    if (radius > 128) {
                        radius = 32;
                    }
                }
                if (nexus.setSpawnRadius(radius)) {
                    player.displayClientMessage(Component.translatable("message.invasion.adjuster.radius", nexus.getSpawnRadius()), false);
                } else {
                    player.displayClientMessage(Component.translatable("message.invasion.adjuster.active").withStyle(ChatFormatting.RED), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (probe) {
            if (!level.isClientSide && player != null) {
                float strength = BlockStrength.getStrength(level, pos, level.getBlockState(pos));
                player.displayClientMessage(Component.translatable("message.invasion.probe.strength",
                        String.format("%.2f", strength)), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
