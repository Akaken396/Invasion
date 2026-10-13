package com.invasion.entity;

import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Shoots at defenders near its path and attacks the Nexus up close. Does not burn in daylight. */
public class IMSkeleton extends Skeleton implements InvasionMob {
    private final NexusLink link = new NexusLink();

    public IMSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return AbstractSkeleton.createAttributes().add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        // The bow / melee attack goal is added by AbstractSkeleton#reassessWeaponGoal.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new NexusAttackGoal<>(this, 1.0D));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new InvasionTargetGoal<>(this, Player.class));
    }

    @Override
    public NexusLink getNexusLink() {
        return link;
    }

    @Override
    public void onInvasionSpawn(int power) {
        InvasionMob.super.onInvasionSpawn(power);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        return result;
    }

    @Override
    protected boolean isSunBurnTick() {
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
