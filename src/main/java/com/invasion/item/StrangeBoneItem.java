package com.invasion.item;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.IMWolf;
import com.invasion.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Use on a tamed wolf standing near a Nexus to turn it into a Nexus guardian wolf. */
public class StrangeBoneItem extends DescribedItem {
    public StrangeBoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Wolf wolf) || target instanceof IMWolf || !wolf.isTame()) {
            return InteractionResult.PASS;
        }
        Level level = target.level();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos nexusPos = findNexus(level, wolf.blockPosition());
        if (nexusPos == null) {
            player.displayClientMessage(Component.translatable("message.invasion.strange_bone.no_nexus"), false);
            return InteractionResult.CONSUME;
        }
        IMWolf nexusWolf = ModEntities.WOLF.get().create(level);
        if (nexusWolf == null) {
            return InteractionResult.FAIL;
        }
        CompoundTag tag = wolf.saveWithoutId(new CompoundTag());
        tag.remove("UUID");
        tag.remove("Attributes");
        nexusWolf.load(tag);
        nexusWolf.setTame(true);
        nexusWolf.setHealth(nexusWolf.getMaxHealth());
        nexusWolf.bindToNexus(nexusPos);
        level.addFreshEntity(nexusWolf);
        wolf.discard();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private static BlockPos findNexus(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-7, -4, -7), center.offset(7, 4, 7))) {
            if (level.getBlockEntity(pos) instanceof NexusBlockEntity) {
                return pos.immutable();
            }
        }
        return null;
    }
}
