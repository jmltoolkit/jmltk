/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.builders;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static com.github.javaparser.ast.Modifier.DefaultKeyword.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

class NodeWithThrownExceptionsBuildersTest {
    private final CompilationUnit cu = new CompilationUnit();

    @Test
    void testThrows() {
        MethodDeclaration addMethod = cu.addClass("test").addMethod("foo", PUBLIC);
        addMethod.addThrownException(IllegalStateException.class);
        assertEquals(1, addMethod.getThrownExceptions().size());
        assertTrue(addMethod.isThrown(IllegalStateException.class));
        addMethod.addThrownException(parseClassOrInterfaceType("Test"));
        assertEquals(2, addMethod.getThrownExceptions().size());
        assertEquals("Test", addMethod.getThrownException(1).toString());
    }
}
