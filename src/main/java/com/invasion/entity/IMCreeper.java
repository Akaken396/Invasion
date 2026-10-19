package com.invasion.entity;

import com.invasion.Config;
import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import com.invasion.nexus.wave.EntityConstruct;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Creeper that blows itself up to breach walls standing between it and the Nexus. */
public class IMCreeper extends Creeper implements InvasionMob {
    /** Invaders are allies: Iron's Spells and vanilla targeting skip allied mobs. */
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return InvasionAllies.areAllies(this, other) || super.isAlliedTo(other);
    }

    private final NexusLink link = new NexusLink();
    private boolean commitToExplode;

    public IMCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Creeper.createAttributes().add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(3, new NexusAttackGoal<>(this, 1.0D));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new InvasionTargetGoal<>(this, Player.class, InvasionTargetGoal.SENSE_RANGE, false));
        targetSelector.addGoal(2, new InvasionTargetGoal<>(this, Player.class, InvasionTargetGoal.AGGRO_RANGE, true));
    }

    @Override
    public NexusLink getNexusLink() {
        return link;
    }

    @Override
    public void applyConstruct(@Nullable EntityConstruct construct) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Config.HP_CREEPER.get());
        setHealth(getMaxHealth());
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        applyConstruct(null);
        return result;
    }

    /** Blocked on the way to the Nexus: light the fuse and blow the wall away. */
    @Override
    public boolean handleObstacle(BlockPos nexusPos) {
        commitToExplode = true;
        return true;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && isAlive()) {
            LivingEntity target = getTarget();
            boolean nearTarget = target != null && target.isAlive() && distanceToSqr(target) < 9.0D && hasLineOfSight(target);
            setSwellDir(nearTarget || commitToExplode ? 1 : -1);
        }
        super.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            link.tick(this);
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
        link.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
    }
}
