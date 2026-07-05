/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.AbstractResolutionTest;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Issue116Test extends AbstractResolutionTest {

    @Test
    void arrayTypeIsNotPartOfTheTree() {
        CompilationUnit cu = parseSample("Issue116");
        ClassOrInterfaceDeclaration clazz = Navigator.demandClass(cu, "JavaTest");
        MethodDeclaration methodDeclaration = Navigator.demandMethod(clazz, "foo");
        TypeSolver typeSolver = new ReflectionTypeSolver();
        JavaParserFacade javaParserFacade = JavaParserFacade.get(typeSolver);
        com.github.javaparser.ast.type.Type typeNode =
                methodDeclaration.getParameters().get(0).getType();
        ResolvedType type = javaParserFacade.convert(typeNode, typeNode);
        assertEquals("java.lang.String[]", type.describe());

        ExpressionStmt expressionStmt = (ExpressionStmt)
                methodDeclaration.getBody().get().getStatements().get(0);
        Expression argRef = expressionStmt.getExpression();
        assertEquals("java.lang.String[]", javaParserFacade.getType(argRef).describe());
    }
}
