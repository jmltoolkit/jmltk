/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSymbolSolverTest extends AbstractResolutionTest {

    @Test
    void resolveMethodDeclaration() {
        CompilationUnit cu = parseSample("SymbolResolverExample", new ReflectionTypeSolver());

        MethodDeclaration methodDeclaration =
                cu.getClassByName("A").get().getMethods().get(0);
        ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();
        assertEquals("foo", resolvedMethodDeclaration.getName());
        assertEquals("A[]", resolvedMethodDeclaration.getReturnType().describe());
        assertEquals(
                "java.lang.String[]",
                resolvedMethodDeclaration.getParam(0).getType().describe());
        assertEquals("int[]", resolvedMethodDeclaration.getParam(1).getType().describe());
    }

    @Test
    void resolveMethodReferenceExpr() {
        JavaParser parser = createParserWithResolver(new ReflectionTypeSolver());
        MethodReferenceExpr methodRef = parser.parse(
                        "import java.util.function.Function; class X{void x(){Function<Object, Integer>r=Object::hashCode;}}")
                .getResult()
                .get()
                .findFirst(MethodReferenceExpr.class)
                .get();
        ResolvedMethodDeclaration resolvedMethodRef = methodRef.resolve();
        assertEquals("hashCode", resolvedMethodRef.getName());
        assertEquals("int", resolvedMethodRef.getReturnType().describe());
        assertEquals(0, resolvedMethodRef.getNumberOfParams());
    }

    @Test
    void resolveArrayType() {
        CompilationUnit cu = parseSample("SymbolResolverExample", new ReflectionTypeSolver());

        MethodDeclaration methodDeclaration =
                cu.getClassByName("A").get().getMethods().get(0);
        ResolvedType resolvedType = methodDeclaration.getType().resolve();
        assertEquals("A[]", resolvedType.describe());
    }
}
