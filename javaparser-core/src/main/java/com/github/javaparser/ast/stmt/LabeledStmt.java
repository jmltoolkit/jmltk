/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.stmt;

import static com.github.javaparser.utils.Utils.assertNotNull;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.LabeledStmtMetaModel;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;

/**
 * A statement that is labeled, like {@code label123: println("continuing");}
 *
 * @author Julio Vilmar Gesser
 */
public class LabeledStmt extends Statement {

    private SimpleName label;

    private Statement statement;

    public LabeledStmt() {
        this(null, new SimpleName(), new ReturnStmt());
    }

    public LabeledStmt(final String label, final Statement statement) {
        this(null, new SimpleName(label), statement);
    }

    @AllFieldsConstructor
    public LabeledStmt(final SimpleName label, final Statement statement) {
        this(null, label, statement);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public LabeledStmt(TokenRange tokenRange, SimpleName label, Statement statement) {
        super(tokenRange);
        setLabel(label);
        setStatement(statement);
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
    public Statement getStatement() {
        return statement;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public LabeledStmt setStatement(final @NonNull() Statement statement) {
        assertNotNull(statement);
        if (statement == this.statement) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.STATEMENT, this.statement, statement);
        if (this.statement != null) this.statement.setParentNode(null);
        this.statement = statement;
        setAsParentNodeOf(statement);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public SimpleName getLabel() {
        return label;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public LabeledStmt setLabel(final @NonNull() SimpleName label) {
        assertNotNull(label);
        if (label == this.label) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.LABEL, this.label, label);
        if (this.label != null) this.label.setParentNode(null);
        this.label = label;
        setAsParentNodeOf(label);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public LabeledStmt clone() {
        return (LabeledStmt) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public LabeledStmtMetaModel getMetaModel() {
        return JavaParserMetaModel.labeledStmtMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        if (node == label) {
            setLabel((SimpleName) replacementNode);
            return true;
        }
        if (node == statement) {
            setStatement((Statement) replacementNode);
            return true;
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public boolean isLabeledStmt() {
        return true;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public LabeledStmt asLabeledStmt() {
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public void ifLabeledStmt(Consumer<LabeledStmt> action) {
        action.accept(this);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.TypeCastingGenerator")
    public Optional<LabeledStmt> toLabeledStmt() {
        return Optional.of(this);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() SimpleName label() {
        return Objects.requireNonNull(label);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() Statement statement() {
        return Objects.requireNonNull(statement);
    }
}
