package com.invasion.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Charges up whenever it hits something. Once fully charged, right click to
 * release the stored energy and heal yourself.
 */
public class InfusedSwordItem extends SwordItem {
    public static final int MAX_CHARGE = 20;
    private static final String TAG_CHARGE = "InfusedCharge";

    public InfusedSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    public static int getCharge(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(TAG_CHARGE) : 0;
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt(TAG_CHARGE, Mth.clamp(charge, 0, MAX_CHARGE));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        setCharge(stack, getCharge(stack) + 1);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getCharge(stack) >= MAX_CHARGE && player.getHealth() < player.getMaxHealth()) {
            if (!level.isClientSide) {
                player.heal(12.0F);
                setCharge(stack, 0);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getCharge(stack) >= MAX_CHARGE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.invasion.infused_sword.charge", getCharge(stack), MAX_CHARGE)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.invasion.infused_sword.desc").withStyle(ChatFormatting.GRAY));
    }
}
