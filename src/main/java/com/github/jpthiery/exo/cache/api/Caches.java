package com.github.jpthiery.exo.cache.api;

import com.github.jpthiery.exo.cache.impl.DefaultCache;

import static java.util.Objects.requireNonNull;

public interface Caches {

    default <K,V> Cache<K,V> getInstance(ValueProvider<K,V> valueProvider) {
        requireNonNull(valueProvider, "valueProvider must be defined.");
        return new DefaultCache<>(valueProvider);
    }

}
