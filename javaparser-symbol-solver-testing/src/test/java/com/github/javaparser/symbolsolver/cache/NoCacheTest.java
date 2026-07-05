/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoCacheTest {

    private final NoCache<Object, Object> cache = new NoCache<>();

    @Test
    void create_ShouldCreateDifferentCache() {
        NoCache<Object, Object> firstCache = NoCache.create();
        assertNotNull(firstCache);

        NoCache<Object, Object> secondCache = NoCache.create();
        assertNotNull(secondCache);
        assertNotEquals(firstCache, secondCache);
    }

    @Test
    void put_shouldNotRegisterTheKey() {
        assertEquals(0, cache.size());
        cache.put("key", "value");
        assertEquals(0, cache.size());
    }

    @Test
    void get_ShouldNotBePresent() {
        assertFalse(cache.get("key").isPresent());
    }

    @Test
    void remove_ShouldDoNothing() {
        assertEquals(0, cache.size());
        cache.remove("key");
        assertEquals(0, cache.size());
    }

    @Test
    void removeAll_ShouldDoNothing() {
        assertEquals(0, cache.size());
        cache.removeAll();
        assertEquals(0, cache.size());
    }

    @Test
    void contains_ShouldNotContainsKey() {
        assertFalse(cache.contains("key"));
    }

    @Test
    void size_ShouldHaveSizeOfZero() {
        assertEquals(0, cache.size());
    }

    @Test
    void isEmpty_ShouldAlwaysBeTrue() {
        assertTrue(cache.isEmpty());
    }
}
