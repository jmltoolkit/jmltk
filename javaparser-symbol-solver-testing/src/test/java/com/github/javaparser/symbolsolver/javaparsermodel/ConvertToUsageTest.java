/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConvertToUsageTest extends AbstractResolutionTest {

    private final TypeSolver typeSolver = new ReflectionTypeSolver();

    @Test
    void testConvertTypeToUsage() {
        CompilationUnit cu = parseSample("LocalTypeDeclarations");
        List<NameExpr> n = cu.findAll(NameExpr.class);

        assertEquals("int", usageDescribe(n, "a"));
        assertEquals("java.lang.Integer", usageDescribe(n, "b"));
        assertEquals("java.lang.Class<java.lang.Integer>", usageDescribe(n, "c"));
        assertEquals("java.lang.Class<? super java.lang.Integer>", usageDescribe(n, "d"));
        assertEquals("java.lang.Class<? extends java.lang.Integer>", usageDescribe(n, "e"));
        assertEquals(
                "java.lang.Class<? extends java.lang.Class<? super java.lang.Class<? extends java.lang.Integer>>>",
                usageDescribe(n, "f"));
        assertEquals(
                "java.lang.Class<? super java.lang.Class<? extends java.lang.Class<? super java.lang.Integer>>>",
                usageDescribe(n, "g"));
    }

    private String usageDescribe(List<NameExpr> n, String name) {
        return n.stream()
                .filter(x -> x.getNameAsString().equals(name))
                .map(JavaParserFacade.get(typeSolver)::getType)
                .map(ResolvedType::describe)
                .findFirst()
                .orElse(null);
    }
}
