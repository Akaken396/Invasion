package com.invasion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A bow that works normally, but when held drawn for about three seconds it
 * releases a "searing" shot: a burning arrow with 1.5x damage, as in the original.
 */
public class SearingBowItem extends BowItem {
    /** Draw power at which the special shot is released (power of the original formula). */
    public static final float SEARING_POWER = 3.8F;

    public SearingBowItem(Properties properties) {
        super(properties);
    }

    public static float rawPower(int ticks) {
        float f = ticks / 20.0F;
        return (f * f + f * 2.0F) / 3.0F;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeLeft) {
        if (!(shooter instanceof Player player)) {
            return;
        }
        int charge = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack, level, player, getUseDuration(stack) - timeLeft, true);
        if (charge < 0) {
            return;
        }
        boolean infinite = player.getAbilities().instabuild;
        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty() && !infinite) {
            return;
        }
        if (ammo.isEmpty()) {
            ammo = new ItemStack(Items.ARROW);
        }
        float power = rawPower(charge);
        if (power < 0.1F) {
            return;
        }
        boolean searing = power >= SEARING_POWER;
        power = Math.min(power, 1.0F);
        if (!level.isClientSide) {
            ArrowItem arrowItem = (ArrowItem) (ammo.getItem() instanceof ArrowItem ? ammo.getItem() : Items.ARROW);
            AbstractArrow arrow = arrowItem.createArrow(level, ammo, player);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            if (searing) {
                arrow.setSecondsOnFire(100);
                arrow.setBaseDamage((arrow.getBaseDamage() + 1.0D) * 1.5D + 1.0D);
                arrow.setCritArrow(true);
            } else if (power == 1.0F) {
                arrow.setCritArrow(true);
            }
            if (infinite) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            level.addFreshEntity(arrow);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        if (!infinite) {
            ammo.shrink(1);
            if (ammo.isEmpty()) {
                player.getInventory().removeItem(ammo);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
