package com.invasion;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Nexus
    public static final ForgeConfigSpec.IntValue DEFAULT_SPAWN_RADIUS;
    public static final ForgeConfigSpec.BooleanValue DESTROYED_NEXUS_KILLS_PLAYERS;
    public static final ForgeConfigSpec.IntValue MIN_CONTINUOUS_DAYS;
    public static final ForgeConfigSpec.IntValue MAX_CONTINUOUS_DAYS;

    // Late waves / performance
    public static final ForgeConfigSpec.IntValue LATE_WAVE_START;
    public static final ForgeConfigSpec.IntValue MAX_WAVE_MOBS;
    public static final ForgeConfigSpec.IntValue MAX_ALIVE_MOBS;
    public static final ForgeConfigSpec.IntValue MAX_TNT_THROWERS;
    public static final ForgeConfigSpec.DoubleValue LATE_HEALTH_BONUS;
    public static final ForgeConfigSpec.DoubleValue LATE_HEALTH_PER_WAVE;
    public static final ForgeConfigSpec.DoubleValue LATE_DAMAGE_BONUS;
    public static final ForgeConfigSpec.DoubleValue LATE_DAMAGE_PER_WAVE;
    public static final ForgeConfigSpec.DoubleValue SPELL_CASTER_CHANCE;
    public static final ForgeConfigSpec.IntValue SPELL_CAST_INTERVAL;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SPELL_BLACKLIST;

    // Boss rush
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BOSS_RUSH_BOSSES;
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_PER_ROUND;
    public static final ForgeConfigSpec.IntValue BOSS_RUSH_REST_SECONDS;
    public static final ForgeConfigSpec.IntValue BOSS_RUSH_ESCORTS;

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

        BUILDER.push("lateWaves");
        LATE_WAVE_START = BUILDER.comment("From this wave on: mob count cap, armour and stat bonuses, vindicators/evokers instead of zombies,",
                        "warded pigman engineers and Iron's Spells casters.")
                .defineInRange("lateWaveStart", 50, 12, 10000);
        MAX_WAVE_MOBS = BUILDER.comment("Maximum number of mobs in one wave from lateWaveStart on (performance).")
                .defineInRange("maxWaveMobs", 150, 10, 10000);
        MAX_ALIVE_MOBS = BUILDER.comment("Maximum number of invaders alive at once per Nexus, in every wave (performance).")
                .defineInRange("maxAliveMobs", 80, 5, 1000);
        MAX_TNT_THROWERS = BUILDER.comment("Maximum number of TNT throwing throwers (tier 2) alive at once. Extra ones become tier 1.")
                .defineInRange("maxTntThrowers", 10, 0, 1000);
        LATE_HEALTH_BONUS = BUILDER.comment("Extra health of late-wave mobs at lateWaveStart (0.5 = +50%).")
                .defineInRange("healthBonus", 0.5D, 0.0D, 1000.0D);
        LATE_HEALTH_PER_WAVE = BUILDER.comment("Additional health per wave after lateWaveStart.")
                .defineInRange("healthPerWave", 0.05D, 0.0D, 100.0D);
        LATE_DAMAGE_BONUS = BUILDER.comment("Extra attack damage of late-wave mobs at lateWaveStart.")
                .defineInRange("damageBonus", 0.25D, 0.0D, 1000.0D);
        LATE_DAMAGE_PER_WAVE = BUILDER.comment("Additional attack damage per wave after lateWaveStart.")
                .defineInRange("damagePerWave", 0.03D, 0.0D, 100.0D);
        SPELL_CASTER_CHANCE = BUILDER.comment("Share of late-wave mobs that cast random Iron's Spells 'n Spellbooks spells (needs that mod).")
                .defineInRange("spellCasterChance", 0.1D, 0.0D, 1.0D);
        SPELL_CAST_INTERVAL = BUILDER.comment("Seconds between two spells of a caster.")
                .defineInRange("spellCastInterval", 6, 1, 600);
        SPELL_BLACKLIST = BUILDER.comment("Spells that invaders never cast.")
                .defineList("spellBlacklist", java.util.List.of(
                        "irons_spellbooks:none", "irons_spellbooks:recall", "irons_spellbooks:portal", "irons_spellbooks:planar_sight",
                        "irons_spellbooks:heal", "irons_spellbooks:greater_heal", "irons_spellbooks:cleanse", "irons_spellbooks:invisibility",
                        "irons_spellbooks:gluttony", "irons_spellbooks:angel_wing", "irons_spellbooks:telekinesis",
                        "irons_spellbooks:counterspell", "irons_spellbooks:frost_step", "irons_spellbooks:evasion"), o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("bossRush");
        BOSS_RUSH_BOSSES = BUILDER.comment("Bosses of the boss rush, in order (modelled on the progression of All the Mods 9).",
                        "Bosses from mods that are not installed are skipped. Special ids: invasion:brute_lord, invasion:brood_mother, invasion:thrower_titan.")
                .defineList("bosses", java.util.List.of(
                        "invasion:brute_lord",
                        "twilightforest:naga",
                        "blue_skies:summoner",
                        "twilightforest:lich",
                        "invasion:brood_mother",
                        "twilightforest:minoshroom",
                        "blue_skies:alchemist",
                        "cataclysm:netherite_monstrosity",
                        "twilightforest:alpha_yeti",
                        "blue_skies:starlit_crusher",
                        "blue_skies:arachnarch",
                        "minecraft:wither",
                        "invasion:thrower_titan",
                        "cataclysm:ignis",
                        "twilightforest:snow_queen",
                        "undergarden:forgotten_guardian",
                        "irons_spellbooks:dead_king",
                        "ars_nouveau:wilden_boss",
                        "cataclysm:the_harbinger",
                        "cataclysm:ender_guardian",
                        "cataclysm:ancient_remnant",
                        "allthemodium:piglich",
                        "minecraft:warden"), o -> o instanceof String);
        BOSS_HEALTH_MULTIPLIER = BUILDER.comment("Health multiplier applied to every boss of the boss rush.")
                .defineInRange("bossHealthMultiplier", 1.0D, 0.1D, 1000.0D);
        BOSS_HEALTH_PER_ROUND = BUILDER.comment("Extra health per round (0.1 = +10% per round).")
                .defineInRange("bossHealthPerRound", 0.1D, 0.0D, 100.0D);
        BOSS_RUSH_REST_SECONDS = BUILDER.comment("Pause between two bosses.")
                .defineInRange("restSeconds", 30, 0, 3600);
        BOSS_RUSH_ESCORTS = BUILDER.comment("Invaders that come with each boss (plus one per round).")
                .defineInRange("escorts", 2, 0, 100);
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
