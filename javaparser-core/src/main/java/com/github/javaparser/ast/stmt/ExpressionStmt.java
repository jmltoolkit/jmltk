/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.stmt;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithExpression;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.ExpressionStmtMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import org.jspecify.annotations.NonNull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * Used to wrap an expression so that it can take the place of a statement.
 *
 * @author Julio Vilmar Gesser
 */
public class ExpressionStmt extends Statement implements NodeWithExpression<ExpressionStmt> {

    private Expression expression;

    public ExpressionStmt() {
        this(null, new BooleanLiteralExpr());
    }

    @AllFieldsConstructor
    public ExpressionStmt(final Expression expression) {
        this(null, expression);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public ExpressionStmt(TokenRange tokenRange, Expression expression) {
        super(tokenRange);
        setExpression(expression);
        customInitialization();
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.AcceptGenerator")
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.AcceptGenerator")
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public Expression getExpression() {
        return expression;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public ExpressionStmt setExpression(final @NonNull() Expression expression) {
        assertNotNull(expression);
        if (expression == this.expression) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.EXPRESSION, this.expression, expression);
        if (this.expression != null)
            this.expression.setParentNode(null);
        this.expression = expression;
        setAsParentNodeOf(expression);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public ExpressionStmt clone() {
        return (ExpressionStmt) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public ExpressionStmtMetaModel getMetaModel() {
        return JavaParserMetaModel.expressionStmtMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        if (node == expression) {
            setExpression((Expression) replacementNode);
            return true;
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isExpressionStmt() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public ExpressionStmt asExpressionStmt() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifExpressionStmt(Consumer<ExpressionStmt> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<ExpressionStmt> toExpressionStmt() {
        return Optional.of(this);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() Expression expression() {
        return Objects.requireNonNull(expression);
    }
}
