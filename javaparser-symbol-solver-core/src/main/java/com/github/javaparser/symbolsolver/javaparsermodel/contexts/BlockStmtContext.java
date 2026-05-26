/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.symbolsolver.javaparsermodel.contexts;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class BlockStmtContext extends StatementContext<BlockStmt> {

    public BlockStmtContext(BlockStmt wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public List<VariableDeclarator> localVariablesExposedToChild(Node child) {
        int position = wrappedNode.getStatements().indexOf(child);
        if (position == -1) {
            throw new RuntimeException();
        }
        List<VariableDeclarator> variableDeclarators = new LinkedList<>();
        for (int i = position - 1; i >= 0; i--) {
            variableDeclarators.addAll(localVariablesDeclaredIn(wrappedNode.getStatement(i)));
        }
        return variableDeclarators;
    }

    private List<VariableDeclarator> localVariablesDeclaredIn(Statement statement) {
        if (statement instanceof ExpressionStmt) {
            ExpressionStmt expressionStmt = (ExpressionStmt) statement;
            if (expressionStmt.getExpression() instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr variableDeclarationExpr =
                        (VariableDeclarationExpr) expressionStmt.getExpression();
                List<VariableDeclarator> variableDeclarators = new LinkedList<>();
                variableDeclarators.addAll(variableDeclarationExpr.getVariables());
                return variableDeclarators;
            }
        }
        return Collections.emptyList();
    }

    /**
     * The following rule applies to a block statement S contained in a block that is not a switch block:
     * - A pattern variable introduced by S is definitely matched at all the block statements following S, if any,
     *   in the block.
     *
     * https://docs.oracle.com/javase/specs/jls/se22/html/jls-6.html#jls-6.3.2.1
     */
    @Override
    public List<TypePatternExpr> typePatternExprsExposedToChild(Node child) {
        int position = wrappedNode.getStatements().indexOf(child);
        if (position == -1) {
            throw new RuntimeException();
        }
        List<TypePatternExpr> patternExprs = new LinkedList<>();
        for (int i = position - 1; i >= 0; i--) {
            Context context = JavaParserFactory.getContext(wrappedNode.getStatement(i), typeSolver);
            if (!(context instanceof StatementContext)) {
                throw new IllegalStateException("Got non-statement context for statement");
            }
            List<TypePatternExpr> introducedPatterns = ((StatementContext<?>) context).getIntroducedTypePatterns();
            patternExprs.addAll(introducedPatterns);
        }
        return patternExprs;
    }
}
