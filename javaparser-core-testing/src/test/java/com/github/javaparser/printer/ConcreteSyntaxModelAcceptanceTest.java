/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.printer;

import static com.github.javaparser.StaticJavaParser.parse;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConcreteSyntaxModelAcceptanceTest {
    private final Path rootDir = CodeGenerationUtils.mavenModuleRoot(ConcreteSyntaxModelAcceptanceTest.class)
            .resolve("src/test/test_sourcecode");

    private String prettyPrint(Node node) {
        return ConcreteSyntaxModel.genericPrettyPrint(node);
    }

    private String prettyPrintedExpectation(String name) throws IOException {
        return TestUtils.readResource("com/github/javaparser/printer/" + name + "_prettyprinted");
    }

    @Test
    void printingExamplePrettyPrintVisitor() throws IOException {
        CompilationUnit cu = parse(rootDir.resolve("com/github/javaparser/printer/PrettyPrintVisitor.java"));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("PrettyPrintVisitor"), prettyPrint(cu));
    }

    @Test
    void printingExampleJavaConcepts() throws IOException {
        CompilationUnit base = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsBase.java"));
        CompilationUnit enums = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsEnums.java"));
        CompilationUnit innerClass =
                parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsInnerClasses.java"));
        CompilationUnit methods = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsMethods.java"));
        CompilationUnit ugly = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsUgly.java"));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsBase"), prettyPrint(base));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsEnums"), prettyPrint(enums));
        TestUtils.assertEqualsStringIgnoringEol(
                prettyPrintedExpectation("JavaConceptsInnerClasses"), prettyPrint(innerClass));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsMethods"), prettyPrint(methods));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsUgly"), prettyPrint(ugly));
    }
}
