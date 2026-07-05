/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.cache;

import com.github.javaparser.resolution.cache.Cache;
import com.github.javaparser.resolution.cache.CacheStats;
import java.util.Optional;

/**
 * A cache implementation that does not store any information.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class NoCache<K, V> implements Cache<K, V> {

    /**
     * Create a new instance.
     *
     * @param <expectedK> The expected type for the key.
     * @param <expectedV> The expected type for the value.
     *
     * @return A newly created instance of {@link NoCache}.
     */
    public static <expectedK, expectedV> NoCache<expectedK, expectedV> create() {
        return new NoCache<>();
    }

    @Override
    public void put(K key, V value) {
        // Nothing to do here.
    }

    @Override
    public Optional<V> get(K key) {
        return Optional.empty();
    }

    @Override
    public void remove(K key) {
        // Nothing to do here.
    }

    @Override
    public void removeAll() {
        // Nothing to do here.
    }

    @Override
    public boolean contains(K key) {
        return false;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public CacheStats stats() {
        return new DefaultCacheStats();
    }
}
