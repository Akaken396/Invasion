package com.invasion.nexus.wave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Wave definitions ported from the original Invasion Mod (1.7.10): eleven hand-made
 * waves, endlessly scaling waves after that, and a generator for continuous mode.
 */
public final class WaveBuilder {
    public static final String ALERT_SMALL_GROUP = "message.invasion.alert.small_group";
    public static final String ALERT_GROUP = "message.invasion.alert.group";
    public static final String ALERT_LARGE = "message.invasion.alert.large";

    private static final Map<String, EntityPattern> PATTERNS = new HashMap<>();
    private static final Random RANDOM = new Random();

    static {
        PATTERNS.put("zombie_t1_any", new EntityPattern(IMEntityType.ZOMBIE).tier(1, 1).flavour(0, 3).flavour(1, 1));
        PATTERNS.put("zombie_t2_any_basic", new EntityPattern(IMEntityType.ZOMBIE).tier(2, 1).flavour(0, 2).flavour(1, 1).flavour(2, 0.4F));
        PATTERNS.put("zombie_t2_plain", new EntityPattern(IMEntityType.ZOMBIE).tier(2, 1).flavour(0, 1));
        PATTERNS.put("zombie_t2_tar", new EntityPattern(IMEntityType.ZOMBIE).tier(2, 1).flavour(2, 1).texture(5, 1));
        PATTERNS.put("zombie_t3_any", new EntityPattern(IMEntityType.ZOMBIE).tier(3, 1));
        PATTERNS.put("zombiePigman_t1_any", new EntityPattern(IMEntityType.ZOMBIE_PIGMAN).tier(1, 1));
        PATTERNS.put("zombiePigman_t2_any", new EntityPattern(IMEntityType.ZOMBIE_PIGMAN).tier(2, 1));
        PATTERNS.put("zombiePigman_t3_any", new EntityPattern(IMEntityType.ZOMBIE_PIGMAN).tier(3, 1));
        PATTERNS.put("spider_t1_any", new EntityPattern(IMEntityType.SPIDER).tier(1, 1));
        PATTERNS.put("spider_t2_any", new EntityPattern(IMEntityType.SPIDER).tier(2, 1).flavour(0, 1).flavour(1, 1));
        PATTERNS.put("pigengy_t1_any", new EntityPattern(IMEntityType.PIG_ENGINEER).tier(1, 1));
        PATTERNS.put("skeleton_t1_any", new EntityPattern(IMEntityType.SKELETON).tier(1, 1));
        PATTERNS.put("thrower_t1", new EntityPattern(IMEntityType.THROWER).tier(1, 1));
        PATTERNS.put("thrower_t2", new EntityPattern(IMEntityType.THROWER).tier(2, 1));
        PATTERNS.put("creeper_t1_basic", new EntityPattern(IMEntityType.CREEPER).tier(1, 1));
        PATTERNS.put("imp_t1", new EntityPattern(IMEntityType.IMP).tier(1, 1));
        PATTERNS.put("vindicator", new EntityPattern(IMEntityType.VINDICATOR).tier(1, 1));
        PATTERNS.put("evoker", new EntityPattern(IMEntityType.EVOKER).tier(1, 1));
        PATTERNS.put("pigengy_t2_warded", new EntityPattern(IMEntityType.PIG_ENGINEER).tier(2, 1));
    }

    public static EntityPattern pattern(String name) {
        EntityPattern pattern = PATTERNS.get(name);
        return pattern != null ? pattern : PATTERNS.get("zombie_t1_any");
    }

    private static RandomSelectionPool<EntityPattern> random() {
        return new RandomSelectionPool<>();
    }

    private static FiniteSelectionPool<EntityPattern> finite() {
        return new FiniteSelectionPool<>();
    }

    // ------------------------------------------------------------------
    // Main invasion (wave mode)

    public static Wave mainInvasionWave(int wave) {
        Wave result = buildMainWave(wave);
        int engineers = wave < 6 ? 0 : Math.min(com.invasion.Config.ENGINEERS_MAX.get(), com.invasion.Config.ENGINEERS_BASE.get() + wave / 4);
        if (engineers > 0) {
            // Extra pigman engineers spread over the wave, so there is always someone to build bridges and ladders.
            result.addEntry(new WaveEntry(0, (int) (result.getTotalTime() * 0.85F), engineers, 2000,
                    late("pigengy_t1_any", wave >= com.invasion.Config.LATE_WAVE_START.get())));
        }
        return result;
    }

    private static Wave buildMainWave(int wave) {
        if (wave > 11) {
            return extendedWave(wave);
        }
        List<WaveEntry> e = new ArrayList<>();
        switch (wave) {
            case 1 -> {
                e.add(new WaveEntry(0, 90000, 8, 2000, random().add(pattern("zombie_t1_any"), 3).add(pattern("spider_t1_any"), 1)));
                e.add(new WaveEntry(70000, 73000, 3, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t1_any"), 2), 25, 3));
                return new Wave(110000, 15000, e);
            }
            case 2 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 3).add(pattern("spider_t1_any"), 1)
                        .add(pattern("skeleton_t1_any"), 0.7F).add(pattern("zombiePigman_t1_any"), 0.5F).add(pattern("creeper_t1_basic"), 0.038F);
                e.add(new WaveEntry(0, 50000, 5, 2000, base, 110, 5));
                e.add(new WaveEntry(50000, 100000, 5, 2000, base.copy(), 110, 5));
                e.add(new WaveEntry(20000, 23000, 1, 500, random().add(pattern("pigengy_t1_any"), 1)));
                e.add(new WaveEntry(65000, 68000, 3, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t1_any"), 2), 25, 2));
                return new Wave(120000, 15000, e);
            }
            case 3 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 3).add(pattern("spider_t1_any"), 1)
                        .add(pattern("skeleton_t1_any"), 0.7F).add(pattern("creeper_t1_basic"), 0.04F);
                e.add(new WaveEntry(0, 30000, 6, 2000, base, 45, 3));
                e.add(new WaveEntry(80000, 100000, 5, 2000, base.copy(), 45, 3));
                e.add(new WaveEntry(10000, 12000, 1, 500, random().add(pattern("spider_t2_any"), 1)));
                e.add(new WaveEntry(50000, 55000, 5, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t1_any"), 1)
                        .add(pattern("zombie_t2_plain"), 1).add(pattern("skeleton_t1_any"), 1).add(pattern("spider_t1_any"), 1)
                        .add(pattern("creeper_t1_basic"), 1), 25, 6).alert(ALERT_SMALL_GROUP, 0));
                return new Wave(120000, 18000, e);
            }
            case 4 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 3).add(pattern("spider_t1_any"), 1)
                        .add(pattern("skeleton_t1_any"), 0.7F).add(pattern("creeper_t1_basic"), 0.058F);
                e.add(new WaveEntry(0, 50000, 6, 2000, base, 110, 5));
                e.add(new WaveEntry(50000, 100000, 6, 2000, base.copy(), 110, 5));
                e.add(new WaveEntry(0, 90000, 3, 500, finite().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)));
                e.add(new WaveEntry(70000, 75000, 2, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t2_any_basic"), 1)
                        .add(pattern("zombiePigman_t2_any"), 1), 25, 2));
                return new Wave(120000, 18000, e);
            }
            case 5 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 3).add(pattern("spider_t1_any"), 1)
                        .add(pattern("skeleton_t1_any"), 0.7F).add(pattern("creeper_t1_basic"), 0.054F);
                e.add(new WaveEntry(0, 40000, 6, 2000, base, 110, 5));
                e.add(new WaveEntry(40000, 80000, 6, 2000, base.copy(), 110, 5));
                e.add(new WaveEntry(0, 80000, 3, 500, random().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)
                        .add(pattern("zombiePigman_t1_any"), 0.5F)));
                e.add(new WaveEntry(115000, 118000, 8, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t1_any"), 3)
                        .add(pattern("zombie_t2_any_basic"), 1).add(pattern("skeleton_t1_any"), 1).add(pattern("spider_t2_any"), 1)
                        .add(pattern("thrower_t1"), 1), 35, 5).alert(ALERT_LARGE, 0));
                e.add(new WaveEntry(135000, 165000, 7, 500, finite().add(pattern("zombie_t1_any"), 3).add(pattern("zombie_t2_any_basic"), 1)
                        .add(pattern("skeleton_t1_any"), 1).add(pattern("spider_t2_any"), 1).add(pattern("spider_t1_any"), 1)));
                return new Wave(130000, 80000, e);
            }
            case 6 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 2).add(pattern("zombie_t2_any_basic"), 1)
                        .add(pattern("spider_t1_any"), 0.7F).add(pattern("skeleton_t1_any"), 0.7F).add(pattern("creeper_t1_basic"), 0.064F);
                e.add(new WaveEntry(0, 50000, 7, 2000, base, 110, 5));
                e.add(new WaveEntry(50000, 100000, 6, 2000, base.copy(), 110, 5));
                e.add(new WaveEntry(0, 90000, 2, 500, finite().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)));
                e.add(new WaveEntry(70000, 75000, 4, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t2_any_basic"), 2)
                        .add(pattern("zombie_t1_any"), 1).add(pattern("zombiePigman_t1_any"), 1), 25, 2));
                return new Wave(110000, 25000, e);
            }
            case 7 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 2).add(pattern("zombie_t2_any_basic"), 1)
                        .add(pattern("spider_t1_any"), 0.7F).add(pattern("skeleton_t1_any"), 0.7F).add(pattern("creeper_t1_basic"), 0.064F);
                e.add(new WaveEntry(0, 30000, 7, 2000, base, 45, 5));
                e.add(new WaveEntry(0, 60000, 3, 500, finite().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)
                        .add(pattern("thrower_t1"), 1)));
                e.add(new WaveEntry(65000, 67000, 5, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t2_any_basic"), 2)
                        .add(pattern("zombie_t1_any"), 1).add(pattern("zombiePigman_t1_any"), 1).add(pattern("spider_t2_any"), 1), 45, 2));
                e.add(new WaveEntry(95000, 97000, 4, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombiePigman_t1_any"), 3), 45, 2));
                return new Wave(120000, 36000, e);
            }
            case 8 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 2).add(pattern("zombie_t2_any_basic"), 1.5F)
                        .add(pattern("spider_t1_any"), 0.7F).add(pattern("skeleton_t1_any"), 0.7F).add(pattern("creeper_t1_basic"), 0.064F);
                e.add(new WaveEntry(0, 35000, 7, 2000, base, 110, 5));
                e.add(new WaveEntry(80000, 110000, 4, 2000, base.copy(), 110, 5));
                e.add(new WaveEntry(0, 90000, 2, 500, finite().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)));
                e.add(new WaveEntry(60000, 63000, 8, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombie_t2_any_basic"), 3)
                        .add(pattern("zombie_t1_any"), 2).add(pattern("skeleton_t1_any"), 1).add(pattern("creeper_t1_basic"), 1), 25, 2)
                        .alert(ALERT_GROUP, 0));
                return new Wave(110000, 30000, e);
            }
            case 9 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 2).add(pattern("zombie_t2_any_basic"), 2)
                        .add(pattern("spider_t1_any"), 0.7F).add(pattern("skeleton_t1_any"), 0.7F).add(pattern("creeper_t1_basic"), 0.074F);
                e.add(new WaveEntry(0, 30000, 7, 2000, base, 45, 5));
                e.add(new WaveEntry(0, 90000, 3, 500, finite().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)));
                e.add(new WaveEntry(65000, 67000, 6, 500, finite().add(pattern("pigengy_t1_any"), 1).add(pattern("zombiePigman_t1_any"), 3)
                        .add(pattern("zombie_t1_any"), 1).add(pattern("skeleton_t1_any"), 1).add(pattern("zombie_t3_any"), 1), 25, 3));
                e.add(new WaveEntry(95000, 97000, 6, 500, finite().add(pattern("zombie_t2_any_basic"), 2).add(pattern("zombie_t1_any"), 3)
                        .add(pattern("spider_t2_any"), 1), 45, 2));
                return new Wave(120000, 35000, e);
            }
            case 10 -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 1.5F).add(pattern("zombie_t2_any_basic"), 2.2F)
                        .add(pattern("zombiePigman_t1_any"), 0.7F).add(pattern("spider_t1_any"), 0.7F).add(pattern("skeleton_t1_any"), 0.7F)
                        .add(pattern("creeper_t1_basic"), 0.084F);
                e.add(new WaveEntry(0, 40000, 9, 2000, base, 110, 5));
                e.add(new WaveEntry(40000, 80000, 7, 2000, base.copy(), 110, 5));
                e.add(new WaveEntry(0, 80000, 3, 500, random().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)));
                e.add(new WaveEntry(125000, 128000, 12, 500, finite().add(pattern("pigengy_t1_any"), 2).add(pattern("zombie_t1_any"), 2)
                        .add(pattern("zombie_t2_any_basic"), 2).add(pattern("zombiePigman_t1_any"), 3).add(pattern("skeleton_t1_any"), 1)
                        .add(pattern("spider_t2_any"), 1).add(pattern("thrower_t1"), 1), 35, 5).alert(ALERT_LARGE, 0));
                e.add(new WaveEntry(152000, 170000, 7, 500, finite().add(pattern("zombie_t1_any"), 2).add(pattern("zombie_t2_any_basic"), 2)
                        .add(pattern("skeleton_t1_any"), 1).add(pattern("spider_t2_any"), 1).add(pattern("spider_t1_any"), 2)));
                return new Wave(172000, 60000, e);
            }
            default -> {
                RandomSelectionPool<EntityPattern> base = random().add(pattern("zombie_t1_any"), 1.5F).add(pattern("zombie_t2_any_basic"), 2.2F)
                        .add(pattern("zombie_t3_any"), 0.185F).add(pattern("zombiePigman_t1_any"), 0.8F).add(pattern("skeleton_t1_any"), 0.7F)
                        .add(pattern("thrower_t1"), 0.1F).add(pattern("creeper_t1_basic"), 0.064F);
                e.add(new WaveEntry(0, 30000, 7, 2000, base, 45, 5));
                e.add(new WaveEntry(0, 90000, 3, 500, finite().add(pattern("spider_t2_any"), 1).add(pattern("pigengy_t1_any"), 1)));
                e.add(new WaveEntry(65000, 67000, 7, 500, random().add(pattern("pigengy_t1_any"), 1).add(pattern("zombiePigman_t1_any"), 2)
                        .add(pattern("zombie_t2_any_basic"), 3).add(pattern("zombie_t1_any"), 1).add(pattern("skeleton_t1_any"), 1)
                        .add(pattern("thrower_t1"), 0.8F).add(pattern("creeper_t1_basic"), 0.8F), 25, 3));
                e.add(new WaveEntry(95000, 97000, 6, 500, finite().add(pattern("zombie_t2_any_basic"), 2).add(pattern("zombie_t1_any"), 3)
                        .add(pattern("spider_t2_any"), 1), 45, 2));
                return new Wave(120000, 35000, e);
            }
        }
    }

    /** From the late-wave threshold on, zombies are replaced by illagers and warded engineers join the pig engineers. */
    private static ISelect<EntityPattern> late(String name, boolean late) {
        if (!late) {
            return new SingleSelection<>(pattern(name));
        }
        if (name.startsWith("zombie_t")) {
            return name.equals("zombie_t3_any")
                    ? random().add(pattern("evoker"), 1).add(pattern("vindicator"), 1)
                    : random().add(pattern("vindicator"), 3).add(pattern("evoker"), 1);
        }
        if (name.equals("pigengy_t1_any")) {
            return random().add(pattern("pigengy_t1_any"), 1).add(pattern("pigengy_t2_warded"), 1);
        }
        return new SingleSelection<>(pattern(name));
    }

    private static Wave extendedWave(int wave) {
        boolean late = wave >= com.invasion.Config.LATE_WAVE_START.get();
        float mobScale = (float) Math.pow(1.09F, wave - 11);
        if (late) {
            // 23 = sum of the base amounts below; keep the whole wave under the cap.
            mobScale = Math.min(mobScale, com.invasion.Config.MAX_WAVE_MOBS.get() / 23.0F);
        }
        float timeScale = 1.0F + (wave - 11) * 0.04F;
        if (late) {
            // Late waves: more mobs in a shorter time, so that many invaders are on the field at once.
            timeScale = Math.min(timeScale, 1.5F);
        }
        List<WaveEntry> e = new ArrayList<>();
        RandomSelectionPool<EntityPattern> base = random().addPool(late("zombie_t1_any", late), 1.5F).addPool(late("zombie_t2_any_basic", late), 2.2F)
                .addPool(late("zombie_t3_any", late), 0.26F).addPool(late("zombiePigman_t1_any", late), 0.8F).addPool(late("zombiePigman_t2_any", late), 0.5F)
                .addPool(late("zombiePigman_t3_any", late), 0.054F).addPool(late("skeleton_t1_any", late), 0.7F).addPool(late("thrower_t1", late), 0.18F)
                .addPool(late("thrower_t2", late), 0.054F).addPool(late("creeper_t1_basic", late), 0.054F).addPool(late("imp_t1", late), 0.4F);
        e.add(new WaveEntry(0, (int) (timeScale * 30000), (int) (mobScale * 7), 2000, base, 45, 5));
        e.add(new WaveEntry(0, (int) (timeScale * 90000), (int) (mobScale * 3), 500,
                finite().addPool(late("spider_t2_any", late), 2).addPool(late("pigengy_t1_any", late), 1)));
        RandomSelectionPool<EntityPattern> burst = random().addPool(late("zombiePigman_t1_any", late), 1.5F).addPool(late("zombiePigman_t2_any", late), 0.7F)
                .addPool(late("zombiePigman_t3_any", late), 0.35F).addPool(late("zombie_t2_any_basic", late), 1.5F).addPool(late("spider_t2_any", late), 1)
                .addPool(late("zombie_t1_any", late), 1).addPool(late("skeleton_t1_any", late), 1).addPool(late("thrower_t1", late), 0.5F)
                .addPool(late("thrower_t2", late), 0.42F).addPool(late("zombie_t3_any", late), 0.5F).addPool(late("creeper_t1_basic", late), 0.42F)
                .addPool(late("imp_t1", late), 0.4F);
        e.add(new WaveEntry((int) (timeScale * 65000), (int) (timeScale * 67000), (int) (mobScale * 7), 500, burst, 25, 3));
        e.add(new WaveEntry((int) (timeScale * 95000), (int) (timeScale * 97000), (int) (mobScale * 6), 500,
                finite().addPool(late("zombie_t2_any_basic", late), 2).addPool(late("zombie_t1_any", late), 3).addPool(late("spider_t2_any", late), 1), 45, 2));
        return new Wave((int) (timeScale * 120000), (int) (timeScale * 35000), e);
    }

    // ------------------------------------------------------------------
    // Continuous mode

    public static Wave generateWave(float difficulty, float tierLevel, int lengthSeconds) {
        float basicMobsPerSecond = 0.12F * difficulty;
        int numberOfGroups = 7;
        int numberOfBigGroups = 1;
        float proportionInGroups = 0.5F;
        int mobsPerGroup = Math.round(proportionInGroups * basicMobsPerSecond * lengthSeconds / (numberOfGroups + numberOfBigGroups * 2));
        int mobsPerBigGroup = mobsPerGroup * 2;
        int remainingMobs = (int) (basicMobsPerSecond * lengthSeconds) - mobsPerGroup * numberOfGroups - mobsPerBigGroup * numberOfBigGroups;
        int mobsPerSteady = Math.round(0.7F * remainingMobs / numberOfGroups);
        int extraMobsForFinale = Math.round(0.3F * remainingMobs);
        int extraMobsForCleanup = (int) (basicMobsPerSecond * lengthSeconds * 0.2F);
        float timeForGroups = 0.5F;
        int groupTimeInterval = (int) (lengthSeconds * 1000 * timeForGroups / (numberOfGroups + numberOfBigGroups * 3));
        int steadyTimeInterval = (int) (lengthSeconds * 1000 * (1.0F - timeForGroups) / numberOfGroups);
        int time = 0;
        List<WaveEntry> e = new ArrayList<>();
        for (int i = 0; i < numberOfGroups; i++) {
            if (RANDOM.nextInt(2) == 0) {
                e.add(new WaveEntry(time, time + 3500, mobsPerGroup, 500, groupPool(tierLevel), 25, 3));
                time += groupTimeInterval;
                e.add(new WaveEntry(time, time + steadyTimeInterval, mobsPerSteady, 2000, steadyPool(tierLevel), 160, 5));
                time += steadyTimeInterval;
            } else {
                e.add(new WaveEntry(time, time + steadyTimeInterval, mobsPerSteady, 2000, steadyPool(tierLevel), 160, 5));
                time += steadyTimeInterval;
                e.add(new WaveEntry(time, time + 5000, mobsPerGroup, 500, groupPool(tierLevel), 25, 3));
                time += groupTimeInterval;
            }
        }
        time = (int) (time + groupTimeInterval * 0.75D);
        FiniteSelectionPool<EntityPattern> finale = finite();
        finale.add(pattern("thrower_t1"), mobsPerBigGroup / 5);
        RandomSelectionPool<EntityPattern> finaleRandom = random();
        fillGroupPool(tierLevel + 0.5F, finaleRandom, 6.0F);
        finale.addPool(finaleRandom, mobsPerBigGroup);
        e.add(new WaveEntry(time, time + 8000, mobsPerBigGroup + mobsPerBigGroup / 7, 500, finale, 45, 3).alert(ALERT_LARGE, 0));
        int finaleEnd = (int) (time + groupTimeInterval * 2.25F);
        for (int i = 0; i < 3; i++) {
            e.add(new WaveEntry(time + 5000, finaleEnd, extraMobsForFinale / 2, 500, steadyPool(tierLevel), 160, 5));
        }
        e.add(new WaveEntry(time + 15000, (int) (time + 10000 + groupTimeInterval * 2.25F), extraMobsForCleanup, 500, steadyPool(tierLevel)));
        time = finaleEnd;
        return new Wave(time + 16000, groupTimeInterval * 3, e);
    }

    private static float[] tierWeights(float tierLevel) {
        float[] weights = new float[6];
        for (int i = 0; i < 6; i++) {
            if (tierLevel - i * 0.5F > 0.0F) {
                weights[i] = tierLevel - i <= 1.0F ? tierLevel - i * 0.5F : 1.0F;
            }
        }
        return weights;
    }

    private static RandomSelectionPool<EntityPattern> basicPool(float[] w) {
        RandomSelectionPool<EntityPattern> zombies = random().add(pattern("zombie_t1_any"), w[0])
                .add(pattern("zombie_t2_any_basic"), 2.0F * w[2]).add(pattern("zombiePigman_t1_any"), w[3]);
        RandomSelectionPool<EntityPattern> spiders = random().add(pattern("spider_t1_any"), w[0]).add(pattern("spider_t2_any"), 2.0F * w[2]);
        return random().addPool(zombies, 3.1F).addPool(spiders, 0.7F).add(pattern("skeleton_t1_any"), 0.8F);
    }

    private static void fillGroupPool(float tierLevel, RandomSelectionPool<EntityPattern> pool, float weight) {
        float[] w = tierWeights(tierLevel);
        RandomSelectionPool<EntityPattern> special = random().add(pattern("pigengy_t1_any"), 4.0F).add(pattern("thrower_t1"), 1.1F * w[4])
                .add(pattern("zombie_t3_any"), 1.1F * w[5]).add(pattern("creeper_t1_basic"), 0.7F * w[3]);
        pool.addPool(basicPool(w), weight * 0.8333333F);
        pool.addPool(special, weight * 0.1666667F);
    }

    private static ISelect<EntityPattern> groupPool(float tierLevel) {
        RandomSelectionPool<EntityPattern> pool = random();
        fillGroupPool(tierLevel, pool, 6.0F);
        return pool;
    }

    private static ISelect<EntityPattern> steadyPool(float tierLevel) {
        float[] w = tierWeights(tierLevel);
        RandomSelectionPool<EntityPattern> special = random().add(pattern("pigengy_t1_any"), 3.0F)
                .add(pattern("zombie_t3_any"), 1.1F * w[5]).add(pattern("creeper_t1_basic"), 0.8F * w[3]);
        return random().addPool(basicPool(w), 9.0F).addPool(special, 1.0F);
    }

    private WaveBuilder() {
    }
}
