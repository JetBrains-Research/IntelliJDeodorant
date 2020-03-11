package org.jetbrains.research.intellijdeodorant.utils;

@FunctionalInterface
public interface QuadriFunction<T, U, V, S, R> {
    R apply(T t, U u, V v, S s);
}
