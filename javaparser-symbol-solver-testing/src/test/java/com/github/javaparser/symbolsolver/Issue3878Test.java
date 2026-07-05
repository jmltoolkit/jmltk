/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue3878Test {

    @BeforeEach
    void setup() {
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(new ReflectionTypeSolver());
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    @Test
    void resolve_method_reference_in_ObjectCreationExpr() {
        String code = "package test;\n" + "import java.util.function.Consumer;\n"
                + "\n"
                + "class A {\n"
                + "A(Consumer<Integer> f) {}\n"
                + "void bar(int i) {}\n"
                + "void foo() { new A(this::bar); }\n"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        MethodReferenceExpr expr = cu.findFirst(MethodReferenceExpr.class).get();

        ResolvedMethodDeclaration decl = expr.resolve();

        assertEquals("test.A.bar(int)", decl.getQualifiedSignature());
    }
}
