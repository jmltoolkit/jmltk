/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StringProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SolveMethodDeclaredInEnumTest extends AbstractSymbolResolutionTest {

    @Test
    void methodDeclaredInEnum_enumFromJar() throws IOException {
        String code = "public class A { public void callEnum() { MyEnum.CONST.method(); }}";
        Path jarPath = adaptPath("src/test/resources/solveMethodDeclaredInEnum/MyEnum.jar");
        TypeSolver typeSolver = new CombinedTypeSolver(new JarTypeSolver(jarPath), new ReflectionTypeSolver());

        ParserConfiguration parserConfiguration =
                new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
        JavaParser javaParser = new JavaParser(parserConfiguration);

        CompilationUnit cu = javaParser
                .parse(ParseStart.COMPILATION_UNIT, new StringProvider(code))
                .getResult()
                .get();

        MethodCallExpr call = cu.findFirst(MethodCallExpr.class).orElse(null);
        ResolvedMethodDeclaration resolvedCall = call.resolve();

        assertNotNull(resolvedCall);
        assertEquals("MyEnum.method()", resolvedCall.getQualifiedSignature());
    }
}
