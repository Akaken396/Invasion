package com.invasion.nexus;

import com.invasion.Config;
import com.invasion.InvasionMod;
import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Mobs of other mods (the non-boss mobs of All the Mods 9) that join special waves.
 * They get a goal to march on the Nexus and hit it, and vanish when the invasion ends.
 * Also keeps unwanted entities (EvilCraft vengeance spirits...) away from active Nexuses.
 */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForeignInvaders {
    public static final String TAG = "InvasionForeignNexus";

    public static List<EntityType<?>> availableTypes() {
        List<EntityType<?>> list = new ArrayList<>();
        for (String id : Config.FOREIGN_MOBS.get()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null && ForgeRegistries.ENTITY_TYPES.containsKey(key)) {
                list.add(ForgeRegistries.ENTITY_TYPES.getValue(key));
            }
        }
        return list;
    }

    public static void tag(Entity entity, BlockPos nexusPos) {
        entity.getPersistentData().putLong(TAG, nexusPos.asLong());
    }

    @Nullable
    public static BlockPos nexusOf(Entity entity) {
        return entity.getPersistentData().contains(TAG) ? BlockPos.of(entity.getPersistentData().getLong(TAG)) : null;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Entity entity = event.getEntity();
        if (isBlockedNearNexus(event.getLevel(), entity)) {
            event.setCanceled(true);
            return;
        }
        BlockPos nexus = nexusOf(entity);
        if (nexus != null && entity instanceof PathfinderMob mob) {
            mob.goalSelector.addGoal(2, new MarchOnNexusGoal(mob, nexus));
        }
    }

    private static boolean isBlockedNearNexus(net.minecraft.world.level.Level level, Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null || !Config.BLOCKED_ENTITIES.get().contains(id.toString())) {
            return false;
        }
        for (NexusBlockEntity nexus : NexusBlockEntity.activeNexuses()) {
            if (nexus.getLevel() == level) {
                double range = nexus.getSpawnRadius() + 32.0D;
                if (entity.distanceToSqr(nexus.getCenter()) < range * range) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Foreign invaders vanish when their Nexus is no longer active. */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 200 != 0) {
            return;
        }
        BlockPos nexus = nexusOf(entity);
        if (nexus != null && entity.level().isLoaded(nexus)
                && !(entity.level().getBlockEntity(nexus) instanceof NexusBlockEntity be && be.isActive())) {
            entity.discard();
        }
    }

    /** Walks to the Nexus and hits it (2 damage every 2 seconds) when there is nobody else to fight. */
    static class MarchOnNexusGoal extends Goal {
        private final PathfinderMob mob;
        private final BlockPos nexusPos;
        private int cooldown;
        private int repath;

        MarchOnNexusGoal(PathfinderMob mob, BlockPos nexusPos) {
            this.mob = mob;
            this.nexusPos = nexusPos;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mob.getTarget() == null && mob.level().getBlockEntity(nexusPos) instanceof NexusBlockEntity nexus && nexus.isActive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 center = Vec3.atCenterOf(nexusPos);
            mob.getLookControl().setLookAt(center.x, center.y, center.z);
            if (cooldown > 0) {
                cooldown--;
            }
            if (mob.getBoundingBox().inflate(1.0D, 0.5D, 1.0D).intersects(new AABB(nexusPos))) {
                mob.getNavigation().stop();
                if (cooldown <= 0 && mob.level().getBlockEntity(nexusPos) instanceof NexusBlockEntity nexus) {
                    cooldown = 40;
                    mob.swing(InteractionHand.MAIN_HAND);
                    nexus.damageNexus(2, mob);
                }
                return;
            }
            if (--repath <= 0) {
                repath = 20;
                Vec3 delta = center.subtract(mob.position());
                double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                Vec3 target = horizontal > 20.0D ? mob.position().add(delta.scale(20.0D / horizontal)) : center;
                mob.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
            }
        }
    }

    /** Spawns one foreign mob on the spawn ring. */
    public static boolean spawn(ServerLevel level, NexusBlockEntity nexus, EntityType<?> type, int wave) {
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return false;
        }
        if (!nexus.getSpawner().placeOnRing(level, mob)) {
            mob.discard();
            return false;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.EVENT, null, null);
        mob.setPersistenceRequired();
        tag(mob, nexus.getBlockPos());
        LateWaveScaling.apply(mob, wave, level.random);
        EliteMobs.maybeMakeElite(mob, nexus, level.random);
        level.addFreshEntity(mob);
        nexus.noteSpawned();
        return true;
    }

    private ForeignInvaders() {
    }
}
