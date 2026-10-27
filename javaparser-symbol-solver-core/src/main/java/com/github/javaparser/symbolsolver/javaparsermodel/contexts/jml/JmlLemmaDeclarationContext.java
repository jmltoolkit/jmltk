/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts.jml;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.jml.body.JmlLemmaDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.Value;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.AbstractJavaParserContext;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;

import java.util.Optional;

/**
 * The scope introduced by a JML class-body {@code lemma} declaration: its
 * parameters are visible inside the body, e.g.
 *
 * <pre>{@code
 * //@ lemma nospace(\seq s) \by { use_lemma nospace(s); auto; };
 * }</pre>
 *
 * <p>Parameter resolution mirrors {@code MethodContext} (per-parameter
 * {@code JavaParserSymbolDeclaration}), but the lemma node is not a
 * {@code NodeWithParameters}/{@code NodeWithTypeParameters}, so the method-like
 * base context cannot be reused; only the parameters are handled here and all
 * other names are delegated to the enclosing (class) context.
 */
public class JmlLemmaDeclarationContext extends AbstractJavaParserContext<JmlLemmaDeclaration> {

    public JmlLemmaDeclarationContext(JmlLemmaDeclaration wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        for (Parameter parameter : wrappedNode.getParameters()) {
            if (parameter.getNameAsString().equals(name)) {
                return SymbolReference.solved(JavaParserSymbolDeclaration.parameter(parameter, typeSolver));
            }
        }
        return solveSymbolInParentContext(name);
    }

    @Override
    public Optional<Value> solveSymbolAsValue(String name) {
        for (Parameter parameter : wrappedNode.getParameters()) {
            if (parameter.getNameAsString().equals(name)) {
                return Optional.of(Value.from(JavaParserSymbolDeclaration.parameter(parameter, typeSolver)));
            }
        }
        return solveSymbolAsValueInParentContext(name);
    }
}
