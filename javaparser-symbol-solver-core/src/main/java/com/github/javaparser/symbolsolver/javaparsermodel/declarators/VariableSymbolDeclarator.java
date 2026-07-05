/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.declarators;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Federico Tomassetti
 */
public class VariableSymbolDeclarator extends AbstractSymbolDeclarator<VariableDeclarationExpr> {

    public VariableSymbolDeclarator(VariableDeclarationExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
        wrappedNode.getParentNode().ifPresent(p -> {
            if (p instanceof FieldDeclaration) {
                throw new IllegalArgumentException();
            }
        });
    }

    @Override
    public List<ResolvedValueDeclaration> getSymbolDeclarations() {
        List<ResolvedValueDeclaration> variables = wrappedNode.getVariables().stream()
                .map(v -> JavaParserSymbolDeclaration.localVar(v, typeSolver))
                .collect(Collectors.toCollection(ArrayList::new));

        //        // FIXME: This returns ALL PatternExpr, regardless of whether it is in scope or not.
        //        List<JavaParserSymbolDeclaration> patterns = wrappedNode.getVariables()
        //                .stream()
        //                .filter(variableDeclarator -> variableDeclarator.getInitializer().isPresent())
        //                .map(variableDeclarator -> variableDeclarator.getInitializer().get())
        //                .map(expression -> expression.findAll(PatternExpr.class))
        //                .flatMap(Collection::stream)
        //                .map(v -> JavaParserSymbolDeclaration.patternVar(v, typeSolver))
        //                .collect(Collectors.toCollection(ArrayList::new));

        List<ResolvedValueDeclaration> all = new ArrayList<>(variables);
        //        all.addAll(patterns);

        return all;
    }
}
