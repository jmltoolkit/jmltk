/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast;

import com.github.javaparser.ast.expr.SimpleName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataKeyTest {
    private static final DataKey<String> ABC = new DataKey<String>() {};
    private static final DataKey<String> DEF = new DataKey<String>() {};
    private static final DataKey<List<String>> LISTY = new DataKey<List<String>>() {};
    private static final DataKey<List<String>> DING = new DataKey<List<String>>() {};

    @Test
    void addAFewKeysAndSeeIfTheyAreStoredCorrectly() {
        Node node = new SimpleName();

        node.setData(ABC, "Hurray!");
        node.setData(LISTY, Arrays.asList("a", "b"));
        node.setData(ABC, "w00t");

        assertThat(node.getData(ABC)).contains("w00t");
        assertThat(node.getData(LISTY)).containsExactly("a", "b");
        assertThat(node.containsData(ABC)).isTrue();
        assertThat(node.containsData(LISTY)).isTrue();
        assertThat(node.containsData(DING)).isFalse();
    }

    @Test
    void removeWorks() {
        Node node = new SimpleName();
        node.setData(ABC, "Hurray!");

        node.removeData(ABC);

        assertThat(node.containsData(ABC)).isFalse();
    }

    @Test
    void aNonExistentKeyThrowsAnException() {
        Node node = new SimpleName();

        assertThrows(IllegalStateException.class, () -> node.getData(DING));
    }

    @Test
    void cloningCopiesData() {
        Node node = new SimpleName();
        node.setData(ABC, "ABC!");
        node.setData(DEF, "DEF!");

        Node clone = node.clone();
        assertEquals("ABC!", clone.getData(ABC));
        assertEquals("DEF!", clone.getData(DEF));
    }
}
