package com.invasion;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.InvasionAllies;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps invasions fun for players: no gravity / snares / roots near a Nexus under attack,
 * and no spawn camping - a short grace period after respawning, and the lasting spells and
 * summons of invaders around the respawn point are cleared.
 */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerProtection {
    private static final String GRACE_TAG = "InvasionRespawnGrace";

    public static boolean nearActiveNexus(Entity entity) {
        for (NexusBlockEntity nexus : NexusBlockEntity.activeNexuses()) {
            if (nexus.getLevel() == entity.level()) {
                double range = nexus.getSpawnRadius() + 32.0D;
                if (entity.distanceToSqr(nexus.getCenter()) < range * range) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean listed(List<? extends String> list, ResourceLocation id) {
        return id != null && list.contains(id.toString());
    }

    public static boolean hasGrace(Entity entity) {
        return entity instanceof Player player && player.getPersistentData().getLong(GRACE_TAG) > player.level().getGameTime();
    }

    // ------------------------------------------------------------------
    // No gravity, snares or roots

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide
                && listed(Config.BLOCKED_PLAYER_EFFECTS.get(), ForgeRegistries.MOB_EFFECTS.getKey(event.getEffectInstance().getEffect()))
                && nearActiveNexus(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof Player player && event.getEntityBeingMounted() != null
                && !player.level().isClientSide
                && listed(Config.BLOCKED_PLAYER_VEHICLES.get(), ForgeRegistries.ENTITY_TYPES.getKey(event.getEntityBeingMounted().getType()))
                && nearActiveNexus(player)) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------
    // Respawn grace

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (event.isEndConquered() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int grace = Config.RESPAWN_GRACE_SECONDS.get();
        if (grace > 0) {
            player.getPersistentData().putLong(GRACE_TAG, level.getGameTime() + grace * 20L);
        }
        int radius = Config.RESPAWN_CLEAR_RADIUS.get();
        if (radius > 0 && nearActiveNexus(player)) {
            List<Entity> remove = new ArrayList<>();
            for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(radius))) {
                if (InvasionAllies.isInvader(entity)) {
                    if (entity instanceof Mob mob && mob.getTarget() == player) {
                        mob.setTarget(null);
                    }
                } else if (InvasionAllies.isInvaderSide(entity)) {
                    // Spell entities, fields, projectiles and summons left by invaders.
                    remove.add(entity);
                }
            }
            remove.forEach(Entity::discard);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (hasGrace(event.getNewTarget()) && InvasionAllies.isInvaderSide(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.level().isClientSide && hasGrace(victim)) {
            Entity attacker = event.getSource().getEntity();
            Entity direct = event.getSource().getDirectEntity();
            if (InvasionAllies.isInvaderSide(attacker) || InvasionAllies.isInvaderSide(direct)) {
                event.setCanceled(true);
            }
        }
    }

    /** Attacking ends the grace period, so it cannot be abused. */
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide) {
            event.getEntity().getPersistentData().remove(GRACE_TAG);
        }
    }

    private PlayerProtection() {
    }
}
