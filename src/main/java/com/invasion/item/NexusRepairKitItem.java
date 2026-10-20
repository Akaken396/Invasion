package com.invasion.item;

import com.invasion.Config;
import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Dropped by elite invaders: right-click a Nexus to restore part of its hit points. */
public class NexusRepairKitItem extends DescribedItem {
    public NexusRepairKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof NexusBlockEntity nexus)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!level.isClientSide && player != null) {
            int amount = Math.max(1, (int) Math.round(nexus.getMaxHp() * Config.REPAIR_KIT_AMOUNT.get()));
            if (nexus.repair(amount)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, context.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.4F);
                player.displayClientMessage(Component.translatable("message.invasion.repaired", nexus.getHp(), nexus.getMaxHp())
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("message.invasion.repair_full").withStyle(ChatFormatting.YELLOW), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
