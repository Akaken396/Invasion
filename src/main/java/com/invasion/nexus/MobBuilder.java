package com.invasion.nexus;

import com.invasion.entity.InvasionMob;
import com.invasion.nexus.wave.EntityConstruct;
import com.invasion.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Creates invaders from wave constructs. */
public final class MobBuilder {
    public static EntityType<? extends Mob> typeOf(EntityConstruct construct) {
        return switch (construct.type()) {
            case ZOMBIE -> ModEntities.ZOMBIE.get();
            case ZOMBIE_PIGMAN -> ModEntities.ZOMBIE_PIGMAN.get();
            case SPIDER -> ModEntities.SPIDER.get();
            case SKELETON -> ModEntities.SKELETON.get();
            case PIG_ENGINEER -> ModEntities.PIG_ENGINEER.get();
            case THROWER -> ModEntities.THROWER.get();
            case IMP -> ModEntities.IMP.get();
            case CREEPER -> ModEntities.CREEPER.get();
        };
    }

    @Nullable
    public static Mob create(EntityConstruct construct, Level level, @Nullable BlockPos nexusPos) {
        Mob mob = typeOf(construct).create(level);
        if (mob instanceof InvasionMob invader) {
            invader.getNexusLink().setNexus(nexusPos);
            invader.applyConstruct(construct);
            if (nexusPos != null) {
                mob.setPersistenceRequired();
            }
            return mob;
        }
        if (mob != null) {
            mob.discard();
        }
        return null;
    }

    private MobBuilder() {
    }
}
