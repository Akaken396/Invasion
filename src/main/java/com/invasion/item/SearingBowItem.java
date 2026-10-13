package com.invasion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A bow whose arrows are always on fire and hit harder. */
public class SearingBowItem extends BowItem {
    public SearingBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        arrow.setSecondsOnFire(100);
        arrow.setBaseDamage(arrow.getBaseDamage() + 1.5D);
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
