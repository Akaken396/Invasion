package com.invasion.entity;

import com.invasion.InvasionMod;
import com.invasion.nexus.BossRush;
import com.invasion.nexus.ForeignInvaders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invaders are on one side: they never target each other and never hurt each other,
 * neither in melee nor with projectiles, explosions or spells (also those of their summons).
 */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InvasionAllies {
    /** Iron's Spells summons and spell entities expose their caster through getSummoner() / getOwner(). */
    private static final Map<Class<?>, Optional<Method>> SUMMONER = new ConcurrentHashMap<>();

    public static boolean isInvader(@Nullable Entity entity) {
        if (entity == null || entity instanceof IMWolf) {
            return false;
        }
        return entity instanceof InvasionMob || entity.getPersistentData().contains(ForeignInvaders.TAG)
                || entity.getPersistentData().contains(BossRush.TAG);
    }

    /** The invader itself, or anything owned by one (projectiles, TNT, spell entities, summons). */
    public static boolean isInvaderSide(@Nullable Entity entity) {
        if (isInvader(entity)) {
            return true;
        }
        Entity owner = owner(entity);
        return owner != null && owner != entity && isInvader(owner);
    }

    @Nullable
    public static Entity owner(@Nullable Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof TraceableEntity traceable && traceable.getOwner() != null) {
            return traceable.getOwner();
        }
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() != null) {
            return ownable.getOwner();
        }
        Optional<Method> method = SUMMONER.computeIfAbsent(entity.getClass(), c -> {
            for (String name : new String[]{"getSummoner", "getOwner"}) {
                try {
                    Method m = c.getMethod(name);
                    if (Entity.class.isAssignableFrom(m.getReturnType())) {
                        return Optional.of(m);
                    }
                } catch (NoSuchMethodException | SecurityException ignored) {
                    // try the next name
                }
            }
            return Optional.empty();
        });
        if (method.isPresent()) {
            try {
                return (Entity) method.get().invoke(entity);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean areAllies(Entity a, @Nullable Entity b) {
        return b != null && a != b && isInvaderSide(a) && isInvaderSide(b);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !isInvaderSide(victim)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if ((attacker != null && attacker != victim && isInvaderSide(attacker))
                || (direct != null && direct != victim && attacker != victim && isInvaderSide(direct))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (areAllies(event.getEntity(), event.getNewTarget())) {
            event.setCanceled(true);
        }
    }

    private InvasionAllies() {
    }
}
