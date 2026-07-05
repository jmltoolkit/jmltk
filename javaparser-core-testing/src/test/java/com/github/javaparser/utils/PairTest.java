/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PairTest {
    @Test
    void testToString() {
        Pair<String, String> pair = new Pair<>("abc", "def");

        assertEquals("<abc, def>", pair.toString());
    }

    @Test
    void testToStringNulls() {
        Pair<String, String> pair = new Pair<>(null, null);

        assertEquals("<null, null>", pair.toString());
    }
}
