package com.invasion.entity;

import com.invasion.blockentity.NexusBlockEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** Common behaviour of all invaders. */
public interface InvasionMob {
    UUID POWER_HEALTH_UUID = UUID.fromString("8a3d5f0e-36c5-4b8e-9d43-6f6a0c1d7e21");
    UUID POWER_DAMAGE_UUID = UUID.fromString("0b7c2e91-5d4a-4f62-8c1e-3a9f7b2d4e58");

    NexusLink getNexusLink();

    /** Damage dealt to the Nexus per hit. */
    default int getNexusDamage() {
        return 2;
    }

    /** Ticks between two hits on the Nexus. */
    default int getNexusAttackInterval() {
        return 40;
    }

    default boolean canDig() {
        return false;
    }

    default boolean canBuild() {
        return false;
    }

    default float getDigSpeed() {
        return 1.0F;
    }

    /** Called when the mob is next to the Nexus and ready to attack it. */
    default void attackNexus(NexusBlockEntity nexus) {
        nexus.damageNexus(getNexusDamage());
    }

    /** Called right after a Nexus spawned this mob. */
    default void onInvasionSpawn(int power) {
        if (this instanceof Mob mob) {
            applyPowerBonus(mob, power);
        }
    }

    static void applyPowerBonus(Mob mob, int power) {
        int bonus = Math.max(0, power - 3);
        if (bonus <= 0) {
            return;
        }
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(POWER_HEALTH_UUID) == null) {
            health.addPermanentModifier(new AttributeModifier(POWER_HEALTH_UUID, "Invasion power health",
                    Math.min(bonus * 0.08D, 2.0D), AttributeModifier.Operation.MULTIPLY_BASE));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && damage.getModifier(POWER_DAMAGE_UUID) == null) {
            damage.addPermanentModifier(new AttributeModifier(POWER_DAMAGE_UUID, "Invasion power damage",
                    Math.min(bonus * 0.05D, 1.5D), AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}
