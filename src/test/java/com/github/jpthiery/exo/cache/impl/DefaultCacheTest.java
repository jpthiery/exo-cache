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

    @Test
    public void get_single_key() {
        //  given

        var key = "1234";
        var expectedValue = key.hashCode();

        var valueProvider = new HashValueProvider();
        var cache = getInstance(valueProvider);

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

        var valueProvider = new HashValueProvider();
        var cache = getInstance(valueProvider);

        executeInMultiThreadedContext(4_000_00, cache, valueProvider, key, expectedValue);
    }

    //@Test
    public void single_key_call_be_several_threads_performance() {
        var key = "Matrix";
        var expectedValue = key.hashCode();


        var valueProvider = new LongHashValueProvider(10);
        var cache = getInstance(valueProvider);

        var start = System.currentTimeMillis();
        executeInMultiThreadedContext(4_000_00, cache, valueProvider, key, expectedValue);
        var end = System.currentTimeMillis();
        var durationInSeconde = TimeUnit.SECONDS.convert((end - start), TimeUnit.MILLISECONDS);
        assertThat(durationInSeconde)
                .as("Duration must be less than 15 secondes")
                .isLessThan(15);
    }

    public void executeInMultiThreadedContext(int nbTasks,Cache<String,Integer> cache,  HashValueProvider currentValueProvider, String key, Integer expectedValue) {
        //  given
        var executor = Executors.newFixedThreadPool(10);


        var task = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return cache.get(key);
            }
        };

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


        assertThat(currentValueProvider.getNbCall())
                .as("ValueProvider had to provided a single value.")
                .isEqualTo(1);

        assertThat(currentValueProvider.getNbCallForKey(key))
                .as("ValueProvider had to provided a single value for key %s", key)
                .isEqualTo(1);

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

    private class LongHashValueProvider extends HashValueProvider {

        private final int waitingTimeInSeconds;

        private LongHashValueProvider(int waitingTimeInSeconds) {
            this.waitingTimeInSeconds = waitingTimeInSeconds;
        }

        @Override
        public Integer provide(String key) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(waitingTimeInSeconds));
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            return super.provide(key);
        }
    }

}