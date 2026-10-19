package com.invasion.nexus.wave;

import java.util.List;

/** A full wave: a set of timed {@link WaveEntry}s, its total length and the break after it (milliseconds). */
public class Wave {
    private final List<WaveEntry> entries;
    private final int totalTime;
    private final int breakTime;
    private int elapsed;

    public Wave(int totalTime, int breakTime, List<WaveEntry> entries) {
        this.totalTime = totalTime;
        this.breakTime = breakTime;
        this.entries = entries;
    }

    public int doNextSpawns(int elapsedMillis, ISpawnerAccess spawner, boolean spawnMode) {
        int spawned = 0;
        elapsed += elapsedMillis;
        for (WaveEntry entry : entries) {
            if (elapsed >= entry.getTimeBegin() && elapsed < entry.getTimeEnd()) {
                spawned += entry.doNextSpawns(elapsedMillis, spawner, spawnMode);
            }
        }
        return spawned;
    }

    public void addEntry(WaveEntry entry) {
        entries.add(entry);
    }

    public boolean isComplete() {
        return elapsed > totalTime;
    }

    public void reset() {
        elapsed = 0;
        for (WaveEntry entry : entries) {
            entry.resetToBeginning();
        }
    }

    public int getElapsed() {
        return elapsed;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public int getBreakTime() {
        return breakTime;
    }

    public int getTotalMobAmount() {
        int total = 0;
        for (WaveEntry entry : entries) {
            total += entry.getAmount();
        }
        return total;
    }

    /** Mobs of this wave that have not been spawned yet. */
    public int getPendingMobs() {
        int pending = 0;
        for (WaveEntry entry : entries) {
            if (elapsed < entry.getTimeEnd()) {
                pending += entry.getPending();
            }
        }
        return pending;
    }
}
