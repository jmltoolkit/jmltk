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
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlProofScriptMetaModel;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A proof script attached to a JML assert/assume statement via {@code \by}.
 * Holds the proof commands ({@code cmds}) and the proof command cases ({@code cases}).
 */
public class JmlProofScript extends Node {

    private NodeList<JmlProofCmd> cmds;

    private NodeList<JmlProofCmdCase> cases;

    @AllFieldsConstructor
    public JmlProofScript(NodeList<JmlProofCmd> cmds, NodeList<JmlProofCmdCase> cases) {
        this(null, cmds, cases);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlProofScript(TokenRange tokenRange, NodeList<JmlProofCmd> cmds, NodeList<JmlProofCmdCase> cases) {
        super(tokenRange);
        setCmds(cmds);
        setCases(cases);
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
    public NodeList<JmlProofCmd> getCmds() {
        return cmds;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofScript setCmds(final @NonNull() NodeList<JmlProofCmd> cmds) {
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

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<JmlProofCmdCase> getCases() {
        return cases;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofScript setCases(final @NonNull() NodeList<JmlProofCmdCase> cases) {
        assertNotNull(cases);
        if (cases == this.cases) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.CASES, this.cases, cases);
        if (this.cases != null) this.cases.setParentNode(null);
        this.cases = cases;
        setAsParentNodeOf(cases);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < cmds.size(); i++) {
            if (cmds.get(i) == node) {
                cmds.set(i, (JmlProofCmd) replacementNode);
                return true;
            }
        }
        for (int i = 0; i < cases.size(); i++) {
            if (cases.get(i) == node) {
                cases.set(i, (JmlProofCmdCase) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlProofScript clone() {
        return (JmlProofScript) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlProofScriptMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlProofScriptMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < cmds.size(); i++) {
            if (cmds.get(i) == node) {
                cmds.remove(i);
                return true;
            }
        }
        for (int i = 0; i < cases.size(); i++) {
            if (cases.get(i) == node) {
                cases.remove(i);
                return true;
            }
        }
        return super.remove(node);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofCmd> cmds() {
        return Objects.requireNonNull(cmds);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofCmdCase> cases() {
        return Objects.requireNonNull(cases);
    }
}
