package com.invasion;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.IMThrower;
import com.invasion.nexus.BossRush;
import net.minecraft.core.BlockPos;
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
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof Mob mob && mob instanceof InvasionMob invader) {
            invader.getNexusLink().onDeath(mob);
        }
        if (event.getEntity().getPersistentData().contains(BossRush.TAG)) {
            BlockPos pos = BlockPos.of(event.getEntity().getPersistentData().getLong(BossRush.TAG));
            if (event.getEntity().level().getBlockEntity(pos) instanceof NexusBlockEntity nexus) {
                nexus.onBossKilled(event.getEntity().getUUID());
            }
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

    /** Spell casters of the late waves (Iron's Spells 'n Spellbooks). */
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (event.getEntity().tickCount % 20 == 0 && event.getEntity() instanceof Mob mob && !mob.level().isClientSide
                && mob.isAlive() && com.invasion.compat.IronsSpellsCompat.isCaster(mob)) {
            com.invasion.compat.IronsSpellsCompat.tick(mob);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        InvasionCommand.register(event.getDispatcher());
    }

    private ModEvents() {
    }
}
