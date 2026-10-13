package com.invasion;

import com.invasion.entity.IMThrower;
import com.invasion.entity.InvasionMob;
import com.invasion.registry.ModItems;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModEvents {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Mob mob && mob instanceof InvasionMob invader) {
            invader.getNexusLink().onDeath(mob);
        }
    }

    /** Invaders drop Small Remnants: one in four, Throwers always (as in the original). */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof InvasionMob)) {
            return;
        }
        boolean drop = event.getEntity() instanceof IMThrower || event.getEntity().getRandom().nextInt(4) == 0;
        if (drop) {
            ItemEntity item = new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(),
                    event.getEntity().getZ(), new ItemStack(ModItems.SMALL_REMNANTS.get()));
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        InvasionCommand.register(event.getDispatcher());
    }

    private ModEvents() {
    }
}
