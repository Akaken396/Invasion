package com.invasion.compat;

import com.invasion.InvasionMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * Magic ward: invaders tagged with it ignore magic of Iron's Spells 'n Spellbooks and
 * Ars Nouveau - its damage, its harmful effects, spells that hold them (root), and
 * spells that move them (black hole, gust, telekinesis...).
 */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MagicWard {
    public static final String TAG = "InvasionWarded";
    private static final Set<String> MAGIC_MODS = Set.of("irons_spellbooks", "ars_nouveau");
    /** Spell entities that drag everything around them. */
    private static final Set<String> PULLING_SPELLS = Set.of("irons_spellbooks:black_hole", "irons_spellbooks:gust");
    private static final double MAX_HORIZONTAL = 0.45D;
    private static final double MAX_UP = 0.45D;

    public static boolean isWarded(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG);
    }

    public static void ward(Entity entity) {
        entity.getPersistentData().putBoolean(TAG, true);
    }

    public static boolean isMagic(ResourceLocation id) {
        return id != null && MAGIC_MODS.contains(id.getNamespace());
    }

    public static boolean isMagicDamage(DamageSource source) {
        if (source.typeHolder().unwrapKey().map(key -> isMagic(key.location())).orElse(false)) {
            return true;
        }
        return source.getDirectEntity() != null && isMagic(ForgeRegistries.ENTITY_TYPES.getKey(source.getDirectEntity().getType()));
    }

    public static boolean isBlockedEffect(MobEffect effect) {
        return effect == MobEffects.MOVEMENT_SLOWDOWN || effect == MobEffects.LEVITATION
                || (isMagic(ForgeRegistries.MOB_EFFECTS.getKey(effect)) && effect.getCategory() != MobEffectCategory.BENEFICIAL);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (isWarded(event.getEntity()) && isMagicDamage(event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (isWarded(event.getEntity()) && isBlockedEffect(event.getEffectInstance().getEffect())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (isWarded(entity) && entity.getLastDamageSource() != null && isMagicDamage(entity.getLastDamageSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityBeingMounted() != null && isWarded(event.getEntityMounting())
                && isMagic(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntityBeingMounted().getType()))) {
            event.setCanceled(true);
        }
    }

    /** Black holes and gusts that exist right now (tracked on spawn, so no area search per mob). */
    private static final java.util.List<Entity> PULLS = new java.util.concurrent.CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && PULLING_SPELLS.contains(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType())))) {
            PULLS.add(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLeave(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            PULLS.remove(event.getEntity());
        }
    }

    /**
     * Whether the entity is caught by a pulling spell it should ignore: any one for warded
     * mobs, and those cast by invaders for every invader (no friendly fire).
     */
    private static boolean caughtByPull(LivingEntity entity, boolean warded, boolean invader) {
        for (Entity pull : PULLS) {
            if (!pull.isAlive() || pull.level() != entity.level()) {
                PULLS.remove(pull);
                continue;
            }
            if (pull.distanceToSqr(entity) < 20.0D * 20.0D
                    && (warded || (invader && com.invasion.entity.InvasionAllies.isInvaderSide(pull)))) {
                return true;
            }
        }
        return false;
    }

    /** Undo motion forced on the mob by spells: strong pushes and pulls are clamped back to walking speed. */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        boolean warded = isWarded(entity);
        if (!warded && (PULLS.isEmpty() || !com.invasion.entity.InvasionAllies.isInvader(entity))) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (!PULLS.isEmpty() && caughtByPull(entity, warded, true)) {
            // Inside a black hole / gust: drop the motion it forced on the mob.
            entity.setDeltaMovement(0.0D, Math.min(motion.y, 0.0D), 0.0D);
            return;
        }
        if (!warded) {
            return;
        }
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal > MAX_HORIZONTAL || (motion.y > MAX_UP && !entity.onClimbable() && !entity.isInWater())) {
            double scale = horizontal > MAX_HORIZONTAL ? MAX_HORIZONTAL / horizontal : 1.0D;
            entity.setDeltaMovement(motion.x * scale, Math.min(motion.y, MAX_UP), motion.z * scale);
        }
        if (entity.tickCount % 80 == 0 && entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.WAX_ON, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6D, entity.getZ(),
                    2, 0.3D, 0.4D, 0.3D, 0.0D);
        }
    }

    private MagicWard() {
    }
}
