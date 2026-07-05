/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue4703Test {
    @Test
    void ecisResolutionTest() {
        String clazz = "public class Test {\n" + "    public Test(Test test, int... values) {\n"
                + "        System.out.println(test);\n"
                + "        System.out.println(values);\n"
                + "    }\n"
                + "    public Test(int... values) {\n"
                + "        this(null, values);\n"
                + "    }\n"
                + "}";
        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        final CompilationUnit cu = StaticJavaParser.parse(clazz);
        final List<ConstructorDeclaration> all = cu.findAll(ConstructorDeclaration.class);
        for (ConstructorDeclaration cd : all) System.out.println(cd.getSignature());
        final Optional<ExplicitConstructorInvocationStmt> ecis = cu.findFirst(ExplicitConstructorInvocationStmt.class);
        assertEquals("Test(Test, int...)", ecis.get().resolve().getSignature());
    }

    @Test
    void specificEcisResolutionTest() {
        String clazz = "public class Test {\n" + "    public Test(Test test, int... values) {\n"
                + "        System.out.println(test);\n"
                + "        System.out.println(values);\n"
                + "    }\n"
                + "    public Test(int... values) {\n"
                + "        this(new Test(), values);\n"
                + "    }\n"
                + "}";
        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
        final CompilationUnit cu = StaticJavaParser.parse(clazz);
        final List<ConstructorDeclaration> all = cu.findAll(ConstructorDeclaration.class);
        for (ConstructorDeclaration cd : all) System.out.println(cd.getSignature());
        final Optional<ExplicitConstructorInvocationStmt> ecis = cu.findFirst(ExplicitConstructorInvocationStmt.class);
        assertEquals("Test(Test, int...)", ecis.get().resolve().getSignature());
    }
}
