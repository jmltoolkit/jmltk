/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.cache;

import com.github.javaparser.resolution.cache.Cache;
import com.github.javaparser.resolution.cache.CacheStats;
import java.util.Objects;
import java.util.Optional;

/**
 * This class is used to wrap a Guava {@link com.google.common.cache.Cache}.
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public class GuavaCache<K, V> implements Cache<K, V> {

    /**
     * Wrap a Guava cache with a custom cache.
     *
     * @param guavaCache The guava cache to be wrapped-
     *
     * @param <expectedK> The expected type for the key.
     * @param <expectedV> The expected type for the value.
     *
     * @return A newly created instance of {@link NoCache}.
     */
    public static <expectedK, expectedV> GuavaCache<expectedK, expectedV> create(
            com.google.common.cache.Cache<expectedK, expectedV> guavaCache) {
        return new GuavaCache<>(guavaCache);
    }

    private final com.google.common.cache.Cache<K, V> guavaCache;

    public GuavaCache(com.google.common.cache.Cache<K, V> guavaCache) {
        this.guavaCache = Objects.requireNonNull(guavaCache, "The argument GuavaCache can't be null.");
    }

    @Override
    public void put(K key, V value) {
        guavaCache.put(key, value);
    }

    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(guavaCache.getIfPresent(key));
    }

    @Override
    public void remove(K key) {
        guavaCache.invalidate(key);
    }

    @Override
    public void removeAll() {
        guavaCache.invalidateAll();
    }

    @Override
    public boolean contains(K key) {
        return get(key).isPresent();
    }

    @Override
    public long size() {
        return guavaCache.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public CacheStats stats() {
        com.google.common.cache.CacheStats stats = guavaCache.stats();
        return new DefaultCacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.loadSuccessCount(),
                stats.loadExceptionCount(),
                stats.totalLoadTime(),
                stats.evictionCount());
    }
}
