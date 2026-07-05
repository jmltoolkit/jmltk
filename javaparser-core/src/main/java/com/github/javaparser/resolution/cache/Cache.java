/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.resolution.cache;

import java.util.Optional;

/**
 * A contract that defines a semi-persistent mapping of keys and values.
 * <br>
 * Cache entries are manually added using put({@link K}, {@link V}),
 * and are stored in the cache until either evicted or manually removed.
 * After storing a value, it can be accessed using get({@link K}).
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public interface Cache<K, V> {

    /**
     * Associates value with key in this cache.
     * <br>
     * If the cache previously contained a value associated with key,
     * the old value is replaced by value.
     *
     * @param key   The key to be used as index.
     * @param value The value to be stored.
     */
    void put(K key, V value);

    /**
     * Returns the value associated with {@code key} in this cache,
     * or empty if there is no cached value for {@code key}.
     *
     * @param key The key to look for.
     * @return The value stored in cache if present.
     */
    Optional<V> get(K key);

    /**
     * Discards any cached value for this key.
     *
     * @param key The key to be discarded.
     */
    void remove(K key);

    /**
     * Discards all entries in the cache.
     */
    void removeAll();

    /**
     * Returns {@code True} if the cache contains a entry with the key,
     * or {@code False} if there is none.
     *
     * @param key The key to be verified.
     * @return {@code True} if the key is present.
     */
    boolean contains(K key);

    /**
     * Returns the number of entries in this cache.
     *
     * @return The cache size.
     */
    long size();

    /**
     * Returns {@code True} if the cache is empty, or {@code False}
     * if there's at least a entry stored in cache.
     *
     * @return {@code True} if is empty.
     */
    boolean isEmpty();

    /**
     * Returns a current snapshot of this cache's cumulative statistics, or a set of default values if
     * the cache is not recording statistics. All statistics begin at zero and never decrease over the
     * lifetime of the cache.
     */
    CacheStats stats();
}
