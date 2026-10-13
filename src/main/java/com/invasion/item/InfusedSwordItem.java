package com.invasion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Diamond-strength sword whose "durability" bar is really a charge meter, as in the
 * original: every hit charges it, and when it is full right click heals 3 hearts
 * (or restores hunger while sneaking) and empties it again. It never breaks.
 */
public class InfusedSwordItem extends SwordItem {
    public static final int MAX_CHARGE = 40;

    public InfusedSwordItem(Properties properties) {
        super(Tiers.DIAMOND, 3, -2.4F, properties.durability(MAX_CHARGE));
    }

    public static int getCharge(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (stack.getDamageValue() > 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
        return true;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() != 0) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                player.getFoodData().eat(6, 0.5F);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F,
                        level.random.nextFloat() * 0.1F + 0.9F);
            } else {
                player.heal(6.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D, player.getZ(), 6, 1.0D, 0.5D, 1.0D, 0.0D);
                }
            }
            stack.setDamageValue(stack.getMaxDamage());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getDamageValue() == 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.invasion.infused_sword.charge", getCharge(stack), MAX_CHARGE)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.invasion.infused_sword.desc").withStyle(ChatFormatting.GRAY));
    }
}
