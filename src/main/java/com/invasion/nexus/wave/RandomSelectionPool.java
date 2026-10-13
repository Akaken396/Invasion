package com.invasion.nexus.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Picks entries at random according to their weight. Entries may be nested pools. */
public class RandomSelectionPool<T> implements ISelect<T> {
    private record Entry<T>(ISelect<T> selection, float weight) {
    }

    private final List<Entry<T>> pool = new ArrayList<>();
    private final Random rand = new Random();
    private float totalWeight;

    public RandomSelectionPool<T> add(T entry, float weight) {
        return addPool(new SingleSelection<>(entry), weight);
    }

    public RandomSelectionPool<T> addPool(ISelect<T> entry, float weight) {
        pool.add(new Entry<>(entry, weight));
        totalWeight += weight;
        return this;
    }

    @Override
    public T selectNext() {
        float r = rand.nextFloat() * totalWeight;
        for (Entry<T> entry : pool) {
            if (r < entry.weight()) {
                return entry.selection().selectNext();
            }
            r -= entry.weight();
        }
        return pool.isEmpty() ? null : pool.get(0).selection().selectNext();
    }

    public RandomSelectionPool<T> copy() {
        RandomSelectionPool<T> copy = new RandomSelectionPool<>();
        for (Entry<T> entry : pool) {
            copy.addPool(entry.selection(), entry.weight());
        }
        return copy;
    }

    @Override
    public void reset() {
    }
}
