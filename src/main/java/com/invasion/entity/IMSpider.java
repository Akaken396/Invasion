package com.invasion.entity;

import com.invasion.Config;
import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import com.invasion.nexus.wave.EntityConstruct;
import com.invasion.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Invading spider. T1: normal spider (flavour 0) or tiny pouncing baby (1).
 * T2: jumping spider (0) or mother spider (1) that lays eggs full of babies.
 */
public class IMSpider extends Spider implements InvasionMob {
    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(IMSpider.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLAVOUR = SynchedEntityData.defineId(IMSpider.class, EntityDataSerializers.INT);

    private static final int EGG_LAY_DELAY = 230;

    private final NexusLink link = new NexusLink();
    private boolean statsApplied;
    private int eggs = 1;
    private int eggTimer = 25;

    public IMSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Spider.createAttributes().add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_TIER, 1);
        entityData.define(DATA_FLAVOUR, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F) {
            @Override
            public boolean canUse() {
                return canPounce() && super.canUse();
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(4, new NexusAttackGoal<>(this, 1.0D));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new InvasionTargetGoal<>(this, Player.class, InvasionTargetGoal.SENSE_RANGE, false));
        targetSelector.addGoal(2, new InvasionTargetGoal<>(this, Player.class, InvasionTargetGoal.AGGRO_RANGE, true));
    }

    public int getTier() {
        return entityData.get(DATA_TIER);
    }

    public int getFlavour() {
        return entityData.get(DATA_FLAVOUR);
    }

    public boolean isBabySpider() {
        return getTier() == 1 && getFlavour() == 1;
    }

    public boolean isJumper() {
        return getTier() == 2 && getFlavour() == 0;
    }

    public boolean isMother() {
        return getTier() == 2 && getFlavour() == 1;
    }

    private boolean canPounce() {
        return isBabySpider() || isJumper();
    }

    /** 0 = normal, 1 = jumping spider, 2 = mother spider (same ids as the original renderer). */
    public int getTextureId() {
        return isJumper() ? 1 : isMother() ? 2 : 0;
    }

    public float getScaleAmount() {
        return isBabySpider() ? 0.35F : isMother() ? 1.3F : 1.0F;
    }

    @Override
    public void applyConstruct(@Nullable EntityConstruct construct) {
        if (construct != null) {
            entityData.set(DATA_TIER, Math.max(1, construct.tier()));
            entityData.set(DATA_FLAVOUR, construct.flavour());
        }
        int tier = getTier();
        int flavour = getFlavour();
        double health;
        double speed;
        double damage;
        if (tier >= 2) {
            if (flavour == 1) {
                health = Config.HP_SPIDER_T2_MOTHER.get();
                speed = 0.22D;
                damage = 4;
            } else {
                health = Config.HP_SPIDER_T2_JUMPER.get();
                speed = 0.3D;
                damage = 5;
            }
        } else if (flavour == 1) {
            health = Config.HP_SPIDER_T1_BABY.get();
            speed = 0.34D;
            damage = 1;
        } else {
            health = Config.HP_SPIDER_T1.get();
            speed = 0.29D;
            damage = 3;
        }
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed * 1.2D);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        setHealth(getMaxHealth());
        statsApplied = true;
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getScaleAmount());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_TIER.equals(key) || DATA_FLAVOUR.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        // No vanilla spider jockeys or potion effects for invaders.
        if (!getPassengers().isEmpty()) {
            getPassengers().forEach(passenger -> passenger.discard());
        }
        removeAllEffects();
        if (!statsApplied) {
            applyConstruct(null);
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            link.tick(this);
            if (isMother()) {
                tickEggLaying();
            }
        }
    }

    private void tickEggLaying() {
        LivingEntity target = getTarget();
        if (eggs <= 0 || target == null || !getSensing().hasLineOfSight(target)) {
            return;
        }
        if (--eggTimer <= 0) {
            eggTimer = EGG_LAY_DELAY;
            eggs--;
            SpiderEgg egg = new SpiderEgg(ModEntities.SPIDER_EGG.get(), level());
            egg.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
            egg.setup(link.getNexusPos(), 6);
            level().addFreshEntity(egg);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !link.hasNexus() && super.removeWhenFarAway(distance);
    }

    @Override
    public NexusLink getNexusLink() {
        return link;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Tier", getTier());
        tag.putInt("Flavour", getFlavour());
        tag.putInt("Eggs", eggs);
        link.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
        if (tag.contains("Tier")) {
            entityData.set(DATA_TIER, Math.max(1, tag.getInt("Tier")));
            entityData.set(DATA_FLAVOUR, tag.getInt("Flavour"));
            eggs = tag.getInt("Eggs");
            statsApplied = true;
            refreshDimensions();
        }
    }
}
