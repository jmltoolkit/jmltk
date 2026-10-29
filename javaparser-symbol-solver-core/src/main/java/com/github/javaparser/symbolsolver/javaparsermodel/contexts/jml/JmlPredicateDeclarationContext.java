/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts.jml;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.jml.body.JmlPredicateDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.AbstractJavaParserContext;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;

/**
 * Resolves names that occur in a {@link JmlPredicateDeclaration} (the body
 * expression) against the predicate's formal parameters.
 *
 * <p>Example:
 * <pre>{@code
 * //@ instance predicate Pred(int i) = i > 0;
 * }</pre>
 * Here {@code i} in the body resolves to the formal parameter {@code int i}.
 */
public class JmlPredicateDeclarationContext extends AbstractJavaParserContext<JmlPredicateDeclaration> {
    public JmlPredicateDeclarationContext(JmlPredicateDeclaration wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        for (Parameter parameter : wrappedNode.getParameters()) {
            if (name.equals(parameter.getNameAsString())) {
                return SymbolReference.solved(JavaParserSymbolDeclaration.parameter(parameter, typeSolver));
            }
        }

        // Fallback to the default implementation to find names defined outside the predicate.
        return super.solveSymbol(name);
    }
}
