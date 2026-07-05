/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import static com.github.javaparser.StaticJavaParser.parseSimpleName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.github.javaparser.ast.observer.AstObserver;
import org.junit.jupiter.api.Test;

class SimpleNameTest {

    @Test
    void defaultConstructorSetsIdentifierToEmpty() {
        assertEquals("empty", new SimpleName().getIdentifier());
    }

    @Test
    void identifierMustNotBeEmpty() {
        assertThrows(AssertionError.class, () -> new SimpleName(""));
    }

    @Test
    void identifierMustNotBeNull() {
        assertThrows(AssertionError.class, () -> new SimpleName(null));
    }

    @Test
    void unicodeEscapesArePreservedInIdentifiers() {
        SimpleName name = parseSimpleName("xxx\\u2122xxx");
        assertEquals("xxx\\u2122xxx", name.asString());
    }

    @Test
    void issue4791Test() {
        String a = new String("someName");
        String b = new String("someName");
        SimpleName expression = new SimpleName(a);

        AstObserver observer = mock(AstObserver.class);
        expression.register(observer);

        expression.setIdentifier(b);

        verifyNoInteractions(observer);
    }
}
