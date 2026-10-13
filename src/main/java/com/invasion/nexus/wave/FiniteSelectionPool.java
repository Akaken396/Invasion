package com.invasion.nexus.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Picks every entry exactly as often as specified (in random order), then starts over. */
public class FiniteSelectionPool<T> implements ISelect<T> {
    private final List<ISelect<T>> entries = new ArrayList<>();
    private final List<Integer> original = new ArrayList<>();
    private final List<Integer> remaining = new ArrayList<>();
    private final Random rand = new Random();
    private int totalAmount;
    private int originalAmount;

    public FiniteSelectionPool<T> add(T entry, int amount) {
        return addPool(new SingleSelection<>(entry), amount);
    }

    public FiniteSelectionPool<T> addPool(ISelect<T> entry, int amount) {
        entries.add(entry);
        original.add(amount);
        remaining.add(amount);
        totalAmount += amount;
        originalAmount = totalAmount;
        return this;
    }

    @Override
    public T selectNext() {
        if (totalAmount < 1) {
            reset();
        }
        if (totalAmount < 1) {
            return null;
        }
        int r = rand.nextInt(totalAmount);
        for (int i = 0; i < entries.size(); i++) {
            int left = remaining.get(i);
            if (r < left) {
                remaining.set(i, left - 1);
                totalAmount--;
                return entries.get(i).selectNext();
            }
            r -= left;
        }
        return null;
    }

    @Override
    public void reset() {
        totalAmount = originalAmount;
        for (int i = 0; i < entries.size(); i++) {
            remaining.set(i, original.get(i));
        }
    }
}
