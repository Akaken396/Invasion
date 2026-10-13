package com.invasion;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Nexus
    public static final ForgeConfigSpec.IntValue DEFAULT_SPAWN_RADIUS;
    public static final ForgeConfigSpec.BooleanValue DESTROYED_NEXUS_KILLS_PLAYERS;
    public static final ForgeConfigSpec.IntValue MIN_CONTINUOUS_DAYS;
    public static final ForgeConfigSpec.IntValue MAX_CONTINUOUS_DAYS;

    // Mobs
    public static final ForgeConfigSpec.BooleanValue DESTROYED_BLOCKS_DROP;
    public static final ForgeConfigSpec.DoubleValue DIG_SPEED;

    // Health (as in the original "invasionSpawn-health" settings)
    public static final ForgeConfigSpec.IntValue HP_ZOMBIE_T1;
    public static final ForgeConfigSpec.IntValue HP_ZOMBIE_T2;
    public static final ForgeConfigSpec.IntValue HP_ZOMBIE_T3;
    public static final ForgeConfigSpec.IntValue HP_ZOMBIE_PIGMAN_T1;
    public static final ForgeConfigSpec.IntValue HP_ZOMBIE_PIGMAN_T2;
    public static final ForgeConfigSpec.IntValue HP_ZOMBIE_PIGMAN_T3;
    public static final ForgeConfigSpec.IntValue HP_SPIDER_T1;
    public static final ForgeConfigSpec.IntValue HP_SPIDER_T1_BABY;
    public static final ForgeConfigSpec.IntValue HP_SPIDER_T2_JUMPER;
    public static final ForgeConfigSpec.IntValue HP_SPIDER_T2_MOTHER;
    public static final ForgeConfigSpec.IntValue HP_THROWER_T1;
    public static final ForgeConfigSpec.IntValue HP_THROWER_T2;
    public static final ForgeConfigSpec.IntValue HP_PIG_ENGINEER;
    public static final ForgeConfigSpec.IntValue HP_SKELETON;
    public static final ForgeConfigSpec.IntValue HP_CREEPER;
    public static final ForgeConfigSpec.IntValue HP_IMP;

    static {
        BUILDER.push("nexus");
        DEFAULT_SPAWN_RADIUS = BUILDER.comment("Default spawn radius of a new Nexus. Change it in game with the Nexus Adjuster.")
                .defineInRange("defaultSpawnRadius", 52, 32, 128);
        DESTROYED_NEXUS_KILLS_PLAYERS = BUILDER.comment("Like the original mod: players whose life is bound to a Nexus die when it is destroyed in wave mode.")
                .define("destroyedNexusKillsBoundPlayers", true);
        MIN_CONTINUOUS_DAYS = BUILDER.comment("Minimum number of days between attacks in continuous mode.")
                .defineInRange("minDaysToAttack", 2, 0, 1000);
        MAX_CONTINUOUS_DAYS = BUILDER.comment("Maximum number of days between attacks in continuous mode.")
                .defineInRange("maxDaysToAttack", 3, 0, 1000);
        BUILDER.pop();

        BUILDER.push("mobs");
        DESTROYED_BLOCKS_DROP = BUILDER.comment("Whether blocks destroyed by invaders drop as items. Invaders only destroy blocks when the mobGriefing game rule is on.")
                .define("destructedBlocksDrop", true);
        DIG_SPEED = BUILDER.comment("Multiplier for the digging speed of invaders.")
                .defineInRange("digSpeed", 1.0D, 0.01D, 100.0D);
        BUILDER.pop();

        BUILDER.push("health");
        HP_ZOMBIE_T1 = hp("zombieT1", 20);
        HP_ZOMBIE_T2 = hp("zombieT2", 30);
        HP_ZOMBIE_T3 = hp("zombieT3", 65);
        HP_ZOMBIE_PIGMAN_T1 = hp("zombiePigmanT1", 20);
        HP_ZOMBIE_PIGMAN_T2 = hp("zombiePigmanT2", 30);
        HP_ZOMBIE_PIGMAN_T3 = hp("zombiePigmanT3", 65);
        HP_SPIDER_T1 = hp("spiderT1", 18);
        HP_SPIDER_T1_BABY = hp("spiderT1Baby", 3);
        HP_SPIDER_T2_JUMPER = hp("spiderT2Jumping", 18);
        HP_SPIDER_T2_MOTHER = hp("spiderT2Mother", 23);
        HP_THROWER_T1 = hp("throwerT1", 50);
        HP_THROWER_T2 = hp("throwerT2", 70);
        HP_PIG_ENGINEER = hp("pigEngineerT1", 20);
        HP_SKELETON = hp("skeletonT1", 20);
        HP_CREEPER = hp("creeperT1", 20);
        HP_IMP = hp("impT1", 20);
        BUILDER.pop();
    }

    private static ForgeConfigSpec.IntValue hp(String name, int def) {
        return BUILDER.defineInRange(name, def, 1, 10000);
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
