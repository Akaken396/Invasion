package com.invasion;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.InvasionAllies;
import com.invasion.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Pets of players during an invasion:
 * - the Strange Bone makes any tamed pet (wolves, cats, Cataclysm's baby leviathan...) a Nexus guardian:
 *   tougher, hits invaders twice as hard, hunts them, and comes back at the Nexus instead of dying;
 * - near a Nexus under attack, players and their pets cannot hurt players' pets, and pets do not turn
 *   on players or other pets;
 * - Ars Nouveau homing projectiles of players ignore those pets, so they don't circle them forever.
 */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PetProtection {
    public static final String GUARDIAN_TAG = "InvasionGuardian";
    private static final UUID HEALTH_ID = UUID.fromString("7a3e9d10-52c4-4b0e-8f61-2c9d4e7b1a01");
    private static final Map<Class<?>, Optional<Method[]>> IGNORE_METHODS = new ConcurrentHashMap<>();

    /** A pet owned by a player (not a summon of an invader). */
    public static boolean isPlayerPet(@Nullable Entity entity) {
        if (!(entity instanceof OwnableEntity ownable) || ownable.getOwnerUUID() == null || InvasionAllies.isInvaderSide(entity)) {
            return false;
        }
        Entity owner = ownable.getOwner();
        return owner == null || owner instanceof Player;
    }

    /** The player behind an attack: the player itself, or the owner of a pet / projectile. */
    private static boolean isPlayerSide(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Player || isPlayerPet(entity)) {
            return true;
        }
        Entity owner = InvasionAllies.owner(entity);
        return owner != null && owner != entity && (owner instanceof Player || isPlayerPet(owner));
    }

    // ------------------------------------------------------------------
    // Strange Bone

    /** Runs before the pet's own interaction (a tamed wolf would just sit down). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.STRANGE_BONE.get()) || !(event.getTarget() instanceof LivingEntity pet)
                || !(pet instanceof OwnableEntity ownable) || ownable.getOwnerUUID() == null) {
            return;
        }
        Player player = event.getEntity();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) {
            return;
        }
        if (!player.getUUID().equals(ownable.getOwnerUUID())) {
            player.displayClientMessage(Component.translatable("message.invasion.strange_bone.not_owner").withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockPos nexus = findNexus(pet);
        if (nexus == null) {
            player.displayClientMessage(Component.translatable("message.invasion.strange_bone.no_nexus"), false);
            return;
        }
        makeGuardian(pet, nexus);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.invasion.strange_bone.bound", pet.getDisplayName())
                .withStyle(ChatFormatting.GREEN), false);
    }

    @Nullable
    private static BlockPos findNexus(LivingEntity pet) {
        BlockPos center = pet.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16, -8, -16), center.offset(16, 8, 16))) {
            if (pet.level().getBlockEntity(pos) instanceof NexusBlockEntity) {
                return pos.immutable();
            }
        }
        return null;
    }

    public static void makeGuardian(LivingEntity pet, BlockPos nexus) {
        pet.getPersistentData().putLong(GUARDIAN_TAG, nexus.asLong());
        AttributeInstance health = pet.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH_ID) == null) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_ID, "Nexus guardian", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        pet.setHealth(pet.getMaxHealth());
        if (pet instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        if (pet.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pet.getX(), pet.getY() + pet.getBbHeight(), pet.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.0D);
        }
    }

    @Nullable
    private static NexusBlockEntity guardedNexus(Entity entity) {
        if (!entity.getPersistentData().contains(GUARDIAN_TAG)) {
            return null;
        }
        BlockPos pos = BlockPos.of(entity.getPersistentData().getLong(GUARDIAN_TAG));
        return entity.level().isLoaded(pos) && entity.level().getBlockEntity(pos) instanceof NexusBlockEntity nexus ? nexus : null;
    }

    /** Guardians hit invaders twice as hard and heal a little. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity guardian && attacker.getPersistentData().contains(GUARDIAN_TAG)
                && InvasionAllies.isInvader(event.getEntity())) {
            event.setAmount(event.getAmount() * 2.0F);
            guardian.heal(2.0F);
        }
    }

    /** Guardians come back at the Nexus instead of dying while it is under attack. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity pet = event.getEntity();
        if (!(pet.level() instanceof ServerLevel level)) {
            return;
        }
        NexusBlockEntity nexus = guardedNexus(pet);
        if (nexus == null || !nexus.isActive()) {
            return;
        }
        event.setCanceled(true);
        pet.setHealth(pet.getMaxHealth());
        pet.removeAllEffects();
        pet.clearFire();
        pet.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (pet instanceof Mob mob) {
            mob.setTarget(null);
        }
        BlockPos pos = nexus.getBlockPos();
        pet.teleportTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        level.sendParticles(ParticleTypes.PORTAL, pet.getX(), pet.getY() + 0.5D, pet.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.3D);
    }

    /** Idle guardians go after nearby invaders. */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 40 != 0 || entity.level().isClientSide || !(entity instanceof Mob pet) || pet.getTarget() != null
                || (pet instanceof TamableAnimal tame && tame.isOrderedToSit())) {
            return;
        }
        NexusBlockEntity nexus = guardedNexus(pet);
        if (nexus == null || !nexus.isActive()) {
            return;
        }
        pet.level().getEntitiesOfClass(Mob.class, pet.getBoundingBox().inflate(16.0D), m -> m.isAlive() && InvasionAllies.isInvader(m))
                .stream().min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(pet)))
                .ifPresent(pet::setTarget);
    }

    // ------------------------------------------------------------------
    // No friendly fire on pets during an invasion

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !isPlayerPet(victim)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if ((isPlayerSide(attacker) || (attacker == null && isPlayerSide(direct))) && attacker != victim
                && PlayerProtection.nearActiveNexus(victim)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && isPlayerPet(event.getEntity()) && (target instanceof Player || isPlayerPet(target))
                && PlayerProtection.nearActiveNexus(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Ars Nouveau homing projectiles of players: also ignore players' pets near a Nexus under attack. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide || !"ars_nouveau".equals(
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getNamespace())) {
            return;
        }
        Optional<Method[]> methods = IGNORE_METHODS.computeIfAbsent(entity.getClass(), c -> {
            try {
                return Optional.of(new Method[]{c.getMethod("getIgnored"), c.getMethod("setIgnored", List.class)});
            } catch (NoSuchMethodException | SecurityException e) {
                return Optional.empty();
            }
        });
        if (methods.isEmpty()) {
            return;
        }
        try {
            Object current = methods.get()[0].invoke(entity);
            if (!(current instanceof List<?> list)) {
                return;
            }
            List<Predicate<LivingEntity>> ignored = new ArrayList<>();
            for (Object o : list) {
                @SuppressWarnings("unchecked")
                Predicate<LivingEntity> p = (Predicate<LivingEntity>) o;
                ignored.add(p);
            }
            ignored.add(e -> isPlayerPet(e) && PlayerProtection.nearActiveNexus(e));
            methods.get()[1].invoke(entity, ignored);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Different Ars Nouveau version: leave the projectile as it is.
        }
    }

    private PetProtection() {
    }
}
