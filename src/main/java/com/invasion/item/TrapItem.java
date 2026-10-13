package com.invasion.item;

import com.invasion.entity.TrapEntity;
import com.invasion.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Places an armed trap on top of the clicked block. Empty traps can't be placed. */
public class TrapItem extends DescribedItem {
    private final int trapType;

    public TrapItem(Properties properties, int trapType) {
        super(properties);
        this.trapType = trapType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (trapType == TrapEntity.TYPE_EMPTY || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            TrapEntity trap = ModEntities.TRAP.get().create(level);
            if (trap == null) {
                return InteractionResult.FAIL;
            }
            trap.setTrapType(trapType);
            trap.moveTo(context.getClickedPos().getX() + 0.5D, context.getClickedPos().getY() + 1.0D,
                    context.getClickedPos().getZ() + 0.5D, 0.0F, 0.0F);
            if (!trap.isValidPlacement() || !level.getEntitiesOfClass(TrapEntity.class, trap.getBoundingBox()).isEmpty()) {
                return InteractionResult.FAIL;
            }
            level.addFreshEntity(trap);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
