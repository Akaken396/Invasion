package com.invasion.nexus;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.nexus.wave.EntityConstruct;
import com.invasion.nexus.wave.ISpawnerAccess;
import com.invasion.nexus.wave.SpawnPoint;
import com.invasion.nexus.wave.SpawnPointContainer;
import com.invasion.nexus.wave.Wave;
import com.invasion.nexus.wave.WaveBuilder;
import com.invasion.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns the mobs of a {@link Wave} on a ring of spawn points around the Nexus
 * (port of the original IMWaveSpawner). Spawn points are searched on the ring at
 * every height within 64 blocks of the Nexus, so invaders can also come out of caves.
 */
public class WaveSpawner implements ISpawnerAccess {
    public static final int MIN_SPAWN_POINTS = 10;

    private final NexusBlockEntity nexus;
    private final SpawnPointContainer spawnPoints = new SpawnPointContainer();
    @Nullable
    private Wave currentWave;
    private boolean active;
    private boolean waveComplete;
    private long elapsed;

    public WaveSpawner(NexusBlockEntity nexus) {
        this.nexus = nexus;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isWaveComplete() {
        return waveComplete;
    }

    public long getElapsed() {
        return elapsed;
    }

    public int getWaveRestTime() {
        return currentWave == null ? 0 : currentWave.getBreakTime();
    }

    public int getPendingMobs() {
        return currentWave == null || waveComplete ? 0 : currentWave.getPendingMobs();
    }

    public int getWaveProgressPercent() {
        if (currentWave == null || currentWave.getTotalTime() <= 0) {
            return 0;
        }
        return Math.min(100, currentWave.getElapsed() * 100 / currentWave.getTotalTime());
    }

    public void beginNextWave(ServerLevel level, int waveNumber) {
        beginNextWave(level, WaveBuilder.mainInvasionWave(waveNumber));
    }

    public void beginNextWave(ServerLevel level, Wave wave) {
        if (!active || spawnPoints.size() < MIN_SPAWN_POINTS) {
            generateSpawnPoints(level);
        }
        wave.reset();
        currentWave = wave;
        waveComplete = false;
        active = true;
        elapsed = 0;
    }

    /** Fast-forwards a wave to the saved time without spawning (used after loading a world). */
    public int resume(ServerLevel level, Wave wave, long elapsedTime) {
        beginNextWave(level, wave);
        int spawned = 0;
        while (elapsed < elapsedTime && !wave.isComplete()) {
            spawned += wave.doNextSpawns(100, this, false);
            elapsed += 100;
        }
        waveComplete = wave.isComplete();
        return spawned;
    }

    public void stop() {
        active = false;
        currentWave = null;
    }

    /** @return false if there are not enough places to spawn. */
    public boolean spawn(ServerLevel level, int elapsedMillis) {
        elapsed += elapsedMillis;
        if (waveComplete || !active || currentWave == null) {
            return true;
        }
        if (spawnPoints.size() < MIN_SPAWN_POINTS) {
            generateSpawnPoints(level);
            if (spawnPoints.size() < MIN_SPAWN_POINTS) {
                return false;
            }
        }
        currentWave.doNextSpawns(elapsedMillis, this, true);
        if (currentWave.isComplete()) {
            waveComplete = true;
        }
        return true;
    }

    /** Spawns a single invader anywhere on the ring (boss rush escorts). */
    public void spawnEscort(ServerLevel level, EntityConstruct construct) {
        if (spawnPoints.size() < MIN_SPAWN_POINTS) {
            generateSpawnPoints(level);
        }
        if (spawnPoints.size() > 0) {
            attemptSpawn(construct, -180, 180);
        }
    }

    public void invalidateSpawnPoints() {
        spawnPoints.clear();
    }

    // ------------------------------------------------------------------
    // ISpawnerAccess

    @Override
    public int getNumberOfPointsInRange(int minAngle, int maxAngle) {
        return spawnPoints.count(minAngle, maxAngle);
    }

    @Override
    public boolean attemptSpawn(EntityConstruct construct, int minAngle, int maxAngle) {
        if (!(nexus.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        Mob mob = MobBuilder.create(construct, level, nexus.getBlockPos());
        if (mob == null) {
            return true;
        }
        int tries = Math.min(20, getNumberOfPointsInRange(minAngle, maxAngle));
        for (int i = 0; i < tries; i++) {
            SpawnPoint point = maxAngle - minAngle >= 360 ? spawnPoints.random() : spawnPoints.random(minAngle, maxAngle);
            if (point == null) {
                break;
            }
            BlockPos pos = point.pos();
            if (!level.isLoaded(pos)) {
                continue;
            }
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) {
                level.addFreshEntity(mob);
                level.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 20, 0.4D, 0.8D, 0.4D, 0.4D);
                return true;
            }
        }
        mob.discard();
        return false;
    }

    @Override
    public void sendSpawnAlert(String translationKey) {
        nexus.messageBoundPlayers(Component.translatable(translationKey));
    }

    // ------------------------------------------------------------------

    private void generateSpawnPoints(ServerLevel level) {
        spawnPoints.clear();
        BlockPos center = nexus.getBlockPos();
        int radius = nexus.getSpawnRadius();
        AABB box = ModEntities.ZOMBIE.get().getDimensions().makeBoundingBox(0, 0, 0);
        List<SpawnPoint> near = new ArrayList<>();
        List<SpawnPoint> far = new ArrayList<>();
        for (int step = 0; step < 129; step++) {
            int vertical = (step + 1) / 2 * (step % 2 == 1 ? 1 : -1);
            int y = center.getY() + vertical;
            if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight() - 3) {
                continue;
            }
            for (int i = 0; i <= radius * 0.7D + 1.0D; i++) {
                int j = (int) Math.round(radius * Math.cos(Math.asin((double) i / radius)));
                int[][] offsets = {{i, j}, {j, i}, {i, -j}, {j, -i}, {-i, j}, {-j, i}, {-i, -j}, {-j, -i}};
                for (int[] o : offsets) {
                    BlockPos pos = new BlockPos(center.getX() + o[0], y, center.getZ() + o[1]);
                    if (isValidSpawn(level, pos, box)) {
                        int angle = (int) (Math.atan2(center.getZ() - pos.getZ(), center.getX() - pos.getX()) * 180.0D / Math.PI);
                        SpawnPoint point = new SpawnPoint(pos, angle);
                        (Math.abs(vertical) <= 35 ? near : far).add(point);
                    }
                }
            }
        }
        near.forEach(spawnPoints::add);
        if (near.size() < 15) {
            far.forEach(spawnPoints::add);
        }
    }

    private static boolean isValidSpawn(ServerLevel level, BlockPos pos, AABB box) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP) || !ground.getFluidState().isEmpty()) {
            return false;
        }
        AABB moved = box.move(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return level.noCollision(moved) && !level.containsAnyLiquid(moved);
    }
}
