/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.TypeSolver;

import java.util.Collections;
import java.util.List;

/**
 * @author Federico Tomassetti
 */
public class VariableDeclarationExprContext extends ExpressionContext<VariableDeclarationExpr> {

    public VariableDeclarationExprContext(VariableDeclarationExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public List<VariableDeclarator> localVariablesExposedToChild(Node child) {
        for (int i = 0; i < wrappedNode.getVariables().size(); i++) {
            if (child == wrappedNode.getVariable(i)) {
                return wrappedNode.getVariables().subList(0, i);
            }
        }
        // TODO: Consider pattern exprs
        return Collections.emptyList();
    }
}
