/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.PatternVariableResult;
import com.github.javaparser.symbolsolver.javaparsermodel.PatternVariableVisitor;

import java.util.*;

public class BinaryExprContext extends ExpressionContext<BinaryExpr> {

    public BinaryExprContext(BinaryExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    public List<TypePatternExpr> typePatternExprsExposedToChild(Node child) {
        if (wrappedNode.getOperator().equals(BinaryExpr.Operator.AND)) {
            return typePatternExprsExposedToChildByAnd(child);
        }
        if (wrappedNode.getOperator().equals(BinaryExpr.Operator.OR)) {
            return typePatternExprsExposedToChildByOr(child);
        }
        return Collections.emptyList();
    }

    /**
     * The following rules apply to a conditional-and expression a && b:
     * - A pattern variable introduced by a when true is definitely matched at b.
     *
     * https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3.1.1
     */
    private List<TypePatternExpr> typePatternExprsExposedToChildByAnd(Node child) {
        if (!wrappedNode.getOperator().equals(BinaryExpr.Operator.AND)) {
            throw new IllegalStateException(
                    "Attempting to find patterns exposed by &&-expression, but wrapped operator is a "
                            + wrappedNode.getOperator().asString());
        }

        List<TypePatternExpr> results = new LinkedList<>();

        if (wrappedNode.getRight().containsWithinRange(child)) {
            PatternVariableVisitor variableVisitor = new PatternVariableVisitor();
            PatternVariableResult patternsInScope = wrappedNode.getLeft().accept(variableVisitor, null);

            results.addAll(patternsInScope.getVariablesIntroducedIfTrue());
        }

        return results;
    }

    /**
     * The following rules apply to a conditional-and expression a || b:
     * - A pattern variable introduced by a when false is definitely matched at b.
     *
     * https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3.1.2
     */
    private List<TypePatternExpr> typePatternExprsExposedToChildByOr(Node child) {
        if (!wrappedNode.getOperator().equals(BinaryExpr.Operator.OR)) {
            throw new IllegalStateException(
                    "Attempting to find patterns exposed by ||-expression, but wrapped operator is a "
                            + wrappedNode.getOperator().asString());
        }

        List<TypePatternExpr> results = new LinkedList<>();

        if (wrappedNode.getRight().containsWithinRange(child)) {
            PatternVariableVisitor variableVisitor = new PatternVariableVisitor();
            PatternVariableResult patternsInScope = wrappedNode.getLeft().accept(variableVisitor, null);

            results.addAll(patternsInScope.getVariablesIntroducedIfFalse());
        }

        return results;
    }
}
