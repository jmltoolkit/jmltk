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
import com.github.javaparser.metamodel.JmlProofCmdCaseMetaModel;
import com.github.javaparser.metamodel.OptionalProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A {@code case label: commands} or {@code default: commands} block inside a proof script.
 */
public class JmlProofCmdCase extends Node {

    @OptionalProperty
    private StringLiteralExpr label;

    private NodeList<JmlProofCmd> cmds;

    @AllFieldsConstructor
    public JmlProofCmdCase(StringLiteralExpr label, NodeList<JmlProofCmd> cmds) {
        this(null, label, cmds);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlProofCmdCase(TokenRange tokenRange, StringLiteralExpr label, NodeList<JmlProofCmd> cmds) {
        super(tokenRange);
        setLabel(label);
        setCmds(cmds);
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
    public NodeList<JmlProofCmd> getCmds() {
        return cmds;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmdCase setLabel(final @Nullable() StringLiteralExpr label) {
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
    public JmlProofCmdCase setCmds(final @NonNull() NodeList<JmlProofCmd> cmds) {
        assertNotNull(cmds);
        if (cmds == this.cmds) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.CMDS, this.cmds, cmds);
        if (this.cmds != null) this.cmds.setParentNode(null);
        this.cmds = cmds;
        setAsParentNodeOf(cmds);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        if (label != null) {
            if (node == label) {
                setLabel((StringLiteralExpr) replacementNode);
                return true;
            }
        }
        for (int i = 0; i < cmds.size(); i++) {
            if (cmds.get(i) == node) {
                cmds.set(i, (JmlProofCmd) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlProofCmdCase clone() {
        return (JmlProofCmdCase) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlProofCmdCaseMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlProofCmdCaseMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        if (label != null) {
            if (node == label) {
                removeLabel();
                return true;
            }
        }
        for (int i = 0; i < cmds.size(); i++) {
            if (cmds.get(i) == node) {
                cmds.remove(i);
                return true;
            }
        }
        return super.remove(node);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCmdCase removeLabel() {
        return setLabel((StringLiteralExpr) null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @Nullable() StringLiteralExpr label() {
        return label;
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofCmd> cmds() {
        return Objects.requireNonNull(cmds);
    }
}
