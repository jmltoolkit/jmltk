/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.changes;

import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoChangeTest {

    @BeforeAll
    static void setUpBeforeClass() throws Exception {}

    @AfterAll
    static void tearDownAfterClass() throws Exception {}

    @BeforeEach
    void setUp() throws Exception {}

    @AfterEach
    void tearDown() throws Exception {}

    @Test
    void getValue_WithMethodFound() {
        Object o = new NoChange().getValue(ObservableProperty.ANNOTATIONS, new ClassOrInterfaceType());
        assertNotNull(o);
    }

    @Test
    void getValue_WithNoMethodFound() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    new NoChange().getValue(ObservableProperty.IMPORTS, new ClassOrInterfaceType());
                },
                "RuntimeException was expected");
    }
}
