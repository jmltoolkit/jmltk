/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserTypePatternDeclaration;

import java.util.Optional;

/**
 * @author Roger Howell
 */
public class InstanceOfExprContext extends ExpressionContext<InstanceOfExpr> {

    public InstanceOfExprContext(InstanceOfExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
        // TODO: Add PatternExprContext and solve in that
        // TODO Look for the resolved pattern in the record pattern tree
        Optional<PatternExpr> optionalPatternExpr = wrappedNode.getPattern();
        if (optionalPatternExpr.isPresent() && (optionalPatternExpr.get().isTypePatternExpr())) {
            TypePatternExpr typePatternExpr = optionalPatternExpr.get().asTypePatternExpr();
            if (typePatternExpr.getNameAsString().equals(name)) {
                JavaParserTypePatternDeclaration decl =
                        JavaParserSymbolDeclaration.patternVar(typePatternExpr, typeSolver);
                return SymbolReference.solved(decl);
            }
        }

        Optional<Context> optionalParentContext = getParent();
        if (!optionalParentContext.isPresent()) {
            return SymbolReference.unsolved();
        }

        Context parentContext = optionalParentContext.get();
        if (parentContext instanceof BinaryExprContext) {
            Optional<TypePatternExpr> optionalPatternExpr1 = parentContext.typePatternExprInScope(name);
            if (optionalPatternExpr1.isPresent() && (optionalPatternExpr1.get().isTypePatternExpr())) {
                TypePatternExpr typePatternExpr = optionalPatternExpr1.get().asTypePatternExpr();
                JavaParserTypePatternDeclaration decl =
                        JavaParserSymbolDeclaration.patternVar(typePatternExpr, typeSolver);
                return SymbolReference.solved(decl);
            }
        } // TODO: Also consider unary expr context

        // if nothing is found we should check for existing patterns and ask the parent context
        return super.solveSymbol(name);
    }
}
