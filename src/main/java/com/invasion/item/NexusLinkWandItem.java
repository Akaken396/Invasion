package com.invasion.item;

import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Nexus Link Wand: right-click a Nexus, then a chest (or any container). Drops of mobs
 * dying near that Nexus then go straight into the chest. Sneak-right-click the Nexus to unlink.
 */
public class NexusLinkWandItem extends DescribedItem {
    private static final String POS = "NexusPos";
    private static final String DIM = "NexusDim";

    public NexusLinkWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NexusBlockEntity nexus) {
            if (!level.isClientSide && player != null) {
                if (player.isShiftKeyDown()) {
                    nexus.setLinkedChest(null);
                    player.displayClientMessage(Component.translatable("message.invasion.wand.unlinked").withStyle(ChatFormatting.YELLOW), true);
                } else {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putLong(POS, pos.asLong());
                    tag.putString(DIM, level.dimension().location().toString());
                    player.displayClientMessage(Component.translatable("message.invasion.wand.selected").withStyle(ChatFormatting.AQUA), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        CompoundTag tag = stack.getTag();
        if (be == null || tag == null || !tag.contains(POS) || !be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player != null) {
            BlockPos nexusPos = BlockPos.of(tag.getLong(POS));
            if (!level.dimension().location().toString().equals(tag.getString(DIM))
                    || !(level.getBlockEntity(nexusPos) instanceof NexusBlockEntity nexus)) {
                player.displayClientMessage(Component.translatable("message.invasion.wand.no_nexus").withStyle(ChatFormatting.RED), true);
            } else {
                nexus.setLinkedChest(pos);
                player.displayClientMessage(Component.translatable("message.invasion.wand.linked",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(POS)) {
            BlockPos pos = BlockPos.of(tag.getLong(POS));
            tooltip.add(Component.translatable("item.invasion.nexus_link_wand.selected", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
