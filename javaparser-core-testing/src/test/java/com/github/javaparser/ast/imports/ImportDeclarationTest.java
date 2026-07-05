/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.imports;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ImportDeclaration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseImport;
import static org.junit.jupiter.api.Assertions.*;

class ImportDeclarationTest {

    @BeforeAll
    static void initParser() {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
    }

    @Test
    void singleTypeImportDeclaration() {
        ImportDeclaration i = parseImport("import a.b.c.X;");
        assertEquals("a.b.c.X", i.getNameAsString());
    }

    @Test
    void typeImportOnDemandDeclaration() {
        ImportDeclaration i = parseImport("import a.b.c.D.*;");
        assertEquals("a.b.c.D", i.getName().toString());
        assertEquals("D", i.getName().getIdentifier());
    }

    @Test
    void singleStaticImportDeclaration() {
        ImportDeclaration i = parseImport("import static a.b.c.X.def;");
        assertEquals("a.b.c.X", i.getName().getQualifier().get().asString());
        assertEquals("def", i.getName().getIdentifier());
    }

    @Test
    void staticImportOnDemandDeclaration() {
        ImportDeclaration i = parseImport("import static a.b.c.X.*;");
        assertEquals("a.b.c.X", i.getNameAsString());
    }

    @Test
    void moduleImport() {
        ImportDeclaration i = parseImport("import module java.base;");
        assertEquals("java.base", i.getNameAsString());
        assertTrue(i.isModule());
    }

    @Test
    void modulePackageImport() {
        ImportDeclaration i = parseImport("import module.base.Foo;");
        assertEquals("module.base.Foo", i.getNameAsString());
        assertFalse(i.isModule());
    }

    @Test
    void staticModulePackageImport() {
        ImportDeclaration i = parseImport("import static module.base.Foo;");
        assertEquals("module.base.Foo", i.getNameAsString());
        assertFalse(i.isModule());
        assertTrue(i.isStatic());
    }
}
