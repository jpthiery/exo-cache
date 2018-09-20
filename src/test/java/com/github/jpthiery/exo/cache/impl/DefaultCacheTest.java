package com.github.jpthiery.exo.cache.impl;

import com.github.jpthiery.exo.cache.api.Cache;
import com.github.jpthiery.exo.cache.api.Caches;
import com.github.jpthiery.exo.cache.api.ValueProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class DefaultCacheTest implements Caches {

    private Cache<String, Integer> cache;
    private HashValueProvider valueProvider;

    @Test
    public void get_single_key() {
        //  given

        var key = "1234";
        var expectedValue = key.hashCode();

        //  when

        var value = cache.get(key);

        //  then

        assertThat(value)
                .as("Value received must be %s", expectedValue)
                .isNotNull()
                .isEqualTo(expectedValue);

        assertThat(valueProvider.getNbCall())
                .as("ValueProvider had to provided a single value.")
                .isEqualTo(1);

        assertThat(valueProvider.getNbCallForKey(key))
                .as("ValueProvider had to provided a single value for key %s", key)
                .isEqualTo(1);

    }

    @Test
    public void single_key_call_be_several_threads() {
        var key = "Matrix";
        var expectedValue = key.hashCode();

        var task = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return cache.get(key);
            }
        };
        executeInMultiThreadedContext(4_000_00, task, key, expectedValue);
    }

    //@Test
    public void single_key_call_be_several_threads_performance() {
        var key = "Matrix";
        var expectedValue = key.hashCode();

        var task = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(10000);
                return cache.get(key);
            }
        };
        var start = System.currentTimeMillis();
        executeInMultiThreadedContext(4_000_00, task, key, expectedValue);
        var end = System.currentTimeMillis();
        var durationInSeconde = TimeUnit.SECONDS.convert((end - start), TimeUnit.MILLISECONDS);
        assertThat(durationInSeconde)
                .as("Duration must be less than 15 secondes")
                .isLessThan(15);
    }

    public void executeInMultiThreadedContext(int nbTasks, Callable<Integer> task, String key, Integer expectedValue) {
        //  given
        var executor = Executors.newFixedThreadPool(10);


        var tasks = IntStream.range(0, nbTasks)
                .boxed()
                .map(i -> task)
                .collect(Collectors.toList());

        //   when
        List<Future<Integer>> results = null;
        try {
            results = executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        //  then
        var nbValuesReturn = new AtomicInteger(0);
        results.forEach(f -> {
            try {
                var res = f.get();
                assertThat(res)
                        .as("Check values is %s", expectedValue)
                        .isEqualTo(expectedValue);
                nbValuesReturn.incrementAndGet();
            } catch (InterruptedException | ExecutionException e) {
                fail(e.getMessage());
            }
        });
        executor.shutdownNow();

        assertThat(results)
                .as("Result was not null.")
                .isNotNull();
        assertThat(nbValuesReturn.get())
                .isEqualTo(nbTasks);


        assertThat(valueProvider.getNbCall())
                .as("ValueProvider had to provided a single value.")
                .isEqualTo(1);

        assertThat(valueProvider.getNbCallForKey(key))
                .as("ValueProvider had to provided a single value for key %s", key)
                .isEqualTo(1);

    }

    @BeforeEach
    public void setup() {
        valueProvider = new HashValueProvider();
        cache = getInstance(valueProvider);
    }

    private class HashValueProvider implements ValueProvider<String, Integer> {

        private final AtomicInteger nbValueProvideCall = new AtomicInteger(0);

        private final Map<String, AtomicInteger> nbValueProvideForGivenKey = new ConcurrentHashMap<>();

        @Override
        public Integer provide(String key) {
            if (isBlank(key)) {
                throw new IllegalArgumentException("key must be defined and be non blank.");
            }
            nbValueProvideCall.incrementAndGet();
            var previous = nbValueProvideForGivenKey.putIfAbsent(key, new AtomicInteger(1));
            if (previous != null) {
                previous.incrementAndGet();
            }
            return key.hashCode();
        }

        public Integer getNbCall() {
            return nbValueProvideCall.get();
        }

        public Integer getNbCallForKey(String key) {
            var res = nbValueProvideForGivenKey.get(key);
            return res == null ? 0 : res.get();
        }
    }

}