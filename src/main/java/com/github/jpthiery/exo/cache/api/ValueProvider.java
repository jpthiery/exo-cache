package com.github.jpthiery.exo.cache.api;

public interface ValueProvider<K,V> {

    V provide(K key);

}
