package com.invasion.nexus.wave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One timed spawn group inside a {@link Wave}: spawns {@code amount} mobs, spread
 * over the time window, from a selection pool, inside an angle range around the Nexus.
 * All times are in milliseconds (one tick = 50 ms), as in the original mod.
 */
public class WaveEntry {
    private static final Random RANDOM = new Random();

    private final int timeBegin;
    private final int timeEnd;
    private final int amount;
    private final int granularity;
    private final ISelect<EntityPattern> pool;
    private final List<EntityConstruct> spawnList = new ArrayList<>();
    private final Map<Integer, String> alerts = new HashMap<>();
    private int minAngle;
    private int maxAngle;
    private int minPointsInRange;
    private int amountQueued;
    private int elapsed;
    private int toNextSpawn;
    private int nextAlert = Integer.MAX_VALUE;

    public WaveEntry(int timeBegin, int timeEnd, int amount, int granularity, ISelect<EntityPattern> pool) {
        this(timeBegin, timeEnd, amount, granularity, pool, -180, 180, 1);
    }

    public WaveEntry(int timeBegin, int timeEnd, int amount, int granularity, ISelect<EntityPattern> pool,
                     int angleRange, int minPointsInRange) {
        this(timeBegin, timeEnd, amount, granularity, pool, 0, 0, minPointsInRange);
        this.minAngle = RANDOM.nextInt(360) - 180;
        this.maxAngle = this.minAngle + angleRange;
        while (this.maxAngle > 180) {
            this.maxAngle -= 360;
        }
    }

    public WaveEntry(int timeBegin, int timeEnd, int amount, int granularity, ISelect<EntityPattern> pool,
                     int minAngle, int maxAngle, int minPointsInRange) {
        this.timeBegin = timeBegin;
        this.timeEnd = timeEnd;
        this.amount = amount;
        this.granularity = granularity;
        this.pool = pool;
        this.minAngle = minAngle;
        this.maxAngle = maxAngle;
        this.minPointsInRange = minPointsInRange;
    }

    public WaveEntry alert(String translationKey, int timeElapsed) {
        alerts.put(timeElapsed, translationKey);
        nextAlert = Math.min(nextAlert, timeElapsed);
        return this;
    }

    public int doNextSpawns(int elapsedMillis, ISpawnerAccess spawner, boolean spawnMode) {
        toNextSpawn -= elapsedMillis;
        if (nextAlert <= elapsed - toNextSpawn) {
            sendNextAlert(spawner, spawnMode);
        }
        if (toNextSpawn <= 0) {
            elapsed += granularity;
            toNextSpawn += granularity;
            if (toNextSpawn < 0) {
                elapsed -= toNextSpawn;
                toNextSpawn = 0;
            }
            int amountToSpawn = amount * elapsed / (timeEnd - timeBegin) - amountQueued;
            if (amountToSpawn > 0) {
                if (amountToSpawn + amountQueued > amount) {
                    amountToSpawn = amount - amountQueued;
                }
                int guard = 0;
                while (amountToSpawn > 0 && guard++ < 1000) {
                    EntityPattern pattern = pool.selectNext();
                    if (pattern == null) {
                        continue;
                    }
                    amountToSpawn--;
                    amountQueued++;
                    spawnList.add(pattern.generate());
                }
            }
        }
        if (spawnList.isEmpty()) {
            return 0;
        }
        int spawned = 0;
        if (spawner.getNumberOfPointsInRange(minAngle, maxAngle) >= minPointsInRange) {
            for (int i = spawnList.size() - 1; i >= 0; i--) {
                if (!spawnMode || spawner.attemptSpawn(spawnList.get(i), minAngle, maxAngle)) {
                    spawned++;
                    spawnList.remove(i);
                }
            }
        } else {
            reviseSpawnAngles(spawner);
        }
        return spawned;
    }

    public void resetToBeginning() {
        elapsed = 0;
        amountQueued = 0;
        toNextSpawn = 0;
        spawnList.clear();
        pool.reset();
    }

    public int getTimeBegin() {
        return timeBegin;
    }

    public int getTimeEnd() {
        return timeEnd;
    }

    public int getAmount() {
        return amount;
    }

    public int getPending() {
        return spawnList.size() + Math.max(0, amount - amountQueued);
    }

    private void sendNextAlert(ISpawnerAccess spawner, boolean spawnMode) {
        String message = alerts.remove(nextAlert);
        if (message != null && spawnMode) {
            spawner.sendSpawnAlert(message);
        }
        nextAlert = Integer.MAX_VALUE;
        for (Integer key : alerts.keySet()) {
            nextAlert = Math.min(nextAlert, key);
        }
    }

    private void reviseSpawnAngles(ISpawnerAccess spawner) {
        int angleRange = maxAngle - minAngle;
        while (angleRange < 0) {
            angleRange += 360;
        }
        if (angleRange == 0) {
            angleRange = 360;
        }
        List<Integer> validAngles = new ArrayList<>();
        for (int angle = -180; angle < 180; angle += angleRange) {
            int nextAngle = angle + angleRange;
            if (nextAngle >= 180) {
                nextAngle -= 360;
            }
            if (spawner.getNumberOfPointsInRange(angle, nextAngle) >= minPointsInRange) {
                validAngles.add(angle);
            }
        }
        if (!validAngles.isEmpty()) {
            minAngle = validAngles.get(RANDOM.nextInt(validAngles.size()));
            maxAngle = minAngle + angleRange;
            while (maxAngle >= 180) {
                maxAngle -= 360;
            }
        }
        if (minPointsInRange > 1) {
            minPointsInRange = 1;
        } else {
            minAngle = -180;
            maxAngle = 180;
        }
    }
}
