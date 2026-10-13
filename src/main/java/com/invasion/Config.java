package com.invasion;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue DEFAULT_SPAWN_RADIUS;
    public static final ForgeConfigSpec.IntValue NEXUS_MAX_HP;
    public static final ForgeConfigSpec.IntValue MAX_ALIVE_MOBS;
    public static final ForgeConfigSpec.IntValue MAX_WAVE_SIZE;
    public static final ForgeConfigSpec.IntValue WAVE_REST_SECONDS;
    public static final ForgeConfigSpec.IntValue CONTINUOUS_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue KILLS_PER_RIFT_FLUX;
    public static final ForgeConfigSpec.DoubleValue RIFT_FLUX_DROP_CHANCE;
    public static final ForgeConfigSpec.BooleanValue MOBS_DESTROY_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue MOBS_BUILD;
    public static final ForgeConfigSpec.BooleanValue DROP_DESTROYED_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue MAX_DIG_HARDNESS;
    public static final ForgeConfigSpec.DoubleValue DIG_SPEED;

    static {
        BUILDER.push("nexus");
        DEFAULT_SPAWN_RADIUS = BUILDER.comment("Default distance (in blocks) from the Nexus at which invaders spawn.")
                .defineInRange("defaultSpawnRadius", 45, 16, 128);
        NEXUS_MAX_HP = BUILDER.comment("Hit points of the Nexus.")
                .defineInRange("nexusMaxHp", 100, 1, 10000);
        KILLS_PER_RIFT_FLUX = BUILDER.comment("Number of invader kills needed for the Nexus to generate one Rift Flux.")
                .defineInRange("killsPerRiftFlux", 6, 1, 1000);
        BUILDER.pop();

        BUILDER.push("waves");
        MAX_ALIVE_MOBS = BUILDER.comment("Maximum number of invaders alive at the same time per Nexus.")
                .defineInRange("maxAliveMobs", 40, 1, 500);
        MAX_WAVE_SIZE = BUILDER.comment("Maximum number of invaders in a single wave.")
                .defineInRange("maxWaveSize", 80, 1, 1000);
        WAVE_REST_SECONDS = BUILDER.comment("Pause between waves in wave mode.")
                .defineInRange("waveRestSeconds", 20, 0, 3600);
        CONTINUOUS_INTERVAL_SECONDS = BUILDER.comment("Pause between attacks in continuous mode.")
                .defineInRange("continuousIntervalSeconds", 180, 10, 86400);
        RIFT_FLUX_DROP_CHANCE = BUILDER.comment("Chance for an invader to drop Rift Flux on death.")
                .defineInRange("riftFluxDropChance", 0.2D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.push("mobs");
        MOBS_DESTROY_BLOCKS = BUILDER.comment("Whether invaders dig through blocks to reach the Nexus.")
                .define("mobsDestroyBlocks", true);
        MOBS_BUILD = BUILDER.comment("Whether Pig Engineers place blocks (pillars and bridges).")
                .define("mobsBuild", true);
        DROP_DESTROYED_BLOCKS = BUILDER.comment("Whether blocks destroyed by invaders drop as items.")
                .define("dropDestroyedBlocks", false);
        MAX_DIG_HARDNESS = BUILDER.comment("Blocks harder than this cannot be dug by invaders (obsidian = 50).")
                .defineInRange("maxDigHardness", 50.0D, 0.0D, 10000.0D);
        DIG_SPEED = BUILDER.comment("Multiplier for invader digging speed.")
                .defineInRange("digSpeed", 1.0D, 0.01D, 100.0D);
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
