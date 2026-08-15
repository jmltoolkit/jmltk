/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts.jml;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.jml.expr.JmlQuantifiedExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.AbstractJavaParserContext;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;

/**
 * @author Alexander Weigl
 * @version 1 (02.07.22)
 */
public class JmlQuantifiedExprContext extends AbstractJavaParserContext<JmlQuantifiedExpr> {
    public JmlQuantifiedExprContext(JmlQuantifiedExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    /*@Override
    public List<Parameter> parametersExposedToChild(Node child) {
        return wrappedNode.getVariables().stream()
                .map(it -> new Parameter(it.type().clone(), it.name().clone()))
                .toList();
    }
     */

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        for (VariableDeclarator variable : wrappedNode.getVariables()) {
            if (variable.getNameAsString().equals(name)) {
                return SymbolReference.solved(JavaParserSymbolDeclaration.quantifiedVar(variable, typeSolver));
            }
        }
        return super.solveSymbol(name);
    }
}
