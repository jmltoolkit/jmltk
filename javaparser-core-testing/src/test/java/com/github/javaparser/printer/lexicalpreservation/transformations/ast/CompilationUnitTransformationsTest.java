/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer.lexicalpreservation.transformations.ast;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.printer.lexicalpreservation.AbstractLexicalPreservingTest;
import com.github.javaparser.utils.LineSeparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Transforming CompilationUnit and verifying the LexicalPreservation works as expected.
 */
class CompilationUnitTransformationsTest extends AbstractLexicalPreservingTest {

    @BeforeEach
    void initParser() {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
    }

    // packageDeclaration

    @Test
    void addingPackageDeclaration() {
        considerCode("class A {}");
        cu.setPackageDeclaration(new PackageDeclaration(new Name(new Name("foo"), "bar")));
        assertTransformedToString("package foo.bar;" + LineSeparator.SYSTEM + LineSeparator.SYSTEM + "class A {}", cu);
    }

    @Test
    void removingPackageDeclaration() {
        considerCode("package foo.bar; class A {}");
        cu.removePackageDeclaration();
        assertTransformedToString("class A {}", cu);
    }

    @Test
    void replacingPackageDeclaration() {
        considerCode("package foo.bar; class A {}");
        cu.setPackageDeclaration(new PackageDeclaration(new Name(new Name("foo2"), "baz")));
        assertTransformedToString(
                "package foo2.baz;" + LineSeparator.SYSTEM + LineSeparator.SYSTEM + " class A {}", cu);
    }

    // imports

    @Test
    void addingModuleImport() {
        considerCode("class A {}");
        cu.addImport("java.base", false, false, true);
        assertTransformedToString(
                "import module java.base;" + LineSeparator.SYSTEM + LineSeparator.SYSTEM + "class A {}", cu);
    }

    @Test
    void removingModuleImport() {
        considerCode("import module java.base; class A {}");
        cu.remove(cu.getImport(0));
        assertTransformedToString("class A {}", cu);
    }

    @Test
    void modifyingModuleImport() {
        considerCode("import module java.base; class A {}");
        cu.getImport(0).setName("a.b");
        assertTransformedToString("import module a.b; class A {}", cu);
    }

    // types
}
