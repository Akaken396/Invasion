package com.invasion.entity;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.nexus.wave.EntityConstruct;
import net.minecraft.core.BlockPos;

/** Common behaviour of all invaders. */
public interface InvasionMob {
    NexusLink getNexusLink();

    /** Applies tier, flavour and texture chosen by the wave spawner (or a spawn egg). */
    void applyConstruct(EntityConstruct construct);

    default int getTier() {
        return 1;
    }

    /** Damage dealt to the Nexus per hit. */
    default int getNexusDamage() {
        return 2;
    }

    /** Ticks between two hits on the Nexus. */
    default int getNexusAttackInterval() {
        return 40;
    }

    /** True while the mob does its own thing (building a tower...) and should not walk to the Nexus. */
    default boolean isBusy() {
        return false;
    }

    default boolean canDig() {
        return false;
    }

    /** Dig rate: dig ticks = block strength * 20 / dig rate. */
    default float getDigRate() {
        return 1.0F;
    }

    default void attackNexus(NexusBlockEntity nexus) {
        nexus.damageNexus(getNexusDamage(), this instanceof net.minecraft.world.entity.Entity entity ? entity : null);
    }

    /**
     * Called when the mob makes no progress towards the Nexus.
     *
     * @return true if the mob handled the obstacle itself (building, exploding, smashing...)
     */
    default boolean handleObstacle(BlockPos nexusPos) {
        return false;
    }
}
