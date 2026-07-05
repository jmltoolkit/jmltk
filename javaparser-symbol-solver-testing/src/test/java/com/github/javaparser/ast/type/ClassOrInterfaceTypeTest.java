/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassOrInterfaceTypeTest {

    private static final ParserConfiguration PARSER_CONFIGURATION = new ParserConfiguration();

    @BeforeAll
    public static void setup() {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(reflectionTypeSolver);
        PARSER_CONFIGURATION.setSymbolResolver(javaSymbolSolver);
    }

    private JavaParser javaParser;

    @BeforeEach
    public void beforeEach() {
        javaParser = new JavaParser(PARSER_CONFIGURATION);
    }

    @Test
    void resolveClassType() {
        ParseResult<CompilationUnit> compilationUnit = javaParser.parse("class A {}");
        assertTrue(compilationUnit.getResult().isPresent());

        ClassOrInterfaceType classOrInterfaceType = StaticJavaParser.parseClassOrInterfaceType("String");
        classOrInterfaceType.setParentNode(compilationUnit.getResult().get());

        ResolvedReferenceType resolved = classOrInterfaceType.resolve().asReferenceType();
        assertEquals(String.class.getCanonicalName(), resolved.getQualifiedName());
    }

    @Test
    void testToDescriptor() {
        ParseResult<CompilationUnit> compilationUnit = javaParser.parse("class A {}");
        assertTrue(compilationUnit.getResult().isPresent());

        ClassOrInterfaceType classOrInterfaceType = StaticJavaParser.parseClassOrInterfaceType("String");
        classOrInterfaceType.setParentNode(compilationUnit.getResult().get());

        assertEquals("Ljava/lang/String;", classOrInterfaceType.toDescriptor());
    }

    @Test
    void testToDescriptorWithTypeVariables() {
        ParseResult<CompilationUnit> compilationUnit =
                javaParser.parse("public class A  { public static <T extends String> void method(T arg); }");

        assertEquals(
                "(Ljava/lang/String;)V",
                compilationUnit
                        .getResult()
                        .get()
                        .getType(0)
                        .getMethodsByName("method")
                        .get(0)
                        .toDescriptor());
    }
}
