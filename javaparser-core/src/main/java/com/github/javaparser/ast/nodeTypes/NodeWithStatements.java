/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.nodeTypes;

import static com.github.javaparser.StaticJavaParser.parseStatement;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

/**
 * A node that contains a list of statements.
 */
public interface NodeWithStatements<N extends Node> {

    NodeList<Statement> getStatements();

    default Statement getStatement(int i) {
        return getStatements().get(i);
    }

    @SuppressWarnings("unchecked")
    default N setStatement(int i, Statement statement) {
        getStatements().set(i, statement);
        return (N) this;
    }

    N setStatements(final NodeList<Statement> statements);

    @SuppressWarnings("unchecked")
    default N addStatement(Statement statement) {
        getStatements().add(statement);
        return (N) this;
    }

    @SuppressWarnings("unchecked")
    default N addStatement(int index, final Statement statement) {
        getStatements().add(index, statement);
        return (N) this;
    }

    default N addStatement(Expression expr) {
        return addStatement(new ExpressionStmt(expr));
    }

    /**
     * It will use {@link JavaParser#parseStatement(String)} inside, so it should end with a semicolon
     */
    default N addStatement(String statement) {
        return addStatement(parseStatement(statement));
    }

    default N addStatement(int index, final Expression expr) {
        Statement stmt = new ExpressionStmt(expr);
        return addStatement(index, stmt);
    }

    default <A extends Statement> A addAndGetStatement(A statement) {
        getStatements().add(statement);
        return statement;
    }

    default Statement addAndGetStatement(int index, final Statement statement) {
        getStatements().add(index, statement);
        return statement;
    }

    default ExpressionStmt addAndGetStatement(Expression expr) {
        ExpressionStmt statement = new ExpressionStmt(expr);
        return addAndGetStatement(statement);
    }

    default ExpressionStmt addAndGetStatement(String statement) {
        return addAndGetStatement(new NameExpr(statement));
    }

    /**
     * @return true if there are no statements contained in this node.
     */
    default boolean isEmpty() {
        return getStatements().isEmpty();
    }

    @SuppressWarnings("unchecked")
    default N copyStatements(NodeList<Statement> nodeList) {
        for (Statement n : nodeList) {
            addStatement(n.clone());
        }
        return (N) this;
    }

    default N copyStatements(NodeWithStatements<?> other) {
        return copyStatements(other.getStatements());
    }
}
