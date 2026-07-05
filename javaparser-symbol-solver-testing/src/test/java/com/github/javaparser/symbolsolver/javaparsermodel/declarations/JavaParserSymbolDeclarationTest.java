/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypePatternDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserSymbolDeclarationTest {

    private final TypeSolver typeSolver = new ReflectionTypeSolver();

    /**
     * Try to create a field using {@link JavaParserSymbolDeclaration#field(VariableDeclarator, TypeSolver)} and check
     * if the returned declaration is marked as a field and can be converted to a
     * {@link com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration} using {@link ResolvedValueDeclaration#asField()}.
     */
    @Test
    void createdFieldShouldBeMarkedAsField() {
        VariableDeclarator variableDeclarator = parseBodyDeclaration("private final int x = 0;")
                .asFieldDeclaration()
                .getVariable(0);
        ResolvedValueDeclaration field = JavaParserSymbolDeclaration.field(variableDeclarator, typeSolver);

        assertTrue(field.isField());
        assertDoesNotThrow(field::asField);
    }

    /**
     * Try to create a parameter using {@link JavaParserSymbolDeclaration#parameter(Parameter, TypeSolver)} and check
     * if the returned declaration is marked as a parameter and can be converted to a
     * {@link com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration} using {@link ResolvedValueDeclaration#asParameter()}.
     */
    @Test
    void createdParameterShouldBeMarkedAsParameter() {
        Parameter parameter = parseParameter("String myStr");
        ;
        ResolvedValueDeclaration parameterDeclaration = JavaParserSymbolDeclaration.parameter(parameter, typeSolver);

        assertTrue(parameterDeclaration.isParameter());
        assertDoesNotThrow(parameterDeclaration::asParameter);
    }

    /**
     * Try to create a local variable using {@link JavaParserSymbolDeclaration#localVar(VariableDeclarator, TypeSolver)}
     * and check if the returned declaration is marked as a variable.
     */
    @Test
    void createdLocalVariableShouldBeMarkedAsVariable() {
        VariableDeclarator variableDeclarator =
                parseVariableDeclarationExpr("int x = 0").getVariable(0);
        ResolvedValueDeclaration localVar = JavaParserSymbolDeclaration.localVar(variableDeclarator, typeSolver);

        assertTrue(localVar.isVariable());
    }

    /**
     * Try to create a pattern variable using {@link JavaParserSymbolDeclaration#patternVar(TypePatternExpr, TypeSolver)} and check
     * if the returned declaration is marked as a pattern and can be converted to a
     * {@link ResolvedTypePatternDeclaration} using {@link ResolvedValueDeclaration#asTypePattern()}.
     */
    @Test
    void createdPatternVariableShouldBeMarkedAsPatternVar() {
        TypePatternExpr typePatternExpr = new TypePatternExpr();
        ResolvedValueDeclaration patternVar = JavaParserSymbolDeclaration.patternVar(typePatternExpr, typeSolver);

        assertTrue(patternVar.isTypePattern());
        assertDoesNotThrow(patternVar::asTypePattern);
    }
}
