/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.resolution.TypeSolver;

import static com.github.javaparser.resolution.Navigator.demandParentNode;

/**
 * This should not be used to represent fields of parameters.
 *
 * Eventually this should be renamed in JavaParserVariableDeclaration.
 *
 * @author Federico Tomassetti
 */
public final class JavaParserSymbolDeclaration {

    public static JavaParserFieldDeclaration field(VariableDeclarator wrappedNode, TypeSolver typeSolver) {
        return new JavaParserFieldDeclaration(wrappedNode, typeSolver);
    }

    public static JavaParserParameterDeclaration parameter(Parameter parameter, TypeSolver typeSolver) {
        return new JavaParserParameterDeclaration(parameter, typeSolver);
    }

    public static JavaParserVariableDeclaration localVar(VariableDeclarator variableDeclarator, TypeSolver typeSolver) {
        return new JavaParserVariableDeclaration(variableDeclarator, typeSolver);
    }

    public static JavaParserTypePatternDeclaration patternVar(TypePatternExpr typePatternExpr, TypeSolver typeSolver) {
        return new JavaParserTypePatternDeclaration(typePatternExpr, typeSolver);
    }

    public static int getParamPos(Parameter parameter) {
        int pos = 0;
        for (Node node : demandParentNode(parameter).getChildNodes()) {
            if (node == parameter) {
                return pos;
            }
            if (node instanceof Parameter) {
                pos++;
            }
        }
        return pos;
    }

    private JavaParserSymbolDeclaration() {
        // This private constructor is used to hide the public one
    }
}
