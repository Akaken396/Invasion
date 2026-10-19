package com.invasion.entity;

import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import com.invasion.nexus.wave.EntityConstruct;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Evoker invader of the late waves (replaces zombies from the late-wave threshold on). */
public class IMEvoker extends Evoker implements InvasionMob {
    private final NexusLink link = new NexusLink();

    public IMEvoker(EntityType<? extends Evoker> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Evoker.createAttributes().add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Keep the vanilla combat goals, but don't flee from players or wander: go for the Nexus.
        goalSelector.removeAllGoals(goal -> goal instanceof AvoidEntityGoal<?> || goal instanceof RandomStrollGoal);
        targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?> || goal instanceof HurtByTargetGoal);
        goalSelector.addGoal(5, new NexusAttackGoal<>(this, 1.0D));
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
    }

    @Override
    public boolean handleObstacle(BlockPos nexusPos) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            link.tick(this);
        }
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
