/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.expr;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.observer.AstObserver;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class NameTest {

    @Test
    void outerNameExprIsTheRightMostIdentifier() {
        Name name = parseName("a.b.c");
        assertEquals("c", name.getIdentifier());
    }

    @Test
    void parsingAndUnparsingWorks() {
        Name name = parseName("a.b.c");
        assertEquals("a.b.c", name.asString());
    }

    @Test
    void parsingEmptyNameThrowsException() {
        assertThrows(ParseProblemException.class, () -> parseName(""));
    }

    @Test
    void importName() {
        ImportDeclaration importDeclaration = parseImport("import java.util.List;");

        assertEquals("import java.util.List;" + LineSeparator.SYSTEM, importDeclaration.toString());
        assertEquals("import java.util.List;", ConcreteSyntaxModel.genericPrettyPrint(importDeclaration));
    }

    @Test
    void packageName() {
        CompilationUnit cu = parse("package p1.p2;");

        assertEquals("package p1.p2;" + LineSeparator.SYSTEM + LineSeparator.SYSTEM, cu.toString());
        assertEquals(
                "package p1.p2;" + LineSeparator.SYSTEM + LineSeparator.SYSTEM,
                ConcreteSyntaxModel.genericPrettyPrint(cu));
    }

    @Test
    void isInternalNegative() {
        Name name = parseName("a.b.c");
        assertFalse(name.isInternal());
    }

    @Test
    void isInternalPositive() {
        Name name = parseName("a.b.c");
        assertTrue(name.getQualifier().get().isInternal());
        assertTrue(name.getQualifier().get().getQualifier().get().isInternal());
    }

    @Test
    void isTopLevelNegative() {
        Name name = parseName("a.b.c");
        assertFalse(name.getQualifier().get().isTopLevel());
        assertFalse(name.getQualifier().get().getQualifier().get().isTopLevel());
    }

    @Test
    void isTopLevelPositive() {
        Name name = parseName("a.b.c");
        assertTrue(name.isTopLevel());
    }

    @Test
    void issue4791Test() {
        String a = new String("c");
        String b = new String("c");
        Name expression = new Name(a);

        AstObserver observer = mock(AstObserver.class);
        expression.register(observer);

        expression.setIdentifier(b);

        verifyNoInteractions(observer);
    }
}
