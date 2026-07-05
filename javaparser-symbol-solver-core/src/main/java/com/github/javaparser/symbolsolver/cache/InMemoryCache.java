/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.cache;

import com.github.javaparser.resolution.cache.Cache;
import com.github.javaparser.resolution.cache.CacheStats;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * A cache implementation that stores the information in memory.
 * <br>
 * The current implementation stores the values in memory in a {@link WeakHashMap}.
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public class InMemoryCache<K, V> implements Cache<K, V> {

    /**
     * Create a new instance for a cache in memory.
     *
     * @param <expectedK> The expected type for the key.
     * @param <expectedV> The expected type for the value.
     *
     * @return A newly created instance of {@link InMemoryCache}.
     */
    public static <expectedK, expectedV> InMemoryCache<expectedK, expectedV> create() {
        return new InMemoryCache<>();
    }

    private final Map<K, V> mappedValues;

    private InMemoryCache() {
        mappedValues = Collections.synchronizedMap(new WeakHashMap<>());
    }

    @Override
    public void put(K key, V value) {
        mappedValues.put(key, value);
    }

    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(mappedValues.get(key));
    }

    @Override
    public void remove(K key) {
        mappedValues.remove(key);
    }

    @Override
    public void removeAll() {
        mappedValues.clear();
    }

    @Override
    public boolean contains(K key) {
        return mappedValues.containsKey(key);
    }

    @Override
    public long size() {
        return mappedValues.size();
    }

    @Override
    public boolean isEmpty() {
        return mappedValues.isEmpty();
    }

    @Override
    public CacheStats stats() {
        return new DefaultCacheStats();
    }
}
