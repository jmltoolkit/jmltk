/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.Value;

import java.util.Optional;

/**
 * @author Johannes Coetzee
 */
public class ExpressionContext<N extends Expression> extends AbstractJavaParserContext<N> {

    public ExpressionContext(N wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public Optional<Value> solveSymbolAsValue(String name) {
        SymbolReference<? extends ResolvedValueDeclaration> solvedSymbol = solveSymbol(name);
        return solvedSymbol.getDeclaration().map(Value::from);
    }

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        Optional<Node> maybeParent = getParent().map(Context::getWrappedNode);

        if (maybeParent.isPresent()) {
            SymbolReference<? extends ResolvedValueDeclaration> symbolFromPattern =
                    findExposedPatternInParentContext(maybeParent.get(), name);
            if (symbolFromPattern.isSolved()) {
                return symbolFromPattern;
            }
        }

        return solveSymbolInParentContext(name);
    }
}
