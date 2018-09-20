package com.github.jpthiery.exo.cache.api;

public interface Cache <K,V> {

    V get(K key);

}
