/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU General Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package com.github.javaparser.ast.jml.proof;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Generated;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlProofCaseStmtMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A {@code case}/{@code default} branch inside a proof script, e.g.
 * {@code case "a": ...}, {@code default: ...} or {@code case: ...}.
 *
 * <p>{@link #getLabel()} is the (optional) case name; it is empty for the {@code default:}
 * and the label-less {@code case:} form. {@link #getCommands()} holds the statements of
 * the branch.
 *
 * <p>For example:
 * <pre>{@code
 *     //@ assert f() \by { case "a": expand on: "x"; default: auto; };
 * }</pre>
 */
public class JmlProofCaseStmt extends JmlProofStmt {

    @OptionalProperty
    private StringLiteralExpr label;

    private NodeList<JmlProofStmt> commands;

    @AllFieldsConstructor
    public JmlProofCaseStmt(StringLiteralExpr label, NodeList<JmlProofStmt> commands) {
        this(null, label, commands);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlProofCaseStmt(TokenRange tokenRange, StringLiteralExpr label, NodeList<JmlProofStmt> commands) {
        super(tokenRange);
        setLabel(label);
        setCommands(commands);
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
    public Optional<StringLiteralExpr> getLabel() {
        return Optional.ofNullable(label);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<JmlProofStmt> getCommands() {
        return commands;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCaseStmt setLabel(final @Nullable() StringLiteralExpr label) {
        if (label == this.label) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.LABEL, this.label, label);
        if (this.label != null) this.label.setParentNode(null);
        this.label = label;
        setAsParentNodeOf(label);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCaseStmt setCommands(final @NonNull() NodeList<JmlProofStmt> commands) {
        assertNotNull(commands);
        if (commands == this.commands) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.COMMANDS, this.commands, commands);
        if (this.commands != null) this.commands.setParentNode(null);
        this.commands = commands;
        setAsParentNodeOf(commands);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i) == node) {
                commands.set(i, (JmlProofStmt) replacementNode);
                return true;
            }
        }
        if (label != null) {
            if (node == label) {
                setLabel((StringLiteralExpr) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlProofCaseStmt clone() {
        return (JmlProofCaseStmt) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlProofCaseStmtMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlProofCaseStmtMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i) == node) {
                commands.remove(i);
                return true;
            }
        }
        if (label != null) {
            if (node == label) {
                removeLabel();
                return true;
            }
        }
        return super.remove(node);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCaseStmt removeLabel() {
        return setLabel((StringLiteralExpr) null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() StringLiteralExpr label() {
        return label;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofStmt> commands() {
        return Objects.requireNonNull(commands);
    }
}
