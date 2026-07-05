/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.body;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.utils.TestParser;
import org.junit.jupiter.api.Test;

class AnnotationMemberDeclarationTest {

    @Test
    void whenSettingNameTheParentOfNameIsAssigned() {
        AnnotationMemberDeclaration decl = new AnnotationMemberDeclaration();
        SimpleName name = new SimpleName("foo");
        decl.setName(name);
        assertTrue(name.getParentNode().isPresent());
        assertSame(decl, name.getParentNode().get());
    }

    @Test
    void removeDefaultValueWhenNoDefaultValueIsPresent() {
        AnnotationMemberDeclaration decl = new AnnotationMemberDeclaration();
        SimpleName name = new SimpleName("foo");
        decl.setName(name);

        decl.removeDefaultValue();

        assertFalse(decl.getDefaultValue().isPresent());
    }

    @Test
    void removeDefaultValueWhenDefaultValueIsPresent() {
        AnnotationMemberDeclaration decl = new AnnotationMemberDeclaration();
        SimpleName name = new SimpleName("foo");
        decl.setName(name);
        Expression defaultValue = new IntegerLiteralExpr("2");
        decl.setDefaultValue(defaultValue);

        decl.removeDefaultValue();

        assertFalse(defaultValue.getParentNode().isPresent());
    }

    @Test
    void annotationDeclarationShouldSupportRecordChild() {
        CompilationUnit cu = TestParser.parseCompilationUnit(
                ParserConfiguration.LanguageLevel.BLEEDING_EDGE,
                "" + "@interface Foo {\n" + "    record Bar(String s) {}\n" + "}");

        RecordDeclaration bar =
                cu.getAnnotationDeclarationByName("Foo").get().getMember(0).asRecordDeclaration();

        assertEquals(1, bar.getParameters().size());

        Parameter parameter = bar.getParameter(0);
        assertEquals("String", parameter.getTypeAsString());
        assertEquals("s", parameter.getNameAsString());
    }
}
