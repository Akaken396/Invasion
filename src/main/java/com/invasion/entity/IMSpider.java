package com.invasion.entity;

import com.invasion.entity.ai.InvasionTargetGoal;
import com.invasion.entity.ai.NexusAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
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

/** Fast wall-climbing invader. It cannot dig, but climbs over most defences. */
public class IMSpider extends Spider implements InvasionMob {
    private final NexusLink link = new NexusLink();

    public IMSpider(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createInvasionAttributes() {
        return Spider.createAttributes().add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(4, new NexusAttackGoal<>(this, 1.0D));
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
    public int getNexusDamage() {
        return 1;
    }

    @Override
    public int getNexusAttackInterval() {
        return 25;
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
