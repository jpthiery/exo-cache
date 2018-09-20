package com.github.jpthiery.exo.cache.api;

import static java.util.Objects.requireNonNull;

public abstract class AbstractGenericCache<K,V> implements Cache<K,V>{

    protected final ValueProvider<K,V> valueProvider;

    protected AbstractGenericCache(ValueProvider<K,V> valueProvider) {
        super();
        requireNonNull(valueProvider, "valueProvider must be defined.");
        this.valueProvider = valueProvider;
    }

}
