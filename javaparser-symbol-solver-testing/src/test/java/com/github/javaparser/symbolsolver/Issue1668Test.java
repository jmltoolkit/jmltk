/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Issue1668Test {

    private JavaParser javaParser;

    @BeforeEach
    void setUp() {
        TypeSolver typeSolver = new ReflectionTypeSolver();
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        javaParser = new JavaParser(config);
    }

    @Test
    void testResolveArrayDeclaration() {
        String code = String.join(
                System.lineSeparator(),
                "public class X {",
                "   public static void main(String[] args) {",
                "       String s = \"a,b,c,d,e\";",
                "       String[] stringArray = s.split(',');",
                "   }",
                "}");

        ParseResult<CompilationUnit> parseResult =
                javaParser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(code));
        assertTrue(parseResult.isSuccessful());
        assertTrue(parseResult.getResult().isPresent());

        CompilationUnit compilationUnit = parseResult.getResult().get();
        VariableDeclarator variableDeclarator = compilationUnit
                .findFirst(VariableDeclarator.class, v -> v.getNameAsString().equals("stringArray"))
                .get();
        VariableDeclarationExpr variableDeclarationExpr =
                (VariableDeclarationExpr) variableDeclarator.getParentNode().get();
        ResolvedType resolvedType = variableDeclarationExpr.calculateResolvedType();
        assertEquals("java.lang.String[]", resolvedType.describe());
        ResolvedValueDeclaration resolve = variableDeclarator.resolve();
        assertEquals("java.lang.String[]", resolve.getType().describe());
    }
}
