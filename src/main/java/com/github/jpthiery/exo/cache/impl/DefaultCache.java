package com.github.jpthiery.exo.cache.impl;

import com.github.jpthiery.exo.cache.api.AbstractGenericCache;
import com.github.jpthiery.exo.cache.api.ValueProvider;
import org.slf4j.Logger;

public class DefaultCache<K,V> extends AbstractGenericCache<K,V> {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DefaultCache.class);

    public DefaultCache(ValueProvider<K,V> valueProvider) {
        super(valueProvider);
    }

    @Override
    public V get(K key) {
        //  Your code goes Here !
        LOGGER.trace("Request value for Key {}.", key);
        return valueProvider.provide(key);
    }
}
