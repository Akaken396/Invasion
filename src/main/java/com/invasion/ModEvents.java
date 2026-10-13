package com.invasion;

import com.invasion.entity.InvasionMob;
import com.invasion.registry.ModItems;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModEvents {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof Mob mob && mob instanceof InvasionMob invader) {
            invader.getNexusLink().onDeath(mob);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof InvasionMob invader && invader.getNexusLink().hasNexus()
                && event.getEntity().getRandom().nextDouble() < Config.RIFT_FLUX_DROP_CHANCE.get()) {
            ItemEntity drop = new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(),
                    event.getEntity().getZ(), new ItemStack(ModItems.RIFT_FLUX.get()));
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }

    private ModEvents() {
    }
}
