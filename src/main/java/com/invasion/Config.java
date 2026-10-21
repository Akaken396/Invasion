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
    public static final ForgeConfigSpec.IntValue SPELL_FIRST_DELAY;
    public static final ForgeConfigSpec.IntValue MAX_SPELLS_PER_SECOND;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SPELL_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> UNFAIR_SPELLS;

    // Nexus upgrades, elites, stats, drop chest
    public static final ForgeConfigSpec.IntValue UPGRADE_HP_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue UPGRADE_COST_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue ELITE_START_WAVE;
    public static final ForgeConfigSpec.IntValue ELITE_EVERY_WAVES;
    public static final ForgeConfigSpec.IntValue ELITE_MAX;
    public static final ForgeConfigSpec.DoubleValue ELITE_HEALTH;
    public static final ForgeConfigSpec.DoubleValue REPAIR_KIT_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue WAVE_STATS;
    public static final ForgeConfigSpec.BooleanValue DROP_CHEST;
    public static final ForgeConfigSpec.BooleanValue ROLES_ENABLED;
    public static final ForgeConfigSpec.IntValue ROLE_XP_PER_KILL;
    public static final ForgeConfigSpec.IntValue ROLE_XP_PER_ELITE;
    public static final ForgeConfigSpec.IntValue ROLE_XP_PER_WAVE;

    // Player protection
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BLOCKED_PLAYER_EFFECTS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BLOCKED_PLAYER_VEHICLES;
    public static final ForgeConfigSpec.IntValue RESPAWN_GRACE_SECONDS;
    public static final ForgeConfigSpec.IntValue RESPAWN_CLEAR_RADIUS;

    // Special waves
    public static final ForgeConfigSpec.IntValue ENGINEERS_BASE;
    public static final ForgeConfigSpec.IntValue ENGINEERS_MAX;
    public static final ForgeConfigSpec.IntValue ENGINEER_TOWER_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue WARD_CHANCE;
    public static final ForgeConfigSpec.DoubleValue WARD_CHANCE_LATE;
    public static final ForgeConfigSpec.IntValue SPELL_WAVE_INTERVAL;
    public static final ForgeConfigSpec.IntValue FOREIGN_WAVE_INTERVAL;
    public static final ForgeConfigSpec.IntValue FOREIGN_MOBS_BASE;
    public static final ForgeConfigSpec.IntValue FOREIGN_MOBS_MAX;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> FOREIGN_MOBS;
    public static final ForgeConfigSpec.IntValue POWER_STEP_START;
    public static final ForgeConfigSpec.IntValue POWER_STEP_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue POWER_STEP_HEALTH;
    public static final ForgeConfigSpec.DoubleValue POWER_STEP_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue POWER_STEP_SPEED;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BLOCKED_ENTITIES;

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
                .defineInRange("maxLateWaveMobs", 400, 10, 10000);
        MAX_ALIVE_MOBS = BUILDER.comment("Maximum number of invaders alive at once per Nexus, in every wave (performance).")
                .defineInRange("maxAliveInvaders", 150, 5, 1000);
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
        SPELL_CAST_INTERVAL = BUILDER.comment("Cooldown in seconds after a caster casts a spell (randomly up to 50% longer).",
                        "If the spell's own cooldown in Iron's Spells is longer, that one is used.")
                .defineInRange("spellCooldownSeconds", 20, 1, 3600);
        SPELL_FIRST_DELAY = BUILDER.comment("Seconds after spawning before a caster may cast its first spell (randomly up to twice as long).")
                .defineInRange("spellFirstDelaySeconds", 10, 0, 3600);
        MAX_SPELLS_PER_SECOND = BUILDER.comment("Maximum number of spells cast by all invaders together per second (also in spell waves).")
                .defineInRange("maxSpellsPerSecond", 2, 1, 1000);
        SPELL_BLACKLIST = BUILDER.comment("Spells that invaders never cast.")
                .defineList("spellBlacklist", java.util.List.of(
                        "irons_spellbooks:none", "irons_spellbooks:recall", "irons_spellbooks:portal", "irons_spellbooks:planar_sight",
                        "irons_spellbooks:heal", "irons_spellbooks:greater_heal", "irons_spellbooks:cleanse", "irons_spellbooks:invisibility",
                        "irons_spellbooks:gluttony", "irons_spellbooks:angel_wing", "irons_spellbooks:telekinesis",
                        "irons_spellbooks:counterspell", "irons_spellbooks:frost_step", "irons_spellbooks:evasion"), o -> o instanceof String);
        UNFAIR_SPELLS = BUILDER.comment("More spells invaders never cast: spells that hold, pin or pull players (root, slow, black hole...).")
                .defineList("holdingSpells", java.util.List.of(
                        "irons_spellbooks:root", "irons_spellbooks:slow", "irons_spellbooks:blight", "irons_spellbooks:black_hole",
                        "irons_spellbooks:gravity", "irons_spellbooks:ice_block", "irons_spellbooks:frostwave",
                        "irons_spellbooks:sculk_tentacles", "irons_spellbooks:earthquake"), o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("upgrades");
        UPGRADE_HP_PER_LEVEL = BUILDER.comment("Extra Nexus hit points per level of the HP upgrade (bought with rift flux in the Nexus GUI).")
                .defineInRange("hpPerLevel", 25, 1, 1000);
        UPGRADE_COST_MULTIPLIER = BUILDER.comment("Multiplier for the rift flux cost of all Nexus upgrades.")
                .defineInRange("costMultiplier", 1.0D, 0.0D, 100.0D);
        BUILDER.pop();

        BUILDER.push("elites");
        ELITE_START_WAVE = BUILDER.comment("First wave with elite invaders (named, glowing, with a special trait; they drop Nexus Repair Kits).")
                .defineInRange("eliteStartWave", 5, 1, 100000);
        ELITE_EVERY_WAVES = BUILDER.comment("Elites per wave: 1 + (wave - eliteStartWave) / eliteEveryWaves.")
                .defineInRange("eliteEveryWaves", 10, 1, 100000);
        ELITE_MAX = BUILDER.comment("Maximum number of elites per wave.")
                .defineInRange("eliteMax", 5, 0, 100);
        ELITE_HEALTH = BUILDER.comment("Health multiplier of elites.")
                .defineInRange("eliteHealthMultiplier", 3.0D, 1.0D, 100.0D);
        REPAIR_KIT_AMOUNT = BUILDER.comment("Share of the Nexus maximum hit points restored by a Nexus Repair Kit.")
                .defineInRange("repairKitAmount", 0.3D, 0.01D, 1.0D);
        BUILDER.pop();

        BUILDER.push("misc");
        WAVE_STATS = BUILDER.comment("Post the kills and damage of each player to the chat at the end of every wave.")
                .define("waveStats", true);
        DROP_CHEST = BUILDER.comment("Drops of mobs dying near a Nexus go into the chest linked to it with the Nexus Link Wand (less item entities = less lag).")
                .define("dropChest", true);
        BUILDER.pop();

        BUILDER.push("roles");
        ROLES_ENABLED = BUILDER.comment("Player roles (/invasionrole or the Nexus GUI): Ars mage, Iron's mage, melee, TaCZ gunner.",
                        "Each role levels up separately (max 10) and its bonuses only apply near a Nexus under attack.")
                .define("enabled", true);
        ROLE_XP_PER_KILL = BUILDER.comment("Role experience per invader killed.")
                .defineInRange("xpPerKill", 1, 0, 10000);
        ROLE_XP_PER_ELITE = BUILDER.comment("Extra role experience per elite killed.")
                .defineInRange("xpPerElite", 10, 0, 10000);
        ROLE_XP_PER_WAVE = BUILDER.comment("Role experience for every player who dealt damage in a wave, when the wave ends.")
                .defineInRange("xpPerWave", 15, 0, 10000);
        BUILDER.pop();

        BUILDER.push("playerProtection");
        BLOCKED_PLAYER_EFFECTS = BUILDER.comment("Effects that cannot be put on players near a Nexus under attack (gravity, snares, slows...).")
                .defineList("blockedPlayerEffects", java.util.List.of(
                        "ars_nouveau:gravity", "ars_nouveau:snare", "irons_spellbooks:slowed", "irons_spellbooks:airborne",
                        "irons_spellbooks:rooted", "irons_spellbooks:frozen", "irons_spellbooks:chilled"), o -> o instanceof String);
        BLOCKED_PLAYER_VEHICLES = BUILDER.comment("Spell entities players cannot be stuck in near a Nexus under attack (root...).")
                .defineList("blockedPlayerVehicles", java.util.List.of("irons_spellbooks:root", "irons_spellbooks:ice_block"),
                        o -> o instanceof String);
        RESPAWN_GRACE_SECONDS = BUILDER.comment("After respawning, invaders ignore and cannot hurt the player for this long",
                        "(ends early when the player attacks). 0 = off.")
                .defineInRange("respawnGraceSeconds", 15, 0, 600);
        RESPAWN_CLEAR_RADIUS = BUILDER.comment("On respawn, spells, fields and summons of invaders within this radius are removed. 0 = off.")
                .defineInRange("respawnClearRadius", 24, 0, 256);
        BUILDER.pop();

        BUILDER.push("specialWaves");
        ENGINEERS_BASE = BUILDER.comment("Extra pigman engineers in every wave from wave 6 on: base + wave / 4.")
                .defineInRange("engineersBase", 1, 0, 1000);
        ENGINEERS_MAX = BUILDER.comment("Maximum number of extra pigman engineers per wave.")
                .defineInRange("engineersMax", 30, 0, 1000);
        ENGINEER_TOWER_DISTANCE = BUILDER.comment("When the Nexus is above them and out of reach, pigman engineers stop this far from it (horizontally),",
                        "build a tower with a ladder up to the height of the Nexus and then a bridge across to it. 0 = off.")
                .defineInRange("engineerTowerDistance", 10, 0, 64);
        WARD_CHANCE = BUILDER.comment("Chance for an invader to be warded against magic (Iron's Spells / Ars Nouveau) before the late waves.")
                .defineInRange("wardChance", 0.05D, 0.0D, 1.0D);
        WARD_CHANCE_LATE = BUILDER.comment("Chance for an invader to be warded against magic from lateWaveStart on.")
                .defineInRange("wardChanceLate", 0.2D, 0.0D, 1.0D);
        SPELL_WAVE_INTERVAL = BUILDER.comment("Every N waves: every invader casts Iron's Spells 'n Spellbooks spells. 0 = off.")
                .defineInRange("spellWaveInterval", 10, 0, 1000);
        FOREIGN_WAVE_INTERVAL = BUILDER.comment("Every N waves: mobs of other mods (non-boss mobs of All the Mods 9) join the wave. 0 = off.")
                .defineInRange("foreignWaveInterval", 15, 0, 1000);
        FOREIGN_MOBS_BASE = BUILDER.comment("Number of foreign mobs in such a wave: base + wave / 3.")
                .defineInRange("foreignMobsBase", 10, 0, 1000);
        FOREIGN_MOBS_MAX = BUILDER.comment("Maximum number of foreign mobs per wave.")
                .defineInRange("foreignMobsMax", 60, 0, 1000);
        FOREIGN_MOBS = BUILDER.comment("Mobs of other mods used in foreign waves. Mobs of mods that are not installed are skipped.")
                .defineList("foreignMobs", java.util.List.of(
                        "cataclysm:deepling", "cataclysm:deepling_angler", "cataclysm:deepling_brute", "cataclysm:deepling_priest",
                        "cataclysm:deepling_warlock", "cataclysm:ender_golem", "cataclysm:endermaptera", "cataclysm:ignited_revenant",
                        "cataclysm:koboleton", "cataclysm:amethyst_crab", "cataclysm:the_prowler", "cataclysm:the_watcher",
                        "irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer", "irons_spellbooks:necromancer",
                        "irons_spellbooks:apothecarist", "irons_spellbooks:citadel_keeper",
                        "undergarden:rotwalker", "undergarden:rotbeast", "undergarden:rotling", "undergarden:brute",
                        "undergarden:forgotten", "undergarden:nargoyle", "undergarden:stoneborn", "undergarden:muncher",
                        "undergarden:sploogie", "twilightforest:redcap", "twilightforest:skeleton_druid", "twilightforest:kobold",
                        "twilightforest:minotaur", "twilightforest:troll", "twilightforest:wraith", "twilightforest:fire_beetle",
                        "twilightforest:slime_beetle", "twilightforest:pinch_beetle", "twilightforest:hostile_wolf",
                        "twilightforest:winter_wolf", "twilightforest:yeti", "artifacts:mimic"), o -> o instanceof String);
        POWER_STEP_START = BUILDER.comment("From this wave on the invaders get stronger every powerStepInterval waves.")
                .defineInRange("powerStepStart", 100, 1, 100000);
        POWER_STEP_INTERVAL = BUILDER.comment("Waves per power step.")
                .defineInRange("powerStepInterval", 5, 1, 1000);
        POWER_STEP_HEALTH = BUILDER.comment("Extra health per power step (0.25 = +25%).")
                .defineInRange("powerStepHealth", 0.25D, 0.0D, 100.0D);
        POWER_STEP_DAMAGE = BUILDER.comment("Extra attack damage per power step.")
                .defineInRange("powerStepDamage", 0.1D, 0.0D, 100.0D);
        POWER_STEP_SPEED = BUILDER.comment("Extra movement speed per power step (capped at +50%).")
                .defineInRange("powerStepSpeed", 0.03D, 0.0D, 10.0D);
        BLOCKED_ENTITIES = BUILDER.comment("Entities that may not spawn near an active Nexus (e.g. EvilCraft vengeance spirits).")
                .defineList("blockedEntities", java.util.List.of("evilcraft:vengeance_spirit"), o -> o instanceof String);
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
