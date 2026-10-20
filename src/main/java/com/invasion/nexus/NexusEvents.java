package com.invasion.nexus;

import com.invasion.Config;
import com.invasion.InvasionMod;
import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.InvasionMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/** Per-wave damage / kill stats of players, and the drop chest linked to a Nexus. */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NexusEvents {
    /** The Nexus an invader (Invasion mob, foreign mob or boss rush boss) belongs to. */
    @Nullable
    public static NexusBlockEntity nexusFor(Entity entity) {
        Level level = entity.level();
        BlockPos pos = null;
        if (entity instanceof InvasionMob invader) {
            pos = invader.getNexusLink().getNexusPos();
        } else if (ForeignInvaders.nexusOf(entity) != null) {
            pos = ForeignInvaders.nexusOf(entity);
        } else if (entity.getPersistentData().contains(BossRush.TAG)) {
            pos = BlockPos.of(entity.getPersistentData().getLong(BossRush.TAG));
        }
        if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
            return nexus;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(event.getSource().getEntity() instanceof Player player) || event.getAmount() <= 0.0F) {
            return;
        }
        NexusBlockEntity nexus = nexusFor(victim);
        if (nexus != null && nexus.isActive()) {
            nexus.recordDamage(player, Math.min(event.getAmount(), victim.getHealth()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || event.isCanceled() || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        NexusBlockEntity nexus = nexusFor(victim);
        if (nexus != null && nexus.isActive()) {
            nexus.recordKill(player);
        }
    }

    /** Drops of mobs dying near a Nexus go into its linked chest; whatever does not fit drops as usual. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!Config.DROP_CHEST.get() || entity.level().isClientSide || event.isCanceled() || entity instanceof Player
                || (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) || event.getDrops().isEmpty()) {
            return;
        }
        IItemHandler chest = chestFor(entity);
        if (chest == null) {
            return;
        }
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity item = it.next();
            ItemStack rest = ItemHandlerHelper.insertItemStacked(chest, item.getItem().copy(), false);
            if (rest.isEmpty()) {
                it.remove();
            } else {
                item.setItem(rest);
            }
        }
    }

    @Nullable
    private static IItemHandler chestFor(LivingEntity entity) {
        NexusBlockEntity own = nexusFor(entity);
        if (own != null && own.getLinkedChest() != null) {
            IItemHandler handler = handlerAt(entity.level(), own.getLinkedChest());
            if (handler != null) {
                return handler;
            }
        }
        for (NexusBlockEntity nexus : NexusBlockEntity.activeNexuses()) {
            if (nexus.getLevel() == entity.level() && nexus.getLinkedChest() != null && nexus.isInArea(entity.position())) {
                IItemHandler handler = handlerAt(entity.level(), nexus.getLinkedChest());
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }

    @Nullable
    private static IItemHandler handlerAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    private NexusEvents() {
    }
}
