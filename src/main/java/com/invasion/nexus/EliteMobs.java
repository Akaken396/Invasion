package com.invasion.nexus;

import com.invasion.Config;
import com.invasion.InvasionMod;
import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.InvasionAllies;
import com.invasion.entity.InvasionMob;
import com.invasion.entity.InvasionMonster;
import com.invasion.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Elite invaders: named and glowing, much tougher, with one special trait.
 * They drop Nexus Repair Kits (and some rift flux).
 */
@Mod.EventBusSubscriber(modid = InvasionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EliteMobs {
    public static final String TAG = "InvasionElite";
    private static final UUID HEALTH_ID = UUID.fromString("5e0c1a52-3f1d-4e7a-9a4e-0d6f3b1c7e11");
    private static final UUID DAMAGE_ID = UUID.fromString("5e0c1a52-3f1d-4e7a-9a4e-0d6f3b1c7e12");
    private static final UUID SPEED_ID = UUID.fromString("5e0c1a52-3f1d-4e7a-9a4e-0d6f3b1c7e13");

    /** Splitting: splits into three on death. Volatile: explodes on death. Mending: heals nearby invaders. Swift: fast. */
    public enum Trait {
        SPLITTING, VOLATILE, MENDING, SWIFT;

        public Component displayName() {
            return Component.translatable("elite.invasion." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static int elitesForWave(int wave) {
        int start = Config.ELITE_START_WAVE.get();
        if (wave < start) {
            return 0;
        }
        return Math.min(Config.ELITE_MAX.get(), 1 + (wave - start) / Config.ELITE_EVERY_WAVES.get());
    }

    public static boolean isElite(Entity entity) {
        return entity.getPersistentData().contains(TAG);
    }

    public static Trait traitOf(Entity entity) {
        Trait[] all = Trait.values();
        return all[Math.floorMod(entity.getPersistentData().getInt(TAG), all.length)];
    }

    /** Called for every invader spawned by a wave. */
    public static void maybeMakeElite(Mob mob, NexusBlockEntity nexus, RandomSource random) {
        if (nexus.takeEliteSlot(random)) {
            makeElite(mob, Trait.values()[random.nextInt(Trait.values().length)]);
        }
    }

    public static void makeElite(Mob mob, Trait trait) {
        mob.getPersistentData().putInt(TAG, trait.ordinal());
        addModifier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, Config.ELITE_HEALTH.get() - 1.0D);
        addModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, 0.5D);
        if (trait == Trait.SWIFT) {
            addModifier(mob.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, 0.35D);
        }
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.translatable("elite.invasion.name", trait.displayName(), mob.getType().getDescription())
                .withStyle(ChatFormatting.GOLD));
        mob.setCustomNameVisible(true);
        mob.setGlowingTag(true);
        mob.setPersistenceRequired();
    }

    private static void addModifier(AttributeInstance attribute, UUID id, double amount) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addPermanentModifier(new AttributeModifier(id, "Invasion elite", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 40 != 0 || !(entity.level() instanceof ServerLevel level) || !isElite(entity)
                || traitOf(entity) != Trait.MENDING) {
            return;
        }
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(8.0D),
                e -> e.isAlive() && InvasionAllies.isInvader(e) && e.getHealth() < e.getMaxHealth())) {
            other.heal(3.0F);
            level.sendParticles(ParticleTypes.HEART, other.getX(), other.getY() + other.getBbHeight() + 0.3D, other.getZ(),
                    1, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !isElite(entity)) {
            return;
        }
        switch (traitOf(entity)) {
            case VOLATILE -> level.explode(entity, entity.getX(), entity.getY() + 0.5D, entity.getZ(), 2.5F, Level.ExplosionInteraction.NONE);
            case SPLITTING -> split(level, entity);
            default -> {
            }
        }
    }

    private static void split(ServerLevel level, LivingEntity elite) {
        for (int i = 0; i < 3; i++) {
            Entity created = elite.getType().create(level);
            if (!(created instanceof Mob copy)) {
                if (created != null) {
                    created.discard();
                }
                return;
            }
            copy.moveTo(elite.getX() + (level.random.nextDouble() - 0.5D), elite.getY(), elite.getZ() + (level.random.nextDouble() - 0.5D),
                    level.random.nextFloat() * 360.0F, 0.0F);
            if (elite instanceof InvasionMonster source && copy instanceof InvasionMonster target) {
                target.setVariant(source.getTier(), source.getFlavour(), source.getTextureId());
            }
            NexusBlockEntity nexus = null;
            if (elite instanceof InvasionMob invader && copy instanceof InvasionMob copyInvader) {
                copyInvader.getNexusLink().setNexus(invader.getNexusLink().getNexusPos());
                nexus = invader.getNexusLink().getNexus(level);
            } else if (ForeignInvaders.nexusOf(elite) != null) {
                ForeignInvaders.tag(copy, ForeignInvaders.nexusOf(elite));
            }
            copy.setHealth(copy.getMaxHealth() * 0.5F);
            copy.setPersistenceRequired();
            level.addFreshEntity(copy);
            if (nexus != null) {
                nexus.noteSpawned();
            }
        }
        level.sendParticles(ParticleTypes.POOF, elite.getX(), elite.getY() + 1.0D, elite.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.05D);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !isElite(entity)) {
            return;
        }
        add(event, new ItemStack(ModItems.NEXUS_REPAIR_KIT.get()));
        add(event, new ItemStack(ModItems.RIFT_FLUX.get(), 2 + entity.getRandom().nextInt(3)));
    }

    private static void add(LivingDropsEvent event, ItemStack stack) {
        LivingEntity entity = event.getEntity();
        ItemEntity item = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        event.getDrops().add(item);
    }

    private EliteMobs() {
    }
}
