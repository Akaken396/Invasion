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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** The basic invader: walks to the Nexus and digs through anything in its way. */
public class IMZombie extends Zombie implements InvasionMob {
    private final NexusLink link = new NexusLink();

    public IMZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Zombie.createAttributes()
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
    public void onInvasionSpawn(int power) {
        InvasionMob.super.onInvasionSpawn(power);
        if (power >= 6 && random.nextInt(3) == 0) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(power >= 12 ? Items.IRON_SWORD : Items.STONE_SWORD));
        }
        if (power >= 8 && random.nextInt(3) == 0) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, new Zombie.ZombieGroupData(false, false), tag);
        setBaby(false);
        return result;
    }

    @Override
    protected void handleAttributes(float difficulty) {
        // No random reinforcement / knockback bonuses.
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
