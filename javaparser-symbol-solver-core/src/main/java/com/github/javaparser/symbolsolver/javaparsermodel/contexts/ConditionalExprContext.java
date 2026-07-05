/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.PatternVariableResult;
import com.github.javaparser.symbolsolver.javaparsermodel.PatternVariableVisitor;
import java.util.LinkedList;
import java.util.List;

public class ConditionalExprContext extends ExpressionContext<ConditionalExpr> {

    public ConditionalExprContext(ConditionalExpr wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    /**
     * The following rules apply to a conditional expression a ? b : c:
     * - A pattern variable introduced by a when true is definitely matched at b.
     * - A pattern variable introduced by a when false is definitely matched at c.
     *
     * https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3.1.4
     */
    @Override
    public List<TypePatternExpr> typePatternExprsExposedToChild(Node child) {
        List<TypePatternExpr> results = new LinkedList<>();

        PatternVariableVisitor variableVisitor = new PatternVariableVisitor();
        PatternVariableResult patternsInScope = wrappedNode.getCondition().accept(variableVisitor, null);

        if (wrappedNode.getThenExpr().containsWithinRange(child)) {
            results.addAll(patternsInScope.getVariablesIntroducedIfTrue());
        } else if (wrappedNode.getElseExpr().containsWithinRange(child)) {
            results.addAll(patternsInScope.getVariablesIntroducedIfFalse());
        }

        return results;
    }
}
