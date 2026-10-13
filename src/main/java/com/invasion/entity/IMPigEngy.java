package com.invasion.entity;

import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import com.invasion.registry.ModItems;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Pig Engineer: builds pillars and bridges to reach the Nexus and digs quickly with its hammer. */
public class IMPigEngy extends Zombie implements InvasionMob {
    private final NexusLink link = new NexusLink();

    public IMPigEngy(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(3, new NexusAttackGoal<>(this, 1.0D));
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
    public boolean canDig() {
        return true;
    }

    @Override
    public boolean canBuild() {
        return true;
    }

    @Override
    public float getDigSpeed() {
        return 1.6F;
    }

    @Override
    public void onInvasionSpawn(int power) {
        InvasionMob.super.onInvasionSpawn(power);
        equipHammer();
    }

    private void equipHammer() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ENGY_HAMMER.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, new Zombie.ZombieGroupData(false, false), tag);
        setBaby(false);
        equipHammer();
        return result;
    }

    @Override
    protected void handleAttributes(float difficulty) {
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
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
