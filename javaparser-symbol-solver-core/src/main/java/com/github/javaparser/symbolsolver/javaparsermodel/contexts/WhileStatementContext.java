/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.NormalCompletionVisitor;
import com.github.javaparser.symbolsolver.javaparsermodel.PatternVariableResult;
import com.github.javaparser.symbolsolver.javaparsermodel.PatternVariableVisitor;
import java.util.LinkedList;
import java.util.List;

public class WhileStatementContext extends StatementContext<WhileStmt> {

    public WhileStatementContext(WhileStmt wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    /**
     *  The following rules apply to a statement while (e) S:
     *  - A pattern variable introduced by e when true is definitely matched at S.
     *
     *  https://docs.oracle.com/javase/specs/jls/se22/html/jls-6.html#jls-6.3.2.3
     */
    @Override
    public List<TypePatternExpr> typePatternExprsExposedToChild(Node child) {
        List<TypePatternExpr> results = new LinkedList<>();

        boolean givenNodeIsWithinBody = wrappedNode.getBody().containsWithinRange(child);
        if (givenNodeIsWithinBody) {
            PatternVariableVisitor variableVisitor = new PatternVariableVisitor();
            Expression condition = wrappedNode.getCondition();
            PatternVariableResult patternsInScope = condition.accept(variableVisitor, null);

            results.addAll(patternsInScope.getVariablesIntroducedIfTrue());
        }

        return results;
    }

    /**
     * The following rules apply to a statement while (e) S:
     * - A pattern variable is introduced by while (e) S iff
     *   (i) it is introduced by e when false and
     *   (ii) S does not contain a reachable break statement for which the while statement is the break target
     *
     * https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3.2.3
     */
    @Override
    public List<TypePatternExpr> getIntroducedTypePatterns() {
        List<TypePatternExpr> results = new LinkedList<>();

        if (!NormalCompletionVisitor.containsCorrespondingBreak(wrappedNode)) {
            Expression condition = wrappedNode.getCondition();
            PatternVariableVisitor variableVisitor = new PatternVariableVisitor();
            PatternVariableResult patternsInScope = condition.accept(variableVisitor, null);

            results.addAll(patternsInScope.getVariablesIntroducedIfFalse());
        }

        return results;
    }
}
