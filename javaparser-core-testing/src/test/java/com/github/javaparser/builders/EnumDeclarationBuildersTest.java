/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.builders;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumDeclarationBuildersTest {
    private final CompilationUnit cu = new CompilationUnit();

    @Test
    void testAddImplements() {
        EnumDeclaration testEnum = cu.addEnum("test");
        testEnum.addImplementedType(Function.class);
        assertEquals(1, cu.getImports().size());
        assertEquals(
                "import " + Function.class.getName() + ";" + LineSeparator.SYSTEM,
                cu.getImport(0).toString());
        assertEquals(1, testEnum.getImplementedTypes().size());
        assertEquals(
                Function.class.getSimpleName(), testEnum.getImplementedTypes(0).getNameAsString());
    }

    @Test
    void testAddEnumConstant() {
        EnumDeclaration testEnum = cu.addEnum("test");
        testEnum.addEnumConstant("MY_ENUM_CONSTANT");
        assertEquals(1, testEnum.getEntries().size());
        assertEquals("MY_ENUM_CONSTANT", testEnum.getEntry(0).getNameAsString());
    }
}
