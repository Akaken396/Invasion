package com.invasion.nexus.wave;

/** Something that can repeatedly pick an element, possibly from nested pools. */
public interface ISelect<T> {
    T selectNext();

    void reset();
}
