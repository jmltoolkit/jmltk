/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParseStart;
import com.github.javaparser.StringProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

class Issue1946Test {

    @Test
    void issueWithInternalEnumConstantReference() {
        String code =
                "package com.github.javaparser.symbolsolver.testingclasses; class Foo { void foo() { UtilityClass.method(SomeClass.InnerEnum.CONSTANT); } }";
        JavaParser jp = new JavaParser();
        CombinedTypeSolver typeSolver = new CombinedTypeSolver(new TypeSolver[] {new ReflectionTypeSolver(false)});
        jp.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
        ParseResult<CompilationUnit> pr = jp.parse(ParseStart.COMPILATION_UNIT, new StringProvider(code));
        assertEquals(true, pr.isSuccessful());
        MethodCallExpr methodCallExpr =
                pr.getResult().get().findFirst(MethodCallExpr.class).get();
        ResolvedMethodDeclaration rmd = methodCallExpr.resolve();
        assertEquals(
                "com.github.javaparser.symbolsolver.testingclasses.UtilityClass.method(com.github.javaparser.symbolsolver.testingclasses.SomeClass.InnerEnum)",
                rmd.getQualifiedSignature());
        FieldAccessExpr fieldAccessExpr =
                methodCallExpr.findFirst(FieldAccessExpr.class).get();
        ResolvedValueDeclaration rvd = fieldAccessExpr.resolve();
        assertEquals("CONSTANT", rvd.getName());
        assertEquals(
                "com.github.javaparser.symbolsolver.testingclasses.SomeClass.InnerEnum",
                rvd.getType().describe());
    }
}
