package com.invasion.entity;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

/** Walks up to the Nexus (or a defender) and explodes, dealing heavy damage to the Nexus. */
public class IMCreeper extends Creeper implements InvasionMob {
    private static final int NEXUS_EXPLOSION_DAMAGE = 12;

    private final NexusLink link = new NexusLink();
    private boolean primedAtNexus;

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
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new InvasionTargetGoal<>(this, Player.class));
    }

    @Override
    public NexusLink getNexusLink() {
        return link;
    }

    @Override
    public int getNexusAttackInterval() {
        return 10;
    }

    @Override
    public void attackNexus(NexusBlockEntity nexus) {
        primedAtNexus = true;
        setSwellDir(1);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && isAlive()) {
            updateSwell();
        }
        boolean wasAlive = isAlive();
        boolean swelling = getSwellDir() > 0;
        super.tick();
        if (!level().isClientSide && wasAlive && swelling && isRemoved() && primedAtNexus) {
            NexusBlockEntity nexus = link.getNexus(level());
            BlockPos pos = link.getNexusPos();
            if (nexus != null && pos != null && distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 25.0D) {
                nexus.damageNexus(NEXUS_EXPLOSION_DAMAGE);
            }
        }
    }

    /** Replaces the vanilla swell goal: ignite next to defenders, keep fuse burning once primed at the Nexus. */
    private void updateSwell() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && distanceToSqr(target) < 9.0D && hasLineOfSight(target)) {
            setSwellDir(1);
        } else if (!primedAtNexus) {
            setSwellDir(-1);
        }
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
