package com.financeanalyzer.util;

import java.io.Serializable;
import java.util.Objects;

/**
 * Generic immutable pair container.
 * Visibly demonstrates Unit 2 Generics and Objects utility usage.
 *
 * @param <A> first element type
 * @param <B> second element type
 */
public final class Pair<A, B> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
