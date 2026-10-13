package com.invasion.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

/**
 * Targets nearby players within a short range, so invaders keep focusing on the
 * Nexus. As in the original: within the "sense" range (6) players are noticed
 * even through walls, within the "aggro" range (12) only when visible.
 */
public class InvasionTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    public static final double AGGRO_RANGE = 12.0D;
    public static final double SENSE_RANGE = 6.0D;

    private final double range;

    public InvasionTargetGoal(Mob mob, Class<T> targetType, double range, boolean mustSee) {
        super(mob, targetType, mustSee);
        this.range = range;
        TargetingConditions conditions = TargetingConditions.forCombat().range(range);
        this.targetConditions = mustSee ? conditions : conditions.ignoreLineOfSight();
    }

    @Override
    protected double getFollowDistance() {
        // Called from the super constructor before 'range' is set.
        return range > 0.0D ? range : AGGRO_RANGE;
    }
}
