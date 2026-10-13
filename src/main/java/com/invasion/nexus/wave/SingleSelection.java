package com.invasion.nexus.wave;

public class SingleSelection<T> implements ISelect<T> {
    private final T value;

    public SingleSelection(T value) {
        this.value = value;
    }

    @Override
    public T selectNext() {
        return value;
    }

    @Override
    public void reset() {
    }
}
