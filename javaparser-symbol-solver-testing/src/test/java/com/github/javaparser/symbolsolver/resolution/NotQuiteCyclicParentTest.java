/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.resolution;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.Navigator;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class NotQuiteCyclicParentTest extends AbstractResolutionTest {

    @Test
    void testSimilarNameInterface() {
        CompilationUnit cu = parseSample("NotQuiteCyclicParent");
        ClassOrInterfaceDeclaration clazz = Navigator.demandInterface(cu, "NotQuiteCyclicParent");
        MethodDeclaration method = Navigator.demandMethod(clazz, "main");
        VariableDeclarationExpr varDec = (VariableDeclarationExpr)
                ((ExpressionStmt) method.getBody().get().getStatement(0)).getExpression();

        try {
            ResolvedType ref = JavaParserFacade.get(new ReflectionTypeSolver()).getType(varDec);
        } catch (UnsolvedSymbolException e) {
            return;
        }

        fail("We shouldn't be able to resolve the type (it is not defined).");
    }
}
