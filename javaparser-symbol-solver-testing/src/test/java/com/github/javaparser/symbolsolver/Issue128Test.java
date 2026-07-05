/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.LeanParserConfiguration;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Issue128Test extends AbstractResolutionTest {

    private TypeSolver typeSolver;

    @BeforeEach
    void setup() throws IOException {
        Path srcDir = adaptPath("src/test/resources/issue128");
        typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(), new JavaParserTypeSolver(srcDir, new LeanParserConfiguration()));
    }

    @Test
    void verifyJavaTestClassIsSolved() {
        typeSolver.solveType("foo.JavaTest");
    }

    @Test
    void loopOnStaticallyImportedType() {
        CompilationUnit cu = parseSampleWithStandardExtension("issue128/foo/Issue128");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "JavaTest");
        ExpressionStmt expressionStmt = (ExpressionStmt)
                clazz.getMethodsByName("test").get(0).getBody().get().getStatement(0);
        MethodCallExpr methodCallExpr = (MethodCallExpr) expressionStmt.getExpression();
        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);

        assertEquals(false, javaParserFacade.solve(methodCallExpr).isSolved());
    }
}
