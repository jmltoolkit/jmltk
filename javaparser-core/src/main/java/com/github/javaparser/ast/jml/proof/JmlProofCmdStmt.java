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
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.JmlProofCmdStmtMetaModel;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * A named proof command inside a {@code \by} proof script, e.g. {@code oss;},
 * {@code expand on: "x";} or {@code instantiate var: "x" with: x;}.
 *
 * <p>{@link #getName()} is the command name, {@link #getArgs()} the optional (possibly
 * labeled) arguments, and {@link #getSuffix()} the statements of a nested {@code \by}
 * script that follows the command (empty if the command is terminated by {@code ;}).
 *
 * <p>The {@code obtain} command is not modeled here but in {@link JmlProofObtainStmt}.
 *
 * <p>For example:
 * <pre>{@code
 *     //@ assert f() \by { oss; expand on: "x"; instantiate var:"x" with: x; };
 * }</pre>
 */
public class JmlProofCmdStmt extends JmlProofStmt {

    private SimpleName name;

    private NodeList<JmlProofArg> args;

    private NodeList<JmlProofStmt> suffix;

    @AllFieldsConstructor
    public JmlProofCmdStmt(SimpleName name, NodeList<JmlProofArg> args, NodeList<JmlProofStmt> suffix) {
        this(null, name, args, suffix);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.javaparser.generator.core.node.MainConstructorGenerator")
    public JmlProofCmdStmt(
            TokenRange tokenRange, SimpleName name, NodeList<JmlProofArg> args, NodeList<JmlProofStmt> suffix) {
        super(tokenRange);
        setName(name);
        setArgs(args);
        setSuffix(suffix);
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
    public SimpleName getName() {
        return name;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<JmlProofArg> getArgs() {
        return args;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public NodeList<JmlProofStmt> getSuffix() {
        return suffix;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmdStmt setName(final @NonNull() SimpleName name) {
        assertNotNull(name);
        if (name == this.name) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.NAME, this.name, name);
        if (this.name != null) this.name.setParentNode(null);
        this.name = name;
        setAsParentNodeOf(name);
        return this;
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmdStmt setArgs(final @NonNull() NodeList<JmlProofArg> args) {
        assertNotNull(args);
        if (args == this.args) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.ARGS, this.args, args);
        if (this.args != null) this.args.setParentNode(null);
        this.args = args;
        setAsParentNodeOf(args);
        return this;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) == node) {
                args.set(i, (JmlProofArg) replacementNode);
                return true;
            }
        }
        if (node == name) {
            setName((SimpleName) replacementNode);
            return true;
        }
        for (int i = 0; i < suffix.size(); i++) {
            if (suffix.get(i) == node) {
                suffix.set(i, (JmlProofStmt) replacementNode);
                return true;
            }
        }
        return super.replace(node, replacementNode);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.CloneGenerator")
    public JmlProofCmdStmt clone() {
        return (JmlProofCmdStmt) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.GetMetaModelGenerator")
    public JmlProofCmdStmtMetaModel getMetaModel() {
        return JavaParserMetaModel.jmlProofCmdStmtMetaModel;
    }

    @Override
    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) == node) {
                args.remove(i);
                return true;
            }
        }
        for (int i = 0; i < suffix.size(); i++) {
            if (suffix.get(i) == node) {
                suffix.remove(i);
                return true;
            }
        }
        return super.remove(node);
    }

    @Generated("com.github.javaparser.generator.core.node.RemoveMethodGenerator")
    public JmlProofCmdStmt removeName() {
        return setName((SimpleName) null);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() SimpleName name() {
        return Objects.requireNonNull(name);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofArg> args() {
        return Objects.requireNonNull(args);
    }

    @com.github.javaparser.ast.key.IgnoreLexPrinting()
    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public @NonNull() NodeList<JmlProofStmt> suffix() {
        return Objects.requireNonNull(suffix);
    }

    @Generated("com.github.javaparser.generator.core.node.PropertyGenerator")
    public JmlProofCmdStmt setSuffix(final @NonNull() NodeList<JmlProofStmt> suffix) {
        assertNotNull(suffix);
        if (suffix == this.suffix) {
            return this;
        }
        notifyPropertyChange(ObservableProperty.SUFFIX, this.suffix, suffix);
        if (this.suffix != null) this.suffix.setParentNode(null);
        this.suffix = suffix;
        setAsParentNodeOf(suffix);
        return this;
    }
}
