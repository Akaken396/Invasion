package com.invasion.entity;

import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import com.invasion.nexus.wave.EntityConstruct;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Base class of the humanoid invaders. Holds the Nexus link and the tier /
 * flavour / texture variant (synced to clients so the right model and texture
 * are used), and sets up the common AI of the original mod.
 */
public abstract class InvasionMonster extends Monster implements InvasionMob {
    /** Invaders are allies: Iron's Spells and vanilla targeting skip allied mobs. */
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return InvasionAllies.areAllies(this, other) || super.isAlliedTo(other);
    }

    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(InvasionMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLAVOUR = SynchedEntityData.defineId(InvasionMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TEXTURE = SynchedEntityData.defineId(InvasionMonster.class, EntityDataSerializers.INT);

    protected final NexusLink link = new NexusLink();
    private boolean statsApplied;

    protected InvasionMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createBaseAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_TIER, 1);
        entityData.define(DATA_FLAVOUR, 0);
        entityData.define(DATA_TEXTURE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(2, new NexusAttackGoal<>(this, 1.0D));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new InvasionTargetGoal<>(this, Player.class, InvasionTargetGoal.SENSE_RANGE, false));
        targetSelector.addGoal(2, new InvasionTargetGoal<>(this, Player.class, InvasionTargetGoal.AGGRO_RANGE, true));
    }

    // ------------------------------------------------------------------
    // Variant

    @Override
    public int getTier() {
        return entityData.get(DATA_TIER);
    }

    public int getFlavour() {
        return entityData.get(DATA_FLAVOUR);
    }

    public int getTextureId() {
        return entityData.get(DATA_TEXTURE);
    }

    @Override
    public void applyConstruct(EntityConstruct construct) {
        setVariant(construct.tier(), construct.flavour(), construct.texture());
    }

    public void setVariant(int tier, int flavour, int texture) {
        entityData.set(DATA_TIER, Math.max(1, tier));
        entityData.set(DATA_FLAVOUR, Math.max(0, flavour));
        entityData.set(DATA_TEXTURE, texture != 0 ? texture : chooseTexture(getTier(), getFlavour()));
        applyStats(getTier(), getFlavour());
        statsApplied = true;
        setHealth(getMaxHealth());
        refreshDimensions();
    }

    /** Sets attributes and equipment for the variant. */
    protected abstract void applyStats(int tier, int flavour);

    protected int chooseTexture(int tier, int flavour) {
        return 0;
    }

    /** Horizontal scale used for size and rendering (vertical scale is (2 + s) / 3). */
    public float getScaleAmount() {
        return 1.0F;
    }

    protected void setStats(double health, double speed, double damage) {
        setBase(Attributes.MAX_HEALTH, health);
        setBase(Attributes.MOVEMENT_SPEED, speed);
        setBase(Attributes.ATTACK_DAMAGE, damage);
    }

    protected void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float s = getScaleAmount();
        return super.getDimensions(pose).scale(s, (2.0F + s) / 3.0F);
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
        if (!statsApplied) {
            setVariant(getTier(), getFlavour(), getTextureId());
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Nexus

    @Override
    public NexusLink getNexusLink() {
        return link;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            link.tick(this);
            // Invaders bound to a Nexus don't burn; independent ones do, like the original night mobs.
            if (!link.hasNexus() && isAlive() && isSunBurnTick() && !fireImmune()) {
                setSecondsOnFire(8);
            }
        }
    }

    /** Invaders aren't swept away by flowing water (they used to get stuck in streams). */
    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !link.hasNexus() && super.removeWhenFarAway(distance);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Tier", getTier());
        tag.putInt("Flavour", getFlavour());
        tag.putInt("TextureId", getTextureId());
        link.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
        if (tag.contains("Tier")) {
            entityData.set(DATA_TIER, Math.max(1, tag.getInt("Tier")));
            entityData.set(DATA_FLAVOUR, tag.getInt("Flavour"));
            entityData.set(DATA_TEXTURE, tag.getInt("TextureId"));
            statsApplied = true;
            refreshDimensions();
        }
    }
}
