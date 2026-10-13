package com.invasion.nexus.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Spawn points around a Nexus, queryable by angle range. */
public class SpawnPointContainer {
    private final List<SpawnPoint> points = new ArrayList<>();
    private final Random rand = new Random();

    public void clear() {
        points.clear();
    }

    public void add(SpawnPoint point) {
        points.add(point);
    }

    public int size() {
        return points.size();
    }

    static boolean inRange(int angle, int minAngle, int maxAngle) {
        if (maxAngle - minAngle >= 360 || minAngle == maxAngle) {
            return true;
        }
        if (minAngle <= maxAngle) {
            return angle >= minAngle && angle <= maxAngle;
        }
        // Range wraps around +-180.
        return angle >= minAngle || angle <= maxAngle;
    }

    public int count(int minAngle, int maxAngle) {
        int n = 0;
        for (SpawnPoint point : points) {
            if (inRange(point.angle(), minAngle, maxAngle)) {
                n++;
            }
        }
        return n;
    }

    public SpawnPoint random() {
        return points.isEmpty() ? null : points.get(rand.nextInt(points.size()));
    }

    public SpawnPoint random(int minAngle, int maxAngle) {
        List<SpawnPoint> matching = new ArrayList<>();
        for (SpawnPoint point : points) {
            if (inRange(point.angle(), minAngle, maxAngle)) {
                matching.add(point);
            }
        }
        return matching.isEmpty() ? null : matching.get(rand.nextInt(matching.size()));
    }
}
