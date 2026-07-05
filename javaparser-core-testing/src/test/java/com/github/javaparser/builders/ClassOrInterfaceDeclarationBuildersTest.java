/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.builders;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassOrInterfaceDeclarationBuildersTest {
    CompilationUnit cu;

    @BeforeEach
    void setup() {
        cu = new CompilationUnit();
    }

    @AfterEach
    void teardown() {
        cu = null;
    }

    @Test
    void testAddExtends() {
        ClassOrInterfaceDeclaration testClass = cu.addClass("test");
        testClass.addExtendedType(List.class);
        assertEquals(1, cu.getImports().size());
        assertEquals(
                "import " + List.class.getName() + ";" + LineSeparator.SYSTEM,
                cu.getImport(0).toString());
        assertEquals(1, testClass.getExtendedTypes().size());
        assertEquals(List.class.getSimpleName(), testClass.getExtendedTypes(0).getNameAsString());
    }

    @Test
    void testAddImplements() {
        ClassOrInterfaceDeclaration testClass = cu.addClass("test");
        testClass.addImplementedType(Function.class);
        assertEquals(1, cu.getImports().size());
        assertEquals(
                "import " + Function.class.getName() + ";" + LineSeparator.SYSTEM,
                cu.getImport(0).toString());
        assertEquals(1, testClass.getImplementedTypes().size());
        assertEquals(
                Function.class.getSimpleName(), testClass.getImplementedTypes(0).getNameAsString());
    }
}
