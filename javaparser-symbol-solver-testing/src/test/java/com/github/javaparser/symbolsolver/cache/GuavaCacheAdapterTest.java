/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuavaCacheTest {

    private Cache<String, String> guavaCache;
    private GuavaCache<String, String> adapter;

    @BeforeEach
    void beforeEach() {
        guavaCache = CacheBuilder.newBuilder().build();
        adapter = new GuavaCache<>(guavaCache);
    }

    @Test
    void constructor_withNull_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> new GuavaCache<String, String>(null));
    }

    @Test
    void put_ShouldStoreTheValue() {
        assertTrue(adapter.isEmpty());
        assertFalse(adapter.contains("key"));

        adapter.put("key", "");
        assertFalse(adapter.isEmpty());
        assertTrue(adapter.contains("key"));
    }

    @Test
    void get_ShouldReturnTheCachedValue() {
        adapter.put("foo", "bar");
        adapter.put("rab", "oof");

        String key = "key";
        String value = "value";

        assertFalse(adapter.get(key).isPresent(), "No value expected at the moment");

        adapter.put(key, value);
        Optional<String> cachedValue = adapter.get(key);
        assertTrue(cachedValue.isPresent(), "No value expected at the moment");
        assertEquals(value, cachedValue.get(), "The values seem to be different");

        adapter.remove(key);
        assertFalse(adapter.get(key).isPresent(), "No value expected at the moment");
    }

    @Test
    void remove_ShouldOnlyRemoveTheKey() {

        // Prepare the values
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";

        adapter.put(key1, "");
        adapter.put(key2, "");
        adapter.put(key3, "");

        assertEquals(3, adapter.size());
        assertTrue(adapter.contains(key1));
        assertTrue(adapter.contains(key2));
        assertTrue(adapter.contains(key3));

        // Remove second element
        adapter.remove(key2);
        assertEquals(2, adapter.size());
        assertTrue(adapter.contains(key1));
        assertFalse(adapter.contains(key2));
        assertTrue(adapter.contains(key3));

        // Remove the third element
        adapter.remove(key3);
        assertEquals(1, adapter.size());
        assertTrue(adapter.contains(key1));
        assertFalse(adapter.contains(key3));

        // Remove first element
        adapter.remove(key1);
        assertEquals(0, adapter.size());
        assertFalse(adapter.contains(key2));
    }

    @Test
    void removeAll_ShouldRemoveAllTheKeys() {
        adapter.put("key1", "");
        adapter.put("key2", "");
        adapter.put("key3", "");

        assertFalse(adapter.isEmpty());
        adapter.removeAll();
        assertTrue(adapter.isEmpty());
    }

    @Test
    void contains_ShouldOnlyReturnTrue_WhenTheKeyExists() {
        String key = "key";

        assertFalse(adapter.contains(key), "At this moment, the key should not exists.");
        adapter.put(key, "value");
        assertTrue(adapter.contains(key), "At this moment, the key should be registered.");
        adapter.remove(key);
        assertFalse(adapter.contains(key), "At this moment, the key should not exists.");
    }

    @Test
    void size_ShouldBeEqualToGuavaCacheSize() {
        String key = "key";

        assertEquals(0, guavaCache.size());
        assertEquals(guavaCache.size(), adapter.size());

        adapter.put(key, "value");
        assertEquals(1, guavaCache.size());
        assertEquals(guavaCache.size(), adapter.size());

        adapter.remove(key);
        assertEquals(0, guavaCache.size());
        assertEquals(guavaCache.size(), adapter.size());
    }

    @Test
    void isEmpty_ShouldOnlyReturnTrue_WhenTheSizeIsZero() {
        String key = "key";

        assertTrue(adapter.isEmpty());

        adapter.put(key, "value");
        assertFalse(adapter.isEmpty());

        adapter.remove(key);
        assertTrue(adapter.isEmpty());
    }
}
