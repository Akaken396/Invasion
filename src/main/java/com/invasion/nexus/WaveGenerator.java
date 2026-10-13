package com.invasion.nexus;

import com.invasion.Config;
import com.invasion.registry.ModEntities;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Builds the list of invaders for a wave. Stronger mob types unlock as the
 * power level increases and waves grow larger over time.
 */
public final class WaveGenerator {
    private record Entry(Supplier<? extends EntityType<?>> type, int weight, int minPower) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry(ModEntities.ZOMBIE, 12, 1),
            new Entry(ModEntities.SPIDER, 6, 2),
            new Entry(ModEntities.SKELETON, 6, 3),
            new Entry(ModEntities.PIG_ENGINEER, 4, 4),
            new Entry(ModEntities.CREEPER, 4, 5),
            new Entry(ModEntities.IMP, 5, 6),
            new Entry(ModEntities.THROWER, 2, 8));

    public static int waveSize(int power) {
        return Math.min(Config.MAX_WAVE_SIZE.get(), 4 + power * 2 + power * power / 8);
    }

    public static List<EntityType<?>> generate(int power, RandomSource random) {
        List<Entry> available = new ArrayList<>();
        int totalWeight = 0;
        for (Entry entry : ENTRIES) {
            if (power >= entry.minPower()) {
                available.add(entry);
                totalWeight += entry.weight();
            }
        }

        int size = waveSize(power);
        List<EntityType<?>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int roll = random.nextInt(totalWeight);
            for (Entry entry : available) {
                roll -= entry.weight();
                if (roll < 0) {
                    result.add(entry.type().get());
                    break;
                }
            }
        }
        return result;
    }

    private WaveGenerator() {
    }
}
