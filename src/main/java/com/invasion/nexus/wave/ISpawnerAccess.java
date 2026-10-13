package com.invasion.nexus.wave;

public interface ISpawnerAccess {
    int getNumberOfPointsInRange(int minAngle, int maxAngle);

    boolean attemptSpawn(EntityConstruct construct, int minAngle, int maxAngle);

    void sendSpawnAlert(String translationKey);
}
