package com.invasion.entity;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A big, slow brute that hurls boulders at defenders and the Nexus, smashing blocks on impact. */
public class IMThrower extends Zombie implements InvasionMob {
    private static final double THROW_RANGE = 24.0D;

    private final NexusLink link = new NexusLink();
    private int throwCooldown = 60;

    public IMThrower(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 6.0D)
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
    public float getDigSpeed() {
        return 2.0F;
    }

    @Override
    public int getNexusDamage() {
        return 5;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            link.tick(this);
            if (--throwCooldown <= 0) {
                throwCooldown = 20;
                Vec3 aim = findThrowTarget();
                if (aim != null) {
                    throwBoulder(aim);
                    throwCooldown = 80 + random.nextInt(40);
                }
            }
        }
    }

    @Nullable
    private Vec3 findThrowTarget() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            double dist = distanceTo(target);
            if (dist > 4.0D && dist < THROW_RANGE && hasLineOfSight(target)) {
                return target.getEyePosition();
            }
            return null;
        }
        NexusBlockEntity nexus = link.getNexus(level());
        if (nexus != null) {
            Vec3 center = nexus.getCenter();
            double dist = position().distanceTo(center);
            if (dist > 5.0D && dist < THROW_RANGE) {
                return center;
            }
        }
        return null;
    }

    private void throwBoulder(Vec3 aim) {
        Boulder boulder = new Boulder(level(), this);
        double dx = aim.x - getX();
        double dz = aim.z - getZ();
        double dy = aim.y - boulder.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        boulder.shoot(dx, dy + horizontal * 0.2D, dz, 1.3F, 3.0F);
        swing(InteractionHand.MAIN_HAND);
        playSound(SoundEvents.WITCH_THROW, 1.0F, 0.5F);
        level().addFreshEntity(boulder);
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
    public void setBaby(boolean baby) {
        super.setBaby(false);
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
