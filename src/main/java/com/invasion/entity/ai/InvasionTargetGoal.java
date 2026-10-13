package com.invasion.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Targets nearby players, but only within a short range so invaders keep
 * focusing on the Nexus instead of chasing players across the map.
 */
public class InvasionTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    public InvasionTargetGoal(Mob mob, Class<T> targetType) {
        super(mob, targetType, true);
    }

    @Override
    protected double getFollowDistance() {
        return 14.0D;
    }
}
